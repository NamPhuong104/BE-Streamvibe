package movieapp.dto.Auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    public static class UserLogin {
        private Long id;
        private String email;
        private String username;
        private String role;
        private String fullName;
        private String avatarUrl;
        private String provider;
        private String providerId;
        private Boolean isActive;
        private Boolean isEmailVerified;
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
    }
}
