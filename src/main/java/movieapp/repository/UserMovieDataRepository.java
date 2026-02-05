package movieapp.repository;

import movieapp.dto.MovieDetail.UserMovieDataProjection;
import movieapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserMovieDataRepository extends JpaRepository<User, Long> {
    /**
     * Gọi stored procedure lấy tất cả user data cho 1 movie
     *
     * @param userId    ID của user (từ User entity)
     * @param movieSlug Slug của movie
     * @return Optional chứa UserMovieDataProjection
     */
    @Query(value = "SELECT * FROM get_user_movie_data(:userId, :movieSlug)", nativeQuery = true)
    Optional<UserMovieDataProjection> getUserMovieData(@Param("userId") Long userId, @Param("movieSlug") String movieSlug);

}
