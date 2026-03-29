package gitlite.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Provides hashing utilities for content integrity verification.
 *
 * Two levels of hashing are used:
 *  1. SHA-256  – used for content hashes (tamper detection).
 *  2. Custom polynomial hash – lightweight version ID generator
 *     (illustrates custom hash function for the data-structures course).
 */
public class HashUtils {

    private HashUtils() {}   // utility class – no instances

    // ── SHA-256 (tamper detection) ────────────────────────────────────────────

    /**
     * Computes the SHA-256 hash of the given content.
     * Used to detect whether a file version has been tampered with.
     *
     * @param content  the file content to hash
     * @return         64-character hex string, e.g. "a3f1..."
     */
    public static String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed by the JVM spec — this path is unreachable
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * Verifies that content matches a previously stored hash.
     *
     * @param content       current content
     * @param expectedHash  hash stored at commit time
     * @return              true = content unchanged; false = tampered
     */
    public static boolean verifyIntegrity(String content, String expectedHash) {
        return sha256(content).equals(expectedHash);
    }

    // ── Polynomial Version ID ────────────────────────────────────────────────

    /**
     * Generates a short, unique version ID from content + metadata.
     *
     * Algorithm: polynomial rolling hash (similar to Java's String.hashCode
     * but extended to 64-bit and seeded with a timestamp for uniqueness).
     *
     * This is the *custom* hash function required for the course. It
     * intentionally differs from SHA-256 to demonstrate that multiple
     * hash algorithms serve different purposes.
     *
     * Steps:
     *  1. Combine content, author, and timestamp into one seed string.
     *  2. Apply FNV-1a (Fowler–Noll–Vo) variant for strong avalanche effect.
     *  3. Convert to 16-character hex string.
     *
     * @param content   file content
     * @param author    commit author
     * @param timestamp current time millis (provides uniqueness)
     * @return          16-char hex version ID
     */
    public static String generateVersionId(String content, String author, long timestamp) {
        String seed = content + author + timestamp;

        // FNV-1a over the seed bytes
        long hash = 0xcbf29ce484222325L;              // FNV offset basis (64-bit)
        for (byte b : seed.getBytes(StandardCharsets.UTF_8)) {
            hash ^= (b & 0xFFL);
            hash *= 0x100000001b3L;                   // FNV prime (64-bit)
        }

        return String.format("%016x", hash);          // 16-char hex
    }

    // ── Hash Collision Demo ───────────────────────────────────────────────────

    /**
     * A deliberately simple (and collision-prone) hash function.
     * Included for educational contrast against SHA-256.
     *
     * This is a naïve sum-of-ASCII hash — demonstrates WHY we need
     * cryptographic hashes for security.
     *
     * @param content  input string
     * @return         integer hash value
     */
    public static int naiveHash(String content) {
        int sum = 0;
        for (char c : content.toCharArray()) {
            sum += c;
        }
        return Math.abs(sum) % 1000;   // mod 1000 — obviously collision-prone
    }

    /**
     * Computes a short "display hash" (first 12 chars of SHA-256)
     * for log output.
     */
    public static String shortHash(String content) {
        return sha256(content).substring(0, 12);
    }
}
