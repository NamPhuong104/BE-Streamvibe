package movieapp.dto.Auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import movieapp.util.constant.ValidationConstant;

@Getter
@Setter
public class CreatePasswordDTO {
    @NotBlank(message = "Mật khẩu mới không được để trống")
    @Size(
            min = ValidationConstant.PASSWORD_MIN_LENGTH,
            max = ValidationConstant.PASSWORD_MAX_LENGTH,
            message = "Mật khẩu phải từ 6 đến 50 ký tự"
    )
    private String newPassword;

    @NotBlank(message = "Xác nhận mật khẩu không được để trống")
    private String confirmPassword;

    @NotBlank(message = "Token không được để trống")
    private String token;
}
