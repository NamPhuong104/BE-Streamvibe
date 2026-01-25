package movieapp.dto.Dashboard;

import java.time.LocalDate;

public interface DailyCountProjection {
    LocalDate getDate();

    Long getCount();
}
