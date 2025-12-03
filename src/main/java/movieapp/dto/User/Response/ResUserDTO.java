package movieapp.dto.User.Response;

import lombok.*;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ResUserDTO {
    private Long id;
    private String email;
    private String username;
    private String fullName;
    private String avatarUrl;
    private String provider;
    private String providerId;
    private Boolean isActive;
    private Boolean isEmailVerified;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
