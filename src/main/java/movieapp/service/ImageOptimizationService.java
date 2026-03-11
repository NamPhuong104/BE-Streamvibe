package movieapp.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import movieapp.config.properties.AppProperties;
import movieapp.config.properties.OPhimProperties;
import movieapp.config.properties.UpAnhNhanhProperties;
import movieapp.dto.UpanhNhanhResponse;
import movieapp.entity.OptimizedImage;
import movieapp.repository.ImageOptimizationRepository;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
@RequiredArgsConstructor
public class ImageOptimizationService {

    private final RestTemplate restTemplate;
    private final ImageOptimizationRepository imageRepository;
    private final AppProperties appProperties;
    private final OPhimProperties oPhimProperties;
    private final UpAnhNhanhProperties upanhNhanhProperties;

    private final ConcurrentHashMap<String, Object> uploadLocks = new ConcurrentHashMap<>();

    // ===== Image Processing Config =====
    private static final int THUMB_WIDTH = 342;
    private static final int POSTER_WIDTH = 1080;
    private static final float WEBP_QUALITY = 0.80f;
    private static final float JPEG_FALLBACK_QUALITY = 0.70f;

    // ===== Rate Limiter (API limit: 30/min, buffer at 25) =====
    private static final int MAX_REQUESTS_PER_MINUTE = 25;
    private final AtomicInteger requestCount = new AtomicInteger(0);
    private volatile long windowStart = System.currentTimeMillis();

    // ===== WebP Support Detection =====
    private boolean webpSupported;

    @PostConstruct
    public void init() {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("webp");
        webpSupported = writers.hasNext();
        if (webpSupported) {
            log.info("✅ WebP ImageWriter available - images will be converted to WebP");
        } else {
            log.warn("⚠️ WebP ImageWriter NOT available - fallback to compressed JPEG");
        }
    }

    // =================================================================
    // PUBLIC METHODS
    // =================================================================

    public String optimizeThumb(String thumbUrl, String slug) {
        if (thumbUrl == null || thumbUrl.isEmpty()) return null;
        if (!appProperties.getImage().isEnableImageOptimization()) return null;
        String fullUrl = buildFullUrl(thumbUrl);
        return getOrUploadImage(fullUrl, "thumb", slug);
    }

    public String optimizedPoster(String posterUrl, String slug) {
        if (posterUrl == null || posterUrl.isEmpty()) return null;
        if (!appProperties.getImage().isEnableImageOptimization()) return null;
        String fullUrl = buildFullUrl(posterUrl);
        return getOrUploadImage(fullUrl, "poster", slug);
    }

    // =================================================================
    // CORE LOGIC
    // =================================================================

    private String getOrUploadImage(String fullUrl, String imageType, String slug) {
        // 1. Quick check DB
        Optional<OptimizedImage> existing = findInDb(fullUrl, imageType, slug);
        if (existing.isPresent()) {
            return existing.get().getOptimizedUrl();
        }

        // 2. Check rate limit
        if (!canMakeRequest()) {
            log.debug("⏳ Rate limit reached, skip upload: {} [{}]", slug, imageType);
            return null;
        }

        // 3. Lock per URL
        Object lock = uploadLocks.computeIfAbsent(fullUrl, k -> new Object());

        synchronized (lock) {
            try {
                // 4. Double-check after lock
                Optional<OptimizedImage> doubleCheck = findInDb(fullUrl, imageType, slug);
                if (doubleCheck.isPresent()) {
                    return doubleCheck.get().getOptimizedUrl();
                }

                // 5. Download → Resize → Convert → Upload
                log.info("📤 Processing {} [{}]: {}", slug, imageType, fullUrl);

                int targetWidth = "thumb".equals(imageType) ? THUMB_WIDTH : POSTER_WIDTH;
                byte[] processedBytes = downloadAndProcess(fullUrl, targetWidth);

                if (processedBytes != null) {
                    // Upload processed file (WebP/JPEG)
                    String ext = webpSupported ? "webp" : "jpg";
                    String filename = slug + "-" + imageType + "." + ext;
                    return uploadFileToUpanhNhanh(processedBytes, filename, fullUrl, imageType, slug);
                } else {
                    // Fallback: upload original URL
                    log.warn("⚠️ Processing failed, fallback to URL upload: {} [{}]", slug, imageType);
                    return uploadUrlToUpanhNhanh(fullUrl, imageType, slug);
                }

            } catch (Exception e) {
                log.error("❌ Failed {} [{}]: {}", slug, imageType, e.getMessage());
                return null;
            } finally {
                uploadLocks.remove(fullUrl);
            }
        }
    }

