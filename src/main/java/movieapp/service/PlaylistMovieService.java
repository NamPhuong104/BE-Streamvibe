package movieapp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import movieapp.dto.MetaAndHead.ResultPaginationDTO;
import movieapp.dto.OphimResponse.OphimMovieDetail;
import movieapp.dto.OphimResponse.OphimMovieDetailResponse;
import movieapp.dto.PlaylistMovie.PlaylistMovieCreateDTO;
import movieapp.dto.PlaylistMovie.PlaylistMovieRes;
import movieapp.entity.Playlist;
import movieapp.entity.PlaylistMovie;
import movieapp.entity.User;
import movieapp.exception.CommonMessageException;
import movieapp.repository.PlaylistMovieRepository;
import movieapp.repository.PlaylistRepository;
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
public class PlaylistMovieService {
    private final PlaylistMovieRepository playlistMovieRepository;
    private final PlaylistRepository playlistRepository;
    private final OPhimClientService oPhimClientService;
    private final UserService userService;
    private final Util util;

    public ResultPaginationDTO handleGetMovieInMyPlaylist(Long playlistId, Pageable pageable) {
        User currentUser = userService.getCurrentUser();

        Page<PlaylistMovie> pagePm = playlistMovieRepository.findLastedByPlaylistIdAndUserId(currentUser.getId(), playlistId, pageable);

        List<PlaylistMovieRes> dtoList = pagePm.getContent().stream().map(playlistMovie -> convertToPlaylistMovieRes(playlistMovie)).collect(Collectors.toList());

        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();

        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());

        mt.setPages(pagePm.getTotalPages());
        mt.setTotal(pagePm.getTotalElements());

        rs.setMeta(mt);
        rs.setResult(dtoList);

