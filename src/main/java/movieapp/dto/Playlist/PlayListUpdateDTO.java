package movieapp.dto.Playlist;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PlayListUpdateDTO {
    @NotNull(message = "userId không được để trống")
    private Long userId;

    @NotNull(message = "Id không được để trống")
    private Long id;

    @NotBlank(message = "name không được để trống")
    @Size(max = 100, message = "Tên playlist không quá 100 ký tự")
    private String name;

}
