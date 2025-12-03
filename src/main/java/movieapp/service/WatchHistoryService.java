package movieapp.service;

import lombok.RequiredArgsConstructor;
import movieapp.domain.User;
import movieapp.domain.WatchHistory;
import movieapp.dto.MetaAndHead.ResultPaginationDTO;
import movieapp.dto.WatchHistory.WatchHistoryCreateReq;
import movieapp.dto.WatchHistory.WatchHistoryRes;
import movieapp.dto.WatchHistory.WatchHistoryUpdateReq;
import movieapp.repository.UserRepository;
import movieapp.repository.WatchHistoryRepository;
import movieapp.util.Util;
import movieapp.util.error.IdInvalidException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WatchHistoryService {
    private final WatchHistoryRepository watchHistoryRepository;
    private final UserRepository userRepository;
    private final Util util;

    private static final double COMPLETE_THRESHOLD = 95;

    public ResultPaginationDTO handleGetAllWatchHistory(Specification spec, Pageable pageable) {
        Page<WatchHistory> pageHistory = watchHistoryRepository.findAll(spec, pageable);
        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();

        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());

        mt.setPages(pageHistory.getTotalPages());
        mt.setTotal(pageHistory.getTotalElements());

        rs.setMeta(mt);
        List<WatchHistoryRes> dtoList = pageHistory.getContent().stream().map(this::convertToRes).toList();

        rs.setResult(dtoList);

        return rs;
    }

    public WatchHistoryRes handleCreateWatchHistory(WatchHistoryCreateReq dto) throws IdInvalidException {
        User user = userRepository.findById(dto.getUserId()).orElseThrow(() -> new IdInvalidException("User không tồn tại với id: " + dto.getUserId()));

//        Normalize episode slug
        String episodeSlug = util.normalizeEpisode(dto.getEpisodeSlug());

//        calculate progress
        double progressPercent = util.calculateProgress(dto.getCurrentTime(), dto.getDuration());
        boolean completed = progressPercent >= COMPLETE_THRESHOLD;

        WatchHistory history = watchHistoryRepository.findByUserAndMovieAndEpisode(dto.getUserId(), dto.getMovieSlug(), dto.getEpisodeSlug()).orElseGet(() -> {
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

        watchHistoryRepository.save(history);

        return convertToRes(history);
    }

    public WatchHistoryRes handleUpdateWatchHistory(WatchHistoryUpdateReq dto) throws IdInvalidException {
        WatchHistory history = watchHistoryRepository.findById(dto.getId()).orElseThrow(() -> new IdInvalidException("lịch sử xem không tồn tại với id: " + dto.getId()));
        User user = userRepository.findById(dto.getUserId()).orElseThrow(() -> new IdInvalidException("User không tồn tại với id: " + dto.getUserId()));

        if (dto.getUserId() != null) history.setUser(user);
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
        return convertToRes(history);
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

    public WatchHistoryRes convertToRes(WatchHistory history) {
        WatchHistoryRes res = new WatchHistoryRes();

        res.setId(history.getId());
        res.setMovieSlug(history.getMovieSlug());
        res.setMovieName(history.getMovieName());
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

        if (history.getUser() != null) {
            res.setUser(new WatchHistoryRes.ResUserDTO(history.getUser().getId(), history.getUser().getEmail()));
        }

        return res;
    }
}
