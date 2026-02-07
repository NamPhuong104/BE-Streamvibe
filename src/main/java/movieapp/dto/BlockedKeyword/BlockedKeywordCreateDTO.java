package movieapp.dto.BlockedKeyword;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BlockedKeywordCreateDTO {
    @NotBlank(message = "Keyword không được để trống")
    @Size(min = 2, max = 100, message = "Keyword phải từ 2-100 ký tự")
    private String keyword;

    private Boolean isActive = true;
}
