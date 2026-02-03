package movieapp.controller;

import com.turkraft.springfilter.boot.Filter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import movieapp.entity.WatchHistory;
import movieapp.dto.MetaAndHead.ResultPaginationDTO;
import movieapp.dto.WatchHistory.WatchHistoryCreateReq;
import movieapp.dto.WatchHistory.WatchHistoryRes;
import movieapp.dto.WatchHistory.WatchHistoryUpdateReq;
import movieapp.service.WatchHistoryService;
import movieapp.util.annotation.ApiMessage;
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

    @GetMapping("/me")
    @ApiMessage("Get watch history by me")
    public ResultPaginationDTO getWatchHistoryByMe(Pageable pageable) {
        return watchHistoryService.handleGetWatchHistoryByMe(pageable);
    }

    @GetMapping("/me/progress/{movieSlug}")
    @ApiMessage("Get watch progress")
    public WatchHistoryRes getWatchProgress(
            @PathVariable String movieSlug,
            @RequestParam(required = false) String episodeSlug
    ) {
        return watchHistoryService.getWatchProgress(movieSlug, episodeSlug);
    }

    @DeleteMapping("/me/{movieSlug}")
    @ApiMessage("Delete Watch History By Movie Slug")
    public Void deleteWatchHistoryBySlug(@PathVariable String movieSlug) {
        watchHistoryService.handleDeleteWatchHistoryBySlug(movieSlug);
        return null;
    }


    @GetMapping
    @ApiMessage("Get watch history")
    public ResultPaginationDTO getAllWatchHistory(@Filter Specification<WatchHistory> spec, Pageable pageable) {
        return watchHistoryService.handleGetAllWatchHistory(spec, pageable);
    }

    @GetMapping("/summary")
    @ApiMessage("Get watch history summary (grouped by user + movie)")
    public ResultPaginationDTO getWatchHistorySummary(Pageable pageable) {
        return watchHistoryService.handleGetWatchHistorySummary(pageable);
    }

    @PostMapping
    @ApiMessage("Create watch history")
    public ResponseEntity<WatchHistoryRes> createWatchHistory(@Valid @RequestBody WatchHistoryCreateReq dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(watchHistoryService.handleCreateWatchHistory(dto));
    }

    @PutMapping
    @ApiMessage("Update Watch History")
    public WatchHistoryRes updateWatchHistory(@Valid @RequestBody WatchHistoryUpdateReq dto) {
        return watchHistoryService.handleUpdateWatchHistory(dto);
    }

    @DeleteMapping("/{id}")
    @ApiMessage("Delete Watch History")
    public Void deleteWatchHistory(@Valid @PathVariable("id") Long id) {
        watchHistoryService.handleDeleteWatchHistory(id);
        return null;
    }

    @DeleteMapping("/user/{id}")
    @ApiMessage("Delete All Watch History By UserId")
    public Void deleteAllWatchHistoryByUserId(@Valid @PathVariable("id") Long id) {
        watchHistoryService.handleDeleteAllWatchHistoryByUserId(id);
        return null;
    }
}
