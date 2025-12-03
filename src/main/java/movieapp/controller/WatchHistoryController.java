package movieapp.controller;

import com.turkraft.springfilter.boot.Filter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import movieapp.domain.WatchHistory;
import movieapp.dto.MetaAndHead.ResultPaginationDTO;
import movieapp.dto.WatchHistory.WatchHistoryCreateReq;
import movieapp.dto.WatchHistory.WatchHistoryRes;
import movieapp.dto.WatchHistory.WatchHistoryUpdateReq;
import movieapp.service.WatchHistoryService;
import movieapp.util.annotation.ApiMessage;
import movieapp.util.error.IdInvalidException;
import org.checkerframework.checker.units.qual.A;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/watch-history")
public class WatchHistoryController {
    private final WatchHistoryService watchHistoryService;


    @GetMapping
    @ApiMessage("Get watch history")
    public ResponseEntity<ResultPaginationDTO> getAllWatchHistory(@Filter Specification<WatchHistory> spec, Pageable pageable) {
        return ResponseEntity.ok(watchHistoryService.handleGetAllWatchHistory(spec, pageable));
    }

    @PostMapping
    @ApiMessage("Create watch history")
    public ResponseEntity<WatchHistoryRes> createWatchHistory(@Valid @RequestBody WatchHistoryCreateReq dto) throws IdInvalidException {
        return ResponseEntity.status(HttpStatus.CREATED).body(watchHistoryService.handleCreateWatchHistory(dto));
    }

    @PutMapping
    @ApiMessage("Update Watch History")
    public ResponseEntity<WatchHistoryRes> updateWatchHistory(@Valid @RequestBody WatchHistoryUpdateReq dto) throws IdInvalidException {
        return ResponseEntity.ok(watchHistoryService.handleUpdateWatchHistory(dto));
    }

    @DeleteMapping("/{id}")
    @ApiMessage("Delete Watch History")
    public ResponseEntity<Void> deleteWatchHistory(@Valid @PathVariable("id") Long id) throws IdInvalidException {
        watchHistoryService.handleDeleteWatchHistory(id);
        return ResponseEntity.ok(null);
    }

    @DeleteMapping("/user/{id}")
    @ApiMessage("Delete All Watch History By UserId")
    public ResponseEntity<Void> deleteAllWatchHistoryByUserId(@Valid @PathVariable("id") Long id) throws IdInvalidException {
        watchHistoryService.handleDeleteAllWatchHistoryByUserId(id);
        return ResponseEntity.ok(null);
    }
}
