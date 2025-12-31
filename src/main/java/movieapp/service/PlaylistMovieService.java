package movieapp.service;

import lombok.RequiredArgsConstructor;
import movieapp.domain.OptimizedImage;
import movieapp.domain.Playlist;
import movieapp.domain.PlaylistMovie;
import movieapp.domain.User;
import movieapp.dto.MetaAndHead.ResultPaginationDTO;
import movieapp.dto.PlaylistMovie.PlaylistMovieCreateDTO;
import movieapp.dto.PlaylistMovie.PlaylistMovieRes;
import movieapp.repository.OptimizedImageRepository;
import movieapp.repository.PlaylistMovieRepository;
import movieapp.repository.PlaylistRepository;
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
public class PlaylistMovieService {
    private final PlaylistMovieRepository playlistMovieRepository;
    private final PlaylistRepository playlistRepository;
    private final OptimizedImageRepository optimizedImageRepository;
    private final UserService userService;

    private static final String IMAGE_TYPE_THUMB = "thumb";
    private static final String IMAGE_TYPE_POSTER = "poster";

    public ResultPaginationDTO handleGetMovieInMyPlaylist(Long playlistId, Pageable pageable) throws IdInvalidException {
        User currentUser = userService.getCurrentUser();

        Page<PlaylistMovie> pagePm = playlistMovieRepository.findLastedByPlaylistIdAndUserId(currentUser.getId(), playlistId, pageable);

        List<String> slugs = pagePm.getContent().stream().map(PlaylistMovie::getMovieSlug).distinct().collect(Collectors.toList());
        Map<String, Map<String, OptimizedImage>> imageMap = buildImageMap(slugs);

        List<PlaylistMovieRes> dtoList = pagePm.getContent().stream().map(playlistMovie -> convertToPlaylistMovieRes(playlistMovie, imageMap)).collect(Collectors.toList());

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

    public ResultPaginationDTO handleGetPlaylistByMe(Pageable pageable) throws IdInvalidException {
        User currentUser = userService.getCurrentUser();

        Page<PlaylistMovie> pagePm = playlistMovieRepository.findLastedByUser(currentUser.getId(), pageable);

        List<String> slugs = pagePm.getContent().stream().map(PlaylistMovie::getMovieSlug).distinct().collect(Collectors.toList());
        Map<String, Map<String, OptimizedImage>> imageMap = buildImageMap(slugs);

        List<PlaylistMovieRes> dtoList = pagePm.getContent().stream().map(playlistMovie -> convertToPlaylistMovieRes(playlistMovie, imageMap)).collect(Collectors.toList());

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

        List<String> slugs = pagePm.getContent().stream().map(PlaylistMovie::getMovieSlug).distinct().collect(Collectors.toList());
        Map<String, Map<String, OptimizedImage>> imageMap = buildImageMap(slugs);

        List<PlaylistMovieRes> dtoList = pagePm.getContent().stream().map(playlistMovie -> convertToPlaylistMovieRes(playlistMovie, imageMap)).collect(Collectors.toList());

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

    @Transactional
    public PlaylistMovieRes handleCreateMovieByMe(PlaylistMovieCreateDTO data) throws IdInvalidException {
        User currentUser = userService.getCurrentUser();

        Playlist newPlaylist = playlistRepository.findById(data.getPlaylistId())
                .orElseThrow(() -> new IdInvalidException("Playlist không tồn tại với id: " + data.getPlaylistId()));

        // Check playlist có thuộc về user không
        if (!newPlaylist.getUser().getId().equals(currentUser.getId())) {
            throw new IdInvalidException("Bạn không có quyền thêm vào playlist này");
        }

        // ⭐ Check phim đã có trong playlist nào chưa
        Optional<PlaylistMovie> existingMovie = playlistMovieRepository
                .findByUserIdAndMovieSlug(currentUser.getId(), data.getMovieSlug());

        if (existingMovie.isPresent()) {
            PlaylistMovie existing = existingMovie.get();
            Playlist oldPlaylist = existing.getPlaylist();

            // Nếu đã có trong cùng playlist → không làm gì
            if (oldPlaylist.getId() == newPlaylist.getId()) {
                throw new IdInvalidException("Phim đã có trong playlist này");
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

        Map<String, Map<String, OptimizedImage>> imageMap = buildImageMap(List.of(data.getMovieSlug()));

        return convertToPlaylistMovieRes(pm, imageMap);
    }

    public Long handleCheckMovieInMyPlaylist(String movieSlug) throws IdInvalidException {
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


//    public PlaylistMovieRes handleCreatePlaylistMovie(PlaylistMovieCreateDTO data) throws IdInvalidException {
//        User currentUser = userService.getCurrentUser();
//
//        Playlist newPlaylist = playlistRepository.findById(data.getPlaylistId())
//                .orElseThrow(() -> new IdInvalidException("Playlist không tồn tại với id: " + data.getPlaylistId()));
//
//        // Check playlist có thuộc về user không
//        if (!newPlaylist.getUser().getId().equals(currentUser.getId())) {
//            throw new IdInvalidException("Bạn không có quyền thêm vào playlist này");
//        }
//
//        // ⭐ Check phim đã có trong playlist nào chưa
//        Optional<PlaylistMovie> existingMovie = playlistMovieRepository
//                .findByUserIdAndMovieSlug(currentUser.getId(), data.getMovieSlug());
//
//        if (existingMovie.isPresent()) {
//            PlaylistMovie existing = existingMovie.get();
//            Playlist oldPlaylist = existing.getPlaylist();
//
//            // Nếu đã có trong cùng playlist → không làm gì
//            if (oldPlaylist.getId() == newPlaylist.getId()) {
//                throw new IdInvalidException("Phim đã có trong playlist này");
//            }
//
//            // ⭐ Xóa khỏi playlist cũ
//            playlistMovieRepository.delete(existing);
//            oldPlaylist.setMovieCount(Math.max(0, oldPlaylist.getMovieCount() - 1));
//            playlistRepository.save(oldPlaylist);
//        }
//
//        // Thêm vào playlist mới
//        PlaylistMovie pm = buildPlaylistMovie(newPlaylist, data);
//        playlistMovieRepository.save(pm);
//
//        newPlaylist.setMovieCount(newPlaylist.getMovieCount() + 1);
//        playlistRepository.save(newPlaylist);
//
//        Map<String, Map<String, OptimizedImage>> imageMap = buildImageMap(List.of(data.getMovieSlug()));
//
//        return convertToPlaylistMovieRes(pm, imageMap);
//    }

    public void handleDeletePlaylistMovie(Long id) {
        Optional<PlaylistMovie> pm = playlistMovieRepository.findById(id);

        if (pm != null) playlistMovieRepository.deleteById(pm.get().getId());
    }


    //    HELPER METHODS
    private Map<String, Map<String, OptimizedImage>> buildImageMap(List<String> slugs) {
        if (slugs.isEmpty()) return Map.of();

        List<OptimizedImage> images = optimizedImageRepository.findBySlugIn(slugs);

        return images.stream().collect(Collectors.groupingBy(OptimizedImage::getSlug, Collectors.toMap(OptimizedImage::getImageType, img -> img, (existing, replacement) -> existing)));
    }

    private PlaylistMovie buildPlaylistMovie(Playlist playlist, PlaylistMovieCreateDTO dto) {
        return PlaylistMovie.builder()
                .playlist(playlist)
                .movieSlug(dto.getMovieSlug())
                .movieName(dto.getMovieName())
                .originName(dto.getOriginName())
                .posterUrl(dto.getPosterUrl())
                .thumbUrl(dto.getThumbUrl())
                .quality(dto.getQuality())
                .lang(dto.getLang())
                .episodeCurrent(dto.getEpisodeCurrent())
                .build();
    }

    private PlaylistMovieRes convertToPlaylistMovieRes(PlaylistMovie
                                                               pm, Map<String, Map<String, OptimizedImage>> imageMap) {
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

        Map<String, OptimizedImage> slugImages = imageMap.get(pm.getMovieSlug());

        if (slugImages != null) {
            OptimizedImage thumbImage = slugImages.get(IMAGE_TYPE_THUMB);
            OptimizedImage posterImage = slugImages.get(IMAGE_TYPE_POSTER);

            if (thumbImage != null) res.setOptimizedThumb(thumbImage.getCloudinaryUrl());
            if (posterImage != null) res.setOptimizedPoster(posterImage.getCloudinaryUrl());
        }

        if (pm.getPlaylist() != null) {
            res.setPlaylist(new PlaylistMovieRes.PlaylistInfo(pm.getPlaylist().getId(), pm.getPlaylist().getName()));
        }

        return res;
    }
}
