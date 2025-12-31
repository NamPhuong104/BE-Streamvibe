package movieapp.repository;

import movieapp.domain.Playlist;
import movieapp.domain.PlaylistMovie;
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
public interface PlaylistMovieRepository extends JpaRepository<PlaylistMovie, Long>, JpaSpecificationExecutor<PlaylistMovie> {
    boolean existsByPlaylistIdAndMovieSlug(Long playlistId, String movieSlug);

    //    Lấy movies trong playlist (có phân trang, mới nhất trước)
    Page<PlaylistMovie> findByPlaylistIdOrderByAddedAtDesc(Long playlistId, Pageable pageable);

    //    Lấy movie có phân trang mới nhất trước theo user
    @Query("""
            SELECT pm FROM PlaylistMovie pm
            WHERE pm.playlist.user.id = :userId
            ORDER BY pm.addedAt DESC
            """)
    Page<PlaylistMovie> findLastedByUser(@Param("userId") Long userId, Pageable pageable);

    //    Lấy movie có phân trang mới nhất trước theo user và playlist Id
    @Query("""
            SELECT pm FROM PlaylistMovie pm
            WHERE pm.playlist.user.id = :userId AND pm.playlist.id = :playlistId
            ORDER BY pm.addedAt DESC
            """)
    Page<PlaylistMovie> findLastedByPlaylistIdAndUserId(@Param("userId") Long userId, @Param("playlistId") Long id, Pageable pageable);

    @Query("""
            SELECT pm FROM PlaylistMovie pm 
            WHERE pm.playlist.user.id = :userId AND pm.movieSlug = :movieSlug
            """)
    Optional<PlaylistMovie> findByUserIdAndMovieSlug(@Param("userId") Long userId, @Param("movieSlug") String movieSlug);

    @Query("""
            SELECT pm.playlist.id FROM PlaylistMovie pm
            WHERE pm.playlist.user.id = :userId AND pm.movieSlug = :movieSlug
            """)
    Long findPlaylistIdByUserIdAndMovieSlug(@Param("userId") Long userId, @Param("movieSlug") String movieSlug);

    @Modifying
    @Query("""
            DELETE FROM PlaylistMovie pm
            WHERE pm.playlist.id = :playlistId AND pm.movieSlug = :movieSlug
            """)
    void deleteMovieByPlaylistIdAndMovieSlug(@Param("playlistId") Long playlistId, @Param("movieSlug") String movieSlug);

    @Modifying
    @Query("""
            DELETE FROM PlaylistMovie pm 
            WHERE pm.playlist.id = :playlistId AND pm.playlist.user.id = :userId
            """)
    void deleteAllMovieByPlaylistIdAndUserId(@Param("playlistId") Long playlistId, @Param("userId") Long id);
}