    // =================================================================
    // IMAGE PROCESSING: Download → Resize → Convert
    // =================================================================

    private byte[] downloadAndProcess(String imageUrl, int targetWidth) {
        try {
            // Step 1: Download
            byte[] originalBytes = downloadImage(imageUrl);
            if (originalBytes == null) return null;

            // Step 2: Decode
            BufferedImage original = ImageIO.read(new java.io.ByteArrayInputStream(originalBytes));
            if (original == null) {
                log.warn("⚠️ Cannot decode image: {}", imageUrl);
                return null;
            }

            // Step 3: Resize (only downscale, keep aspect ratio)
            BufferedImage resized = resizeImage(original, targetWidth);

            // Step 4: Convert format
            byte[] result;
            if (webpSupported) {
                result = toWebP(resized);
                if (result == null) {
                    result = toCompressedJpeg(resized);
                }
            } else {
                result = toCompressedJpeg(resized);
            }

            // Log compression stats
            if (result != null) {
                int originalKB = originalBytes.length / 1024;
                int resultKB = result.length / 1024;
                String ratio = String.format("%.0f%%", (float) result.length / originalBytes.length * 100);
                log.info("📊 Compressed: {}KB → {}KB ({})", originalKB, resultKB, ratio);
            }

            return result;

        } catch (Exception e) {
            log.warn("⚠️ Image processing failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Download image bytes from URL
     */
    private byte[] downloadImage(String imageUrl) {
        try {
            URI uri = URI.create(imageUrl);
            try (InputStream is = uri.toURL().openStream()) {
                return is.readAllBytes();
            }
        } catch (Exception e) {
            log.warn("⚠️ Download failed: {} - {}", imageUrl, e.getMessage());
            return null;
        }
    }

    /**
     * Resize image maintaining aspect ratio (only downscale)
     */
    private BufferedImage resizeImage(BufferedImage original, int targetWidth) {
        int originalWidth = original.getWidth();
        int originalHeight = original.getHeight();

        // Don't upscale
        if (originalWidth <= targetWidth) {
            return original;
        }

        // Calculate height maintaining aspect ratio
        int targetHeight = (int) ((double) targetWidth / originalWidth * originalHeight);

        BufferedImage resized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resized.createGraphics();

        // White background (for PNG with transparency)
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, targetWidth, targetHeight);

        // High quality resize
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.drawImage(original, 0, 0, targetWidth, targetHeight, null);
        g2d.dispose();

        return resized;
    }

    /**
     * Convert BufferedImage to WebP bytes
     */
    private byte[] toWebP(BufferedImage image) {
        try {
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("webp");
            if (!writers.hasNext()) return null;

            ImageWriter writer = writers.next();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
            writer.setOutput(ios);

            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(WEBP_QUALITY);
            }

            writer.write(null, new IIOImage(image, null, null), param);
            writer.dispose();
            ios.close();

            return baos.toByteArray();

        } catch (Exception e) {
            log.warn("⚠️ WebP conversion failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Fallback: Convert BufferedImage to compressed JPEG bytes
     */
    private byte[] toCompressedJpeg(BufferedImage image) {
        try {
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
            if (!writers.hasNext()) return null;

            ImageWriter writer = writers.next();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
            writer.setOutput(ios);

            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(JPEG_FALLBACK_QUALITY);

            writer.write(null, new IIOImage(image, null, null), param);
            writer.dispose();
            ios.close();

            return baos.toByteArray();

        } catch (Exception e) {
            log.error("❌ JPEG compression failed: {}", e.getMessage());
            return null;
        }
    }

    // =================================================================
    // UPLOAD TO UPANHNHANH
    // =================================================================

    /**
     * Upload processed file (WebP/JPEG) as multipart
     */
    private String uploadFileToUpanhNhanh(byte[] fileBytes, String filename,
                                          String originalUrl, String imageType, String slug) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-API-Key", upanhNhanhProperties.getApiKey());
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            ByteArrayResource resource = new ByteArrayResource(fileBytes) {
                @Override
                public String getFilename() {
                    return filename;
                }
            };
            body.add("images[]", resource);

            incrementRequestCount();

            HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<UpanhNhanhResponse> response = restTemplate.postForEntity(
                    upanhNhanhProperties.getBaseUrl() + "/upload",
                    request,
                    UpanhNhanhResponse.class
            );

            return handleResponse(response, originalUrl, imageType, slug);

        } catch (HttpClientErrorException.TooManyRequests e) {
            log.warn("⚠️ Rate limited (429): {} [{}]", slug, imageType);
            requestCount.set(MAX_REQUESTS_PER_MINUTE);
            return null;

        } catch (HttpClientErrorException e) {
            log.error("❌ HTTP {}: {} [{}] - {}", e.getStatusCode(), slug, imageType, e.getResponseBodyAsString());
            return null;

        } catch (Exception e) {
            log.error("❌ File upload error: {} [{}] - {}", slug, imageType, e.getMessage());
            return null;
        }
    }

    /**
     * Fallback: Upload via URL (original format, no conversion)
     */
    private String uploadUrlToUpanhNhanh(String imageUrl, String imageType, String slug) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-API-Key", upanhNhanhProperties.getApiKey());
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> body = Map.of("image_url", imageUrl);
            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

            incrementRequestCount();

            ResponseEntity<movieapp.dto.UpanhNhanhResponse> response = restTemplate.postForEntity(
                    upanhNhanhProperties.getBaseUrl() + "/upload",
                    request,
                    UpanhNhanhResponse.class
            );

            return handleResponse(response, imageUrl, imageType, slug);

        } catch (HttpClientErrorException.TooManyRequests e) {
            log.warn("⚠️ Rate limited (429): {} [{}]", slug, imageType);
            requestCount.set(MAX_REQUESTS_PER_MINUTE);
            return null;

        } catch (Exception e) {
            log.error("❌ URL upload error: {} [{}] - {}", slug, imageType, e.getMessage());
            return null;
        }
    }

