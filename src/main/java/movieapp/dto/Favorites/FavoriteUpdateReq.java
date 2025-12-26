package movieapp.dto.Favorites;

import lombok.Data;

@Data
public class FavoriteUpdateReq {
    private Long id;
    private Long userId;

    private String movieSlug;
    private String movieName;
    private String originName;
    private String posterUrl;
    private String thumbUrl;
    private String episodeCurrent;
    private String lang;
    private String quality;
}
