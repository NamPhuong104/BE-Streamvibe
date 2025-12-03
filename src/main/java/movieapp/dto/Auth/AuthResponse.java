package movieapp.dto.Auth;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String username;
    private String email;
    private String provider;
    private String providerId;
    private Boolean isActive;
    private String isEmailVerified;

}
