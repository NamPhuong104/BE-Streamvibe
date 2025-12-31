package movieapp.controller;

import com.turkraft.springfilter.boot.Filter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import movieapp.dto.MetaAndHead.ResultPaginationDTO;
import movieapp.dto.PlaylistMovie.PlaylistMovieCreateDTO;
import movieapp.dto.PlaylistMovie.PlaylistMovieRes;
import movieapp.service.PlaylistMovieService;
import movieapp.util.annotation.ApiMessage;
import movieapp.util.error.IdInvalidException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequiredArgsConstructor
@RequestMapping("/playlist-movies")
public class PlaylistMovieController {
    private final PlaylistMovieService playlistMovieService;

    @GetMapping("/me")
    @ApiMessage("Get Playlist Movie By Me")
    public ResultPaginationDTO getMovieByMe(Pageable pageable) throws IdInvalidException {
        return playlistMovieService.handleGetPlaylistByMe(pageable);
    }

    @GetMapping("/me/check/{movieSlug}")
    @ApiMessage("Check Playlist Movie In Playlist")
    public Long checkMovieInMyPlaylist(@PathVariable String movieSlug) throws IdInvalidException {
        return playlistMovieService.handleCheckMovieInMyPlaylist(movieSlug);
    }

    @GetMapping("/me/{playlistId}")
    @ApiMessage("Get Playlist Movie In My Playlist")
    public ResultPaginationDTO getMovieInMyPlaylist(@PathVariable Long playlistId, Pageable pageable) throws IdInvalidException {
        return playlistMovieService.handleGetMovieInMyPlaylist(playlistId, pageable);
    }

    @PostMapping("/me")
    @ApiMessage("Create Movie In My Playlist")
    public ResponseEntity<PlaylistMovieRes> createMovieInPlaylist(@Valid @RequestBody PlaylistMovieCreateDTO dto) throws IdInvalidException {
        return ResponseEntity.status(HttpStatus.CREATED).body(playlistMovieService.handleCreateMovieByMe(dto));
    }


    @DeleteMapping("/me/{playlistId}/{movieSlug}")
    @ApiMessage("Delete Movie By PlaylistId And MovieSlug")
    public void deleteMovieByPlaylistIdAndMovieSlug(@PathVariable Long playlistId, @PathVariable String movieSlug) {
        playlistMovieService.handleDeleteMovieInMyPlaylist(playlistId, movieSlug);
    }

    @GetMapping
    @ApiMessage("Get All Playlist Movie")
    public ResultPaginationDTO getAllPlaylistMovie(@Filter Specification spec, Pageable pageable) {
        return playlistMovieService.handleGetAllPlaylistMovie(spec, pageable);
    }

//    @PostMapping
//    @ApiMessage("Create Playlist Movie")
//    public ResponseEntity<PlaylistMovieRes> createPlaylistMovie(@Valid @RequestBody PlaylistMovieCreateDTO dto) throws IdInvalidException {
//        return ResponseEntity.status(HttpStatus.CREATED).body(playlistMovieService.handleCreatePlaylistMovie(dto));
//    }

    @DeleteMapping("/{id}")
    @ApiMessage("Delete Playlist Movie")
    public void deletePlaylist(@Valid @PathVariable("id") Long id) {
        playlistMovieService.handleDeletePlaylistMovie(id);

    }
}
