package movieapp.util;

import lombok.RequiredArgsConstructor;
import movieapp.dto.Auth.ResLoginDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SecurityUtil {
    public static final MacAlgorithm JWT_ALGORITHM = MacAlgorithm.HS512;
    private final JwtEncoder jwtEncoder;
    @Value("${jwt.base64-secret}")
    private String jwtKey;

    @Value("${jwt.access-token-validity-in-seconds}")
    private Long accessTokenExpiration;

    @Value("${jwt.refresh-token-validity-in-seconds}")
    private Long refreshTokenExpiration;

    // 5. Lấy thông tin người dùng đang đăng nhập hiện tại
    public static Optional<String> getCurrentUserLogin() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        return Optional.ofNullable(extractPrincipal(securityContext.getAuthentication()));
    }

    private static String extractPrincipal(Authentication authentication) {
        if (authentication == null) {
            return null;
        } else if (authentication.getPrincipal() instanceof UserDetails springSecurityUser) {
            return springSecurityUser.getUsername();
        } else if (authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getSubject();
        } else if (authentication.getPrincipal() instanceof String s) {
            return s;
        }
        return null;
    }

    /**
     * Lấy danh sách roles của user hiện tại
     */
    public static Optional<String> getCurrentUserRole() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        Authentication authentication = securityContext.getAuthentication();

        if (authentication == null) return Optional.empty();

        if (authentication.getPrincipal() instanceof Jwt jwt) {
            return Optional.ofNullable(jwt.getClaimAsString("role"));
        }

        // Fallback: lấy từ authorities (nếu có)
        return authentication.getAuthorities().stream()
                .map(auth -> auth.getAuthority())
                .filter(auth -> auth.startsWith("ROLE_"))
                .findFirst();
    }

    //  Lấy role priority của user hiện tại
    public static Optional<Integer> getCurrentUserRolePriority() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        Authentication authentication = securityContext.getAuthentication();

        if (authentication == null) return Optional.empty();

        if (authentication.getPrincipal() instanceof Jwt jwt) {
            Object priority = jwt.getClaim("rolePriority");
            if (priority instanceof Number) {
                return Optional.of(((Number) priority).intValue());
            }
        }

        return Optional.empty();
    }

    /**
     * Check user hiện tại có role cụ thể không
     */
    public static boolean hasRole(String role) {
        return getCurrentUserRole()
                .map(currentRole -> currentRole.equals(role))
                .orElse(false);
    }

    // Check user có minimum priority không
    public static boolean hasMinimumPriority(int requiredPriority) {
        return getCurrentUserRolePriority()
                .map(priority -> priority <= requiredPriority)
                .orElse(false);
    }

    /**
     * Check user hiện tại có phải Admin không (priority <= 10)
     */
    public static boolean isAdmin() {
        return hasMinimumPriority(10);
    }

    /**
     * Check user hiện tại có phải Super Admin không (priority <= 0)
     */
    public static boolean isSuperAdmin() {
        return hasMinimumPriority(0);
    }

    // Check user có phải Moderator trở lên không (priority <= 50)
    public static boolean isModerator() {
        return hasMinimumPriority(50);
    }

    // Check user có phải Premium trở lên không (priority <= 80)
    public static boolean isPremium() {
        return hasMinimumPriority(80);
    }

    // 1. Tạo Access Token
    public String createAccessToken(String email, ResLoginDTO dto) {
        ResLoginDTO.UserInsideToken userToken = new ResLoginDTO.UserInsideToken();
        userToken.setId(dto.getUser().getId());
        userToken.setEmail(dto.getUser().getEmail());
        userToken.setUsername(dto.getUser().getUsername());
        userToken.setAvatarUrl(dto.getUser().getAvatarUrl());
        userToken.setProvider(dto.getUser().getProvider());
        userToken.setProviderId(dto.getUser().getProviderId());
        userToken.setActive(dto.getUser().getIsActive());
        userToken.setEmailVerified(dto.getUser().getIsEmailVerified());
        userToken.setRole(dto.getUser().getRole());
        userToken.setRolePriority(dto.getUser().getRolePriority());

        Instant now = Instant.now();
        Instant validity = now.plus(accessTokenExpiration, ChronoUnit.SECONDS);

        // Hardcode tạm quyền (sau này lấy từ DB)
        String role = dto.getUser().getRole() != null ? dto.getUser().getRole() : "ROLE_USER";

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuedAt(now)
                .expiresAt(validity)
                .subject(email)
                .claim("user", userToken) // Lưu thông tin user vào token
                .claim("role", role) // Lưu quyền vào token
                .claim("rolePriority", dto.getUser().getRolePriority())
                .claim("permission", role)
                .build();
        JwsHeader jwsHeader = JwsHeader.with(JWT_ALGORITHM).build();
        return this.jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
    }

    // 2. Tạo Refresh Token
    public String createRefreshToken(String email, ResLoginDTO dto) {
        Instant now = Instant.now();
        Instant validity = now.plus(refreshTokenExpiration, ChronoUnit.SECONDS);

        ResLoginDTO.UserInsideToken userToken = new ResLoginDTO.UserInsideToken();
        userToken.setId(dto.getUser().getId());
        userToken.setEmail(dto.getUser().getEmail());
        userToken.setUsername(dto.getUser().getUsername());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuedAt(now)
                .expiresAt(validity)
                .subject(email)
                .claim("user", userToken)
                .build();
        JwsHeader jwsHeader = JwsHeader.with(JWT_ALGORITHM).build();

        return this.jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
    }

    // 3. Helper lấy Key
    private SecretKey getSecretKey() {
        byte[] keyBytes = Base64.getDecoder().decode(jwtKey);
        return new SecretKeySpec(keyBytes, 0, keyBytes.length, JWT_ALGORITHM.getName());
    }

    // 4. Check Refresh Token có hợp lệ không
    public Jwt checkValidRefreshToken(String token) {
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withSecretKey(
                getSecretKey()).macAlgorithm(SecurityUtil.JWT_ALGORITHM).build();
        try {
            return jwtDecoder.decode(token);
        } catch (Exception e) {
            System.out.println(">>> Refresh token error: " + e.getMessage());
            throw e;
        }
    }
}