        return rs;
    }

    public ResultPaginationDTO handleGetPlaylistByMe(Pageable pageable) {
        User currentUser = userService.getCurrentUser();

        Page<PlaylistMovie> pagePm = playlistMovieRepository.findLastedByUser(currentUser.getId(), pageable);

        List<PlaylistMovieRes> dtoList = pagePm.getContent().stream().map(playlistMovie -> convertToPlaylistMovieRes(playlistMovie)).collect(Collectors.toList());

        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();

        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());

        mt.setPages(pagePm.getTotalPages());
        mt.setTotal(pagePm.getTotalElements());

        rs.setResult(dtoList);
        rs.setMeta(mt);

        return rs;
    }

    public ResultPaginationDTO handleGetAllPlaylistMovie(Specification spec, Pageable pageable) {
        Page<PlaylistMovie> pagePm = playlistMovieRepository.findAll(spec, pageable);

        List<PlaylistMovieRes> dtoList = pagePm.getContent().stream().map(playlistMovie -> convertToPlaylistMovieRes(playlistMovie)).collect(Collectors.toList());

        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();

        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());

        mt.setPages(pagePm.getTotalPages());
        mt.setTotal(pagePm.getTotalElements());

        rs.setResult(dtoList);
        rs.setMeta(mt);

        return rs;
    }

    public ResultPaginationDTO handleGetMoviesByPlaylistId(Long playlistId, Pageable pageable) {
        if (!playlistRepository.existsById(playlistId))
            throw new CommonMessageException("Playlist không tồn tại với id: " + playlistId);

        Page<PlaylistMovie> pagePm = playlistMovieRepository.findByPlaylistIdOrderByAddedAtDesc(playlistId, pageable);

        List<PlaylistMovieRes> dtoList = pagePm.getContent().stream().map(pm -> convertToPlaylistMovieRes(pm)).collect(Collectors.toList());

        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();

        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());
        mt.setPages(pagePm.getTotalPages());
        mt.setTotal(pagePm.getTotalElements());

        rs.setMeta(mt);
        rs.setResult(dtoList);

        return rs;
    }

    @Transactional
    public PlaylistMovieRes handleCreateMovieByMe(PlaylistMovieCreateDTO data) {
        User currentUser = userService.getCurrentUser();

        Playlist newPlaylist = playlistRepository.findById(data.getPlaylistId())
                .orElseThrow(() -> new CommonMessageException("Playlist không tồn tại với id: " + data.getPlaylistId()));

        // Check playlist có thuộc về user không
        if (!newPlaylist.getUser().getId().equals(currentUser.getId())) {
            throw new CommonMessageException("Bạn không có quyền thêm vào playlist này");
        }

        // ⭐ Check phim đã có trong playlist nào chưa
        Optional<PlaylistMovie> existingMovie = playlistMovieRepository
                .findByUserIdAndMovieSlug(currentUser.getId(), data.getMovieSlug());

        if (existingMovie.isPresent()) {
            PlaylistMovie existing = existingMovie.get();
            Playlist oldPlaylist = existing.getPlaylist();

            // Nếu đã có trong cùng playlist → không làm gì
            if (oldPlaylist.getId() == newPlaylist.getId()) {
                throw new CommonMessageException("Phim đã có trong playlist này");
            }

            // ⭐ Xóa khỏi playlist cũ
            playlistMovieRepository.delete(existing);
            oldPlaylist.setMovieCount(Math.max(0, oldPlaylist.getMovieCount() - 1));
            playlistRepository.save(oldPlaylist);
        }

        // Thêm vào playlist mới
        PlaylistMovie pm = buildPlaylistMovie(newPlaylist, data);
        playlistMovieRepository.save(pm);

        newPlaylist.setMovieCount(newPlaylist.getMovieCount() + 1);
        playlistRepository.save(newPlaylist);

        return convertToPlaylistMovieRes(pm);
    }

    public Long handleCheckMovieInMyPlaylist(String movieSlug) {
        User currentUser = userService.getCurrentUser();
        return playlistMovieRepository.findPlaylistIdByUserIdAndMovieSlug(currentUser.getId(), movieSlug);
    }

    @Transactional
    public void handleDeleteMovieInMyPlaylist(Long playlistId, String movieSlug) {

        if (playlistId != null && movieSlug != null) {
            Optional<Playlist> currentPlaylist = playlistRepository.findById(playlistId);

            if (currentPlaylist.isPresent()) {
                Playlist existing = currentPlaylist.get();
                existing.setMovieCount(Math.max(0, existing.getMovieCount() - 1));

                playlistRepository.save(existing);
                playlistMovieRepository.deleteMovieByPlaylistIdAndMovieSlug(playlistId, movieSlug);
            }
        }
    }


    public PlaylistMovieRes handleCreatePlaylistMovie(PlaylistMovieCreateDTO data) {
        User currentUser = userService.getCurrentUser();

        Playlist newPlaylist = playlistRepository.findById(data.getPlaylistId())
                .orElseThrow(() -> new CommonMessageException("Playlist không tồn tại với id: " + data.getPlaylistId()));

        // ⭐ Check phim đã có trong playlist nào chưa
        Optional<PlaylistMovie> existingMovie = playlistMovieRepository
                .findByUserIdAndMovieSlug(currentUser.getId(), data.getMovieSlug());

        if (existingMovie.isPresent()) {
            PlaylistMovie existing = existingMovie.get();
            Playlist oldPlaylist = existing.getPlaylist();

            // Nếu đã có trong cùng playlist → không làm gì
            if (oldPlaylist.getId() == newPlaylist.getId()) {
                throw new CommonMessageException("Phim đã có trong playlist này");
            }

            // ⭐ Xóa khỏi playlist cũ
            playlistMovieRepository.delete(existing);
            oldPlaylist.setMovieCount(Math.max(0, oldPlaylist.getMovieCount() - 1));
            playlistRepository.save(oldPlaylist);
        }

        // Thêm vào playlist mới
        PlaylistMovie pm = buildPlaylistMovie(newPlaylist, data);
        playlistMovieRepository.save(pm);

        newPlaylist.setMovieCount(newPlaylist.getMovieCount() + 1);
        playlistRepository.save(newPlaylist);

        return convertToPlaylistMovieRes(pm);
    }

    public void handleDeletePlaylistMovie(Long id) {
        Optional<PlaylistMovie> pm = playlistMovieRepository.findById(id);

        if (pm != null) playlistMovieRepository.deleteById(pm.get().getId());
    }


    private PlaylistMovie buildPlaylistMovie(Playlist playlist, PlaylistMovieCreateDTO dto) {
        String poster = util.buildFullUrl(dto.getPosterUrl());
        String thumb = util.buildFullUrl(dto.getThumbUrl());

        if (poster == null || thumb == null) {
            try {
                OphimMovieDetailResponse detailResponse = oPhimClientService.getMovieDetail(dto.getMovieSlug());

                OphimMovieDetail movie = detailResponse.getData().getItem();
                if (poster == null) poster = util.buildFullUrl(movie.getPosterUrl());
                if (thumb == null) thumb = util.buildFullUrl(movie.getThumbUrl());
            } catch (Exception e) {
                log.warn("Không lấy được poster mới từ Ophim cho slug {}: {}", dto.getMovieSlug(), e.getMessage());
            }
        }
        String posterUrl = poster;
        String thumbUrl = thumb;

        return PlaylistMovie.builder()
                .playlist(playlist)
                .movieSlug(dto.getMovieSlug())
                .movieName(dto.getMovieName())
                .originName(dto.getOriginName())
                .posterUrl(posterUrl)
                .thumbUrl(thumbUrl)
                .quality(dto.getQuality())
                .lang(dto.getLang())
                .episodeCurrent(dto.getEpisodeCurrent())
                .build();
    }

    private PlaylistMovieRes convertToPlaylistMovieRes(PlaylistMovie
                                                               pm) {
        PlaylistMovieRes res = PlaylistMovieRes.builder()
                .id(pm.getId())
                .movieSlug(pm.getMovieSlug())
                .movieName(pm.getMovieName())
                .originName(pm.getOriginName())
                .quality(pm.getQuality())
                .episodeCurrent(pm.getEpisodeCurrent())
                .addedAt(pm.getAddedAt())
                .createdAt(pm.getCreatedAt())
                .thumbUrl(pm.getThumbUrl())
                .posterUrl(pm.getPosterUrl())
                .lang(pm.getLang())
                .build();
        if (pm.getPlaylist() != null) {
            res.setPlaylist(new PlaylistMovieRes.PlaylistInfo(pm.getPlaylist().getId(), pm.getPlaylist().getName()));
        }

        return res;
    }
}
