package movieapp.dto.Dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryResponse {
    private UserStatsResponse userStats;
    private List<TopMovieDTO> topWatchedMovie;
    private List<TrendDataPoint> registrationTrend;
    private String generatedAt;
    private String timeRange;
}
