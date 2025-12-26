package movieapp.dto.Favorites;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FavoriteCreateReq {
    @NotNull(message = "user id không được để trống")
    private Long userId;

    @NotBlank(message = "Slug phim không được để trống")
    private String movieSlug;

    @NotBlank(message = "Tên phim không được để trống")
    private String movieName;

    @NotBlank(message = "Origin name không được để trống")
    private String originName;

    private String posterUrl;

    @NotBlank(message = "ThumbUrl không được để trống")
    private String thumbUrl;

    @NotBlank(message = "Episode current không được để trống")
    private String episodeCurrent;
    
    @NotBlank(message = "Language không được để trống")
    private String lang;

    @NotBlank(message = "Quality không được để trống")
    private String quality;
}
