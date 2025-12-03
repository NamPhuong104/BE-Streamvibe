package movieapp.dto.CustomFieldsResponse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import movieapp.dto.OphimResponse.OphimMovieItem;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MovieItemDTO extends OphimMovieItem {
    private String content;
    private String optimizedThumb;
    private String optimizedPoster;
    private Boolean isFavorite;
}
