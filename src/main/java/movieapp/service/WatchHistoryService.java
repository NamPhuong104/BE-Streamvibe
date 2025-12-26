package movieapp.service;

import lombok.RequiredArgsConstructor;
import movieapp.domain.OptimizedImage;
import movieapp.domain.User;
import movieapp.domain.WatchHistory;
import movieapp.dto.MetaAndHead.ResultPaginationDTO;
import movieapp.dto.WatchHistory.WatchHistoryCreateReq;
import movieapp.dto.WatchHistory.WatchHistoryRes;
import movieapp.dto.WatchHistory.WatchHistoryUpdateReq;
import movieapp.repository.OptimizedImageRepository;
import movieapp.repository.UserRepository;
import movieapp.repository.WatchHistoryRepository;
import movieapp.util.SecurityUtil;
import movieapp.util.Util;
import movieapp.util.error.IdInvalidException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WatchHistoryService {
    private final WatchHistoryRepository watchHistoryRepository;
    private final OptimizedImageRepository optimizedImageRepository;
    private final UserRepository userRepository;
    private final Util util;

    private static final double COMPLETE_THRESHOLD = 95;
    private static final String IMAGE_TYPE_THUMB = "thumb";
    private static final String IMAGE_TYPE_POSTER = "poster";

    public ResultPaginationDTO handleGetAllWatchHistory(Specification<WatchHistory> spec, Pageable pageable) {
        Page<WatchHistory> pageHistory = watchHistoryRepository.findAll(spec, pageable);

        // ⭐ BATCH QUERY: Lấy tất cả slugs từ kết quả
        List<String> slugs = pageHistory.getContent().stream()
                .map(WatchHistory::getMovieSlug)
                .distinct()
                .collect(Collectors.toList());

        // ⭐ 1 QUERY duy nhất lấy tất cả ảnh
        Map<String, Map<String, OptimizedImage>> imageMap = buildImageMap(slugs);

        // Convert với ảnh đã có sẵn
        List<WatchHistoryRes> dtoList = pageHistory.getContent().stream()
                .map(history -> convertToRes(history, imageMap))
                .collect(Collectors.toList());

        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();

        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());
        mt.setPages(pageHistory.getTotalPages());
        mt.setTotal(pageHistory.getTotalElements());

        rs.setMeta(mt);
        rs.setResult(dtoList);

        return rs;
    }

    //    * ⭐ Lấy lịch sử xem - MỖI PHIM CHỈ HIỆN 1 LẦN (tập mới nhất)
    public ResultPaginationDTO handleGetWatchHistoryByMe(Pageable pageable) throws IdInvalidException {
        String email = SecurityUtil.getCurrentUserLogin().orElseThrow(() -> new IdInvalidException("Bạn chưa đăng nhập"));
        User currentUser = userRepository.findByEmail(email).orElseThrow(() -> new IdInvalidException("Không tìm thấy user với email: " + email));

        int page = pageable.getPageNumber();
        int size = pageable.getPageSize();
        int offset = page * size;

        List<WatchHistory> historyList = watchHistoryRepository.findLatestEpisodePerMovieNative(currentUser.getId(), size, offset);

//        Count total movie unique
        long total = watchHistoryRepository.countDistincMoviesByUserId(currentUser.getId());

        List<String> slugs = historyList.stream().map(WatchHistory::getMovieSlug).distinct().toList();

        Map<String, Map<String, OptimizedImage>> imageMap = buildImageMap(slugs);

        List<WatchHistoryRes> dtoList = historyList.stream().map(history -> convertToRes(history, imageMap))
                .collect(Collectors.toList());

        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();

        mt.setPage(page + 1);
        mt.setPageSize(size);
        mt.setPages((int) Math.ceil((double) total / size));
        mt.setTotal(total);

        rs.setResult(dtoList);
        rs.setMeta(mt);
        return rs;
    }

    /**
     * ⭐ Lấy watch progress
     * - Nếu có episodeSlug → trả về progress của tập đó
     * - Nếu KHÔNG có episodeSlug → trả về record MỚI NHẤT (tập đang xem dở)
     */
    public WatchHistoryRes getWatchProgress(String movieSlug, String episodeSlug) throws IdInvalidException {
        String email = SecurityUtil.getCurrentUserLogin().orElseThrow(() -> new IdInvalidException("Bạn chưa đăng nhập"));
        User currentUser = userRepository.findByEmail(email).orElseThrow(() -> new IdInvalidException("Không tìm thấy user"));

        Optional<WatchHistory> historyOpt;

        if (episodeSlug == null || episodeSlug.trim().isEmpty()) {
            historyOpt = watchHistoryRepository.findLastedByUserAndMovie(currentUser.getId(), movieSlug);
        } else {
            String normalizedEpisodeSlug = util.normalizeEpisode(episodeSlug);
            historyOpt = watchHistoryRepository.findByUserAndMovieAndEpisode(currentUser.getId(), movieSlug, normalizedEpisodeSlug);
        }

        if (historyOpt.isEmpty()) return null;

        return convertToRes(historyOpt.get(), Map.of());
    }

    @Transactional
    public void handleDeleteWatchHistoryBySlug(String slug) throws IdInvalidException {
        String email = SecurityUtil.getCurrentUserLogin().orElseThrow(() -> new IdInvalidException("Bạn chưa đăng nhập"));
        User currentUser = userRepository.findByEmail(email).orElseThrow(() -> new IdInvalidException("Không tìm thấy user"));

        watchHistoryRepository.deleteHistoryByUserIdAndMovieSlug(currentUser.getId(), slug);
    }

    /**
     * Build map: slug -> {imageType -> OptimizedImage}
     * Ví dụ: {"tiger-crane" -> {"thumb" -> OptimizedImage, "poster" -> OptimizedImage}}
     */

    private Map<String, Map<String, OptimizedImage>> buildImageMap(List<String> slugs) {
        if (slugs.isEmpty()) {
            return Map.of();
        }

        List<OptimizedImage> images = optimizedImageRepository.findBySlugIn(slugs);

        return images.stream()
                .collect(Collectors.groupingBy(
                        OptimizedImage::getSlug,
                        Collectors.toMap(
                                OptimizedImage::getImageType,
                                img -> img,
                                (existing, replacement) -> existing  // Giữ cái đầu tiên nếu duplicate
                        )
                ));
    }

    public WatchHistoryRes handleCreateWatchHistory(WatchHistoryCreateReq dto) throws IdInvalidException {
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new IdInvalidException("User không tồn tại với id: " + dto.getUserId()));

        String episodeSlug = util.normalizeEpisode(dto.getEpisodeSlug());
        double progressPercent = util.calculateProgress(dto.getCurrentTime(), dto.getDuration());
        boolean completed = progressPercent >= COMPLETE_THRESHOLD;

        WatchHistory history = watchHistoryRepository
                .findByUserAndMovieAndEpisode(dto.getUserId(), dto.getMovieSlug(), dto.getEpisodeSlug())
                .orElseGet(() -> {
                    WatchHistory h = new WatchHistory();
                    h.setUser(user);
                    h.setMovieSlug(dto.getMovieSlug());
                    h.setEpisodeSlug(episodeSlug);
                    return h;
                });

        history.setMovieName(dto.getMovieName());
        history.setMovieType(dto.getMovieType());
        history.setEpisodeName(dto.getEpisodeName());
        history.setServerName(dto.getServerName());
        history.setCurrentTime(dto.getCurrentTime());
        history.setDuration(dto.getDuration());
        history.setProgressPercent(util.roundToOneDecimal(progressPercent));
        history.setCompleted(completed);
        history.setOriginName(dto.getOriginName());

        watchHistoryRepository.save(history);

        // Lấy ảnh cho single record
        Map<String, Map<String, OptimizedImage>> imageMap =
                buildImageMap(List.of(history.getMovieSlug()));

        return convertToRes(history, imageMap);
    }

    public WatchHistoryRes handleUpdateWatchHistory(WatchHistoryUpdateReq dto) throws IdInvalidException {
        WatchHistory history = watchHistoryRepository.findById(dto.getId())
                .orElseThrow(() -> new IdInvalidException("Lịch sử xem không tồn tại với id: " + dto.getId()));

        if (dto.getUserId() != null) {
            User user = userRepository.findById(dto.getUserId())
                    .orElseThrow(() -> new IdInvalidException("User không tồn tại với id: " + dto.getUserId()));
            history.setUser(user);
        }

        if (dto.getMovieSlug() != null) history.setMovieSlug(util.emptyToNull(dto.getMovieSlug()));
        if (dto.getMovieName() != null) history.setMovieName(util.emptyToNull(dto.getMovieName()));
        if (dto.getMovieType() != null) history.setMovieType(util.emptyToNull(dto.getMovieType()));
        if (dto.getEpisodeSlug() != null) history.setEpisodeSlug(util.emptyToNull(dto.getEpisodeSlug()));
        if (dto.getEpisodeName() != null) history.setEpisodeName(util.emptyToNull(dto.getEpisodeName()));
        if (dto.getServerName() != null) history.setServerName(util.emptyToNull(dto.getServerName()));


        if (dto.getCurrentTime() != null && dto.getDuration() != null) {
            history.setCurrentTime(dto.getCurrentTime());
            history.setDuration(dto.getDuration());
            double progress = util.calculateProgress(dto.getCurrentTime(), dto.getDuration());
            history.setProgressPercent(util.roundToOneDecimal(progress));
            history.setCompleted(progress >= COMPLETE_THRESHOLD);
        }

        watchHistoryRepository.save(history);

        Map<String, Map<String, OptimizedImage>> imageMap =
                buildImageMap(List.of(history.getMovieSlug()));

        return convertToRes(history, imageMap);
    }

    public void handleDeleteWatchHistory(Long id) throws IdInvalidException {
        if (!watchHistoryRepository.existsById(id))
            throw new IdInvalidException("Watch History không tồn tại với id: " + id);
        watchHistoryRepository.deleteById(id);
    }

    @Transactional
    public void handleDeleteAllWatchHistoryByUserId(Long userId) throws IdInvalidException {
        if (!watchHistoryRepository.existsByUserId(userId))
            throw new IdInvalidException("Watch History không tồn tại với userId: " + userId);
        watchHistoryRepository.deleteAllHistoryByUserId(userId);
    }

    /**
     * Convert WatchHistory -> WatchHistoryRes với ảnh từ imageMap
     */
    private WatchHistoryRes convertToRes(WatchHistory history,
                                         Map<String, Map<String, OptimizedImage>> imageMap) {
        WatchHistoryRes res = new WatchHistoryRes();

        res.setId(history.getId());
        res.setMovieSlug(history.getMovieSlug());
        res.setMovieName(history.getMovieName());
        res.setOriginName(history.getOriginName());
        res.setMovieType(history.getMovieType());

        res.setEpisodeSlug(history.getEpisodeSlug());
        res.setEpisodeName(history.getEpisodeName());
        res.setServerName(history.getServerName());

        res.setCurrentTime(history.getCurrentTime());
        res.setDuration(history.getDuration());
        res.setProgressPercent(history.getProgressPercent());
        res.setCompleted(history.getCompleted());

        res.setCurrentTimeFormatted(util.formatTime(history.getCurrentTime()));
        res.setDurationFormatted(util.formatTime(history.getDuration()));
        res.setLastWatchedAt(history.getLastWatchedAt());
        res.setCreatedAt(history.getCreatedAt());
        res.setUpdatedAt(history.getUpdatedAt());

        // ⭐ Set ảnh từ imageMap (đã query batch sẵn)
        setImagesFromMap(res, history.getMovieSlug(), imageMap);

        if (history.getUser() != null) {
            res.setUser(new WatchHistoryRes.ResUserDTO(
                    history.getUser().getId(),
                    history.getUser().getEmail()
            ));
        }

        return res;
    }

    /**
     * Set ảnh cho response từ imageMap
     */
    private void setImagesFromMap(WatchHistoryRes res, String slug,
                                  Map<String, Map<String, OptimizedImage>> imageMap) {
        Map<String, OptimizedImage> slugImages = imageMap.get(slug);

        if (slugImages == null) {
            // Không có ảnh optimize -> để null
            return;
        }

        OptimizedImage thumbImage = slugImages.get(IMAGE_TYPE_THUMB);
        OptimizedImage posterImage = slugImages.get(IMAGE_TYPE_POSTER);

        if (thumbImage != null) {
            res.setThumbUrl(thumbImage.getOriginalUrl());
            res.setOptimizedThumb(thumbImage.getCloudinaryUrl());
        }

        if (posterImage != null) {
            res.setPosterUrl(posterImage.getOriginalUrl());
            res.setOptimizedPoster(posterImage.getCloudinaryUrl());
        }
    }
}