package movieapp.service;

import lombok.RequiredArgsConstructor;
import movieapp.domain.User;
import movieapp.dto.MetaAndHead.ResultPaginationDTO;
import movieapp.dto.User.UseCreateDTO;
import movieapp.dto.User.UserUpdateDTO;
import movieapp.dto.User.ResUserDTO;
import movieapp.repository.UserRepository;
import movieapp.util.SecurityUtil;
import movieapp.util.Util;
import movieapp.util.error.IdInvalidException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final WatchHistoryService watchHistoryService;

    public ResUserDTO convertToResUserDTO(User user) {
        ResUserDTO res = new ResUserDTO();

        res.setId(user.getId());
        res.setFullName(user.getFullName());
        res.setEmail(user.getEmail());
        res.setIsActive(user.getIsActive());
        res.setAvatarUrl(user.getAvatarUrl());
        res.setUsername(user.getUsername());
        res.setProvider(user.getProvider());
        res.setProviderId(user.getProviderId());
        res.setIsEmailVerified(user.getIsEmailVerified());
        res.setCreatedAt(user.getCreatedAt());
        res.setUpdatedAt(user.getUpdatedAt());
        return res;
    }

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

    public boolean isExistEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public ResUserDTO handleFindUserByEmail(String email) throws IdInvalidException {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new
                IdInvalidException("Email " + email + " không tồn tại !!!"));
        return convertToResUserDTO(user);
    }

    public User handleFindUserByEmailEntity(String email) {
        Optional<User> currentUser = userRepository.findByEmail(email);
        if (currentUser.isPresent()) return currentUser.get();
        return null;
    }

    public User getUserByRefreshTokenAndEmail(String token, String email) {
        return userRepository.findByRefreshTokenAndEmail(token, email).orElse(null);
    }

    public void handleUpdateUserToken(String refresh_token, String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.OK));
        user.setRefreshToken(refresh_token);
        userRepository.save(user);
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
            User user = new User();
            user.setEmail(email);
            user.setUsername(name);
            user.setFullName(name);
            user.setAvatarUrl(picture);
            user.setProvider("GOOGLE");
            user.setProviderId(googleSub);
            user.setIsActive(true);
            user.setIsEmailVerified(true);
            user.setPassword(null);
            return userRepository.save(user);
        }
    }

    public void handleChangePassword(String email, String oldPassword, String newPassword) throws IdInvalidException {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new IdInvalidException("User với email " + email + " không tồn tại!"));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IdInvalidException("Mật khẩu cũ không chính xác");
        }

        String hashed = passwordEncoder.encode(newPassword);
        user.setPassword(hashed);
        userRepository.save(user);
    }

    public ResUserDTO handleCreateUser(UseCreateDTO userReq) throws IdInvalidException {
        boolean isEmailExist = isExistEmail(userReq.getEmail());
        if (isEmailExist)
            throw new IdInvalidException("Email " + userReq.getEmail() + " đã tồn tại, vui lòng sử dụng email khác");

        User newUser = new User();
        String hashPassword = passwordEncoder.encode(userReq.getPassword());

        newUser.setEmail(userReq.getEmail());
        newUser.setUsername(userReq.getUsername());
        newUser.setFullName(userReq.getFullName());
        newUser.setAvatarUrl(userReq.getAvatarUrl());
        newUser.setPassword(hashPassword);

        newUser.setProvider("LOCAL");
        newUser.setIsActive(true);
        newUser.setIsEmailVerified(false);

        String tokenOtp = Util.generateOtp();

        newUser.setVerifyEmailToken(tokenOtp);
        newUser.setVerifyEmailExpiry(LocalDateTime.now().plusHours(24));

        userRepository.save(newUser);
        String displayName = newUser.getFullName() != null ? newUser.getFullName() : newUser.getUsername();
        emailService.sendActiveEmail(newUser.getEmail(), displayName, tokenOtp);

        return convertToResUserDTO(newUser);
    }

    public User handleGetUserById(long id) throws IdInvalidException {
        User user = userRepository.findById(id).orElseThrow(() -> new IdInvalidException("User không tồn tại với id: " + id));
        return user;
    }

    public ResUserDTO handleUpdateUser(long id, UserUpdateDTO userReq) throws IdInvalidException {
        User existingUser = handleGetUserById(id);

        if (existingUser != null) {
            existingUser.setUsername(userReq.getUsername());
            existingUser.setAvatarUrl(userReq.getAvatarUrl());
            existingUser.setFullName(userReq.getFullName());
        }
        userRepository.save(existingUser);
        return convertToResUserDTO(existingUser);
    }

    public void handleDeleteUser(Long id) throws IdInvalidException {
        User existUser = handleGetUserById(id);

        if (existUser == null)
            throw new IdInvalidException("User với id:  " + id + " không tồn tại !!!!!");

        watchHistoryService.handleDeleteAllWatchHistoryByUserId(id);
        userRepository.deleteById(id);
    }

    public void handleForgotPassword(String email) throws IdInvalidException {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new IdInvalidException("Email " + email + " không tồn tại!"));

        String tokenOtp = Util.generateOtp();

        user.setResetPasswordToken(tokenOtp);
        user.setResetPasswordExpiry(LocalDateTime.now().plusMinutes(5));
        userRepository.save(user);
        String displayName = user.getFullName() != null ? user.getFullName() : user.getUsername();

        emailService.sendResetPasswordEmail(user.getEmail(), displayName, tokenOtp);
    }

    public void handleResetPassword(String token, String newPassword) throws IdInvalidException {
        User user = userRepository.findByResetPasswordToken(token).orElseThrow(() -> new IdInvalidException("Token reset password không hợp lệ"));

        if (user.getResetPasswordExpiry() == null || user.getResetPasswordExpiry().isBefore(LocalDateTime.now()))
            throw new IdInvalidException("Token reset password đã hết hạn");

        String hashed = passwordEncoder.encode(newPassword);
        user.setPassword(hashed);
        user.setResetPasswordToken(null);
        user.setResetPasswordExpiry(null);

        userRepository.save(user);
    }

    public void handleVerifyEmail(String token) throws IdInvalidException {
        User user = userRepository.findByVerifyEmailToken(token).orElseThrow(() -> new IdInvalidException("Token xác thực email không hợp lệ"));
        if (user.getVerifyEmailExpiry() == null || user.getVerifyEmailExpiry().isBefore(LocalDateTime.now()))
            throw new IdInvalidException("Token xác thực email đã hết hạn");

        user.setIsEmailVerified(true);
        user.setVerifyEmailToken(null);
        user.setVerifyEmailExpiry(null);

        userRepository.save(user);
    }

    public void handleResendVerifyEmail(String email) throws IdInvalidException {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new IdInvalidException("User với email " + email + " không tồn tại!"));
        if (user.getIsEmailVerified() == true) throw new IdInvalidException("Tài khoản đã kích hoạt email");

        String tokenOtp = Util.generateOtp();
        user.setVerifyEmailToken(tokenOtp);
        user.setVerifyEmailExpiry(LocalDateTime.now().plusMinutes(5));

        userRepository.save(user);

        String displayName = user.getFullName() != null ? user.getFullName() : user.getUsername();
        emailService.sendActiveEmail(email, displayName, tokenOtp);
    }

    public void handleChangeEmail(String currentEmail, String newEmail, String currentPassword) throws IdInvalidException {
        User user = userRepository.findByEmail(currentEmail).orElseThrow(() -> new IdInvalidException("User với email " + currentEmail + " không tồn tại!"));

        if (!"LOCAL".equalsIgnoreCase(user.getProvider()))
            throw new IdInvalidException("Tài khoản đăng nhập bằng " + user.getProvider() + " không thể đổi email tại đây");

        if (!passwordEncoder.matches(currentPassword, user.getPassword()))
            throw new IdInvalidException("Mật khẩu hiện tại không chính xác");
        if (currentEmail.equalsIgnoreCase(newEmail)) throw new IdInvalidException("Email mới phải khác email hiện tại");
        if (userRepository.existsByEmail(newEmail))
            throw new IdInvalidException("Email " + newEmail + " đã tồn tại, vui lòng dùng email khác");

        String tokenOtp = Util.generateOtp();
        user.setPendingEmail(newEmail);
        user.setChangeEmailToken(tokenOtp);
        user.setChangeEmailExpiry(LocalDateTime.now().plusHours(24));

        userRepository.save(user);

        String displayName = user.getFullName() != null ? user.getFullName() : user.getUsername();
        emailService.sendWarningEmail(currentEmail, displayName, currentEmail, newEmail);
        emailService.sendChangeEmail(newEmail, displayName, currentEmail, newEmail, tokenOtp);
    }

    public void handleConfirmChangeEmail(String token) throws IdInvalidException {
        User user = userRepository.findByChangeEmailToken(token).orElseThrow(() -> new IdInvalidException("Token đổi email không hợp lệ"));

        if (user.getChangeEmailExpiry() == null || user.getChangeEmailExpiry().isBefore(LocalDateTime.now()))
            throw new IdInvalidException("Token đổi email đã hết hạn");

        if (user.getPendingEmail() == null || user.getPendingEmail().isBlank()) {
            throw new IdInvalidException("Không tìm thấy email mới để cập nhật");
        }

        String newEmail = user.getPendingEmail();
        if (userRepository.existsByEmail(newEmail) && !newEmail.equalsIgnoreCase(user.getEmail())) {
            throw new IdInvalidException("Email " + newEmail + " đã tồn tại, không thể đổi");
        }

        user.setEmail(newEmail);
        user.setPendingEmail(null);
        user.setChangeEmailExpiry(null);
        user.setChangeEmailToken(null);
        user.setIsEmailVerified(true);

        userRepository.save(user);
    }

    //    HELPER METHOD
    public User getCurrentUser() throws IdInvalidException {
        String email = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new IdInvalidException("Bạn chưa đăng nhập"));

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IdInvalidException("Không tìm thấy user"));
    }
}
