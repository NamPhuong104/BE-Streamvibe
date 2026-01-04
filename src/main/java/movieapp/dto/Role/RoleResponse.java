package movieapp.dto.Role;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleResponse {
    private Long id;
    private String name;
    private String description;
    private Integer priority;
    private Boolean isSystemRole;
    private Long userCount;
    private LocalDateTime createdAt;
}
