package tn.esprit.agri.utils;

public final class PasswordValidator {

    // Strong password rule:
    // - min 8 chars
    // - 1 uppercase, 1 lowercase, 1 digit, 1 special char
    private static final String STRONG_PASSWORD_REGEX =
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&._#-])[A-Za-z\\d@$!%*?&._#-]{8,}$";


    private PasswordValidator() {
        // utility class
    }

    public static boolean isStrong(String password) {
        return password != null && password.matches(STRONG_PASSWORD_REGEX);
    }
}
