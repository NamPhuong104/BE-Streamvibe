package movieapp.controller;

import com.turkraft.springfilter.boot.Filter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import movieapp.dto.BlockedKeyword.BlockedKeywordCreateDTO;
import movieapp.dto.BlockedKeyword.BlockedKeywordResponse;
import movieapp.dto.BlockedKeyword.BlockedKeywordUpdateDTO;
import movieapp.dto.MetaAndHead.ResultPaginationDTO;
import movieapp.entity.BlockedKeyword;
import movieapp.service.BlockedKeywordService;
import movieapp.util.annotation.ApiMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/admin/blocked-keywords")
@RequiredArgsConstructor
public class BlockedKeywordController {
    private final BlockedKeywordService blockedKeywordService;

    @GetMapping("/list-blocked-keywords")
    @ApiMessage("Lấy danh sách từ khóa bị chặn thành công")
    public List<String> getBlockedKeywords() {
        return blockedKeywordService.getActiveKeywords();
    }

    @GetMapping
    @ApiMessage("Lấy danh sách từ khóa bị chặn thành công")
    public ResultPaginationDTO getAll(
            @Filter Specification<BlockedKeyword> spec,
            Pageable pageable) {
        return blockedKeywordService.getAll(spec, pageable);
    }

    @GetMapping("/{id}")
    @ApiMessage("Lấy thông tin từ khóa bị chặn thành công")
    public BlockedKeywordResponse getById(@PathVariable Long id) {
        return blockedKeywordService.getById(id);
    }

    @PostMapping
    @ApiMessage("Tạo từ khóa bị chặn thành công")
    public ResponseEntity<BlockedKeywordResponse> create(
            @Valid @RequestBody BlockedKeywordCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(blockedKeywordService.create(dto));
    }

    @PutMapping("/{id}")
    @ApiMessage("Cập nhật từ khóa bị chặn thành công")
    public BlockedKeywordResponse update(
            @PathVariable Long id,
            @Valid @RequestBody BlockedKeywordUpdateDTO dto) {
        return blockedKeywordService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ApiMessage("Xóa từ khóa bị chặn thành công")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        blockedKeywordService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
