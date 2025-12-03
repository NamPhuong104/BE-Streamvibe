package movieapp.repository;

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
