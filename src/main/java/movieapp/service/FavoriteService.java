package movieapp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import movieapp.client.OphimClient;
import movieapp.domain.Favorite;
import movieapp.domain.OptimizedImage;
import movieapp.domain.User;
import movieapp.dto.Favorites.FavoriteCreateReq;
import movieapp.dto.Favorites.FavoriteRes;
import movieapp.dto.Favorites.FavoriteUpdateReq;
import movieapp.dto.MetaAndHead.ResultPaginationDTO;
import movieapp.dto.OphimResponse.OphimMovieDetail;
import movieapp.dto.OphimResponse.OphimMovieDetailResponse;
import movieapp.repository.FavoriteRepository;
import movieapp.repository.OptimizedImageRepository;
import movieapp.repository.UserRepository;
import movieapp.util.SecurityUtil;
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
@Slf4j
public class FavoriteService {
    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final OphimClient ophimClient;
    private final OptimizedImageRepository optimizedImageRepository;
    private final ImageOptimizationService imageOptimizationService;
    private static final String IMAGE_TYPE_THUMB = "thumb";
    private static final String IMAGE_TYPE_POSTER = "poster";

    public User getUserById(Long id) throws IdInvalidException {
        return userRepository.findById(id).orElseThrow(() -> new IdInvalidException("User không tồn tại với id: " + id));
    }

    public boolean handleCheckIsFavorite(String movieSlug) {
        return SecurityUtil.getCurrentUserLogin()
                .flatMap(email -> userRepository.findByEmail(email))
                .map(user -> favoriteRepository.existsByUserAndMovieSlug(user, movieSlug))
                .orElse(false);
    }

    public ResultPaginationDTO handleGetAllFavorite(Specification<Favorite> spec, Pageable pageable) {
        Page<Favorite> pageFav = favoriteRepository.findAll(spec, pageable);
        List<String> slugs = pageFav.getContent().stream()
                .map(Favorite::getMovieSlug).distinct().toList();
        Map<String, Map<String, OptimizedImage>> imageMap = buildImageMap(slugs);
        List<FavoriteRes> dtoList = pageFav.getContent().stream()
                .map(favorite -> convertFavoriteRes(favorite, imageMap))
                .collect(Collectors.toList());

        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();

        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());

        mt.setPages(pageFav.getTotalPages());
        mt.setTotal(pageFav.getTotalElements());

        rs.setMeta(mt);
        rs.setResult(dtoList);

