package movieapp.controller;

import lombok.RequiredArgsConstructor;
import movieapp.dto.Dashboard.*;
import movieapp.service.DashboardService;
import movieapp.util.annotation.ApiMessage;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class DashboardController {
    private final DashboardService dashboardService;

    // ==================== OVERVIEW TAB ====================

    /**
     * GET /api/v1/admin/dashboard/users
     */
    @GetMapping("/users")
    @ApiMessage("Lấy thống kê user thành công")
    public UserStatsResponse getUserStats() {
        return dashboardService.getUserStats();
    }

    /**
     * GET /admin/dashboard/content-stats
     * Content stats cho Overview tab
     */
    @GetMapping("/content-stats")
    @ApiMessage("Lấy thống kê content thành công")
    public ContentStatsResponse getContentStats() {
        return dashboardService.getContentStats();
    }

    // ==================== TRENDS TAB ====================

    /**
     * GET /api/v1/admin/dashboard/registration-trend?range=MONTH
     */
    @GetMapping("/registration-trend")
    @ApiMessage("Lấy xu hướng đăng ký thành công")
    public List<TrendDataPoint> getRegistrationTrend(
            @RequestParam(defaultValue = "MONTH") TimeRangeEnum range
    ) {
        return dashboardService.getRegistrationTrend(range);
    }

    @GetMapping("/activity-trend")
    @ApiMessage("Lấy xu hướng hoạt động thành công")
    public List<ActivityTrendPoint> getActivityTrend(@RequestParam(defaultValue = "MONTH") TimeRangeEnum range) {
        return dashboardService.getActivityTrend(range);
    }

    // ==================== RANKINGS TAB ====================

    /**
     * GET /api/v1/admin/dashboard/top-watched?range=MONTH&limit=10
     */
    @GetMapping("/top-watched")
    @ApiMessage("Lấy top phim được xem thành công")
    public List<TopMovieDTO> getTopWatchedMovies(
            @RequestParam(defaultValue = "MONTH") TimeRangeEnum range,
            @RequestParam(defaultValue = "10") int limit
    ) {
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        return dashboardService.getTopWatchedMovies(range, safeLimit);
    }

    /**
     * GET /admin/dashboard/top-favorited?range=MONTH&limit=10
     * Top favorited movies cho Rankings tab
     */
    @GetMapping("/top-favorited")
    @ApiMessage("Lấy top phim yêu thích thành công")
    public List<TopMovieDTO> getTopFavoritedMovies(@RequestParam(defaultValue = "MONTH") TimeRangeEnum range, @RequestParam(defaultValue = "10") int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 50);

        return dashboardService.getTopFavoritedMovie(range, safeLimit);
    }


    /**
     * GET /api/v1/admin/dashboard?range=MONTH
     */
    @GetMapping
    @ApiMessage("Lấy tổng quan dashboard thành công")
    public DashboardSummaryResponse getDashboardSummary(
            @RequestParam(defaultValue = "MONTH") TimeRangeEnum range
    ) {
        return dashboardService.getDashboardSummary(range);
    }
}
