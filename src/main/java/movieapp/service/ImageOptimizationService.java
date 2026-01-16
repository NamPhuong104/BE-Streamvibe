package movieapp.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import movieapp.config.properties.AppProperties;
import movieapp.config.properties.OPhimProperties;
import movieapp.entity.OptimizedImage;
import movieapp.repository.ImageOptimizationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class ImageOptimizationService {
    private final Cloudinary cloudinary;
    private final ImageOptimizationRepository imageRepository;
    private final AppProperties appProperties;
    private final OPhimProperties oPhimProperties;

    private final ConcurrentHashMap<String, Object> uploadLocks = new ConcurrentHashMap<>();

    public String optimizeThumb(String thumbUrl, String slug) {
        if (thumbUrl == null || thumbUrl.isEmpty()) {
            return null;
        }

        String fullUrl = buildFullUrl(thumbUrl);

        if (!appProperties.getImage().isEnableCloudinary()) {
            return null;
        }

        return getOrUploadImage(fullUrl, "thumb", slug, "w_342,c_fill,q_auto:best,f_auto");
    }

    public String optimizedPoster(String posterUrl, String slug) {
        if (posterUrl == null || posterUrl.isEmpty()) {
            return null;
        }

        String fullUrl = buildFullUrl(posterUrl);

        if (!appProperties.getImage().isEnableCloudinary()) {
            return null;
        }

        return getOrUploadImage(fullUrl, "poster", slug, "w_780,c_fill,q_auto:best,f_auto");
    }


    private String getOrUploadImage(String fullUrl, String imageType, String slug, String transformation) {
        // 1. Quick check - không cần lock
        Optional<OptimizedImage> existing = findInDb(fullUrl, imageType, slug);
        if (existing.isPresent()) {
            return transformUrl(existing.get().getCloudinaryUrl(), transformation);
        }

        // 2. Lấy lock cho URL này
        Object lock = uploadLocks.computeIfAbsent(fullUrl, k -> new Object());

        synchronized (lock) {
            try {
                // 3. Double-check sau khi có lock
                Optional<OptimizedImage> doubleCheck = findInDb(fullUrl, imageType, slug);
                if (doubleCheck.isPresent()) {
                    log.debug("✅ Found after lock for: {}", slug);
                    return transformUrl(doubleCheck.get().getCloudinaryUrl(), transformation);
                }

                // 4. Thực sự upload
                log.info("📤 Uploading {} for {}: {}", imageType, slug, fullUrl);
                String cloudUrl = uploadToCloudinary(fullUrl, imageType, slug);

                if (cloudUrl != null) {
                    return transformUrl(cloudUrl, transformation);
                }
                return null;

            } catch (Exception e) {
                log.error("❌ Failed to upload {} for {}: {}", imageType, slug, e.getMessage());
                return null;
            } finally {
                uploadLocks.remove(fullUrl);
            }
        }
    }

    /**
     * Tìm trong DB bằng URL hoặc slug+type
     */
    private Optional<OptimizedImage> findInDb(String fullUrl, String imageType, String slug) {
        // Ưu tiên tìm theo URL
        Optional<OptimizedImage> byUrl = imageRepository.findByOriginalUrl(fullUrl);
        if (byUrl.isPresent()) {
            return byUrl;
        }

        // Fallback: tìm theo slug + type
        return imageRepository.findBySlugAndImageType(slug, imageType);
    }

    /**
     * Upload lên Cloudinary và lưu DB
     */
    private String uploadToCloudinary(String imageUrl, String type, String slug) {
        try {
            Map uploadResult = cloudinary.uploader().upload(imageUrl, ObjectUtils.asMap(
                    "folder", "movies",
                    "resource_type", "image",
                    "format", "webp",
                    "quality", "auto:good",
                    "fetch_format", "auto"
            ));

            String cloudinaryUrl = (String) uploadResult.get("secure_url");
            String publicId = (String) uploadResult.get("public_id");

            // Lưu DB
            OptimizedImage optimizedImage = OptimizedImage.builder()
                    .originalUrl(imageUrl)
                    .cloudinaryUrl(cloudinaryUrl)
                    .imageType(type)
                    .cloudinaryPublicId(publicId)
                    .slug(slug)
                    .build();

            imageRepository.save(optimizedImage);
            log.info("✅ Uploaded: {} → {}", slug, cloudinaryUrl);

            return cloudinaryUrl;

        } catch (Exception e) {
            log.error("❌ Cloudinary upload failed: {}", e.getMessage());
            return null;
        }
    }

    private String transformUrl(String url, String transformation) {
        if (url == null || !url.contains("/upload/")) {
            return null;
        }
        return url.replaceFirst("/upload/", "/upload/" + transformation + "/");
    }

    public String buildFullUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return null;
        }
        if (imageUrl.startsWith("http")) {
            return imageUrl;
        }
        return oPhimProperties.getFullUrlImage() + "/" + imageUrl;
    }
}
