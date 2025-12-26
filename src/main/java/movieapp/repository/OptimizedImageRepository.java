package movieapp.repository;

import movieapp.domain.OptimizedImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OptimizedImageRepository extends JpaRepository<OptimizedImage, Long> {
    Optional<OptimizedImage> findByOriginalUrl(String originalUrl);

    Optional<OptimizedImage> findBySlug(String slug);

    @Query("SELECT o FROM OptimizedImage o WHERE o.slug IN :slugs")
    List<OptimizedImage> findAllBySlugs(@Param("slugs") List<String> slugs);

    List<OptimizedImage> findBySlugIn(List<String> slug);
}
