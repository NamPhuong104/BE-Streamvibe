package movieapp.dto.Dashboard;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserStatsResponse {
    private long totalUsers;
    private long activeUsers;
    private long inactiveUsers;
    private long verifiedUsers;
    private long unverifiedUsers;

    private long newUsersToday;
    private long newUsersThisWeek;
    private long newUserThisMonth;

    private Double growthRatePercent;
    private String growthTrend;
}
