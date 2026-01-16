package movieapp.controller;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import movieapp.config.AppConfig;
import movieapp.dto.Auth.*;
import movieapp.dto.User.LoginResult;
import movieapp.dto.User.ResUserDTO;
import movieapp.dto.User.UserCreateDTO;
import movieapp.exception.CommonMessageException;
import movieapp.exception.PartnerAuthenticationException;
import movieapp.exception.ProviderPasswordNotFound;
import movieapp.service.AuthService;
import movieapp.service.UserService;
import movieapp.util.SecurityUtil;
import movieapp.util.annotation.ApiMessage;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final SecurityUtil securityUtil;
    private final UserService userService;
    private final AppConfig app;

    @PostMapping("/register")
    @ApiMessage("Đăng ký tài khoản thành công")
    public ResponseEntity<ResUserDTO> create(@Valid @RequestBody UserCreateDTO createDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.handleCreateUser(createDto));
    }

    @PostMapping("/login")
    @ApiMessage("Login Successfully")
    public ResponseEntity<ResLoginDTO> login(@Valid @RequestBody ReqLoginDTO loginDTO) {
        return buildLoginResponse(authService.handleLogin(loginDTO));
    }

    @PostMapping("/google")
    @ApiMessage("Đăng nhập với Google")
    public ResponseEntity<?> loginWithGoogle(@Valid @RequestBody GoogleLoginDTO request) {
        GoogleIdToken idToken = verifyGoogleToken(request.getIdToken());
        if (idToken == null) throw new CommonMessageException("Google token không hợp lệ");
        LoginResult result = authService.handleLoginWithGoogle(idToken);

        return buildLoginResponse(result);
    }

    @GetMapping("/account")
    @ApiMessage("Lấy thông tin tài khoản thành công")
    public ResUserDTO getAccount() {
        String email = SecurityUtil.getCurrentUserLogin().orElse(null);
        if (email == null) throw new CommonMessageException("Bạn chưa đăng nhập hoặc token không hợp lệ");

        return authService.handleGetAccount(email);
    }

    @GetMapping("/refresh")
    @ApiMessage("Refresh Access Token")
    public ResponseEntity<?> refreshToken(@CookieValue(name = "refresh_token", defaultValue = "") String
                                                  refreshToken) {
        if (refreshToken == null || refreshToken.isEmpty())
            throw new CommonMessageException("Bạn không có refresh token trong cookie");

        // 1. Check token hợp lệ về mặt chữ ký + hạn (exp)
        Jwt decoded;
        try {
            decoded = securityUtil.checkValidRefreshToken(refreshToken);
        } catch (Exception e) {
            throw new CommonMessageException("Refresh token không hợp lệ hoặc đã hết hạn");
        }
        String email = decoded.getSubject();

        // 2. Kiểm tra user có đúng refresh token này không
        LoginResult result = authService.handleRefreshToken(refreshToken, email);

        return buildLoginResponse(result);
    }

    @PostMapping("/logout")
    @ApiMessage("Đăng xuất thành công")
    public ResponseEntity<Void> logout(@CookieValue(name = "refresh_token", defaultValue = "") String refreshToken) {
        if (refreshToken == null || refreshToken.isEmpty())
            throw new CommonMessageException("Bạn không có refresh token trong cookie");
        authService.handleLogout(refreshToken);

        //  Xóa cookie trên browser
        ResponseCookie clearCookie = ResponseCookie.from("refresh_token", null)
                .maxAge(0).path("/").build();

        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, clearCookie.toString()).body(null);
    }

    @PostMapping("/change-password")
    @ApiMessage("Đổi mật khẩu thành công")
    public void changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        String email = SecurityUtil.getCurrentUserLogin().orElse(null);
        if (email == null) throw new ProviderPasswordNotFound("Bạn chưa đăng nhập hoặc token không hợp lệ");

        authService.handleChangePassword(email, dto.getOldPassword(), dto.getNewPassword());
    }

    @PostMapping("/forgot-password")
    @ApiMessage("Yêu cầu đặt lại mật khẩu thành công")
    public void forgotPassword(@Valid @RequestBody ForgotPasswordDTO dto) {
        authService.handleForgotPassword(dto.getEmail());
    }

    @PostMapping("/reset-password")
    @ApiMessage("Đặt lại mật khẩu thành công")
    public void resetPassword(@Valid @RequestBody ResetPasswordDTO dto) {
        authService.handleResetPassword(dto.getToken(), dto.getNewPassword());
    }

    @GetMapping("/verify-email")
    @ApiMessage("Xác thực email thành công")
    public void verifyEmail(@Valid @RequestParam("token") String token) {
        authService.handleVerifyEmail(token);
    }

    @PostMapping("/resend-verify-email")
    @ApiMessage("Yêu cầu gửi lại thành công")
    public void resendVerifyEmail(@Valid @RequestBody ForgotPasswordDTO dto) {
        authService.handleResendVerifyEmail(dto.getEmail());
    }

    @PostMapping("/change-email")
    @ApiMessage("Yêu cầu đổi email thành công, vui lòng kiểm tra email")
    public void changeEmail(@Valid @RequestBody ChangeEmailDTO dto) {
        String currentEmail = SecurityUtil.getCurrentUserLogin().orElse(null);
        if (currentEmail == null) throw new ProviderPasswordNotFound("Bạn chưa đăng nhập hoặc token không hợp lệ");

        authService.handleChangeEmail(currentEmail, dto.getNewEmail(), dto.getCurrentPassword());
    }

    @GetMapping("/change-email/confirm")
    @ApiMessage("Đổi email thành công")
    public void confirmChangeEmail(@Valid @RequestParam("token") String token) {
        authService.handleConfirmChangeEmail(token);
    }

    @PostMapping("/create-password")
    @ApiMessage("Tạo mật khẩu thành công")
    public void createPassword(@Valid @RequestBody CreatePasswordDTO dto) {
        userService.handleCreatePassword(dto.getNewPassword(), dto.getConfirmPassword(), dto.getToken());
    }

    private ResponseEntity<ResLoginDTO> buildLoginResponse(LoginResult result) {
        ResponseCookie cookie = ResponseCookie.from("refresh_token", result.getRefreshToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(app.getJwt().getRefreshTokenValidityInSeconds())
                .sameSite("Strict")
                .build();

        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).body(result.getLoginResponse());
    }

    private GoogleIdToken verifyGoogleToken(String idTokenString) {
        try {
            NetHttpTransport transport = new NetHttpTransport();
            GsonFactory jsonFactory = GsonFactory.getDefaultInstance();

            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(transport, jsonFactory).setAudience(Collections.singletonList(app.getGoogle().getClientId())).build();

            return verifier.verify(idTokenString);
        } catch (Exception e) {
            throw new PartnerAuthenticationException("Đăng nhập Google thất bại: " + e.getMessage());
        }
    }
}
