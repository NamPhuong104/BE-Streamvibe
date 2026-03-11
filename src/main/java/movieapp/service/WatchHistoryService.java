package movieapp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import movieapp.dto.MetaAndHead.ResultPaginationDTO;
import movieapp.dto.OphimResponse.OphimMovieDetail;
import movieapp.dto.OphimResponse.OphimMovieDetailResponse;
import movieapp.dto.WatchHistory.WatchHistoryCreateReq;
import movieapp.dto.WatchHistory.WatchHistoryRes;
import movieapp.dto.WatchHistory.WatchHistorySummaryRes;
import movieapp.dto.WatchHistory.WatchHistoryUpdateReq;
import movieapp.entity.User;
import movieapp.entity.WatchHistory;
import movieapp.exception.CommonMessageException;
import movieapp.repository.UserRepository;
import movieapp.repository.WatchHistoryRepository;
import movieapp.util.SecurityUtil;
import movieapp.util.Util;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WatchHistoryService {
    private static final double COMPLETE_THRESHOLD = 90;

    private final WatchHistoryRepository watchHistoryRepository;
    private final OPhimClientService oPhimClientService;
    private final UserRepository userRepository;
    private final Util util;

    public ResultPaginationDTO handleGetAllWatchHistory(Specification<WatchHistory> spec, Pageable pageable) {
        Page<WatchHistory> pageHistory = watchHistoryRepository.findAll(spec, pageable);

        // Convert với ảnh đã có sẵn
        List<WatchHistoryRes> dtoList = pageHistory.getContent().stream()
                .map(history -> convertToRes(history))
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

    public ResultPaginationDTO handleGetWatchHistorySummary(Pageable pageable) {
        int page = pageable.getPageNumber();
        int size = pageable.getPageSize();
        int offset = page * size;

        List<WatchHistory> historyList = watchHistoryRepository.findLatestPeruserAndMovie(size, offset);

        long total = watchHistoryRepository.countUniqueUserMoviePairs();


        List<WatchHistorySummaryRes> dtoList = historyList.stream().map(history -> {
                    WatchHistorySummaryRes res = convertToSummaryRes(history);
                    int episodeCount = watchHistoryRepository.countEpisodesByUserAndMovie(history.getUser().getId(), history.getMovieSlug());
                    res.setEpisodeCount(episodeCount);

                    return res;
                }
        ).collect(Collectors.toList());

        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();

        mt.setPage(page + 1);
        mt.setPageSize(size);
        mt.setPages((int) Math.ceil((double) total / size));
        mt.setTotal(total);

        rs.setMeta(mt);
        rs.setResult(dtoList);

        return rs;
    }

    //    * ⭐ Lấy lịch sử xem - MỖI PHIM CHỈ HIỆN 1 LẦN (tập mới nhất)
    public ResultPaginationDTO handleGetWatchHistoryByMe(Pageable pageable) {
        String email = SecurityUtil.getCurrentUserLogin().orElseThrow(() -> new CommonMessageException("Bạn chưa đăng nhập"));
        User currentUser = userRepository.findByEmail(email).orElseThrow(() -> new CommonMessageException("Không tìm thấy user với email: " + email));

        int page = pageable.getPageNumber();
        int size = pageable.getPageSize();
        int offset = page * size;

        List<WatchHistory> historyList = watchHistoryRepository.findLatestEpisodePerMovieNative(currentUser.getId(), size, offset);

//        Count total movie unique
        long total = watchHistoryRepository.countDistinctMoviesByUserId(currentUser.getId());


        List<WatchHistoryRes> dtoList = historyList.stream().map(history -> convertToRes(history))
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
    public WatchHistoryRes getWatchProgress(String movieSlug, String episodeSlug) {
        String email = SecurityUtil.getCurrentUserLogin().orElseThrow(() -> new CommonMessageException("Bạn chưa đăng nhập"));
        User currentUser = userRepository.findByEmail(email).orElseThrow(() -> new CommonMessageException("Không tìm thấy user"));

        Optional<WatchHistory> historyOpt;

        if (episodeSlug == null || episodeSlug.trim().isEmpty()) {
            historyOpt = watchHistoryRepository.findLastedByUserAndMovie(currentUser.getId(), movieSlug);
        } else {
            String normalizedEpisodeSlug = util.normalizeEpisode(episodeSlug);
            historyOpt = watchHistoryRepository.findByUserAndMovieAndEpisode(currentUser.getId(), movieSlug, normalizedEpisodeSlug);
        }

        if (historyOpt.isEmpty()) return null;

        return convertToRes(historyOpt.get());
    }

    @Transactional
    public void handleDeleteWatchHistoryBySlug(String slug) {
        String email = SecurityUtil.getCurrentUserLogin().orElseThrow(() -> new CommonMessageException("Bạn chưa đăng nhập"));
        User currentUser = userRepository.findByEmail(email).orElseThrow(() -> new CommonMessageException("Không tìm thấy user"));

        watchHistoryRepository.deleteHistoryByUserIdAndMovieSlug(currentUser.getId(), slug);
    }


    public WatchHistoryRes handleCreateWatchHistory(WatchHistoryCreateReq dto) {
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new CommonMessageException("User không tồn tại với id: " + dto.getUserId()));

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

        String poster = util.buildFullUrl(dto.getPosterUrl());
        String thumb = util.buildFullUrl(dto.getThumbUrl());

        // Nếu thiếu poster hoặc thumb → fetch từ ophim
        if (poster == null || thumb == null) {
            try {
                OphimMovieDetailResponse detailResponse = oPhimClientService.getMovieDetail(dto.getMovieSlug());

                OphimMovieDetail movie = detailResponse.getData().getItem();
                if (poster == null) poster = util.buildFullUrl(movie.getPosterUrl());
                if (thumb == null) thumb = util.buildFullUrl(movie.getThumbUrl());
            } catch (Exception e) {
                log.warn("Không lấy được poster mới từ Ophim cho slug {}: {}", history.getMovieSlug(), e.getMessage());
            }
        }
        String posterUrl = poster;
        String thumbUrl = thumb;

        history.setPosterUrl(posterUrl);
        history.setThumbUrl(thumbUrl);
        watchHistoryRepository.save(history);

        return convertToRes(history);
    }

    public WatchHistoryRes handleUpdateWatchHistory(WatchHistoryUpdateReq dto) {
        WatchHistory history = watchHistoryRepository.findById(dto.getId())
                .orElseThrow(() -> new CommonMessageException("Lịch sử xem không tồn tại với id: " + dto.getId()));

        if (dto.getUserId() != null) {
            User user = userRepository.findById(dto.getUserId())
                    .orElseThrow(() -> new CommonMessageException("User không tồn tại với id: " + dto.getUserId()));
            history.setUser(user);
        }

        if (dto.getMovieSlug() != null) history.setMovieSlug(util.emptyToNull(dto.getMovieSlug()));
        if (dto.getMovieName() != null) history.setMovieName(util.emptyToNull(dto.getMovieName()));
        if (dto.getMovieType() != null) history.setMovieType(util.emptyToNull(dto.getMovieType()));
        if (dto.getOriginName() != null) history.setOriginName(util.emptyToNull(dto.getOriginName()));
        if (dto.getEpisodeSlug() != null) history.setEpisodeSlug(util.emptyToNull(dto.getEpisodeSlug()));
        if (dto.getEpisodeName() != null) history.setEpisodeName(util.emptyToNull(dto.getEpisodeName()));
        if (dto.getServerName() != null) history.setServerName(util.emptyToNull(dto.getServerName()));
        if (dto.getPosterUrl() != null) history.setPosterUrl(util.emptyToNull(dto.getPosterUrl()));
        if (dto.getThumbUrl() != null) history.setThumbUrl(util.emptyToNull(dto.getThumbUrl()));


        if (dto.getCurrentTime() != null && dto.getDuration() != null) {
            history.setCurrentTime(dto.getCurrentTime());
            history.setDuration(dto.getDuration());
            double progress = util.calculateProgress(dto.getCurrentTime(), dto.getDuration());
            history.setProgressPercent(util.roundToOneDecimal(progress));
            history.setCompleted(progress >= COMPLETE_THRESHOLD);
        }

        watchHistoryRepository.save(history);

        return convertToRes(history);
    }

    public void handleDeleteWatchHistory(Long id) {
        if (!watchHistoryRepository.existsById(id))
            throw new CommonMessageException("Watch History không tồn tại với id: " + id);
        watchHistoryRepository.deleteById(id);
    }

    @Transactional
    public void handleDeleteAllWatchHistoryByUserId(Long userId) {
//        if (!watchHistoryRepository.existsByUserId(userId))
//            throw new CommonMessageException("Watch History không tồn tại với userId: " + userId);
        watchHistoryRepository.deleteAllHistoryByUserId(userId);
    }

    /**
     * Convert WatchHistory -> WatchHistoryRes với ảnh từ imageMap
     */
    private WatchHistoryRes convertToRes(WatchHistory history) {
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

        res.setPosterUrl(history.getPosterUrl());
        res.setThumbUrl(history.getThumbUrl());

        res.setCurrentTimeFormatted(util.formatTime(history.getCurrentTime()));
        res.setDurationFormatted(util.formatTime(history.getDuration()));
        res.setLastWatchedAt(history.getLastWatchedAt());
        res.setCreatedAt(history.getCreatedAt());
        res.setUpdatedAt(history.getUpdatedAt());

        if (history.getUser() != null) {
            res.setUser(new WatchHistoryRes.ResUserDTO(
                    history.getUser().getId(),
                    history.getUser().getEmail(),
                    history.getUser().getUsername()
            ));
        }

        return res;
    }

    private WatchHistorySummaryRes convertToSummaryRes(WatchHistory history) {
        WatchHistorySummaryRes res = new WatchHistorySummaryRes();

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

        res.setPosterUrl(history.getPosterUrl());
        res.setThumbUrl(history.getThumbUrl());

        res.setCurrentTimeFormatted(util.formatTime(history.getCurrentTime()));
        res.setDurationFormatted(util.formatTime(history.getDuration()));
        res.setLastWatchedAt(history.getLastWatchedAt());
        res.setCreatedAt(history.getCreatedAt());
        res.setUpdatedAt(history.getUpdatedAt());

        if (history.getUser() != null) {
            res.setUser(new WatchHistoryRes.ResUserDTO(
                    history.getUser().getId(),
                    history.getUser().getEmail(),
                    history.getUser().getUsername()
            ));
        }

        return res;
    }
}