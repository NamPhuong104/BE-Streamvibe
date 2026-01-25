package movieapp.dto.Dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityTrendPoint {
    private LocalDate date;
    private String label;
    private long favoriteCount;
    private long watchCount;
}
