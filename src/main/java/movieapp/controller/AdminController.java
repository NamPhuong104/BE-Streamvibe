package movieapp.controller;

import com.turkraft.springfilter.boot.Filter;
import lombok.RequiredArgsConstructor;
import movieapp.dto.MetaAndHead.ResultPaginationDTO;
import movieapp.dto.User.ResUserDTO;
import movieapp.entity.User;
import movieapp.service.UserService;
import movieapp.util.annotation.ApiMessage;
import movieapp.util.annotation.RequireAdmin;
import movieapp.util.annotation.RequireSuperAdmin;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@RequireAdmin // Tất cả endpoints trong controller này cần ADMIN role
public class AdminController {
    private final UserService userService;

    @GetMapping("/users")
    @ApiMessage("Lấy danh sách users")
    public ResultPaginationDTO getAllUsers(@Filter Specification spec, Pageable pageable) {
        return userService.handleGetAllUser(spec, pageable);
    }

    @GetMapping("/users/{id}")
    @ApiMessage("Lấy thông tin user theo ID")
    public ResUserDTO getUserById(@PathVariable Long id) {
        User user = userService.handleGetUserById(id);
        return userService.convertToResUserDTO(user);
    }

    @PutMapping("/users/{id}/ban")
    @ApiMessage("Khóa tài khoản user")
    public ResUserDTO banUser(@PathVariable Long id) {
        return userService.handleBanUser(id);
    }

    @PutMapping("/users/{id}/unban")
    @ApiMessage("Mở khóa tài khoản user")
    public ResUserDTO unbanUser(@PathVariable Long id) {
        return userService.handleUnbanUser(id);
    }

    // ==================== ROLE MANAGEMENT (SUPER_ADMIN only) ====================

    @PutMapping("/users/{id}/roles")
    @RequireSuperAdmin // Override - chỉ SUPER_ADMIN mới được thay đổi roles
    @ApiMessage("Cập nhật roles cho user")
    public ResUserDTO updateUserRoles(@PathVariable("id") Long id, @RequestBody Set<String> roleNames) {
        return userService.handleUpdateUserRoles(id, roleNames);
    }

    @PutMapping("/users/{id}/add-role")
    @RequireSuperAdmin
    @ApiMessage("Thêm role cho user")
    public ResUserDTO addRoleToUser(@PathVariable Long id, @RequestParam String roleName) {
        return userService.handleAddRoleToUser(id, roleName);
    }

    @PutMapping("/users/{id}/remove-role")
    @RequireSuperAdmin
    @ApiMessage("Xóa role khỏi user")
    public ResUserDTO removeRoleFromUser(@PathVariable Long id, @RequestParam String roleName) {
        return userService.handleRemoveRoleFromUser(id, roleName);
    }

    // ==================== PREMIUM MANAGEMENT ====================
    @PutMapping("/users/{id}/upgrade-premium")
    @ApiMessage("Nâng cấp user lên Premium")
    public ResUserDTO upgradeToPremium(@PathVariable Long id) {
        return userService.handleUpgradeToPremium(id);
    }

    @PutMapping("/users/{id}/downgrade-premium")
    @ApiMessage("Hạ cấp user từ premium")
    public ResUserDTO downgradeFromPremium(@PathVariable Long id) {
        return userService.handleDowngradeFromPremium(id);
    }

    @GetMapping("/stats/users")
    @ApiMessage("Thống kê users")
    public ResponseEntity<?> getUserStats() {
        return ResponseEntity.ok(userService.handleGetUserStatistics());
    }
}
