package movieapp.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import movieapp.domain.OptimizedImage;
import movieapp.repository.OptimizedImageRepository;
import movieapp.util.error.IdInvalidException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class ImageOptimizationService {
    @Value("${ophim.full-url-image}")
    private String imageCdn;

    @Value("${app.image.enable-cloudinary:false}")
    private boolean enableCloudinaryUpload;

    private final Cloudinary cloudinary;
    private final OptimizedImageRepository imageRepository;

    public ImageOptimizationService(Cloudinary cloudinary, OptimizedImageRepository imageRepository) {
        this.cloudinary = cloudinary;
        this.imageRepository = imageRepository;
    }

    public String optimizeThumb(String thumbUrl, String slug) {
        if (thumbUrl == null || thumbUrl.isEmpty()) {
            return null;
        }
        String fullUrl = buildFullUrl(thumbUrl);

        if (!enableCloudinaryUpload) {
            return fullUrl;
        }

        String cloudUrl = imageRepository.findByOriginalUrl(fullUrl).map(OptimizedImage::getCloudinaryUrl).orElseGet(() -> {
            try {
                return uploadToCloudinarySync(fullUrl, "thumb", slug);
            } catch (Exception e) {
                log.warn("⚠️ Failed to upload thumb, using original: {}", e.getMessage());
                return fullUrl;
            }
        });

        return transformUrl(cloudUrl, "w_342,c_fill,q_auto:best,f_auto");
    }

    public String optimizedPoster(String posterUrl, String slug) {
        if (posterUrl == null || posterUrl.isEmpty()) {
            return null;
        }
        String fullUrl = buildFullUrl(posterUrl);
        if (!enableCloudinaryUpload) {
            return fullUrl;
        }
        String cloudUrl = imageRepository.findByOriginalUrl(fullUrl)
                .map(OptimizedImage::getCloudinaryUrl)
                .orElseGet(() -> {
                    try {
                        return uploadToCloudinarySync(fullUrl, "poster", slug);
                    } catch (Exception e) {
                        log.warn("⚠️ Failed to upload poster, using original: {}", e.getMessage());
                        return fullUrl;
                    }
                });
        return transformUrl(cloudUrl, "w_780,c_fill,q_auto:best,f_auto");
    }

    public String getOptimizedThumbOriginal(String thumbUrl) {
        if (thumbUrl == null || thumbUrl.isEmpty()) return null;

        String fullUrl = buildFullUrl(thumbUrl);
        return imageRepository.findByOriginalUrl(fullUrl)
                .map(OptimizedImage::getCloudinaryUrl)
                .map(url -> transformUrl(url, "w_342,c_fill,q_auto:best,f_auto"))
                .orElse(fullUrl);
    }

    public String getOptimizedPosterOriginal(String posterUrl) {
        if (posterUrl == null || posterUrl.isEmpty()) return null;

        String fullUrl = buildFullUrl(posterUrl);

        return imageRepository.findByOriginalUrl(fullUrl).map(OptimizedImage::getCloudinaryUrl)
                .map(url -> transformUrl(fullUrl, "w_780,c_fill,q_auto:best,f_auto"))
                .orElse(fullUrl);
    }

    private String transformUrl(String url, String transformation) {
        if (url == null || !url.contains("/upload")) return null;

        return url.replace("upload", "/upload/" + transformation + "/");
    }

    private String uploadToCloudinarySync(String imageUrl, String type, String slug) {
        try {
            log.info("📤 Uploading {} to Cloudinary: {}", type, imageUrl);

            Map uploadResult = cloudinary.uploader().upload(imageUrl, ObjectUtils.asMap(
                    "folder", "movies",
                    "resource_type", "image",
                    "format", "webp",
                    "quality", "auto:good",
                    "fetch_format", "auto"
            ));
            String cloudinaryUrl = (String) uploadResult.get("secure_url");
            String publicId = (String) uploadResult.get("public_id");

            OptimizedImage optimizedImage = OptimizedImage.builder()
                    .originalUrl(imageUrl)
                    .cloudinaryUrl(cloudinaryUrl)
                    .imageType(type)
                    .cloudinaryPublicId(publicId)
                    .slug(slug)
                    .build();

            imageRepository.save(optimizedImage);

            log.info("✅ Uploaded successfully: {}", cloudinaryUrl);

            return cloudinaryUrl;

        } catch (Exception e) {
            log.error("❌ Failed to upload {}: {}", imageUrl, e.getMessage());
            return imageUrl;
        }
    }

    public String buildFullUrl(String imageUrl) {
        if (imageUrl.startsWith("http")) {
            return imageUrl;
        }
        return imageCdn + "/" + imageUrl;
    }
}
