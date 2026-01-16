package movieapp.controller;

import com.turkraft.springfilter.boot.Filter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import movieapp.dto.MetaAndHead.ResultPaginationDTO;
import movieapp.dto.Role.RoleCreateDTO;
import movieapp.dto.Role.RoleResponse;
import movieapp.entity.Role;
import movieapp.entity.User;
import movieapp.service.RoleService;
import movieapp.service.UserService;
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

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@RequireSuperAdmin
public class RoleController {
    private final RoleService roleService;
    private final UserService userService;

    @GetMapping("/roles")
    @ApiMessage("Lấy danh sách roles")
    public ResultPaginationDTO getAllRoles(@Filter Specification spec, Pageable pageable) {
        return roleService.handleGetAllRoles(spec, pageable);
    }

    @GetMapping("/roles/all")
    @ApiMessage("Lấy tất cả roles (sorted by priority)")
    public List<RoleResponse> getAllRolesSorted() {
        return roleService.handleGetAllRolesSorted();
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
            @RequestParam(defaultValue = "0") Integer minPriority,
            @RequestParam(defaultValue = "1000") Integer maxPriority) {
        return roleService.handleGetRolesByPriorityRange(minPriority, maxPriority);
    }

    /**
     * Lấy danh sách roles có thể assign cho user
     * (Chỉ trả về roles có priority thấp hơn current user)
     */
    @GetMapping("/roles/assignable")
    @ApiMessage("Lấy danh sách roles có thể assign")
    public List<RoleResponse> getAssignableRoles() {
        User currentUser = userService.getCurrentUser();
        return roleService.handleGetAssignableRoles(currentUser);
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
    public ResponseEntity<RoleResponse> cloneRole(@PathVariable Long roleId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roleService.handleCloneRole(roleId));
    }

    @DeleteMapping("/roles/{roleName}")
    @ApiMessage("Xoá role")
    public void deleteRole(@PathVariable String roleName) {
        roleService.handleDeleteRole(roleName);
    }
}
