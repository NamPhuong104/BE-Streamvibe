package movieapp.repository;

import movieapp.dto.Dashboard.DailyCountProjection;
import movieapp.dto.Dashboard.TopFavoritedProjection;
import movieapp.entity.Favorite;
import movieapp.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long>, JpaSpecificationExecutor<Favorite> {
    List<Favorite> findByUser(User user);

    Optional<Favorite> findByUserAndMovieSlug(User user, String movieSlug);

    boolean existsByUserAndMovieSlug(User user, String movieSlug);

    Page<Favorite> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    void deleteByUserAndMovieSlug(User user, String movieSlug);

    // ==================== DASHBOARD STATISTICS ====================
    long count();

    // Đếm unique movies được favorite
    @Query("SELECT COUNT(DISTINCT f.movieSlug) FROM Favorite f")
    long countUniqueMovies();

    // Top phim được yêu thích nhiều nhất  - ALL time
    @Query(value = """
            SELECT 
                movie_slug as movieSlug,
                movie_name as movieName,
                origin_name as originName,
                poster_url as posterUrl,
                thumb_url as thumbUrl,
                COUNT(*) as favoriteCount
            FROM favorites
            GROUP BY movie_slug, movie_name, origin_name, poster_url, thumb_url
            ORDER BY favoriteCount DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<TopFavoritedProjection> findTopFavoritedMoviesAll(@Param("limit") int limit);

    // Top phim được yêu thích - CÓ filter
    @Query(value = """
            SELECT 
                movie_slug as movieSlug,
                movie_name as movieName,
                origin_name as originName,
                poster_url as posterUrl,
                thumb_url as thumbUrl,
                COUNT(*) as favoriteCount
            FROM favorites
            WHERE created_at >= :since
            GROUP BY movie_slug, movie_name, origin_name, poster_url, thumb_url
            ORDER BY favoriteCount DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<TopFavoritedProjection> findTopFavoritedMoviesSince(
            @Param("since") LocalDateTime since,
            @Param("limit") int limit
    );

    // Đếm favorites theo ngày
    @Query(value = """
            SELECT DATE(created_at) as date, COUNT(*) as count
            FROM favorites
            WHERE created_at >= :since
            GROUP BY DATE(created_at)
            ORDER BY date ASC
            """, nativeQuery = true)
    List<DailyCountProjection> countDailyFavorites(@Param("since") LocalDateTime since);
}
