package movieapp.util;

import lombok.RequiredArgsConstructor;
import movieapp.dto.Auth.ResLoginDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
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
import java.util.*;
import java.util.stream.Collectors;

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
    public static List<String> getCurrentUserRoles() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        Authentication authentication = securityContext.getAuthentication();

        if (authentication == null) return Collections.emptyList();

        if (authentication.getPrincipal() instanceof Jwt jwt) {
            Object roleObj = jwt.getClaim("permission");

            if (roleObj instanceof List<?> rolesList) {
                return rolesList.stream().filter(String.class::isInstance).map(String.class::cast).collect(Collectors.toList());
            }
        }

        return authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList());
    }

    /**
     * Check user hiện tại có role cụ thể không
     */
    public static boolean hasRole(String role) {
        return getCurrentUserRoles().contains(role);
    }

    /**
     * Check user hiện tại có một trong các roles không
     */
    public static boolean hasAnyRole(String... roles) {
        List<String> currentRoles = getCurrentUserRoles();
        return Arrays.stream(roles).anyMatch(currentRoles::contains);
    }

    /**
     * Check user hiện tại có phải Admin không
     */
    public static boolean isAdmin() {
        return hasAnyRole("ROLE_ADMIN", "ROLE_SUPER_ADMIN");
    }

    /**
     * Check user hiện tại có phải Super Admin không
     */
    public static boolean isSuperAdmin() {
        return hasRole("ROLE_SUPER_ADMIN");
    }

    // 1. Tạo Access Token
    public String createAccessToken(String email, ResLoginDTO dto, List<String> roles) {
        ResLoginDTO.UserInsideToken userToken = new ResLoginDTO.UserInsideToken();
        userToken.setId(dto.getUser().getId());
        userToken.setEmail(dto.getUser().getEmail());
        userToken.setUsername(dto.getUser().getUsername());
        userToken.setAvatarUrl(dto.getUser().getAvatarUrl());
        userToken.setProvider(dto.getUser().getProvider());
        userToken.setProviderId(dto.getUser().getProviderId());
        userToken.setActive(dto.getUser().getIsActive());
        userToken.setEmailVerified(dto.getUser().getIsEmailVerified());
        userToken.setRoles(roles);

        Instant now = Instant.now();
        Instant validity = now.plus(accessTokenExpiration, ChronoUnit.SECONDS);

        // Hardcode tạm quyền (sau này lấy từ DB)
        List<String> authorities = roles != null && !roles.isEmpty() ? roles : List.of("ROLE_USER");

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuedAt(now)
                .expiresAt(validity)
                .subject(email)
                .claim("user", userToken) // Lưu thông tin user vào token
                .claim("permission", authorities) // Lưu quyền vào token
                .build();
        JwsHeader jwsHeader = JwsHeader.with(JWT_ALGORITHM).build();
        return this.jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
    }

    @Deprecated
    public String createAccessToken(String email, ResLoginDTO dto) {
        return createAccessToken(email, dto, List.of("ROLE_USER"));
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
