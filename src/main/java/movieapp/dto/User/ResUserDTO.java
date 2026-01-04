package movieapp.dto.User;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
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
    private List<String> roles;
    private String primaryRole;
}
