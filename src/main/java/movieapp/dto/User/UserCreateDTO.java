package movieapp.dto.User;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import movieapp.util.constant.ValidationConstant;

@Data
public class UserCreateDTO {
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    @Size(max = ValidationConstant.EMAIL_MAX_LENGTH, message = "Email không được quá 100 ký tự")
    private String email;

    @NotBlank(message = "Username không được để trống")
    @Size(
            min = ValidationConstant.USERNAME_MIN_LENGTH,
            max = ValidationConstant.USERNAME_MAX_LENGTH,
            message = "Username phải từ 3 đến 30 ký tự"
    )
    @Pattern(
            regexp = ValidationConstant.USERNAME_PATTERN,
            message = "Username chỉ được chứa chữ cái, số, dấu gạch dưới và gạch ngang"
    )
    private String username;

    @NotBlank(message = "Password không được để trống")
    @Size(
            min = ValidationConstant.PASSWORD_MIN_LENGTH,
            max = ValidationConstant.PASSWORD_MAX_LENGTH,
            message = "Password phải từ 6 đến 50 ký tự"
    )
    private String password;

    @NotBlank(message = "FullName không được để trống")
    @Size(
            min = ValidationConstant.FULLNAME_MIN_LENGTH,
            max = ValidationConstant.FULLNAME_MAX_LENGTH,
            message = "Họ và tên phải từ 1 đến 50 ký tự"
    )
    private String fullName;

    private String avatarUrl;
    private Long roleId;
}
