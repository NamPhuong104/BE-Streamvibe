package movieapp.controller;

import com.turkraft.springfilter.boot.Filter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import movieapp.entity.Favorite;
import movieapp.dto.Favorites.FavoriteCreateReq;
import movieapp.dto.Favorites.FavoriteRes;
import movieapp.dto.Favorites.FavoriteUpdateReq;
import movieapp.dto.MetaAndHead.ResultPaginationDTO;
import movieapp.service.FavoriteService;
import movieapp.util.annotation.ApiMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/favorites")
public class FavoriteController {
    private final FavoriteService favoriteService;

    @GetMapping
    @ApiMessage("Get All Favorites")
    public ResultPaginationDTO getAllFavorite(@Filter Specification<Favorite> spec, Pageable pageable) {
        return favoriteService.handleGetAllFavorite(spec, pageable);
    }

    @GetMapping("/me")
    @ApiMessage("Get Favorite By Me")
    public ResultPaginationDTO getFavoriteByMe(Pageable pageable) {
        return favoriteService.handleGetFavoriteByMe(pageable);
    }

    @GetMapping("/me/checkIsExist/{movieSlug}")
    @ApiMessage("Check Is Exits Favorite By Movie Slug")
    public Boolean getIsFavoriteByMovieSlug(@PathVariable String movieSlug) {
        return favoriteService.handleCheckIsFavorite(movieSlug);
    }

    @DeleteMapping("/me/{movieSlug}")
    @ApiMessage("Delete By Movie Slug")
    public Void deleteBySlug(@PathVariable String movieSlug) {
        favoriteService.handleDeleteByMovieSlug(movieSlug);
        return null;
    }

    @GetMapping("/{id}")
    @ApiMessage("Get Favorite By Id")
    public FavoriteRes getFavoriteById(@PathVariable("id") long id) {
        return favoriteService.handleGetFavoriteById(id);
    }


    @PostMapping
    @ApiMessage("Create Favorite")
    public ResponseEntity<FavoriteRes> createFavorite(@Valid @RequestBody FavoriteCreateReq dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(favoriteService.handleCreateFavorite(dto));
    }

    @PatchMapping
    @ApiMessage("Update Favorite")
    public FavoriteRes updateFavorite(@RequestBody FavoriteUpdateReq dto) {
        return favoriteService.handleUpdateFavorite(dto);
    }

    @DeleteMapping("/{id}")
    @ApiMessage("Delete Favorite")
    public Void deleteFavorite(@PathVariable("id") Long id) {
        favoriteService.handleDeleteFavorite(id);
        return null;
    }
}
