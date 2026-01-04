package movieapp.controller;

import com.turkraft.springfilter.boot.Filter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import movieapp.dto.MetaAndHead.ResultPaginationDTO;
import movieapp.dto.Role.RoleCreateDTO;
import movieapp.dto.Role.RoleResponse;
import movieapp.entity.Role;
import movieapp.service.RoleService;
import movieapp.util.annotation.ApiMessage;
import movieapp.util.annotation.RequireAdmin;
import movieapp.util.annotation.RequireSuperAdmin;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@RequireAdmin
public class RoleController {
    private final RoleService roleService;

    @GetMapping("/roles")
    @ApiMessage("Lấy danh sách roles")
    public ResultPaginationDTO getAllRoles(@Filter Specification spec, Pageable pageable) {
        return roleService.handleGetAllRoles(spec, pageable);
    }

    @GetMapping("/roles/{id}")
    @ApiMessage("Lấy roles theo Id")
    public RoleResponse getRoleById(@PathVariable("id") Long id) {
        return roleService.convertToRoleResponse(roleService.handleFindById(id));
    }

    @GetMapping("/roles/by-priority")
    @RequireAdmin
    @ApiMessage("Lấy roles theo priority")
    public List<RoleResponse> getRolesByPriority(
            @RequestParam(defaultValue = "1") Integer minPriority,
            @RequestParam(defaultValue = "1000") Integer maxPriority) {
        return roleService.handleGetRolesByPriorityRange(minPriority, maxPriority);
    }

    @PutMapping("/default-roles")
    @ApiMessage("Lấy danh sách role mặc định của user")
    public Set<Role> getDefaultUserRole(@RequestBody Set<String> names) {
        return roleService.handleGetDefaultUserRole(names);
    }

    @PutMapping("/list-roles")
    @ApiMessage("Lấy danh sách role theo tên")
    public Set<Role> getManyRoleByName(@RequestBody Set<String> names) {
        return roleService.handleGetDefaultUserRole(names);
    }

    @GetMapping("/roles/{name}")
    @ApiMessage("Kiểm tra role có tồn tại")
    public boolean existRoleByName(@PathVariable String name) {
        return roleService.handleExits(name);
    }

    @PostMapping("/roles")
    @ApiMessage("tạo role mới")
    public ResponseEntity<RoleResponse> createNewRole(@Valid @RequestBody RoleCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roleService.handleCreateRole(dto));
    }

    @PutMapping("/roles")
    @ApiMessage("Cập nhật role")
    public Role updateRole(@RequestBody Role dto) {
        return roleService.handleUpdateRole(dto.getId(), dto.getName(), dto.getDescription(), dto.getPriority());
    }

    @PostMapping("/roles/{roleId}/clone")
    @RequireSuperAdmin
    @ApiMessage("Clone role")
    public ResponseEntity<RoleResponse> cloneRole(
            @PathVariable Long roleId,
            @RequestBody Map<String, String> body) {
        String newName = body.get("name");
        if (newName == null || newName.isBlank()) {
            throw new IllegalArgumentException("Tên role mới không được để trống");
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(roleService.handleCloneRole(roleId, newName));
    }

    @DeleteMapping("/roles/{roleName}")
    @ApiMessage("Xoá role")
    public void deleteRole(@PathVariable String roleName) {
        roleService.handleDeleteRole(roleName);
    }
}
