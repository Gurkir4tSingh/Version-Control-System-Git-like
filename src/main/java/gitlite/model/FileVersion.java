package gitlite.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents a single version (commit) of a file.
 * Used as a node in the VersionLinkedList.
 *
 * Data Structure: Node element for Doubly Linked List
 */
public class FileVersion {

    // ── Linked List pointers ──────────────────────────────────────────────────
    public FileVersion next;
    public FileVersion prev;

    // ── Version metadata ──────────────────────────────────────────────────────
    private final String versionId;          // Unique commit hash
    private final String content;            // Full file content snapshot
    private final String author;             // Who made the change
    private final String commitMessage;      // Commit message
    private final LocalDateTime timestamp;   // When the commit was made
    private final String contentHash;        // SHA-like hash for tamper detection
    private final int versionNumber;         // Sequential version number

    // ── Branch info ───────────────────────────────────────────────────────────
    private final String branchName;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public FileVersion(String versionId, String content, String author,
                       String commitMessage, String contentHash,
                       int versionNumber, String branchName) {
        this.versionId     = versionId;
        this.content       = content;
        this.author        = author;
        this.commitMessage = commitMessage;
        this.timestamp     = LocalDateTime.now();
        this.contentHash   = contentHash;
        this.versionNumber = versionNumber;
        this.branchName    = branchName;
        this.next          = null;
        this.prev          = null;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public String getVersionId()      { return versionId; }
    public String getContent()        { return content; }
    public String getAuthor()         { return author; }
    public String getCommitMessage()  { return commitMessage; }
    public LocalDateTime getTimestamp(){ return timestamp; }
    public String getContentHash()    { return contentHash; }
    public int getVersionNumber()     { return versionNumber; }
    public String getBranchName()     { return branchName; }

    /**
     * Returns a formatted one-line summary for log display.
     */
    public String getSummary() {
        return String.format("[v%d] %s | Author: %-12s | Branch: %-8s | %s | \"%s\"",
                versionNumber,
                versionId.substring(0, 8),
                author,
                branchName,
                timestamp.format(FORMATTER),
                commitMessage);
    }

    @Override
    public String toString() {
        return String.format(
                "┌─ Version #%d ─────────────────────────────────────\n" +
                "│  ID      : %s\n" +
                "│  Author  : %s\n" +
                "│  Branch  : %s\n" +
                "│  Date    : %s\n" +
                "│  Hash    : %s\n" +
                "│  Message : \"%s\"\n" +
                "└───────────────────────────────────────────────────",
                versionNumber, versionId, author, branchName,
                timestamp.format(FORMATTER), contentHash, commitMessage);
    }
}
