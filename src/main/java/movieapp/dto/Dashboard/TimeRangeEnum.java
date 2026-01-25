package movieapp.dto.Dashboard;

import lombok.Getter;

@Getter
public enum TimeRangeEnum {
    TODAY(1, "Hôm nay"),
    WEEK(7, "7 ngày"),
    MONTH(30, "30 ngày"),
    QUARTER(90, "90 ngày"),
    ALL(0, "Tất cả");

    private final int days;
    private final String label;

    TimeRangeEnum(int days, String label) {
        this.days = days;
        this.label = label;
    }
}
