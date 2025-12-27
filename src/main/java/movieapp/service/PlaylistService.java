package movieapp.service;

import lombok.RequiredArgsConstructor;
import movieapp.domain.Playlist;
import movieapp.domain.User;
import movieapp.dto.MetaAndHead.ResultPaginationDTO;
import movieapp.dto.Playlist.PlayListUpdateDTO;
import movieapp.dto.Playlist.PlaylistCreateDTO;
import movieapp.dto.Playlist.PlaylistResponse;
import movieapp.repository.PlaylistRepository;
import movieapp.repository.UserRepository;
import movieapp.util.SecurityUtil;
import movieapp.util.error.IdInvalidException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlaylistService {
    private final PlaylistRepository playlistRepository;
    private final UserService userService;
    private final UserRepository userRepository;

    //  CLIENT
    public ResultPaginationDTO handleGetPlaylistByMe(Pageable pageable) throws IdInvalidException {
        String email = SecurityUtil.getCurrentUserLogin().orElseThrow(() -> new IdInvalidException("Bạn chưa đăng nhập"));
        User currentUser = userRepository.findByEmail(email).orElseThrow(() -> new IdInvalidException("Không tìm thấy user với email: " + email));

        Page<Playlist> pagePl = playlistRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId(), pageable);

        List<PlaylistResponse> dtoList = pagePl.getContent().stream().map(item -> convertToRes(item)).toList();

        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();

        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());
        mt.setPages(pagePl.getTotalPages());
        mt.setTotal(pagePl.getTotalElements());

        rs.setMeta(mt);
        rs.setResult(dtoList);

        return rs;
    }
    

    @Transactional
    public void handleDeleteAllPlaylistByMe() throws IdInvalidException {
        String email = SecurityUtil.getCurrentUserLogin().orElseThrow(() -> new IdInvalidException("Bạn chưa đăng nhập"));
        User currentUser = userRepository.findByEmail(email).orElseThrow(() -> new IdInvalidException("Không tìm thấy user"));

        playlistRepository.deleteAllHistoryByUserId(currentUser.getId());
    }

    //  ADMIN
    public ResultPaginationDTO handleGetAllPlaylist(Specification<Playlist> spec, Pageable pageable) {
        Page<Playlist> pagePl = playlistRepository.findAll(spec, pageable);

        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();

        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());

        mt.setPages(pagePl.getTotalPages());
        mt.setTotal(pagePl.getTotalElements());

        List<PlaylistResponse> res = new ArrayList<>(pagePl.getContent().stream().map(item -> convertToRes(item)).collect(Collectors.toList()));

        rs.setMeta(mt);
        rs.setResult(res);

        return rs;
    }

    public PlaylistResponse handleGetPlaylistById(Long id) throws IdInvalidException {
        Playlist pl = playlistRepository.findById(id).orElseThrow(() -> new IdInvalidException("Playlist không tồn tại với id: " + id));
        return convertToRes(pl);
    }

    public PlaylistResponse handleCreatePlaylist(PlaylistCreateDTO dto) throws IdInvalidException {
        User currentUser = userService.handleGetUserById(dto.getUserId());

        Playlist playlist = Playlist.builder().user(currentUser).name(dto.getName().trim()).movieCount(0).build();

        playlistRepository.save(playlist);

        return convertToRes(playlist);
    }

    public PlaylistResponse handleUpdatePlaylist(PlayListUpdateDTO dto) throws IdInvalidException {
        User currentUser = userRepository.findById(dto.getUserId()).orElseThrow(() -> new IdInvalidException("User không tồn tại với id: " + dto.getUserId()));

        Playlist pl = playlistRepository.findById(dto.getId()).orElseThrow(() -> new IdInvalidException("Playlist không tồn tại với id: " + dto.getId()));

        if (dto.getName() != null) pl.setName(dto.getName());
        if (dto.getUserId() != null) pl.setUser(currentUser);

        playlistRepository.save(pl);

        return convertToRes(pl);
    }

    public void handleDeletePlaylist(Long id) throws IdInvalidException {
        PlaylistResponse pl = handleGetPlaylistById(id);

        if (pl != null) playlistRepository.deleteById(id);
    }

    private PlaylistResponse convertToRes(Playlist data) {
        PlaylistResponse playlistRes = PlaylistResponse.builder()
                .id(data.getId())
                .movieCount(data.getMovieCount())
                .name(data.getName())
                .createdAt(data.getCreatedAt())
                .updatedAt(data.getUpdatedAt())
                .build();

        if (data.getUser() != null) {
            playlistRes.setUser(new PlaylistResponse.ResUserDTO(
                    data.getUser().getId(),
                    data.getUser().getEmail()
            ));
        }

        return playlistRes;
    }
}
