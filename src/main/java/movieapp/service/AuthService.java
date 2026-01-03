package movieapp.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import lombok.RequiredArgsConstructor;
import movieapp.entity.User;
import movieapp.dto.Auth.ReqLoginDTO;
import movieapp.dto.Auth.ResLoginDTO;
import movieapp.dto.User.LoginResult;
import movieapp.dto.User.ResUserDTO;
import movieapp.dto.User.UserCreateDTO;
import movieapp.exception.CommonMessageException;
import movieapp.exception.ProviderPasswordNotFound;
import movieapp.util.SecurityUtil;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final SecurityUtil securityUtil;
    private final UserService userService;

    public ResUserDTO handleCreateUser(UserCreateDTO data) {
        return userService.handleCreateUser(data);
    }

    public LoginResult handleLogin(ReqLoginDTO dto) {
        // Lấy thông tin user từ DB
        User currentUser = userService.handleFindUserByEmailEntity(dto.getUsername());
        if (currentUser == null)
            throw new UsernameNotFoundException("User không tồn tại");

        if ("GOOGLE".equals(currentUser.getProvider()) && currentUser.getPassword() == null || currentUser.getPassword().isEmpty()) {
            throw new ProviderPasswordNotFound("Tài khoản chưa tạo mật khẩu, vui lòng đăng nhập bằng Google hoặc tạo mật khẩu trong cài đặt");
        }

        // 1. Nạp username/password vào Security để check
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(dto.getUsername(), dto.getPassword());
        // 2. Xác thực (Sẽ gọi UserDetailsCustom để check DB)
        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
        // 3. Nếu đúng pass, lưu thông tin vào Context
        SecurityContextHolder.getContext().setAuthentication(authentication);
        // 4. Tạo Token trả về
        return buildLoginResult(currentUser, authentication.getName());
    }

    public LoginResult handleLoginWithGoogle(GoogleIdToken idToken) {
        GoogleIdToken.Payload payload = idToken.getPayload();
        String email = payload.getEmail();
        String name = (String) payload.get("name");
        String picture = (String) payload.get("picture");
        String sub = payload.getSubject();
        User user = userService.findOrCreateGoogleUser(email, name, picture, sub);

        return buildLoginResult(user, email);
    }

    public ResUserDTO handleGetAccount(String email) {
        User currentUser = userService.handleFindUserByEmailEntity(email);
        if (currentUser == null) throw new CommonMessageException("User không tồn tại !!!");
        if (currentUser.getRefreshToken() == null)
            throw new CommonMessageException("Phiên đăng nhập đã kết thúc, vui lòng đăng nhập lại");

        return userService.convertToResUserDTO(currentUser);
    }

    public LoginResult handleRefreshToken(String refreshToken, String email) {
        User currentUser = userService.getUserByRefreshTokenAndEmail(refreshToken, email);

        return buildLoginResult(currentUser, email);
    }

    public void handleLogout(String refreshToken) {
        // 1. Check valid refresh token
        Jwt decoded;
        try {
            decoded = securityUtil.checkValidRefreshToken(refreshToken);
        } catch (Exception e) {
            throw new CommonMessageException("Refresh token không hợp lệ hoặc đã hết hạn");
        }
        String email = decoded.getSubject();

        // 2. Check trong DB
        User currentUser = userService.getUserByRefreshTokenAndEmail(refreshToken, email);
        if (currentUser == null) throw new CommonMessageException("Refresh token không hợp lệ");

        // 3. Xóa refresh_token khỏi DB
        userService.handleUpdateUserToken(null, email);
    }

    public void handleChangePassword(String email, String oldPassword, String newPassword) {
        userService.handleChangePassword(email, oldPassword, newPassword);
    }

    public void handleForgotPassword(String email) {
        userService.handleForgotPassword(email);
    }

    public void handleResetPassword(String token, String newPassword) {
        userService.handleResetPassword(token, newPassword);
    }

    public void handleVerifyEmail(String token) {
        userService.handleVerifyEmail(token);
    }

    public void handleResendVerifyEmail(String email) {
        userService.handleResendVerifyEmail(email);
    }

    public void handleChangeEmail(String currentEmail, String newEmail, String currentPassword) {
        userService.handleChangeEmail(currentEmail, newEmail, currentPassword);
    }

    public void handleConfirmChangeEmail(String token) {
        userService.handleConfirmChangeEmail(token);
    }

    private LoginResult buildLoginResult(User user, String email) {
        ResLoginDTO.UserLogin userLogin = ResLoginDTO.UserLogin.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role("ROLE_USER")
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .provider(user.getProvider())
                .providerId(user.getProviderId())
                .isActive(user.getIsActive())
                .isEmailVerified(user.getIsEmailVerified())
                .build();

        ResLoginDTO res = new ResLoginDTO();
        res.setUser(userLogin);

        String accessToken = securityUtil.createAccessToken(email, res);
        res.setAccessToken(accessToken);

        String refreshToken = securityUtil.createRefreshToken(email, res);

        userService.handleUpdateUserToken(refreshToken, email);

        return LoginResult.builder()
                .loginResponse(res)
                .refreshToken(refreshToken)
                .build();
    }
}
