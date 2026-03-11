package movieapp.util;

import lombok.RequiredArgsConstructor;
import movieapp.config.properties.OPhimProperties;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
@RequiredArgsConstructor
public class Util {
    private static final SecureRandom random = new SecureRandom();
    private final OPhimProperties oPhimProperties;

    /**
     * Build full image URL từ relative path ophim
     * - Nếu null/empty → return null
     * - Nếu đã là full URL (http...) → return nguyên
     * - Nếu relative path → prefix với ophim CDN
     */
    public String buildFullUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) return null;
        if (imageUrl.startsWith("http")) return imageUrl;
        return oPhimProperties.getFullUrlImage() + "/" + imageUrl;
    }

    public String normalizeEpisode(String episodeSlug) {
        if (episodeSlug == null || episodeSlug.trim().isEmpty()) return null;
        return episodeSlug.trim();
    }

    public double calculateProgress(Long currentTime, Long duration) {
        if (currentTime == null || duration == null) return 0.0;
        return (double) currentTime / duration * 100;
    }

    public double roundToOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    public String formatTime(Long seconds) {
        if (seconds == null || seconds < 0) return "00:00";

        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        if (hours > 0) return String.format("%02d:%02d:%02d", hours, minutes, secs);
        return String.format("%02d:%02d", minutes, secs);
    }

    public String emptyToNull(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        return value.trim();
    }

    public static String generateOtp() {
        int otp = random.nextInt(1_000_000);
        return String.format("%06d", otp);
    }

}
