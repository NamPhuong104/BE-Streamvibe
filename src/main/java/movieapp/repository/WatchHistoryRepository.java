package movieapp.repository;

import movieapp.domain.User;
import movieapp.domain.WatchHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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

    Optional<WatchHistory> findFirstByUserIdAndMovieSlugOrderByLastWatchedAtDesc(Long userId, String movieSlug);

    @Query("SELECT COUNT(DISTINCT wh.movieSlug) FROM WatchHistory wh WHERE wh.user.id = :userId")
    long countDistincMoviesByUserId(@Param("userId") Long userId);

    //    Lịch sử xem (mới nhất)
    Page<WatchHistory> findByUserIdOrderByLastWatchedAtDesc(Long userId, Pageable pageable);

    //    Tiếp tục xem (chưa hoàn thành)
    List<WatchHistory> findByUserIdAndCompletedFalseOrderByLastWatchedAtDesc(Long userId);

    //    Lấy tất cả tập của 1 phim
    List<WatchHistory> findByUserIdAndMovieSlugOrderByEpisodeSlugAsc(Long userId, String movieSlug);

    //    check đã xem phim này chưa
    boolean existsByUserIdAndMovieSlug(Long userId, String movieSlug);

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
}
