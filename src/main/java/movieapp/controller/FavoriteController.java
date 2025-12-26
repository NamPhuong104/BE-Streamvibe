package movieapp.controller;

import com.turkraft.springfilter.boot.Filter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import movieapp.domain.Favorite;
import movieapp.dto.Favorites.FavoriteCreateReq;
import movieapp.dto.Favorites.FavoriteRes;
import movieapp.dto.Favorites.FavoriteUpdateReq;
import movieapp.dto.MetaAndHead.ResultPaginationDTO;
import movieapp.service.FavoriteService;
import movieapp.util.annotation.ApiMessage;
import movieapp.util.error.IdInvalidException;
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
    public ResponseEntity<ResultPaginationDTO> getAllFavorite(@Filter Specification<Favorite> spec, Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK).body(favoriteService.handleGetAllFavorite(spec, pageable));
    }

    @GetMapping("/me")
    @ApiMessage("Get Favorite By Me")
    public ResponseEntity<ResultPaginationDTO> getFavoriteByMe(Pageable pageable) throws IdInvalidException {
        return ResponseEntity.status(HttpStatus.OK).body(favoriteService.handleGetFavoriteByMe(pageable));
    }

    @GetMapping("/me/checkIsExist/{movieSlug}")
    @ApiMessage("Check Is Exits Favorite By Movie Slug")
    public ResponseEntity<Boolean> getIsFavoriteByMovieSlug(@PathVariable String movieSlug) {
        return ResponseEntity.status(200).body(favoriteService.handleCheckIsFavorite(movieSlug));
    }

    @DeleteMapping("/me/{movieSlug}")
    @ApiMessage("Delete By Movie Slug")
    public ResponseEntity<Void> deleteBySlug(@PathVariable String movieSlug) throws IdInvalidException {
        favoriteService.handleDeleteByMovieSlug(movieSlug);
        return ResponseEntity.ok(null);
    }

    @GetMapping("/{id}")
    @ApiMessage("Get Favorite By Id")
    public ResponseEntity<FavoriteRes> getFavoriteById(@PathVariable("id") long id) throws IdInvalidException {
        return ResponseEntity.ok(favoriteService.handleGetFavoriteById(id));
    }


    @PostMapping
    @ApiMessage("Create Favorite")
    public ResponseEntity<FavoriteRes> createFavorite(@Valid @RequestBody FavoriteCreateReq dto) throws IdInvalidException {
        return ResponseEntity.status(HttpStatus.CREATED).body(favoriteService.handleCreateFavorite(dto));
    }

    @PatchMapping
    @ApiMessage("Update Favorite")
    public ResponseEntity<FavoriteRes> updateFavorite(@RequestBody FavoriteUpdateReq dto) throws IdInvalidException {
        return ResponseEntity.ok().body(favoriteService.handleUpdateFavorite(dto));
    }

    @DeleteMapping("/{id}")
    @ApiMessage("Delete Favorite")
    public ResponseEntity<Void> deleteFavorite(@PathVariable("id") Long id) throws IdInvalidException {
        favoriteService.handleDeleteFavorite(id);
        return ResponseEntity.ok(null);
    }
}
