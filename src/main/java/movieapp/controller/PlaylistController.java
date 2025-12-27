package movieapp.controller;

import com.turkraft.springfilter.boot.Filter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import movieapp.domain.Playlist;
import movieapp.dto.MetaAndHead.ResultPaginationDTO;
import movieapp.dto.Playlist.PlayListUpdateDTO;
import movieapp.dto.Playlist.PlaylistCreateDTO;
import movieapp.dto.Playlist.PlaylistResponse;
import movieapp.service.PlaylistService;
import movieapp.util.annotation.ApiMessage;
import movieapp.util.error.IdInvalidException;
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
    public ResponseEntity<ResultPaginationDTO> getPlaylistByMe(Pageable page) throws IdInvalidException {
        return ResponseEntity.status(200).body(playlistService.handleGetPlaylistByMe(page));
    }

    @DeleteMapping("/me")
    @ApiMessage("Delete All Playlist By Me")
    public ResponseEntity<Void> deleteAllPlaylistByMe() throws IdInvalidException {
        playlistService.handleDeleteAllPlaylistByMe();
        return ResponseEntity.ok(null);
    }


    //  ADMIN
    @GetMapping
    @ApiMessage("Get All Playlists")
    public ResponseEntity<ResultPaginationDTO> getAllPlaylist(@Filter Specification<Playlist> spec, Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK).body(playlistService.handleGetAllPlaylist(spec, pageable));
    }

    @GetMapping("/{id}")
    @ApiMessage("Get Playlist By Id")
    public ResponseEntity<PlaylistResponse> getPlaylistById(@Valid @PathVariable("id") Long id) throws IdInvalidException {
        return ResponseEntity.ok(playlistService.handleGetPlaylistById(id));
    }

    @PostMapping
    @ApiMessage("Create Playlist")
    public ResponseEntity<PlaylistResponse> createPlaylist(@Valid @RequestBody PlaylistCreateDTO playlistReq) throws IdInvalidException {

        return ResponseEntity.status(HttpStatus.CREATED).body(playlistService.handleCreatePlaylist(playlistReq));
    }

    @PutMapping
    @ApiMessage("Update Playlist")
    public ResponseEntity<PlaylistResponse> updatePlaylist(@Valid @RequestBody PlayListUpdateDTO dto) throws IdInvalidException {
        return ResponseEntity.ok(playlistService.handleUpdatePlaylist(dto));
    }

    @DeleteMapping("/{id}")
    @ApiMessage("Delete Playlist")
    public ResponseEntity<Void> deletePlaylist(@Valid @PathVariable("id") Long id) throws IdInvalidException {
        playlistService.handleDeletePlaylist(id);
        return ResponseEntity.ok(null);
    }

}
