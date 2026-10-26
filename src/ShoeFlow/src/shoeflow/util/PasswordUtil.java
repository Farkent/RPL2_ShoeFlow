package shoeflow.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Pencocokan password. Mendukung password plain-text maupun hash SHA-256 (hex) di kolom users.password. */
public class PasswordUtil {

    public static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean matches(String input, String stored) {
        return stored.equals(input) || stored.equalsIgnoreCase(sha256(input));
    }
}
