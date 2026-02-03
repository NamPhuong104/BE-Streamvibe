package movieapp.repository;

import movieapp.dto.Dashboard.DailyCountProjection;
import movieapp.dto.Dashboard.TopWatchedProjection;
import movieapp.entity.WatchHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WatchHistoryRepository extends JpaRepository<WatchHistory, Long>, JpaSpecificationExecutor<WatchHistory> {

    //  Tìm record cụ thể (xử lý tất cả episodeSlug = null
    @Query("""
                SELECT wh FROM WatchHistory wh
                WHERE wh.user.id = :userId
                AND wh.movieSlug = :movieSlug
                AND ((:episodeSlug IS NULL AND wh.episodeSlug IS NULL) OR wh.episodeSlug = :episodeSlug)
            """)
    Optional<WatchHistory> findByUserAndMovieAndEpisode(
            @Param("userId") Long userId,
            @Param("movieSlug") String movieSlug,
            @Param("episodeSlug") String episodeSlug
    );

    // ⭐ Lấy record MỚI NHẤT của mỗi phim (group by movieSlug)
    // Dùng cho trang "Lịch sử xem" - chỉ hiện 1 phim 1 lần
    // FIX: Không dùng Pageable sort, hardcode ORDER BY trong query
    @Query(value = """
            SELECT wh.* FROM watch_history wh
            INNER JOIN (
                SELECT movie_slug, MAX(last_watched_at) as max_time
                FROM watch_history
                WHERE user_id = :userId
                GROUP BY movie_slug
            ) latest ON wh.movie_slug = latest.movie_slug 
                      AND wh.last_watched_at = latest.max_time
                      AND wh.user_id = :userId
            ORDER BY wh.last_watched_at DESC
            LIMIT :limit OFFSET :offset
            """,
            countQuery = """
                        SELECT COUNT(DISTINCT movie_slug) FROM watch_history WHERE user_id = :userId
                    """,
            nativeQuery = true)
    List<WatchHistory> findLatestEpisodePerMovieNative(
            @Param("userId") Long userId,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    @Query("""
                SELECT wh FROM WatchHistory wh
                WHERE wh.user.id = :userId AND wh.movieSlug = :movieSlug
                ORDER BY wh.lastWatchedAt DESC
                LIMIT 1
            """)
    Optional<WatchHistory> findLastedByUserAndMovie(@Param("userId") Long userId, @Param("movieSlug") String movieSlug);

    @Query("SELECT COUNT(DISTINCT wh.movieSlug) FROM WatchHistory wh WHERE wh.user.id = :userId")
    long countDistinctMoviesByUserId(@Param("userId") Long userId);

    //    Check user có trong bảng ko
    boolean existsByUserId(Long id);

    //    Xóa lịch sử 1 phim (xóa tất cả các tập)
    @Modifying
    @Query("DELETE FROM WatchHistory wh WHERE wh.user.id = :userId AND wh.movieSlug = :movieSlug")
    void deleteHistoryByUserIdAndMovieSlug(@Param("userId") Long userId, @Param("movieSlug") String movieSlug);

    //    Xóa toàn bộ lịch sử của user
    @Modifying
    @Query("DELETE FROM WatchHistory wh WHERE wh.user.id = :userId")
    void deleteAllHistoryByUserId(@Param("userId") Long userId);


    /**
     * Summary view: Lấy record MỚI NHẤT của mỗi cặp (user + movie)
     */
    @Query(value = """
            SELECT wh.* FROM watch_history wh
                  INNER JOIN (
                            SELECT user_id, movie_slug, MAX(last_watched_at) as max_time
                            FROM watch_history
                            GROUP BY user_id, movie_slug
                           ) latest ON wh.user_id = latest.user_id
                                    AND wh.movie_slug = latest.movie_slug
                                    AND wh.last_watched_at = latest.max_time
                                    ORDER BY wh.last_watched_at DESC
                                    LIMIT :limit OFFSET :offset
            """,
            nativeQuery = true)
    List<WatchHistory> findLatestPeruserAndMovie(@Param("limit") int limit, @Param("offset") int offset);

    /**
     * Đếm tổng số cặp unique (user, movie) cho summary view
     */
    @Query(value = """
            SELECT COUNT(*) FROM (
               SELECT DISTINCT user_id, movie_slug FROM watch_history
            ) as unique_pairs
            """, nativeQuery = true)
    long countUniqueUserMoviePairs();

    /**
     * Đếm số tập đã xem của 1 user cho 1 movie
     */
    @Query("SELECT COUNT(wh) FROM WatchHistory wh WHERE wh.user.id = :userId AND wh.movieSlug = :movieSlug")
    int countEpisodesByUserAndMovie(@Param("userId") Long userId, @Param("movieSlug") String movieSlug);

    // ==================== DASHBOARD STATISTICS ====================

    /**
     * Top watched movies - CÓ filter theo thời gian
     */
    @Query(value = """
            SELECT 
                movie_slug as movieSlug,
                movie_name as movieName,
                origin_name as originName,
                poster_url as posterUrl,
                thumb_url as thumbUrl,
                COUNT(DISTINCT user_id) as viewCount,
                COALESCE(SUM(watch_time), 0) as totalWatchTime
            FROM watch_history
            WHERE last_watched_at >= :since
            GROUP BY movie_slug, movie_name, origin_name, poster_url, thumb_url
            ORDER BY viewCount DESC, totalWatchTime DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<TopWatchedProjection> findTopWatchedMoviesSince(
            @Param("since") LocalDateTime since,
            @Param("limit") int limit
    );

    /**
     * Top watched movies - KHÔNG filter (ALL time)
     */
    @Query(value = """
            SELECT 
                movie_slug as movieSlug,
                movie_name as movieName,
                origin_name as originName,
                poster_url as posterUrl,
                thumb_url as thumbUrl,
                COUNT(DISTINCT user_id) as viewCount,
                COALESCE(SUM(watch_time), 0) as totalWatchTime
            FROM watch_history
            GROUP BY movie_slug, movie_name, origin_name, poster_url, thumb_url
            ORDER BY viewCount DESC, totalWatchTime DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<TopWatchedProjection> findTopWatchedMoviesAll(@Param("limit") int limit);

    // Tổng thời gian xem (seconds)
    @Query("SELECT COALESCE(SUM(wh.currentTime), 0) FROM WatchHistory wh")
    long getTotalWatchTime();

    // Số lần xem hoàn thành
    long countByCompletedTrue();

    // Tỷ lệ hoàn thành trung bình
    @Query("SELECT COALESCE(AVG(wh.progressPercent), 0) FROM WatchHistory wh")
    double getAvgCompletionRate();

    // Đếm unique movies
    @Query("SELECT COUNT (DISTINCT wh.movieSlug) FROM WatchHistory wh")
    long countUniqueMovies();

    // Đếm watches theo ngày
    @Query(value = """
            SELECT DATE(last_watched_at) as date, COUNT(*) as count
            FROM watch_history
            WHERE last_watched_at >= :since
            GROUP BY DATE(last_watched_at)
            ORDER BY date ASC
            """, nativeQuery = true)
    List<DailyCountProjection> countDailyWatches(@Param("since") LocalDateTime since);
}
