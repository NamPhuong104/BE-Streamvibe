package movieapp.dto.Role;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleUpdateDTO {
    @Pattern(regexp = "^ROLE_[A-Z_]+$", message = "Tên role phải theo format: ROLE_XXX (viết hoa)")
    private String name;

    private String description;

    @Min(value = 1, message = "Priority phải >= 1")
    @Max(value = 1000, message = "Priority phải <= 1000")
    private Integer priority;
}
