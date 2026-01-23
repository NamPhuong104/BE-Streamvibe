package movieapp.dto.Cache;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class CacheDetailResponse {
    private CacheItemDTO info;
    private Object data;
    private String dataPreview;
}
