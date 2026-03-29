package gitlite.security;

import gitlite.model.UserLog;
import gitlite.model.UserLog.LogLevel;

import java.time.LocalDateTime;
import java.util.*;

/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║                    SECURITY MONITOR                              ║
 * ║                                                                  ║
 * ║  Responsibilities:                                               ║
 * ║   • Maintain an append-only activity log                         ║
 * ║   • Detect rollback attacks (reverting to vulnerable versions)   ║
 * ║   • Detect rapid-fire commits (potential automation / bot)       ║
 * ║   • Detect hash mismatches (file tampering)                      ║
 * ║   • Detect mass-delete operations                                ║
 * ╚══════════════════════════════════════════════════════════════════╝
 */
public class SecurityMonitor {

    // ── Tunable thresholds ────────────────────────────────────────────────────
    private static final int    RAPID_COMMIT_THRESHOLD    = 5;    // commits
    private static final long   RAPID_COMMIT_WINDOW_MS    = 10_000; // 10 s
    private static final int    ROLLBACK_SUSPICIOUS_RANGE = 3;    // versions back
    private static final int    MASS_DELETE_THRESHOLD     = 3;    // files

    // ── Internal state ────────────────────────────────────────────────────────
    private final List<UserLog>              allLogs;
    private final List<UserLog>              suspiciousLogs;
    private final Map<String, List<Long>>    commitTimestamps;   // user → commit times
    private final Map<String, Integer>       deleteCounters;     // user → deletes in session

    public SecurityMonitor() {
        allLogs          = new ArrayList<>();
        suspiciousLogs   = new ArrayList<>();
        commitTimestamps = new HashMap<>();
        deleteCounters   = new HashMap<>();
    }

    // ── Logging ───────────────────────────────────────────────────────────────

    /** Records any user action at INFO level. */
    public UserLog log(String username, String action, String file, String details) {
        return addLog(username, action, file, LogLevel.INFO, details);
    }

    /** Records a warning-level event. */
    public UserLog warn(String username, String action, String file, String details) {
        return addLog(username, action, file, LogLevel.WARNING, details);
    }

    /** Records a suspicious event and triggers alert. */
    public UserLog flagSuspicious(String username, String action, String file, String details) {
        UserLog entry = addLog(username, action, file, LogLevel.SUSPICIOUS, details);
        suspiciousLogs.add(entry);
        printAlert("SUSPICIOUS ACTIVITY", entry);
        return entry;
    }

    /** Records a critical security event. */
    public UserLog flagCritical(String username, String action, String file, String details) {
        UserLog entry = addLog(username, action, file, LogLevel.CRITICAL, details);
        suspiciousLogs.add(entry);
        printAlert("CRITICAL SECURITY EVENT", entry);
        return entry;
    }

    private UserLog addLog(String username, String action, String file,
                           LogLevel level, String details) {
        UserLog entry = new UserLog(username, action, file, level, details);
        allLogs.add(entry);
        return entry;
    }

    // ── Detection Routines ────────────────────────────────────────────────────

    /**
     * Rollback-attack detection.
     *
     * A "rollback attack" attempts to revert a system to an older version
     * that contains known vulnerabilities. We flag as suspicious if the
     * rollback goes back more than ROLLBACK_SUSPICIOUS_RANGE versions
     * AND is performed by an unfamiliar action pattern.
     *
     * @param username        user performing the rollback
     * @param currentVersion  current (latest) version number
     * @param targetVersion   version being rolled back to
     * @param filename        file being rolled back
     * @return                true if flagged as suspicious
     */
    public boolean checkRollbackAttack(String username, int currentVersion,
                                       int targetVersion, String filename) {
        int delta = currentVersion - targetVersion;
        log(username, "ROLLBACK", filename,
                String.format("Rolling back from v%d → v%d (delta=%d)",
                        currentVersion, targetVersion, delta));

        if (delta > ROLLBACK_SUSPICIOUS_RANGE) {
            flagSuspicious(username, "ROLLBACK_ATTACK_SIM", filename,
                    String.format("Large rollback detected! v%d → v%d (delta=%d). " +
                            "Possible attempt to restore vulnerable version.",
                            currentVersion, targetVersion, delta));
            return true;
        }
        return false;
    }

