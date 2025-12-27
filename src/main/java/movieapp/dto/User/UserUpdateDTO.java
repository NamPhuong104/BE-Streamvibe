package movieapp.dto.User;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UserUpdateDTO {
    @NotBlank(message = "username không được có kí tự trắng ở đầu")
    @NotEmpty(message = "username không dược để trống")
    private String username;

    @NotBlank(message = "FullName không được có kí tự trắng ở đầu")
    @NotEmpty(message = "FullName không được để trống")
    private String fullName;

    //    @NotBlank(message = "avatarUrl không được có kí tự trắng ở đầu")
//    @NotEmpty(message = "avatarUrl không được để trống")
    private String avatarUrl;
}
