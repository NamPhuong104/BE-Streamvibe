package movieapp.dto.User;

import lombok.*;
import movieapp.dto.Auth.ResLoginDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResult {
    private ResLoginDTO loginResponse;
    private String refreshToken;
}
