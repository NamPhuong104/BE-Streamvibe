package movieapp.dto.Auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Getter
@Setter
public class ResLoginDTO {
    @JsonProperty("access_token")
    private String accessToken;
    private UserLogin user;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class UserLogin {
        private Long id;
        private String email;
        private String username;
        private String fullName;
        private String avatarUrl;
        private String provider;
        private String providerId;
        private Boolean isActive;
        private Boolean isEmailVerified;
        private String role;
        private Integer rolePriority;
        private Boolean hasPassword;
    }

    @Setter
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserInsideToken {
        private Long id;
        private String email;
        private String username;
        private String fullName;
        private String avatarUrl;
        private String provider;
        private String providerId;
        private boolean isActive;
        private boolean isEmailVerified;
        private String role;
        private Integer rolePriority;
        private Boolean hasPassword;
    }
}
