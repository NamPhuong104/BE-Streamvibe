package movieapp.controller;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import movieapp.domain.User;
import movieapp.dto.Auth.*;
import movieapp.dto.RestResponse;
import movieapp.dto.User.Request.UseCreateDTO;
import movieapp.dto.User.Response.ResUserDTO;
import movieapp.service.UserService;
import movieapp.util.SecurityUtil;
import movieapp.util.annotation.ApiMessage;
import movieapp.util.error.IdInvalidException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    @Value("${jwt.refresh-token-validity-in-seconds}")
    private Long refreshTokenExpiration;
    @Value("${google.client-id}")
    private String googleClientId;

    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final SecurityUtil securityUtil;
    private final UserService userService;

    @PostMapping("/register")
    @ApiMessage("Đăng ký tài khoản thành công")
    public ResponseEntity<ResUserDTO> register(@Valid @RequestBody UseCreateDTO registerDto) throws IdInvalidException {
        // Gọi lại service tạo user (đã có logic mã hóa pass rồi)
        ResUserDTO newUser = userService.handleCreateUser(registerDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(newUser);
    }

    @PostMapping("/login")
    @ApiMessage("Login Successfully")
    public ResponseEntity<?> login(@Valid @RequestBody ReqLoginDTO loginDTO) {
        try {
            // 1. Nạp username/password vào Security để check
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(loginDTO.getUsername(), loginDTO.getPassword());
            // 2. Xác thực (Sẽ gọi UserDetailsCustom để check DB)
            Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
            // 3. Nếu đúng pass, lưu thông tin vào Context
            SecurityContextHolder.getContext().setAuthentication(authentication);
            // 4. Tạo Token trả về
            ResLoginDTO res = new ResLoginDTO();

            // Lấy thông tin user từ DB
            User currentUser = userService.handleFindUserByEmailEntity(loginDTO.getUsername());
            if (currentUser != null) {
                ResLoginDTO.UserLogin userLogin = new ResLoginDTO.UserLogin(
                        currentUser.getId(),
                        currentUser.getEmail(),
                        currentUser.getUsername(),
                        "ROLE_USER",
                        currentUser.getFullName(),
                        currentUser.getAvatarUrl(),
                        currentUser.getProvider(),
                        currentUser.getProviderId(),
                        currentUser.getIsActive(),
                        currentUser.getIsEmailVerified()
                );
                res.setUser(userLogin);
            }
            // Tạo Access Token
            String access_token = securityUtil.createAccessToken(authentication.getName(), res);
            res.setAccessToken(access_token);
            // Tạo Refresh Token
            String refresh_token = securityUtil.createRefreshToken(authentication.getName(), res);

            userService.handleUpdateUserToken(refresh_token, loginDTO.getUsername());

            // Set cookie
            ResponseCookie responseCookie = ResponseCookie.from("refresh_token", refresh_token)
                    .httpOnly(true).secure(true).path("/").maxAge(refreshTokenExpiration).build();

            return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, responseCookie.toString()).body(res);
        } catch (BadCredentialsException | UsernameNotFoundException ex) {
            RestResponse<Object> error = new RestResponse<>();
            error.setStatusCode(401);
            error.setError("Unauthorized");
            error.setMessage("Tài khoản hoặc mật khẩu không chính xác");
            error.setData(null);

            return ResponseEntity.status(401).body(error);
        }
    }

    @PostMapping("/google")
    @ApiMessage("Đăng nhập với Google")
    public ResponseEntity<?> loginWithGoogle(@Valid @RequestBody GoogleLoginDTO request) throws IdInvalidException {

        try {
            NetHttpTransport transport = new NetHttpTransport();
            GsonFactory jsonFactory = GsonFactory.getDefaultInstance();
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(transport, jsonFactory)
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();
            GoogleIdToken idToken = verifier.verify(request.getIdToken());
            if (idToken == null) throw new IdInvalidException("Google token không hợp lệ");

            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            String name = (String) payload.get("name");
            String picture = (String) payload.get("picture");
            String sub = payload.getSubject();

            User user = userService.findOrCreateGoogleUser(email, name, picture, sub);

            ResLoginDTO res = new ResLoginDTO();
            ResLoginDTO.UserLogin userLogin = new ResLoginDTO.UserLogin(
                    user.getId(),
                    user.getEmail(),
                    user.getUsername(),
                    "ROLE_USER",
                    user.getFullName(),
                    user.getAvatarUrl(),
                    user.getProvider(),
                    user.getProviderId(),
                    user.getIsActive(),
                    user.getIsEmailVerified()
            );
            res.setUser(userLogin);

            String accessToken = securityUtil.createAccessToken(email, res);
            res.setAccessToken(accessToken);

            String refreshToken = securityUtil.createRefreshToken(email, res);
            userService.handleUpdateUserToken(refreshToken, email);

            ResponseCookie cookie = ResponseCookie.from("refresh_token", refreshToken)
                    .httpOnly(true)
                    .secure(true)
                    .path("/")
                    .maxAge(refreshTokenExpiration)
                    .build();

            return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).body(res);
        } catch (Exception e) {
            RestResponse<Object> error = new RestResponse<>();
            error.setStatusCode(401);
            error.setError("Unauthorized");
            error.setMessage("Đăng nhập Google thất bại: " + e.getMessage());
            error.setData(null);
            return ResponseEntity.status(401).body(error);
        }
    }

    @GetMapping("/account")
    @ApiMessage("Lấy thông tin tài khoản thành công")
    public ResponseEntity<ResUserDTO> getAccount() throws IdInvalidException {
        String email = SecurityUtil.getCurrentUserLogin().orElse(null);

        if (email == null) throw new IdInvalidException("Bạn chưa đăng nhập hoặc token không hợp lệ");

        User currentUser = userService.handleFindUserByEmailEntity(email);
        if (currentUser == null) throw new IdInvalidException("User không tồn tại !!!");
        if (currentUser.getRefreshToken() == null)
            throw new IdInvalidException("Phiên đăng nhập đã kết thúc, vui lòng đăng nhập lại");

        ResUserDTO dto = userService.convertToResUserDTO(currentUser);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/refresh")
    @ApiMessage("Refresh Access Token")
    public ResponseEntity<?> refreshToken(@CookieValue(name = "refresh_token", defaultValue = "") String
                                                  refreshToken) throws IdInvalidException {
        if (refreshToken == null || refreshToken.isEmpty())
            throw new IdInvalidException("Bạn không có refresh token trong cookie");

        // 1. Check token hợp lệ về mặt chữ ký + hạn (exp)
        Jwt decoded;
        try {
            decoded = securityUtil.checkValidRefreshToken(refreshToken);
        } catch (Exception e) {
            throw new IdInvalidException("Refresh token không hợp lệ hoặc đã hết hạn");
        }
        String email = decoded.getSubject();

        // 2. Kiểm tra user có đúng refresh token này không
        User currentUser = userService.getUserByRefreshTokenAndEmail(refreshToken, email);
        if (currentUser == null) throw new IdInvalidException("Refresh token không tồn tại hoặc đã bị thu hồi");

        // 3. Build lại ResLoginDTO từ user hiện tại
        ResLoginDTO res = new ResLoginDTO();
        ResLoginDTO.UserLogin userLogin = new ResLoginDTO.UserLogin(
                currentUser.getId(),
                currentUser.getEmail(),
                currentUser.getUsername(),
                "ROLE_USER",
                currentUser.getFullName(),
                currentUser.getAvatarUrl(),
                currentUser.getProvider(),
                currentUser.getProviderId(),
                currentUser.getIsActive(),
                currentUser.getIsEmailVerified()
        );
        res.setUser(userLogin);

        // 4. Tạo access token mới
        String newAccessToken = securityUtil.createAccessToken(email, res);
        res.setAccessToken(newAccessToken);

        // 5. Tạo refresh token mới (rotate)
        String newRefreshToken = securityUtil.createRefreshToken(email, res);
        userService.handleUpdateUserToken(newRefreshToken, email);

        // 6. Set cookie refresh_token mới
        ResponseCookie responseCookie = ResponseCookie.from("refresh_token", newRefreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(refreshTokenExpiration)
                .build();

        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, responseCookie.toString()).body(res);
    }

    @PostMapping("/logout")
    @ApiMessage("Đăng xuất thành công")
    public ResponseEntity<Void> logout(@CookieValue(name = "refresh_token", defaultValue = "") String refreshToken) throws
            IdInvalidException {
        if (refreshToken == null || refreshToken.isEmpty())
            throw new IdInvalidException("Bạn không có refresh token trong cookie");

        // 1. Check valid refresh token
        Jwt decoded;
        try {
            decoded = securityUtil.checkValidRefreshToken(refreshToken);
        } catch (Exception e) {
            throw new IdInvalidException("Refresh token không hợp lệ hoặc đã hết hạn");
        }
        String email = decoded.getSubject();

        // 2. Check trong DB
        User currentUser = userService.getUserByRefreshTokenAndEmail(refreshToken, email);
        if (currentUser == null) throw new IdInvalidException("Refresh token không hợp lệ");

        // 3. Xóa refresh_token khỏi DB
        userService.handleUpdateUserToken(null, email);

        // 4. Xóa cookie trên browser
        ResponseCookie clearCookie = ResponseCookie.from("refresh_token", null)
                .maxAge(0).path("/").build();

        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, clearCookie.toString()).body(null);
    }

    @PostMapping("/change-password")
    @ApiMessage("Đổi mật khẩu thành công")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordDTO dto) throws IdInvalidException {
        String email = SecurityUtil.getCurrentUserLogin().orElse(null);

        if (email == null) {
            RestResponse<Object> error = new RestResponse<>();
            error.setStatusCode(401);
            error.setError("Unauthorized");
            error.setMessage("Bạn chưa đăng nhập hoặc token không hợp lệ");
            error.setData(null);
            return ResponseEntity.status(401).body(error);
        }

        userService.handleChangePassword(email, dto.getOldPassword(), dto.getNewPassword());

        return ResponseEntity.ok(HttpStatus.OK);
    }

    @PostMapping("/forgot-password")
    @ApiMessage("Yêu cầu đặt lại mật khẩu thành công")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordDTO dto) throws IdInvalidException {
        userService.handleForgotPassword(dto.getEmail());

        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    @ApiMessage("Đặt lại mật khẩu thành công")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordDTO dto) throws IdInvalidException {
        userService.handleResetPassword(dto.getToken(), dto.getNewPassword());
        return ResponseEntity.ok(HttpStatus.OK);
    }

    @GetMapping("/verify-email")
    @ApiMessage("Xác thực email thành công")
    public ResponseEntity<?> verifyEmail(@RequestParam("token") String token) throws IdInvalidException {
        userService.handleVerifyEmail(token);
        return ResponseEntity.ok(HttpStatus.OK);
    }

    @PostMapping("/resend-verify-email")
    @ApiMessage("Yêu cầu gửi lại thành công")
    public ResponseEntity<?> resendVerifyEmail(@Valid @RequestBody ForgotPasswordDTO dto) throws IdInvalidException {
        userService.handleResendVerifyEmail(dto.getEmail());
        return ResponseEntity.ok(HttpStatus.OK);
    }

    @PostMapping("/change-email")
    @ApiMessage("Yêu cầu đổi email thành công, vui lòng kiểm tra email")
    public ResponseEntity<?> changeEmail(@Valid @RequestBody ChangeEmailDTO dto) throws IdInvalidException {
        String currentEmail = SecurityUtil.getCurrentUserLogin().orElse(null);

        if (currentEmail == null) {
            RestResponse<Object> error = new RestResponse<>();
            error.setStatusCode(401);
            error.setError("Unauthorized");
            error.setMessage("Bạn chưa đăng nhập hoặc token không hợp lệ");
            error.setData(null);
            return ResponseEntity.status(401).body(error);
        }

        userService.handleChangeEmail(currentEmail, dto.getNewEmail(), dto.getCurrentPassword());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/change-email/confirm")
    @ApiMessage("Đổi email thành công")
    public ResponseEntity<?> confirmChangeEmail(@Valid @RequestParam("token") String token) throws IdInvalidException {
        userService.handleConfirmChangeEmail(token);
        return ResponseEntity.ok(HttpStatus.OK);
    }
}
