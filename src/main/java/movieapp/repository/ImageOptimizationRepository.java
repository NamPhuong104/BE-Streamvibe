package movieapp.repository;

import movieapp.entity.OptimizedImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ImageOptimizationRepository extends JpaRepository<OptimizedImage, Long> {
    Optional<OptimizedImage> findByOriginalUrl(String originalUrl);

    Optional<OptimizedImage> findBySlugAndImageType(String slug, String imageType);

    List<OptimizedImage> findBySlugIn(List<String> slug);
}