        return rs;
    }

    public ResultPaginationDTO handleGetFavoriteByMe(Pageable pageable) throws IdInvalidException {
        String email = SecurityUtil.getCurrentUserLogin().orElseThrow(() -> new IdInvalidException("Bạn chưa đăng nhập"));
        User curentUser = userRepository.findByEmail(email).orElseThrow(() -> new IdInvalidException("Không tìm thấy user với email: " + email));

        Page<Favorite> pageFavorite = favoriteRepository.findByUserIdOrderByCreatedAtDesc(curentUser.getId(), pageable);

        List<String> slugs = pageFavorite.getContent().stream().map(Favorite::getMovieSlug).distinct().toList();
        Map<String, Map<String, OptimizedImage>> imageMap = buildImageMap(slugs);

        List<FavoriteRes> dtoList = pageFavorite.getContent().stream().map(history -> convertFavoriteRes(history, imageMap)).toList();

        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();

        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());
        mt.setPages(pageFavorite.getTotalPages());
        mt.setTotal(pageFavorite.getTotalElements());

        rs.setMeta(mt);
        rs.setResult(dtoList);

        return rs;
    }

    @Transactional
    public void handleDeleteByMovieSlug(String movieSlug) throws IdInvalidException {
        String email = SecurityUtil.getCurrentUserLogin().orElseThrow(() -> new IdInvalidException("Bạn chưa đăng nhập"));
        User curentUser = userRepository.findByEmail(email).orElseThrow(() -> new IdInvalidException("Không tìm thấy user với email: " + email));
        if (!favoriteRepository.existsByUserAndMovieSlug(curentUser, movieSlug))
            throw new IdInvalidException("Phim không có trong danh sách yêu thích");

        favoriteRepository.deleteByUserAndMovieSlug(curentUser, movieSlug);
    }

    public FavoriteRes handleGetFavoriteById(long id) throws IdInvalidException {
        Favorite res = favoriteRepository.findById(id).orElseThrow(() -> new IdInvalidException("Favorite ko tồn tại với id " + id));
        Map<String, Map<String, OptimizedImage>> imageMap = buildImageMap(List.of(res.getMovieSlug()));
        return convertFavoriteRes(res, imageMap);
    }

    public FavoriteRes handleCreateFavorite(FavoriteCreateReq dto) throws IdInvalidException {
        User user = getUserById(dto.getUserId());
        if (favoriteRepository.existsByUserAndMovieSlug(user, dto.getMovieSlug()))
            throw new IdInvalidException("Phim đã có trong danh sách");

        Favorite fav = new Favorite();
        fav.setUser(user);
        fav.setMovieSlug(dto.getMovieSlug());
        fav.setMovieName(dto.getMovieName());
        fav.setOriginName(dto.getOriginName());
        fav.setThumbUrl(dto.getThumbUrl());
        fav.setEpisodeCurrent(dto.getEpisodeCurrent());
        fav.setLang(dto.getLang());
        fav.setQuality(dto.getQuality());
        String poster = imageOptimizationService.buildFullUrl(dto.getPosterUrl());
        String thumb = imageOptimizationService.buildFullUrl(fav.getThumbUrl());

        if (dto.getPosterUrl() == null || dto.getThumbUrl() == null) {
            try {
                OphimMovieDetailResponse detail = ophimClient.getMovieDetail(dto.getMovieSlug());
                OphimMovieDetail movie = detail.getData().getItem();
                poster = imageOptimizationService.buildFullUrl(movie.getPosterUrl());
                thumb = imageOptimizationService.buildFullUrl(movie.getThumbUrl());
                if (poster != null) {
                    fav.setPosterUrl(poster);
                }
                if (thumb != null) {
                    fav.setThumbUrl(thumb);
                }
            } catch (Exception e) {
                log.warn("Không lấy được poster mới từ Ophim cho slug {}: {}", fav.getMovieSlug(), e.getMessage());
            }
        }
        String posterUrl = poster != null ? poster : null;
        String thumbUrl = thumb != null ? thumb : null;

        fav.setPosterUrl(posterUrl);
        fav.setThumbUrl(thumbUrl);
        favoriteRepository.save(fav);

        Map<String, Map<String, OptimizedImage>> imageMap = buildImageMap(List.of(fav.getMovieSlug()));
        return convertFavoriteRes(fav, imageMap);
    }

    public FavoriteRes handleUpdateFavorite(FavoriteUpdateReq dto) throws IdInvalidException {
        Favorite fav = favoriteRepository.findById(dto.getId()).orElseThrow(() -> new IdInvalidException("Favorite ko tồn tại với id " + dto.getId()));

        if (dto.getUserId() != null) {
            User user = getUserById(dto.getUserId());
            fav.setUser(user);
        }

        if (dto.getMovieSlug() != null) fav.setMovieSlug(dto.getMovieSlug());
        if (dto.getMovieName() != null) fav.setMovieName(dto.getMovieName());
        if (dto.getOriginName() != null) fav.setOriginName(dto.getOriginName());
        if (dto.getEpisodeCurrent() != null) fav.setEpisodeCurrent(dto.getEpisodeCurrent());
        if (dto.getThumbUrl() != null) fav.setThumbUrl(imageOptimizationService.buildFullUrl(dto.getThumbUrl()));
        if (dto.getPosterUrl() != null) fav.setPosterUrl(imageOptimizationService.buildFullUrl(dto.getPosterUrl()));
        if (dto.getLang() != null) fav.setLang(dto.getLang());
        if (dto.getQuality() != null) fav.setQuality(dto.getQuality());

        favoriteRepository.save(fav);

        Map<String, Map<String, OptimizedImage>> imageMap = buildImageMap(List.of(fav.getMovieSlug()));
        return convertFavoriteRes(fav, imageMap);
    }

    public void handleDeleteFavorite(long id) throws IdInvalidException {
        favoriteRepository.findById(id).orElseThrow(() -> new IdInvalidException("Favorite không tồn tại với id: " + id));
        favoriteRepository.deleteById(id);
    }


    private FavoriteRes convertFavoriteRes(Favorite fav, Map<String, Map<String, OptimizedImage>> imageMap) {
        FavoriteRes res = new FavoriteRes();

        res.setId(fav.getId());
        res.setMovieSlug(fav.getMovieSlug());
        res.setMovieName(fav.getMovieName());
        res.setOriginName(fav.getOriginName());
        res.setEpisodeCurrent(fav.getEpisodeCurrent());
        res.setLang(fav.getLang());
        res.setQuality(fav.getQuality());
        res.setCreatedAt(fav.getCreatedAt());
        res.setUpdatedAt(fav.getUpdatedAt());
        res.setUpdatedAt(fav.getUpdatedAt());
        setImagesFromMap(res, fav.getMovieSlug(), imageMap);

        if (fav.getUser() != null) {
            FavoriteRes.ResUserDTO userDTO = new FavoriteRes.ResUserDTO();
            userDTO.setId(fav.getUser().getId());
            userDTO.setEmail(fav.getUser().getEmail());
            res.setUser(userDTO);
        }
        return res;
    }

    private Map<String, Map<String, OptimizedImage>> buildImageMap(List<String> slugs) {
        if (slugs.isEmpty()) {
            return Map.of();
        }
        List<OptimizedImage> images = optimizedImageRepository.findBySlugIn(slugs);

        return images.stream().collect((Collectors.groupingBy(
                OptimizedImage::getSlug,
                Collectors.toMap(
                        OptimizedImage::getImageType,
                        img -> img,
                        (existing, replace) -> existing
                )
        )));
    }

    private void setImagesFromMap(FavoriteRes res, String slug, Map<String, Map<String, OptimizedImage>> imageMap) {
        Map<String, OptimizedImage> slugImages = imageMap.get(slug);

        if (slugImages == null) return;

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
