package movieapp.util.constant;

public class ValidationConstant {
    // ========== USERNAME ==========
    public static final int USERNAME_MIN_LENGTH = 3;
    public static final int USERNAME_MAX_LENGTH = 30;
    public static final String USERNAME_PATTERN = "^[a-zA-Z0-9_-]+$";
    public static final String USERNAME_MESSAGE = "Username chỉ được chứa chữ cái, số, dấu gạch dưới và gạch ngang";

    // ========== PASSWORD ==========
    public static final int PASSWORD_MIN_LENGTH = 6;
    public static final int PASSWORD_MAX_LENGTH = 50;

    // ========== EMAIL ==========
    public static final int EMAIL_MAX_LENGTH = 100;

    // ========== FULLNAME ==========
    public static final int FULLNAME_MIN_LENGTH = 1;
    public static final int FULLNAME_MAX_LENGTH = 50;
}