    /**
     * Handle API response and save to DB
     */
    private String handleResponse(ResponseEntity<movieapp.dto.UpanhNhanhResponse> response,
                                  String originalUrl, String imageType, String slug) {
        movieapp.dto.UpanhNhanhResponse body = response.getBody();
        if (body == null || !body.isSuccess()
                || body.getData() == null || body.getData().isEmpty()) {
            log.error("❌ Invalid response: {} [{}] - errors: {}",
                    slug, imageType,
                    body != null ? body.getErrors() : "null");
            return null;
        }

        String proxyUrl = body.getData().get(0).getProxyUrl();

        // Save to DB
        OptimizedImage image = OptimizedImage.builder()
                .originalUrl(originalUrl)
                .optimizedUrl(proxyUrl)
                .imageType(imageType)
                .slug(slug)
                .build();
        imageRepository.save(image);

        log.info("✅ Uploaded {} [{}] → {}", slug, imageType, proxyUrl);
        return proxyUrl;
    }

    // =================================================================
    // RATE LIMITER
    // =================================================================

    private boolean canMakeRequest() {
        resetWindowIfNeeded();
        return requestCount.get() < MAX_REQUESTS_PER_MINUTE;
    }

    private void incrementRequestCount() {
        resetWindowIfNeeded();
        requestCount.incrementAndGet();
    }

    private void resetWindowIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - windowStart > 60_000) {
            requestCount.set(0);
            windowStart = now;
        }
    }

    // =================================================================
    // HELPER
    // =================================================================

    private Optional<OptimizedImage> findInDb(String fullUrl, String imageType, String slug) {
        Optional<OptimizedImage> byUrl = imageRepository.findByOriginalUrl(fullUrl);
        if (byUrl.isPresent()) return byUrl;
        return imageRepository.findBySlugAndImageType(slug, imageType);
    }

    public String buildFullUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) return null;
        if (imageUrl.startsWith("http")) return imageUrl;
        return oPhimProperties.getFullUrlImage() + "/" + imageUrl;
    }
}