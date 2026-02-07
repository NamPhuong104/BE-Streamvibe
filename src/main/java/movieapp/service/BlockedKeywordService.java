package movieapp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import movieapp.dto.BlockedKeyword.BlockedKeywordCreateDTO;
import movieapp.dto.BlockedKeyword.BlockedKeywordResponse;
import movieapp.dto.BlockedKeyword.BlockedKeywordUpdateDTO;
import movieapp.dto.MetaAndHead.ResultPaginationDTO;
import movieapp.entity.BlockedKeyword;
import movieapp.exception.CommonMessageException;
import movieapp.repository.BlockedKeywordRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class BlockedKeywordService {
    private final BlockedKeywordRepository blockedKeywordRepository;

    public List<String> getActiveKeywords() {
        return blockedKeywordRepository.findAllActiveKeyword();
    }

    /**
     * Check if keyword is blocked (contains match, case-insensitive)
     */
    public boolean isKeywordBlocked(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) return false;

        String normalizedKeyword = keyword.toLowerCase().trim();
        List<String> blockedKeywords = getActiveKeywords();

        return blockedKeywords.stream().anyMatch(blocked -> normalizedKeyword.contains(blocked.toLowerCase()));
    }

    public ResultPaginationDTO getAll(Specification<BlockedKeyword> spec, Pageable pageable) {
        Page<BlockedKeyword> page = blockedKeywordRepository.findAll(spec, pageable);

        List<BlockedKeywordResponse> content = page.getContent().stream().map(this::toResponse).toList();

        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();

        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());

        mt.setPages(page.getTotalPages());
        mt.setTotal(page.getTotalElements());

        rs.setResult(content);
        rs.setMeta(mt);

        return rs;
    }

    public BlockedKeywordResponse getById(Long id) {
        BlockedKeyword currentKeyword = blockedKeywordRepository.findById(id).orElseThrow(() -> new CommonMessageException("Không tìm thấy keyword với ID: " + id));

        return toResponse(currentKeyword);
    }

    public BlockedKeywordResponse create(BlockedKeywordCreateDTO dto) {
        String normalizedKeyword = dto.getKeyword().toLowerCase().trim();

        if (blockedKeywordRepository.existsByKeywordIgnoreCase(normalizedKeyword))
            throw new CommonMessageException("Keyword đã tồn tại: " + normalizedKeyword);

        BlockedKeyword entity = BlockedKeyword.builder()
                .keyword(normalizedKeyword)
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .build();

        BlockedKeyword saved = blockedKeywordRepository.save(entity);

        return toResponse(saved);
    }

    public BlockedKeywordResponse update(Long id, BlockedKeywordUpdateDTO dto) {
        BlockedKeyword entity = blockedKeywordRepository.findById(id).orElseThrow(() -> new CommonMessageException("Không tìm thấy keyword với ID: " + id));

        if (dto.getKeyword() != null && !dto.getKeyword().trim().isEmpty()) {
            String normalizedKeyword = dto.getKeyword().toLowerCase().trim();

            blockedKeywordRepository.findByKeywordIgnoreCase(normalizedKeyword).ifPresent(existing -> {
                if (!existing.getId().equals(id)) {
                    throw new CommonMessageException("Keyword đã tồn tại: " + normalizedKeyword);
                }
            });
            entity.setKeyword(normalizedKeyword);
        }

        if (dto.getIsActive() != null) entity.setIsActive(dto.getIsActive());

        BlockedKeyword saved = blockedKeywordRepository.save(entity);

        return toResponse(saved);
    }

    public void delete(Long id) {
        BlockedKeyword entity = blockedKeywordRepository.findById(id)
                .orElseThrow(() -> new CommonMessageException("Không tìm thấy keyword với ID: " + id));

        blockedKeywordRepository.delete(entity);
    }


    private BlockedKeywordResponse toResponse(BlockedKeyword entity) {
        return BlockedKeywordResponse.builder()
                .id(entity.getId())
                .keyword(entity.getKeyword())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
