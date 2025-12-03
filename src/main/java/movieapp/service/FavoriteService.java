package movieapp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import movieapp.client.OphimClient;
import movieapp.domain.Favorite;
import movieapp.domain.User;
import movieapp.dto.Favorites.FavoriteCreateReq;
import movieapp.dto.Favorites.FavoriteRes;
import movieapp.dto.Favorites.FavoriteUpdateReq;
import movieapp.dto.MetaAndHead.ResultPaginationDTO;
import movieapp.dto.OphimResponse.OphimMovieDetail;
import movieapp.dto.OphimResponse.OphimMovieDetailResponse;
import movieapp.dto.OphimResponse.OphimMovieItem;
import movieapp.dto.User.Response.ResUserDTO;
import movieapp.repository.FavoriteRepository;
import movieapp.repository.UserRepository;
import movieapp.util.error.IdInvalidException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FavoriteService {
    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final ImageOptimizationService imageOptimizationService;
    private final UserService userService;
    private final OphimClient ophimClient;

    public User getUserById(Long id) throws IdInvalidException {
        return userRepository.findById(id).orElseThrow(() -> new IdInvalidException("User không tồn tại với id: " + id));
    }

    public ResultPaginationDTO handleGetAllFavorite(Specification<Favorite> spec, Pageable pageable) {
        Page<Favorite> pageFav = favoriteRepository.findAll(spec, pageable);
        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();

        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());

        mt.setPages(pageFav.getTotalPages());
        mt.setTotal(pageFav.getTotalElements());

        rs.setMeta(mt);

        List<FavoriteRes> dtoList = pageFav.getContent().stream().map(this::convertFavoriteRes).toList();

        rs.setResult(dtoList);

        return rs;
    }

    public FavoriteRes handleGetFavoriteById(long id) throws IdInvalidException {
        Favorite res = favoriteRepository.findById(id).orElseThrow(() -> new IdInvalidException("Favorite ko tồn tại với id " + id));

        return convertFavoriteRes(res);
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

        String poster = dto.getPosterUrl();
        String thumb = fav.getThumbUrl();

        if (dto.getPosterUrl() == null) {
            try {
                OphimMovieDetailResponse detail = ophimClient.getMovieDetail(dto.getMovieSlug());
                OphimMovieDetail movie = detail.getData().getItem();
                poster = movie.getPosterUrl();
                if (poster != null) {
                    fav.setPosterUrl(poster);
                    favoriteRepository.save(fav);
                }
            } catch (Exception e) {
                log.warn("Không lấy được poster mới từ Ophim cho slug {}: {}", fav.getMovieSlug(), e.getMessage());
            }
        }
        String posterUrl = (poster != null) ? poster : null;
        String thumbUrl = thumb;

        fav.setPosterUrl(posterUrl);
        fav.setThumbUrl(thumbUrl);
        favoriteRepository.save(fav);

        return convertFavoriteRes(fav);
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
        if (dto.getThumbUrl() != null) fav.setThumbUrl(dto.getThumbUrl());
        if (dto.getPosterUrl() != null) fav.setPosterUrl(dto.getPosterUrl());

        favoriteRepository.save(fav);
        return convertFavoriteRes(fav);
    }

    public void handleDeleteFavorite(long id) throws IdInvalidException {
        favoriteRepository.findById(id).orElseThrow(() -> new IdInvalidException("Favorite không tồn tại với id: " + id));
        favoriteRepository.deleteById(id);
    }


    private FavoriteRes convertFavoriteRes(Favorite fav) {
        FavoriteRes res = new FavoriteRes();

        res.setId(fav.getId());
        res.setMovieSlug(fav.getMovieSlug());
        res.setMovieName(fav.getMovieName());
        res.setOriginName(fav.getOriginName());
        res.setPosterUrl(imageOptimizationService.getOptimizedPosterOriginal(fav.getPosterUrl()));
        res.setThumbUrl(imageOptimizationService.getOptimizedThumbOriginal(fav.getThumbUrl()));
        res.setEpisodeCurrent(fav.getEpisodeCurrent());
        res.setCreatedAt(fav.getCreatedAt());
        res.setUpdatedAt(fav.getUpdatedAt());
        res.setUpdatedAt(fav.getUpdatedAt());
        if (fav.getUser() != null) {
            FavoriteRes.ResUserDTO userDTO = new FavoriteRes.ResUserDTO();
            userDTO.setId(fav.getUser().getId());
            userDTO.setEmail(fav.getUser().getEmail());
            res.setUser(userDTO);
        }
        return res;
    }
}
