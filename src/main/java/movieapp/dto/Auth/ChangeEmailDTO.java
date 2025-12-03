package movieapp.dto.Auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChangeEmailDTO {
    @NotBlank(message = "Email mới không được để trống")
    @Email(message = "Email mới không đúng định dạng")
    private String newEmail;

    @NotBlank(message = "Mật khẩu hiện tại không được để trống")
    private String currentPassword;
}
