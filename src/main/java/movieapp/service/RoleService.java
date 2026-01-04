package movieapp.service;

import lombok.RequiredArgsConstructor;
import movieapp.dto.MetaAndHead.ResultPaginationDTO;
import movieapp.dto.Role.RoleCreateDTO;
import movieapp.dto.Role.RoleResponse;
import movieapp.entity.Role;
import movieapp.entity.User;
import movieapp.exception.CommonMessageException;
import movieapp.repository.RoleRepository;
import movieapp.util.constant.RoleEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleService {
    private final RoleRepository roleRepository;

    public Role handleFindByName(String name) {
        return roleRepository.findByName(name).orElseThrow(() -> new CommonMessageException("Role không tồn tại: " + name));
    }

    public Role handleFindById(Long id) {
        return roleRepository.findById(id).orElseThrow(() -> new CommonMessageException("Role không tồn tại với id: " + id));
    }

    public Role getDefaultUserRole() {
        return handleFindByName(RoleEnum.ROLE_USER.getName());
    }

    public Set<Role> handleGetDefaultUserRole(Set<String> names) {
        Set<Role> roles = roleRepository.findByNameIn(names);
        if (roles.size() != names.size()) throw new CommonMessageException("Một số role không tồn tại");

        return roles;
    }

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
        List<RoleResponse> dtoRole = pageRole.getContent().stream().map(item -> convertToRoleResponse(item)).collect(Collectors.toList());
        rs.setResult(dtoRole);

        return rs;
    }

    public Set<Role> handleFindByNames(Set<String> names) {
        Set<Role> roles = roleRepository.findByNameIn(names);
        if (roles.size() != names.size()) {
            throw new CommonMessageException("Một số role không tồn tại");
        }
        return roles;
    }

    public boolean handleExits(String name) {
        return roleRepository.existsByName(name);
    }


    public RoleResponse handleCreateRole(RoleCreateDTO dto) {
        String roleName = dto.getName().toUpperCase().trim();

        if (!roleName.startsWith("ROLE_")) {
            roleName = "ROLE_" + roleName;
        }

        if (roleRepository.existsByName(roleName)) throw new CommonMessageException("Role đã tồn tại: " + roleName);

        if (dto.getPriority() != null && dto.getPriority() <= 0)
            throw new CommonMessageException("Priority phải > 0. Priority <= 0 dành cho system roles.");

        Role newRole = Role.builder()
                .name(roleName)
                .description(dto.getDescription())
                .priority(dto.getPriority() != null ? dto.getPriority() : 100)
                .build();

        Role saved = roleRepository.save(newRole);

        return convertToRoleResponse(saved);
    }

    public Role handleUpdateRole(Long roleId, String newName, String newDescription, Integer newPriority) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new CommonMessageException("Role không tồn tại với id: " + roleId));

        // Cập nhật thông tin
        if (newName != null && !newName.isBlank()) {
            // Check tên mới không trùng với role khác
            if (roleRepository.existsByName(newName) && !role.getName().equals(newName)) {
                throw new CommonMessageException("Role name đã tồn tại: " + newName);
            }
            role.setName(newName);
        }

        if (newDescription != null) {
            role.setDescription(newDescription);
        }

        if (newPriority != null) {
            role.setPriority(newPriority);
        }

        return roleRepository.save(role);
    }

    public void handleDeleteRole(String roleName) {
        if (roleName.equals(RoleEnum.ROLE_USER.getName()))
            throw new CommonMessageException("Không thể xóa role USER cơ bản");

        Role role = roleRepository.findByName(roleName).orElseThrow(() -> new CommonMessageException("Role không tồn tại: " + roleName));

        for (User user : role.getUsers()) {
            user.getRoles().remove(role);
        }

        roleRepository.delete(role);
    }

    //    ==================== HELPER METHODS ====================
    public RoleResponse convertToRoleResponse(Role role) {
        return RoleResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .priority(role.getPriority())
                .isSystemRole(RoleEnum.isSystemRole(role.getName()))
                .userCount(roleRepository.countUsersByRoleId(role.getId()))
                .createdAt(role.getCreatedAt())
                .build();
    }

    //    Lấy danh sách roles theo priority range
    public List<RoleResponse> handleGetRolesByPriorityRange(Integer minPriority, Integer maxPriority) {
        return roleRepository.findByPriorityBetween(minPriority, maxPriority)
                .stream()
                .map(this::convertToRoleResponse)
                .collect(Collectors.toList());
    }

    //    Clone role (tạo role mới từ role có sẵn)
    public RoleResponse handleCloneRole(Long sourceRoleId, String newName) {
        Role source = handleFindById(sourceRoleId);

        String roleName = newName.toUpperCase().trim();
        if (!roleName.startsWith("ROLE_")) {
            roleName = "ROLE_" + roleName;
        }

        if (roleRepository.existsByName(roleName)) {
            throw new CommonMessageException("Role đã tồn tại: " + roleName);
        }

        Role newRole = Role.builder()
                .name(roleName)
                .description("Cloned from: " + source.getName() + ". " +
                        (source.getDescription() != null ? source.getDescription() : ""))
                .priority(source.getPriority() + 1) // Priority thấp hơn 1 bậc
                .build();

        Role saved = roleRepository.save(newRole);

        return convertToRoleResponse(saved);
    }
}
