package movieapp.repository;

import movieapp.domain.Favorite;
import movieapp.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long>, JpaSpecificationExecutor<Favorite> {
    List<Favorite> findByUser(User user);

    Optional<Favorite> findByUserAndMovieSlug(User user, String movieSlug);

    boolean existsByUserAndMovieSlug(User user, String movieSlug);
}
