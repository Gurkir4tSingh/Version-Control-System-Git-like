package gitlite.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents a log entry recording user activity.
 * Used for audit trails and suspicious activity detection.
 */
public class UserLog {

    public enum LogLevel {
        INFO,
        WARNING,
        SUSPICIOUS,
        CRITICAL
    }

    private final String    username;
    private final String    action;
    private final String    targetFile;
    private final LocalDateTime timestamp;
    private final LogLevel  level;
    private final String    details;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public UserLog(String username, String action, String targetFile,
                   LogLevel level, String details) {
        this.username   = username;
        this.action     = action;
        this.targetFile = targetFile;
        this.timestamp  = LocalDateTime.now();
        this.level      = level;
        this.details    = details;
    }

    public String    getUsername()   { return username; }
    public String    getAction()     { return action; }
    public String    getTargetFile() { return targetFile; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public LogLevel  getLevel()      { return level; }
    public String    getDetails()    { return details; }

    public boolean isSuspicious() {
        return level == LogLevel.SUSPICIOUS || level == LogLevel.CRITICAL;
    }

    @Override
    public String toString() {
        String levelTag = switch (level) {
            case INFO      -> "[INFO    ]";
            case WARNING   -> "[WARNING ]";
            case SUSPICIOUS-> "[SUSPECT ]";
            case CRITICAL  -> "[CRITICAL]";
        };
        return String.format("%s %s | User: %-12s | Action: %-20s | File: %-15s | %s",
                levelTag,
                timestamp.format(FORMATTER),
                username, action, targetFile, details);
    }
}
