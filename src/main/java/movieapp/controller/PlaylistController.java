package movieapp.controller;

import com.turkraft.springfilter.boot.Filter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import movieapp.entity.Playlist;
import movieapp.dto.MetaAndHead.ResultPaginationDTO;
import movieapp.dto.Playlist.PlayListUpdateDTO;
import movieapp.dto.Playlist.PlaylistCreateDTO;
import movieapp.dto.Playlist.PlaylistResponse;
import movieapp.service.PlaylistService;
import movieapp.util.annotation.ApiMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/playlist")
@RequiredArgsConstructor
public class PlaylistController {
    private final PlaylistService playlistService;

    @GetMapping("/me")
    @ApiMessage("Get PlayList By Me")
    public ResultPaginationDTO getPlaylistByMe(Pageable page) {
        return playlistService.handleGetPlaylistByMe(page);
    }

    @PostMapping("/me")
    @ApiMessage("Create Playlist By Me")
    public ResponseEntity<PlaylistResponse> createPlaylistByMe(@Valid @RequestBody PlaylistCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(playlistService.handleCreatePlaylistByMe(dto));
    }

    @DeleteMapping("/me/{id}")
    @ApiMessage("Delete Playlist By Me")
    public Void deletePlaylistByMe(@PathVariable("id") Long id) {
        playlistService.handleDeletePlaylistByMe(id);
        return null;
    }


    //  ADMIN
    @GetMapping
    @ApiMessage("Get All Playlists")
    public ResultPaginationDTO getAllPlaylist(@Filter Specification<Playlist> spec, Pageable pageable) {
        return playlistService.handleGetAllPlaylist(spec, pageable);
    }

    @GetMapping("/{id}")
    @ApiMessage("Get Playlist By Id")
    public PlaylistResponse getPlaylistById(@Valid @PathVariable("id") Long id) {
        return playlistService.handleGetPlaylistById(id);
    }

    @PostMapping
    @ApiMessage("Create Playlist")
    public ResponseEntity<PlaylistResponse> createPlaylist(@Valid @RequestBody PlaylistCreateDTO playlistReq) {

        return ResponseEntity.status(HttpStatus.CREATED).body(playlistService.handleCreatePlaylist(playlistReq));
    }

    @PutMapping
    @ApiMessage("Update Playlist")
    public PlaylistResponse updatePlaylist(@Valid @RequestBody PlayListUpdateDTO dto) {
        return playlistService.handleUpdatePlaylist(dto);
    }

    @DeleteMapping("/{id}")
    @ApiMessage("Delete Playlist")
    public Void deletePlaylist(@Valid @PathVariable("id") Long id) {
        playlistService.handleDeletePlaylist(id);
        return null;
    }

}