    /**
     * Rapid-commit detection.
     *
     * Tracks commit timestamps per user and flags if too many commits
     * occur within a short window (possible automated injection).
     *
     * @param username  user making the commit
     * @param filename  file being committed
     * @return          true if flagged
     */
    public boolean checkRapidCommits(String username, String filename) {
        long now = System.currentTimeMillis();
        commitTimestamps.putIfAbsent(username, new ArrayList<>());
        List<Long> times = commitTimestamps.get(username);

        // Keep only timestamps within the window
        times.removeIf(t -> (now - t) > RAPID_COMMIT_WINDOW_MS);
        times.add(now);

        log(username, "COMMIT", filename,
                "Commit recorded. Recent commit count: " + times.size());

        if (times.size() >= RAPID_COMMIT_THRESHOLD) {
            flagSuspicious(username, "RAPID_COMMITS", filename,
                    String.format("%d commits in %ds — possible automated injection attack.",
                            times.size(), RAPID_COMMIT_WINDOW_MS / 1000));
            return true;
        }
        return false;
    }

    /**
     * Hash-mismatch (file-tampering) detection.
     *
     * Should be called whenever a file's content is read from storage.
     * Compares the recomputed hash against the stored commit hash.
     *
     * @param username      user reading the file
     * @param filename      file being read
     * @param storedContent content stored in the version snapshot
     * @param storedHash    hash recorded at commit time
     * @return              true if tampering detected
     */
    public boolean checkTampering(String username, String filename,
                                  String storedContent, String storedHash) {
        boolean ok = HashUtils.verifyIntegrity(storedContent, storedHash);
        if (!ok) {
            flagCritical(username, "HASH_MISMATCH", filename,
                    "Content hash does not match stored hash! File may have been tampered with.\n" +
                    "  Expected : " + storedHash + "\n" +
                    "  Got      : " + HashUtils.sha256(storedContent));
        } else {
            log(username, "INTEGRITY_CHECK", filename, "Hash verified OK → " + storedHash.substring(0, 12));
        }
        return !ok;
    }

    /**
     * Mass-delete detection.
     *
     * Flags users who delete many files in one session.
     *
     * @param username  user performing the deletion
     * @param filename  file being deleted
     * @return          true if flagged
     */
    public boolean checkMassDelete(String username, String filename) {
        deleteCounters.merge(username, 1, Integer::sum);
        int count = deleteCounters.get(username);
        log(username, "DELETE", filename, "Total deletes this session: " + count);

        if (count >= MASS_DELETE_THRESHOLD) {
            flagSuspicious(username, "MASS_DELETE", filename,
                    String.format("User '%s' has deleted %d files this session — possible sabotage.",
                            username, count));
            return true;
        }
        return false;
    }

    // ── Reporting ─────────────────────────────────────────────────────────────

    /** Prints all logs in chronological order. */
    public void printAllLogs() {
        if (allLogs.isEmpty()) {
            System.out.println("  No activity logged yet.");
            return;
        }
        allLogs.forEach(l -> System.out.println("  " + l));
    }

    /** Prints only suspicious/critical logs. */
    public void printSuspiciousLogs() {
        if (suspiciousLogs.isEmpty()) {
            System.out.println("  ✓ No suspicious activity detected.");
            return;
        }
        System.out.println("  ⚠  SUSPICIOUS ACTIVITY REPORT (" + suspiciousLogs.size() + " events)");
        System.out.println("  " + "─".repeat(70));
        suspiciousLogs.forEach(l -> System.out.println("  " + l));
    }

    /** Returns count of all logs. */
    public int totalLogs()      { return allLogs.size(); }

    /** Returns count of suspicious/critical logs. */
    public int suspiciousCount(){ return suspiciousLogs.size(); }

    /** Returns a copy of all logs. */
    public List<UserLog> getAllLogs()        { return Collections.unmodifiableList(allLogs); }

    /** Returns a copy of suspicious logs only. */
    public List<UserLog> getSuspiciousLogs(){ return Collections.unmodifiableList(suspiciousLogs); }

    // ── Alert printing ────────────────────────────────────────────────────────

    private void printAlert(String type, UserLog log) {
        System.out.println();
        System.out.println("  ╔══════════════════════════════════════════════════╗");
        System.out.printf ("  ║  ⚠  %-44s  ║%n", type);
        System.out.println("  ╠══════════════════════════════════════════════════╣");
        System.out.printf ("  ║  User    : %-38s  ║%n", log.getUsername());
        System.out.printf ("  ║  Action  : %-38s  ║%n", log.getAction());
        System.out.printf ("  ║  File    : %-38s  ║%n", log.getTargetFile());
        System.out.printf ("  ║  Details : %-38s  ║%n",
                log.getDetails().length() > 38
                        ? log.getDetails().substring(0, 35) + "..." : log.getDetails());
        System.out.println("  ╚══════════════════════════════════════════════════╝");
        System.out.println();
    }
}
