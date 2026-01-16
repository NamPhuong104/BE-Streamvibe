package movieapp.service;

import lombok.RequiredArgsConstructor;
import movieapp.dto.MetaAndHead.ResultPaginationDTO;
import movieapp.dto.Role.RoleCreateDTO;
import movieapp.dto.Role.RoleResponse;
import movieapp.entity.Role;
import movieapp.entity.User;
import movieapp.exception.CommonMessageException;
import movieapp.repository.RoleRepository;
import movieapp.repository.UserRepository;
import movieapp.util.UsernameGenerator;
import movieapp.util.constant.RoleEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleService {
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    public Role handleFindByName(String name) {
        return roleRepository.findByName(name).orElseThrow(() -> new CommonMessageException("Role không tồn tại: " + name));
    }

    public Role handleFindById(Long id) {
        return roleRepository.findById(id).orElseThrow(() -> new CommonMessageException("Role không tồn tại với id: " + id));
    }

    public Role getDefaultUserRole() {
        return handleFindByName(RoleEnum.ROLE_USER.getName());
    }

    public boolean handleExists(String name) {
        return roleRepository.existsByName(name);
    }

    // ==================== GET ALL ====================

    public ResultPaginationDTO handleGetAllRoles(Specification<Role> spec, Pageable pageable) {
        Page<Role> pageRole = roleRepository.findAll(spec, pageable);
        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();

        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());

        mt.setPages(pageRole.getTotalPages());
        mt.setTotal(pageRole.getTotalElements());

        rs.setMeta(mt);
//        rs.setResult(pageRole.getContent());
        List<RoleResponse> dtoRole = pageRole.getContent().stream().map(this::convertToRoleResponse).collect(Collectors.toList());
        rs.setResult(dtoRole);

        return rs;
    }

    public List<RoleResponse> handleGetAllRolesSorted() {
        return roleRepository.findAllByOrderByPriorityAsc().stream().map(this::convertToRoleResponse).collect(Collectors.toList());
    }

    public List<RoleResponse> handleGetRolesByPriorityRange(Integer minPriority, Integer maxPriority) {
        return roleRepository.findByPriorityBetween(minPriority, maxPriority)
                .stream()
                .map(this::convertToRoleResponse)
                .collect(Collectors.toList());
    }

    public RoleResponse handleCreateRole(RoleCreateDTO dto) {
        String roleName = normalizeRoleName(dto.getName());

        if (roleRepository.existsByName(roleName)) throw new CommonMessageException("Role đã tồn tại: " + roleName);
        if (dto.getPriority() != null && dto.getPriority() <= 0)
            throw new CommonMessageException("Priority phải > 0. Priority <= 0 dành cho system roles.");


        Role newRole = Role.builder()
                .name(roleName)
                .description(dto.getDescription())
                .priority(dto.getPriority() != null ? dto.getPriority() : 100)
                .isSystemRole(false)
                .build();

        Role saved = roleRepository.save(newRole);

        return convertToRoleResponse(saved);
    }

    public Role handleUpdateRole(Long roleId, String newName, String newDescription, Integer newPriority) {
        Role role = handleFindById(roleId);

        // Check system role protection
        if (Boolean.TRUE.equals(role.getIsSystemRole())) {
            if (newName != null && !newName.equals(role.getName())) {
                throw new CommonMessageException("Không thể đổi tên system role: " + role.getName());
            }
            if (newPriority != null && !newPriority.equals(role.getPriority())) {
                throw new CommonMessageException("Không thể đổi priority của system role: " + role.getName());
            }
        }

        // Update name
        if (newName != null && !newName.isBlank()) {
            String normalizedName = normalizeRoleName(newName);
            if (!role.getName().equals(normalizedName) && roleRepository.existsByName(normalizedName)) {
                throw new CommonMessageException("Role name đã tồn tại: " + normalizedName);
            }
            role.setName(normalizedName);
        }

        // Update description
        if (newDescription != null) {
            role.setDescription(newDescription);
        }

        // Update priority
        if (newPriority != null) {
            if (newPriority <= 0) {
                throw new CommonMessageException("Priority phải > 0");
            }
            role.setPriority(newPriority);
        }

        return roleRepository.save(role);
    }

    @Transactional
    public void handleDeleteRole(String roleName) {
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new CommonMessageException("Role không tồn tại: " + roleName));

        // Không cho xóa system roles
        if (Boolean.TRUE.equals(role.getIsSystemRole())) {
            throw new CommonMessageException("Không thể xóa system role: " + roleName);
        }


        Role defaultRole = getDefaultUserRole();

        // ⭐ Kiểm tra có user nào đang dùng role này không
//        Long userCount = roleRepository.countUsersByRoleId(role.getId());
//        if (userCount > 0) {
//            throw new CommonMessageException(
//                    String.format("Không thể xóa role '%s' vì đang có %d user sử dụng. " +
//                            "Vui lòng chuyển users sang role khác trước.", roleName, userCount)
//            );
//        }
        userRepository.updateAllUsersRoleByOldRoleId(role.getId(), defaultRole.getId());
        roleRepository.delete(role);
    }

    // ==================== SPECIAL METHODS ====================

    /**
     * Lấy danh sách roles mà user có quyền assign cho người khác
     */
    public List<RoleResponse> handleGetAssignableRoles(User currentUser) {
        if (currentUser == null || currentUser.getRole() == null) {
            return List.of();
        }

        // User chỉ có thể assign roles có priority cao hơn (số lớn hơn) role của mình
        return roleRepository.findAssignableRoles(currentUser.getRolePriority())
                .stream()
                .map(this::convertToRoleResponse)
                .collect(Collectors.toList());
    }

    public RoleResponse handleCloneRole(Long sourceRoleId) {
        Role source = handleFindById(sourceRoleId);
        String roleName = UsernameGenerator.generateFromName(source.getName());

        if (roleRepository.existsByName(roleName)) {
            throw new CommonMessageException("Role đã tồn tại: " + roleName);
        }

        Role newRole = Role.builder()
                .name(roleName.toUpperCase())
                .description("Cloned from: " + source.getName() + ". " +
                        (source.getDescription() != null ? source.getDescription() : ""))
                .priority(source.getPriority() + 1)
                .isSystemRole(false)
                .build();

        Role saved = roleRepository.save(newRole);

        return convertToRoleResponse(saved);
    }


    //    ==================== HELPER METHODS ====================
    private String normalizeRoleName(String name) {
        String roleName = name.toUpperCase().trim();
        if (!roleName.startsWith("ROLE_")) {
            roleName = "ROLE_" + roleName;
        }
        return roleName;
    }

    public RoleResponse convertToRoleResponse(Role role) {
        return RoleResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .priority(role.getPriority())
                .isSystemRole(role.getIsSystemRole())
                .userCount(roleRepository.countUsersByRoleId(role.getId()))
                .createdAt(role.getCreatedAt())
                .build();
    }
}
