package movieapp.repository;

import movieapp.domain.OptimizedImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OptimizedImageRepository extends JpaRepository<OptimizedImage, Long> {
    Optional<OptimizedImage> findByOriginalUrl(String originalUrl);

    boolean existsByOriginalUrl(String originalUrl);
}
