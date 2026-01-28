package movieapp.service;

import lombok.RequiredArgsConstructor;
import movieapp.dto.MetaAndHead.ResultPaginationDTO;
import movieapp.dto.User.ResUserDTO;
import movieapp.dto.User.UserCreateDTO;
import movieapp.dto.User.UserUpdateDTO;
import movieapp.entity.Role;
import movieapp.entity.User;
import movieapp.exception.CommonMessageException;
import movieapp.repository.UserRepository;
import movieapp.util.SecurityUtil;
import movieapp.util.UsernameGenerator;
import movieapp.util.Util;
import movieapp.util.constant.RoleEnum;
import movieapp.util.constant.ValidationConstant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final WatchHistoryService watchHistoryService;
    private final RoleService roleService;

    public ResUserDTO convertToResUserDTO(User user) {
        String roleName = user.getRoleName();
        Integer rolePriority = user.getRolePriority();

        return ResUserDTO.builder().id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .provider(user.getProvider())
                .providerId(user.getProviderId())
                .isActive(user.getIsActive())
                .isEmailVerified(user.getIsEmailVerified())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .role(roleName)
                .rolePriority(rolePriority)
                .hasPassword(user.getPassword() != null && !user.getPassword().isEmpty())
                .build();
    }

    public boolean isExistEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public ResUserDTO handleFindUserByEmail(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new
                CommonMessageException("Email " + email + " không tồn tại !!!"));
        return convertToResUserDTO(user);
    }

    public User handleFindUserByEmailEntity(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    public User getUserByRefreshTokenAndEmail(String token, String email) {
        return userRepository.findByRefreshTokenAndEmail(token, email).orElse(null);
    }

    public void handleUpdateUserToken(String refresh_token, String email) {
        userRepository.updateRefreshTokenByEmail(refresh_token, email);
    }

    public User findOrCreateGoogleUser(String email, String name, String picture, String googleSub) {
        Optional<User> opt = userRepository.findByEmail(email);
        if (opt.isPresent()) {
            User user = opt.get();
            user.setFullName(name);
            user.setAvatarUrl(picture);
            user.setProvider("GOOGLE");
            user.setProviderId(googleSub);
            user.setIsEmailVerified(true);
            return userRepository.save(user);
        } else {
            Role userRole = roleService.getDefaultUserRole();
            String username = generateUniqueUsername(name);

            User user = User.builder()
                    .email(email)
                    .username(username)
                    .fullName(name)
                    .avatarUrl(picture)
                    .provider("GOOGLE")
                    .providerId(googleSub)
                    .isActive(true)
                    .isEmailVerified(true)
                    .password(null)
                    .role(userRole)
                    .build();
            return userRepository.save(user);
        }
    }

    public void handleChangePassword(String email, String oldPassword, String newPassword) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new CommonMessageException("User với email " + email + " không tồn tại!"));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new CommonMessageException("Mật khẩu cũ không chính xác");
        }

        String hashed = passwordEncoder.encode(newPassword);
        user.setPassword(hashed);
        userRepository.save(user);
    }

    public User handleFindUserByEmailOrUsername(String emailOrUsername) {
        return userRepository.findByEmailOrUsername(emailOrUsername, emailOrUsername).orElse(null);
    }

    public ResultPaginationDTO handleSearchUserByEmailOrUsername(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<User> pageUsers = userRepository.searchByEmailOrUsername(query, pageable);

        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();

        mt.setPage(page);
        mt.setPageSize(size);
        mt.setPages(pageUsers.getTotalPages());
        mt.setTotal(pageUsers.getTotalElements());

        rs.setMeta(mt);

        List<ResUserDTO> listUser = pageUsers.getContent().stream().map(this::convertToResUserDTO).collect(Collectors.toList());

        rs.setResult(listUser);

        return rs;
    }

    public Boolean handleFindByUserName(String username) {
        return userRepository.existsByUsername(username);
    }

    public ResUserDTO handleCreateUser(UserCreateDTO userReq) {
        validateUsername(userReq.getUsername());

        if (isExistEmail(userReq.getEmail()))
            throw new CommonMessageException("Email " + userReq.getEmail() + " đã tồn tại, vui lòng sử dụng email khác");

        if (handleFindByUserName(userReq.getUsername()))
            throw new CommonMessageException("Username " + userReq.getUsername() + " đã tồn tại, vui lòng sử dụng username khác");

        Role currentRole;

        if (userReq.getRoleId() != null) {
            currentRole = roleService.handleFindById(userReq.getRoleId());
        } else {
            currentRole = roleService.getDefaultUserRole();
        }

        User newUser = User.builder()
                .email(userReq.getEmail())
                .username(userReq.getUsername())
                .fullName(userReq.getFullName())
                .avatarUrl(userReq.getAvatarUrl())
                .password(passwordEncoder.encode(userReq.getPassword()))
                .provider("LOCAL")
                .isActive(true)
                .isEmailVerified(false)
                .role(currentRole)
                .build();

        String tokenOtp = Util.generateOtp();

        newUser.setVerifyEmailToken(tokenOtp);
        newUser.setVerifyEmailExpiry(LocalDateTime.now().plusHours(24));

        userRepository.save(newUser);
        String displayName = newUser.getFullName() != null ? newUser.getFullName() : newUser.getUsername();
        emailService.sendActiveEmail(newUser.getEmail(), displayName, tokenOtp);

        return convertToResUserDTO(newUser);
    }

    public User handleGetUserById(long id) {
        return userRepository.findById(id).orElseThrow(() -> new CommonMessageException("User không tồn tại với id: " + id));
    }

    public ResUserDTO handleUpdateUser(long id, UserUpdateDTO userReq) {
        User existingUser = handleGetUserById(id);

        if (userReq.getUsername() != null) {
            // Validate username mới
            validateUsername(userReq.getUsername());

            // Check không phải username hiện tại và đã tồn tại
            if (handleFindByUserName(userReq.getUsername())) {
                throw new CommonMessageException("Username " + userReq.getUsername() + " đã tồn tại, vui lòng sử dụng username khác");
            } else {
                existingUser.setUsername(userReq.getUsername());
            }

            if (userReq.getAvatarUrl() != null) existingUser.setAvatarUrl(userReq.getAvatarUrl());
            if (userReq.getFullName() != null) existingUser.setFullName(userReq.getFullName());
            if (userReq.getRoleId() != null) {
                Role currentRole = roleService.handleFindById(userReq.getRoleId());
                existingUser.setRole(currentRole);
            }
        }
        userRepository.save(existingUser);
        return convertToResUserDTO(existingUser);
    }

    public ResUserDTO handleUpdateEmail(long id) {
        User ex = handleGetUserById(id);
        if (ex != null) {
            ex.setIsEmailVerified(true);
            ex.setVerifyEmailToken(null);
            ex.setVerifyEmailExpiry(null);
        }
        userRepository.save(ex);
        return convertToResUserDTO(ex);
    }

    public void handleDeleteUser(Long id) {
        User existUser = handleGetUserById(id);

        if (existUser == null)
            throw new CommonMessageException("User với id:  " + id + " không tồn tại !!!!!");

        watchHistoryService.handleDeleteAllWatchHistoryByUserId(id);
        userRepository.deleteById(id);
    }

    public void handleForgotPassword(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new CommonMessageException("Email " + email + " không tồn tại!"));

        String tokenOtp = Util.generateOtp();

        user.setResetPasswordToken(tokenOtp);
        user.setResetPasswordExpiry(LocalDateTime.now().plusMinutes(5));
        userRepository.save(user);
        String displayName = user.getFullName() != null ? user.getFullName() : user.getUsername();

        emailService.sendResetPasswordEmail(user.getEmail(), displayName, tokenOtp);
    }

    public void handleResetPassword(String token, String newPassword) {
        User user = userRepository.findByResetPasswordToken(token).orElseThrow(() -> new CommonMessageException("Token reset password không hợp lệ"));

        if (user.getResetPasswordExpiry() == null || user.getResetPasswordExpiry().isBefore(LocalDateTime.now()))
            throw new CommonMessageException("Token reset password đã hết hạn");

        String hashed = passwordEncoder.encode(newPassword);
        user.setPassword(hashed);
        user.setResetPasswordToken(null);
        user.setResetPasswordExpiry(null);

        userRepository.save(user);
    }

    public void handleVerifyEmail(String token) {
        User user = userRepository.findByVerifyEmailToken(token).orElseThrow(() -> new CommonMessageException("Token xác thực email không hợp lệ"));
        if (user.getVerifyEmailExpiry() == null || user.getVerifyEmailExpiry().isBefore(LocalDateTime.now()))
            throw new CommonMessageException("Token xác thực email đã hết hạn");

        user.setIsEmailVerified(true);
        user.setVerifyEmailToken(null);
        user.setVerifyEmailExpiry(null);

        userRepository.save(user);
    }

    public void handleResendVerifyEmail(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new CommonMessageException("User với email " + email + " không tồn tại!"));
        if (user.getIsEmailVerified() == true) throw new CommonMessageException("Tài khoản đã kích hoạt email");

        String tokenOtp = Util.generateOtp();
        user.setVerifyEmailToken(tokenOtp);
        user.setVerifyEmailExpiry(LocalDateTime.now().plusMinutes(5));

        userRepository.save(user);

        String displayName = user.getFullName() != null ? user.getFullName() : user.getUsername();
        emailService.sendActiveEmail(email, displayName, tokenOtp);
    }

    public void handleChangeEmail(String currentEmail, String newEmail, String currentPassword) {
        User user = userRepository.findByEmail(currentEmail).orElseThrow(() -> new CommonMessageException("User với email " + currentEmail + " không tồn tại!"));

        if (!"LOCAL".equalsIgnoreCase(user.getProvider()))
            throw new CommonMessageException("Tài khoản đăng nhập bằng " + user.getProvider() + " không thể đổi email tại đây");

        if (!passwordEncoder.matches(currentPassword, user.getPassword()))
            throw new CommonMessageException("Mật khẩu hiện tại không chính xác");
        if (currentEmail.equalsIgnoreCase(newEmail))
            throw new CommonMessageException("Email mới phải khác email hiện tại");
        if (userRepository.existsByEmail(newEmail))
            throw new CommonMessageException("Email " + newEmail + " đã tồn tại, vui lòng dùng email khác");

        String tokenOtp = Util.generateOtp();
        user.setPendingEmail(newEmail);
        user.setChangeEmailToken(tokenOtp);
        user.setChangeEmailExpiry(LocalDateTime.now().plusHours(24));

        userRepository.save(user);

        String displayName = user.getFullName() != null ? user.getFullName() : user.getUsername();
        emailService.sendWarningEmail(currentEmail, displayName, currentEmail, newEmail);
        emailService.sendChangeEmail(newEmail, displayName, currentEmail, newEmail, tokenOtp);
    }

    public void handleConfirmChangeEmail(String token) {
        User user = userRepository.findByChangeEmailToken(token).orElseThrow(() -> new CommonMessageException("Token đổi email không hợp lệ"));

        if (user.getChangeEmailExpiry() == null || user.getChangeEmailExpiry().isBefore(LocalDateTime.now()))
            throw new CommonMessageException("Token đổi email đã hết hạn");

        if (user.getPendingEmail() == null || user.getPendingEmail().isBlank()) {
            throw new CommonMessageException("Không tìm thấy email mới để cập nhật");
        }

        String newEmail = user.getPendingEmail();
        if (userRepository.existsByEmail(newEmail) && !newEmail.equalsIgnoreCase(user.getEmail())) {
            throw new CommonMessageException("Email " + newEmail + " đã tồn tại, không thể đổi");
        }

        user.setEmail(newEmail);
        user.setPendingEmail(null);
        user.setChangeEmailExpiry(null);
        user.setChangeEmailToken(null);
        user.setIsEmailVerified(true);

        userRepository.save(user);
    }

    public void handleCreatePassword(String newPassword, String confirmPassword, String token) {
        User user = getCurrentUser();

        // Check nếu user đã có password
        if (user.getPassword() != null && !user.getPassword().isEmpty())
            throw new CommonMessageException("Tài khoản đã có mật khẩu. Vui lòng sử dụng chức năng đổi mật khẩu.");

        // Check provider
        if ("LOCAL".equalsIgnoreCase(user.getProvider()))
            throw new CommonMessageException("Tài khoản LOCAL phải có mật khẩu");

        // Validate password match
        if (!newPassword.equals(confirmPassword))
            throw new CommonMessageException("Mật khẩu xác nhận không khớp");

        // Validate password length
        if (newPassword.length() < ValidationConstant.PASSWORD_MIN_LENGTH)
            throw new CommonMessageException(
                    "Mật khẩu phải có ít nhất " + ValidationConstant.PASSWORD_MIN_LENGTH + " ký tự"
            );

        if (newPassword.length() > ValidationConstant.PASSWORD_MAX_LENGTH)
            throw new CommonMessageException(
                    "Mật khẩu không được quá " + ValidationConstant.PASSWORD_MAX_LENGTH + " ký tự"
            );

        if (token.length() < 0 || token.isEmpty()) throw new CommonMessageException("Token không được để trống");

        if (user.getResetPasswordExpiry() == null || user.getResetPasswordExpiry().isBefore(LocalDateTime.now()))
            throw new CommonMessageException("Token reset password đã hết hạn");

        // Encode và save
        String hashed = passwordEncoder.encode(newPassword);
        user.setPassword(hashed);
        user.setResetPasswordToken(null);
        user.setResetPasswordExpiry(null);

        userRepository.save(user);
    }

    // ==================== ROLE MANAGEMENT ====================
    public ResultPaginationDTO handleGetAllUser(Specification<User> spec, Pageable pageable) {
        Page<User> pageUser = userRepository.findAll(spec, pageable);
        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();

        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());

        mt.setPages(pageUser.getTotalPages());
        mt.setTotal(pageUser.getTotalElements());

        rs.setMeta(mt);
        rs.setResult(pageUser.getContent());

        List<ResUserDTO> listUser = pageUser.getContent().stream().map(item -> convertToResUserDTO(item)).collect(Collectors.toList());
        rs.setResult(listUser);

        return rs;
    }

    public ResUserDTO handleUpdateUserRoles(Long userId, String roleName) {
        User user = handleGetUserById(userId);
        User currentUser = getCurrentUser();

        // Validation
        if (user.getId().equals(currentUser.getId()))
            throw new CommonMessageException("Không thể thay đổi role của chính mình");

        if (user.isSuperAdmin() && !currentUser.isSuperAdmin())
            throw new CommonMessageException("Không có quyền thay đổi role của Super Admin");

        // Kiểm tra current user có quyền assign role này không
        Role newRole = roleService.handleFindByName(roleName);
        if (!currentUser.canManageRole(newRole)) {
            throw new CommonMessageException("Bạn không có quyền assign role: " + roleName);
        }

        // Kiểm tra current user có quyền cao hơn target user không
        if (!currentUser.hasPrivilegeOver(user)) {
            throw new CommonMessageException("Bạn không có quyền thay đổi role của user này");
        }

        // ⭐ Set single role
        user.setRole(newRole);
        userRepository.save(user);

        return convertToResUserDTO(user);
    }

    public ResUserDTO handleUpgradeToPremium(Long userId) {
        User user = handleGetUserById(userId);

        // Chỉ upgrade nếu user đang là ROLE_USER
        if (!user.hasRole(RoleEnum.ROLE_USER.getName())) {
            throw new CommonMessageException("Chỉ có thể upgrade user thường lên Premium");
        }

        Role premiumRole = roleService.handleFindByName(RoleEnum.ROLE_PREMIUM.getName());
        user.setRole(premiumRole);
        userRepository.save(user);

        return convertToResUserDTO(user);
    }

    public ResUserDTO handleDowngradeFromPremium(Long userId) {
        User user = handleGetUserById(userId);

        // Chỉ downgrade nếu user đang là ROLE_PREMIUM
        if (!user.hasRole(RoleEnum.ROLE_PREMIUM.getName())) {
            throw new CommonMessageException("User không phải Premium");
        }

        Role userRole = roleService.getDefaultUserRole();
        user.setRole(userRole);
        userRepository.save(user);

        return convertToResUserDTO(user);
    }

    public ResUserDTO handleBanUser(Long userId) {
        User user = handleGetUserById(userId);
        User currentUser = getCurrentUser();

        // Không cho phép ban Super Admin
        if (user.isSuperAdmin()) {
            throw new CommonMessageException("Không thể khóa tài khoản Super Admin");
        }

        // Không cho phép ban chính mình
        if (user.getId().equals(currentUser.getId())) {
            throw new CommonMessageException("Không thể khóa tài khoản của chính mình");
        }

        user.setIsActive(false);
        user.setRefreshToken(null); // Clear session
        userRepository.save(user);

        return convertToResUserDTO(user);
    }

    public ResUserDTO handleUnbanUser(Long userId) {
        User user = handleGetUserById(userId);
        user.setIsActive(true);
        userRepository.save(user);

        return convertToResUserDTO(user);
    }

    public Map<String, Object> handleGetUserStatistics() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalUsers", userRepository.count());
        stats.put("activeUsers", userRepository.countByIsActiveTrue());
        stats.put("verifiedUsers", userRepository.countByIsEmailVerifiedTrue());
        // Thêm các thống kê khác nếu cần

        return stats;
    }

    //    HELPER METHOD
    public User getCurrentUser() {
        String email = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new CommonMessageException("Bạn chưa đăng nhập"));

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new CommonMessageException("Không tìm thấy user"));
    }

    /**
     * ⭐ Generate unique username, retry nếu trùng
     */
    private String generateUniqueUsername(String fullName) {
        int maxAttempts = 10;

        for (int i = 0; i < maxAttempts; i++) {
            String username = UsernameGenerator.generateFromName(fullName);
            if (!userRepository.existsByUsername(username)) {
                return username;
            }
        }

        return "user_" + System.currentTimeMillis();
    }

    /**
     * ⭐ Validate username với pattern đồng bộ FE
     */
    public void validateUsername(String username) {
        if (username == null || username.isBlank()) throw new CommonMessageException("Username không được để trống");
        if (username.length() < ValidationConstant.USERNAME_MIN_LENGTH)
            throw new CommonMessageException("Username phải có ít nhất " + ValidationConstant.USERNAME_MIN_LENGTH + " ký tự");
        if (username.length() > ValidationConstant.USERNAME_MAX_LENGTH)
            throw new CommonMessageException("Username không được quá " + ValidationConstant.USERNAME_MIN_LENGTH + " ký tự");
        if (!UsernameGenerator.isValidUserName(username))
            throw new CommonMessageException(ValidationConstant.USERNAME_MESSAGE);
    }
}
