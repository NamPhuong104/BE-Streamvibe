package movieapp.dto.WatchHistory;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class WatchHistorySummaryRes extends WatchHistoryRes {
    private int episodeCount;
}
