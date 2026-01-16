package movieapp.util;

import java.text.Normalizer;
import java.util.Random;
import java.util.regex.Pattern;

public class UsernameGenerator {
    private static final Random random = new Random();

    public static final String USERNAME_PATTERN = "^[a-zA-Z0-9_-]+$";
    public static final Pattern USERNAME_REGEX = Pattern.compile(USERNAME_PATTERN);

    public static final int MIN_LENGTH = 3;
    public static final int MAX_LENGTH = 30;

    /**
     * Validate username
     */
    public static boolean isValidUserName(String username) {
        if (username == null || username.isBlank()) return false;
        if (username.length() < MIN_LENGTH || username.length() > MAX_LENGTH) return false;

        return USERNAME_REGEX.matcher(username).matches();
    }

    /**
     * Generate username từ tên Google
     * Ví dụ: "Nguyễn Văn An" -> "nguyen_van_an_8x4k"
     */
    public static String generateFromName(String fullName) {
        if (fullName == null || fullName.isBlank()) return generateRandom();

        // 1. Normalize: bỏ dấu tiếng Việt
        String normalized = removeAccent(fullName);

        // 2. Lowercase
        normalized = normalized.toLowerCase();

        // 3. Replace khoảng trắng bằng underscore
        normalized = normalized.replace("\\s+", "_");

        // 4. Bỏ tất cả ký tự không hợp lệ
        normalized = normalized.replaceAll("[^a-z0-9_-]", "");

        // 5. Bỏ underscore ở đầu/cuối và duplicate
        normalized = normalized.replaceAll("^_+|_+$", "");
        normalized = normalized.replaceAll("_+", "_");

        // 6. Giới hạn độ dài phần tên (để còn chỗ cho suffix)
        if (normalized.length() > 20) normalized = normalized.substring(0, 20);


        // 7. Thêm suffix random
        String suffix = generateRandomSuffix(4);

        // 8. Nếu phần tên rỗng, dùng prefix mặc định
        if (normalized.isEmpty()) {
            normalized = "user";
        }

        return normalized + "_" + suffix;
    }

    /**
     * Generate username hoàn toàn random
     * Ví dụ: "user_a8k2m9x4"
     */
    public static String generateRandom() {
        return "user_" + generateRandomSuffix(8);
    }

    /**
     * Generate random suffix (chữ và số)
     */
    public static String generateRandomSuffix(int length) {
        String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }

        return sb.toString();
    }

    /**
     * Bỏ dấu tiếng Việt và các ký tự đặc biệt
     */
    public static String removeAccent(String input) {
        if (input == null) return null;

        // Normalize to NFD (Canonical Decomposition)
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);

        // Remove diacritical marks
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String result = pattern.matcher(normalized).replaceAll("");

        // Handle special Vietnamese characters
        result = result.replace("đ", "d").replace("Đ", "D");

        return result;
    }
}
