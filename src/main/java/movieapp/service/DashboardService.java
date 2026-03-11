package movieapp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import movieapp.dto.Dashboard.*;
import movieapp.repository.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {
    private final UserRepository userRepository;
    private final WatchHistoryRepository watchHistoryRepository;
    private final FavoriteRepository favoriteRepository;
    private final PlaylistRepository playlistRepository;
    private final PlaylistMovieRepository playlistMovieRepository;

    // ==================== USER STATS ====================
    public UserStatsResponse getUserStats() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByIsActiveTrue();
        long verifiedUsers = userRepository.countByIsEmailVerifiedTrue();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime startOfWeek = now.minusDays(7);
        LocalDateTime startOfMonth = now.minusDays(30);

        long newToday = userRepository.countUserCreatedSince(startOfToday);
        long newThisWeek = userRepository.countUserCreatedSince(startOfWeek);
        long newThisMonth = userRepository.countUserCreatedSince(startOfMonth);

        LocalDateTime previousMonthStart = now.minusDays(60);
        LocalDateTime previousMonthEnd = now.minusDays(30);
        long usersLastMonth = userRepository.countUsersCreatedBetween(previousMonthStart, previousMonthEnd);

        Double growthRate = null;
        String growthTrend = "STABLE";

        if (usersLastMonth > 0) {
            growthRate = ((double) (newThisMonth - usersLastMonth) / usersLastMonth) * 100;
            growthRate = Math.round(growthRate * 100.0) / 100.0;

            if (growthRate > 5) growthTrend = "UP";
            else if (growthRate < -5) growthTrend = "DOWN";
        } else if (newThisMonth > 0) {
            growthRate = 100.0;
            growthTrend = "UP";
        }

        return UserStatsResponse.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .inactiveUsers(totalUsers - activeUsers)
                .verifiedUsers(verifiedUsers)
                .unverifiedUsers(totalUsers - verifiedUsers)
                .newUsersToday(newToday)
                .newUsersThisWeek(newThisWeek)
                .newUserThisMonth(newThisMonth)
                .growthRatePercent(growthRate)
                .growthTrend(growthTrend)
                .build();

    }

    // ==================== CONTENT STATS ====================
    public ContentStatsResponse getContentStats() {
        long totalFavorites = favoriteRepository.count();
        long totalPlaylists = playlistRepository.count();
        long totalPlaylistMovies = playlistMovieRepository.count();
        long totalWatchTimeSeconds = watchHistoryRepository.getTotalWatchTime();
        long completedWatches = watchHistoryRepository.countByCompletedTrue();
        double avgCompletionRate = watchHistoryRepository.getAvgCompletionRate();
        long uniqueMoviesWatched = watchHistoryRepository.countUniqueMovies();
        long uniqueMoviesFavorited = favoriteRepository.countUniqueMovies();

        return ContentStatsResponse.builder()
                .totalFavorites(totalFavorites)
                .totalPlaylists(totalPlaylists)
                .totalPlaylistMovies(totalPlaylistMovies)
                .totalWatchTimeSeconds(totalWatchTimeSeconds)
                .totalWatchTimeFormatted(formatWatchTime(totalWatchTimeSeconds))
                .completedWatches(completedWatches)
                .avgCompletionRate(Math.round(avgCompletionRate * 100.0) / 100.0)
                .uniqueMoviesWatched(uniqueMoviesWatched)
                .uniqueMoviesFavorited(uniqueMoviesFavorited)
                .build();
    }

    // ==================== ACTIVITY TREND ====================
    public List<ActivityTrendPoint> getActivityTrend(TimeRangeEnum timeRange) {
        LocalDateTime since = getStartDate(timeRange);
        if (since == null) since = LocalDateTime.now().minusDays(30);

        List<DailyCountProjection> favoriteResults = favoriteRepository.countDailyFavorites(since);
        List<DailyCountProjection> watchResults = watchHistoryRepository.countDailyWatches(since);

        // Convert to Map for easy lookup
        Map<LocalDate, Long> favoriteMap = new HashMap<>();
        Map<LocalDate, Long> watchMap = new HashMap<>();

        for (DailyCountProjection row : favoriteResults) {
            favoriteMap.put(row.getDate(), row.getCount());
        }

        for (DailyCountProjection row : watchResults) {
            watchMap.put(row.getDate(), row.getCount());
        }

        Set<LocalDate> allDates = new TreeSet<>();
        allDates.addAll(favoriteMap.keySet());
        allDates.addAll(watchMap.keySet());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");
        List<ActivityTrendPoint> trend = new ArrayList<>();

        for (LocalDate date : allDates) {
            trend.add(ActivityTrendPoint.builder()
                    .date(date)
                    .label(date.format(formatter))
                    .favoriteCount(favoriteMap.getOrDefault(date, 0L))
                    .watchCount(watchMap.getOrDefault(date, 0L))
                    .build());
        }

        return trend;
    }

    // ==================== TOP FAVORITED ====================
    public List<TopMovieDTO> getTopFavoritedMovie(TimeRangeEnum timeRange, int limit) {
        LocalDateTime since = getStartDate(timeRange);

        List<TopFavoritedProjection> results;
        if (since == null) {
            results = favoriteRepository.findTopFavoritedMoviesAll(limit);
        } else {
            results = favoriteRepository.findTopFavoritedMoviesSince(since, limit);
        }

        if (results.isEmpty()) return new ArrayList<>();

        List<TopMovieDTO> movies = new ArrayList<>();
        int rank = 1;

        for (TopFavoritedProjection row : results) {
            TopMovieDTO dto = TopMovieDTO.builder()
                    .rank(rank++)
                    .movieSlug(row.getMovieSlug())
                    .movieName(row.getMovieName())
                    .originName(row.getOriginName())
                    .posterUrl(row.getPosterUrl())
                    .thumbUrl(row.getThumbUrl())
                    .viewCount(row.getFavoriteCount())
                    .totalWatchTime(0L)
                    .build();
            movies.add(dto);
        }

        return movies;
    }

    // ==================== TOP WATCHED MOVIES ====================
    public List<TopMovieDTO> getTopWatchedMovies(TimeRangeEnum timeRange, int limit) {
        LocalDateTime since = getStartDate(timeRange);

        // Gọi query phù hợp
        List<TopWatchedProjection> results;
        if (since == null) {
            results = watchHistoryRepository.findTopWatchedMoviesAll(limit);
        } else {
            results = watchHistoryRepository.findTopWatchedMoviesSince(since, limit);
        }

        if (results.isEmpty()) {
            return new ArrayList<>();
        }

        // Build response
        List<TopMovieDTO> movies = new ArrayList<>();
        int rank = 1;

        for (TopWatchedProjection row : results) {
            TopMovieDTO dto = TopMovieDTO.builder()
                    .rank(rank++)
                    .movieSlug(row.getMovieSlug())
                    .movieName(row.getMovieName())
                    .originName(row.getOriginName())
                    .posterUrl(row.getPosterUrl())
                    .thumbUrl(row.getThumbUrl())
                    .viewCount(row.getViewCount())
                    .totalWatchTime(row.getTotalWatchTime())
                    .build();

            movies.add(dto);
        }

        return movies;
    }

    // ==================== REGISTRATION TREND ====================

    public List<TrendDataPoint> getRegistrationTrend(TimeRangeEnum timeRange) {
        LocalDateTime since = getStartDate(timeRange);

        if (since == null) {
            since = LocalDateTime.now().minusDays(90);
        }

        List<DailyCountProjection> results = userRepository.countDailyRegistrations(since);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");
        List<TrendDataPoint> trend = new ArrayList<>();

        for (DailyCountProjection row : results) {
            trend.add(TrendDataPoint.builder()
                    .date(row.getDate())
                    .label(row.getDate().format(formatter))
                    .count(row.getCount())
                    .build());
        }

        return trend;
    }

    // ==================== SUMMARY ====================
    public DashboardSummaryResponse getDashboardSummary(TimeRangeEnum timeRange) {
        return DashboardSummaryResponse.builder()
                .userStats(getUserStats())
                .topWatchedMovie(getTopWatchedMovies(timeRange, 10))
                .registrationTrend(getRegistrationTrend(timeRange))
                .generatedAt(LocalDateTime.now().toString())
                .timeRange(timeRange.getLabel())
                .build();
    }

    // ==================== HELPER ====================
    private LocalDateTime getStartDate(TimeRangeEnum timeRange) {
        if (timeRange == null || timeRange == TimeRangeEnum.ALL) return null;

        if (timeRange == TimeRangeEnum.TODAY) return LocalDate.now().atStartOfDay();

        return LocalDateTime.now().minusDays(timeRange.getDays());

    }

    private String formatWatchTime(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;

        if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        } else {
            return String.format("%dm", minutes);
        }
    }
}
