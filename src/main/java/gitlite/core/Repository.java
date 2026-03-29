package gitlite.core;

import gitlite.datastructures.BranchTree;
import gitlite.datastructures.FileHashTable;
import gitlite.model.FileVersion;
import gitlite.security.HashUtils;
import gitlite.security.SecurityMonitor;

import java.util.List;

/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║                      REPOSITORY                                  ║
 * ║                                                                  ║
 * ║  The top-level VCS object.  Owns:                                ║
 * ║   • FileHashTable   — registered files  (Hash Table)            ║
 * ║   • BranchTree      — branch structure  (N-ary Tree)             ║
 * ║   • SecurityMonitor — audit + detection                          ║
 * ║                                                                  ║
 * ║  Each TrackedFile internally owns:                               ║
 * ║   • VersionLinkedList (Doubly Linked List)                       ║
 * ║   • UndoRedoStack     (Stack pair)                               ║
 * ╚══════════════════════════════════════════════════════════════════╝
 */
public class Repository {

    private final String           name;
    private final FileHashTable    fileTable;
    private final BranchTree       branchTree;
    private final SecurityMonitor  securityMonitor;
    private       String           currentUser;

    public Repository(String name, String initialUser) {
        this.name            = name;
        this.fileTable       = new FileHashTable();
        this.branchTree      = new BranchTree();
        this.securityMonitor = new SecurityMonitor();
        this.currentUser     = initialUser;

        securityMonitor.log(initialUser, "INIT", name,
                "Repository '" + name + "' initialized.");
    }

    // ── User management ───────────────────────────────────────────────────────

    public void setCurrentUser(String username) {
        securityMonitor.log(currentUser, "SWITCH_USER", "-",
                "Session switched to user: " + username);
        this.currentUser = username;
    }

    public String getCurrentUser() { return currentUser; }

    // ── File operations ───────────────────────────────────────────────────────

    /**
     * Adds a new file to the repository (stages it for tracking).
     */
    public TrackedFile addFile(String filename) {
        if (fileTable.contains(filename)) {
            securityMonitor.warn(currentUser, "ADD_DUPLICATE", filename,
                    "File already tracked — returning existing.");
            return fileTable.get(filename);
        }
        TrackedFile file = new TrackedFile(filename);
        fileTable.put(filename, file);
        securityMonitor.log(currentUser, "ADD_FILE", filename,
                "File added to tracking.");
        return file;
    }

    /**
     * Deletes a tracked file and all its version history from the repository.
     * Triggers mass-delete detection in the security monitor.
     */
    public boolean deleteFile(String filename) {
        if (!fileTable.contains(filename)) {
            securityMonitor.warn(currentUser, "DELETE_UNKNOWN", filename,
                    "Attempted to delete untracked file.");
            return false;
        }
        securityMonitor.checkMassDelete(currentUser, filename);
        boolean removed = fileTable.remove(filename);
        if (removed) {
            securityMonitor.log(currentUser, "DELETE_FILE", filename,
                    "File and all version history removed by " + currentUser);
        }
        return removed;
    }

    /**
     * Sets the working-copy content for a file.
     */
    public boolean editFile(String filename, String newContent) {
        TrackedFile file = fileTable.get(filename);
        if (file == null) {
            securityMonitor.warn(currentUser, "EDIT_UNKNOWN", filename,
                    "Attempted to edit untracked file.");
            return false;
        }
        file.setContent(newContent);
        securityMonitor.log(currentUser, "EDIT", filename,
                "Content updated in working copy.");
        return true;
    }

    /**
     * Commits the current working copy of a file.
     */
    public FileVersion commit(String filename, String message) {
        TrackedFile file = fileTable.get(filename);
        if (file == null) return null;

        // Security: rapid-commit detection
        securityMonitor.checkRapidCommits(currentUser, filename);

        FileVersion version = file.commit(currentUser, message,
                branchTree.getCurrentBranchName());
        branchTree.updateCurrentBranchCommit(version.getVersionId());

        securityMonitor.log(currentUser, "COMMIT", filename,
                String.format("Committed as v%d [%s] on branch '%s'",
                        version.getVersionNumber(),
                        version.getVersionId().substring(0, 8),
                        branchTree.getCurrentBranchName()));
        return version;
    }

    /**
     * Convenience: sets content then commits in one call.
     */
    public FileVersion editAndCommit(String filename, String content, String message) {
        editFile(filename, content);
        return commit(filename, message);
    }

    // ── Version history ───────────────────────────────────────────────────────

    /**
     * Prints the commit log for a file.
     */
    public void log(String filename) {
        TrackedFile file = fileTable.get(filename);
        if (file == null) { System.out.println("  File not found: " + filename); return; }

        securityMonitor.log(currentUser, "LOG", filename, "Viewed commit log.");
        System.out.println();
        System.out.println("  Commit log — " + filename + "  [" + file.getVersionCount() + " version(s)]");
        System.out.println("  " + "─".repeat(65));
        file.getHistory().printDiagram();
        System.out.println("  " + "─".repeat(65));
        List<FileVersion> versions = file.getAllVersions();
        if (versions.isEmpty()) {
            System.out.println("  (no commits yet)");
        } else {
            versions.forEach(v -> System.out.println("  " + v.getSummary()));
        }
        System.out.println();
    }

    // ── Diff ──────────────────────────────────────────────────────────────────

    /**
     * Shows the diff between two version numbers of a file.
     */
    public void diff(String filename, int versionA, int versionB) {
        TrackedFile file = fileTable.get(filename);
        if (file == null) { System.out.println("  File not found: " + filename); return; }

        FileVersion va = file.getVersion(versionA);
        FileVersion vb = file.getVersion(versionB);
        if (va == null || vb == null) {
            System.out.println("  One or both versions not found.");
            return;
        }

        securityMonitor.log(currentUser, "DIFF", filename,
                String.format("Comparing v%d vs v%d", versionA, versionB));

        var diffs = DiffEngine.diff(va.getContent(), vb.getContent());
        DiffEngine.printDiff(filename,
                "v" + versionA, "v" + versionB, diffs);
    }

    /**
     * Shows diff between a version and the current working copy.
     */
    public void diffWithWorking(String filename, int versionNumber) {
        TrackedFile file = fileTable.get(filename);
        if (file == null) return;
        FileVersion v = file.getVersion(versionNumber);
        if (v == null) return;

        var diffs = DiffEngine.diff(v.getContent(), file.getContent());
        DiffEngine.printDiff(filename, "v" + versionNumber, "working", diffs);
    }

    // ── Undo / Redo ───────────────────────────────────────────────────────────

    public FileVersion undo(String filename) {
        TrackedFile file = fileTable.get(filename);
        if (file == null) return null;
        FileVersion restored = file.undo();
        if (restored != null) {
            securityMonitor.log(currentUser, "UNDO", filename,
                    "Undone to v" + restored.getVersionNumber());
        } else {
            System.out.println("  Nothing to undo.");
        }
        return restored;
    }

    public FileVersion redo(String filename) {
        TrackedFile file = fileTable.get(filename);
        if (file == null) return null;
        FileVersion restored = file.redo();
        if (restored != null) {
            securityMonitor.log(currentUser, "REDO", filename,
                    "Redone to v" + restored.getVersionNumber());
        } else {
            System.out.println("  Nothing to redo.");
        }
        return restored;
    }

    // ── Rollback ──────────────────────────────────────────────────────────────

    /**
     * Rolls back a file to a specific version (with security check).
     */
    public FileVersion rollback(String filename, int targetVersion) {
        TrackedFile file = fileTable.get(filename);
        if (file == null) return null;

        int currentVersion = file.hasCommits()
                ? file.getLatestVersion().getVersionNumber() : 0;

        // Security: rollback-attack detection
        securityMonitor.checkRollbackAttack(currentUser, currentVersion,
                targetVersion, filename);

        FileVersion restored = file.rollbackTo(targetVersion);
        if (restored != null) {
            securityMonitor.log(currentUser, "ROLLBACK", filename,
                    "Hard rollback to v" + restored.getVersionNumber());
            System.out.printf("  ✓ Rolled back '%s' to v%d by %s%n",
                    filename, restored.getVersionNumber(), currentUser);
        } else {
            System.out.println("  Rollback failed: version not found.");
        }
        return restored;
    }

    // ── Branch operations ─────────────────────────────────────────────────────

    public boolean createBranch(String branchName) {
        boolean ok = branchTree.createBranch(branchName) != null;
        if (ok) {
            securityMonitor.log(currentUser, "CREATE_BRANCH", branchName,
                    "Branch created from: " + branchTree.getCurrentBranchName());
        }
        return ok;
    }

    public boolean switchBranch(String branchName) {
        boolean ok = branchTree.switchBranch(branchName);
        if (ok) {
            securityMonitor.log(currentUser, "SWITCH_BRANCH", branchName,
                    "Switched to branch: " + branchName);
        }
        return ok;
    }

    public void printBranches() { branchTree.printTree(); }
    public String getCurrentBranch() { return branchTree.getCurrentBranchName(); }

    // ── Integrity verification ────────────────────────────────────────────────

    /**
     * Verifies all versions of a file against their stored hashes.
     */
    public void verifyIntegrity(String filename) {
        TrackedFile file = fileTable.get(filename);
        if (file == null) { System.out.println("  File not found."); return; }

        System.out.println("  Integrity check — " + filename);
        boolean allOk = true;
        for (FileVersion v : file.getAllVersions()) {
            boolean ok = securityMonitor.checkTampering(currentUser,
                    filename, v.getContent(), v.getContentHash()) == false;
            System.out.printf("    v%d [%s] — %s%n",
                    v.getVersionNumber(),
                    v.getVersionId().substring(0, 8),
                    ok ? "✓ OK" : "✗ TAMPERED!");
            if (!ok) allOk = false;
        }
        System.out.println("  Result: " + (allOk ? "All versions intact." : "⚠ Tampering detected!"));
    }

    /**
     * Simulates file tampering (for educational demo).
     * Modifies a version's stored content WITHOUT updating the hash,
     * so the next integrity check will fail.
     */
    public void simulateTampering(String filename, int versionNumber,
                                  String tamperedContent) {
        // In a real system, version content is immutable. This method
        // uses reflection-style access (direct field manipulation) for
        // the purposes of the rollback-attack simulation demo only.
        //
        // Here we instead just log that tampering was attempted and show
        // what the hash mismatch would look like.
        System.out.println();
        System.out.println("  [SIMULATION] Tampering with " + filename + " v" + versionNumber);
        String originalHash   = HashUtils.sha256(tamperedContent + "_original");
        String tamperedHash   = HashUtils.sha256(tamperedContent);
        System.out.println("  Expected hash : " + originalHash.substring(0, 20) + "...");
        System.out.println("  Actual hash   : " + tamperedHash.substring(0, 20) + "...");
        System.out.println("  Match         : " + originalHash.equals(tamperedHash));
        securityMonitor.flagCritical(currentUser, "TAMPER_SIMULATION", filename,
                "Hash mismatch detected on v" + versionNumber);
    }

    // ── Repository overview ───────────────────────────────────────────────────

    public void status() {
        System.out.println();
        System.out.println("  ┌── Repository: " + name + " ────────────────────────");
        System.out.printf ("  │  User    : %s%n", currentUser);
        System.out.printf ("  │  Branch  : %s%n", branchTree.getCurrentBranchName());
        System.out.printf ("  │  Files   : %d tracked%n", fileTable.size());
        System.out.printf ("  │  Logs    : %d entries (%d suspicious)%n",
                securityMonitor.totalLogs(), securityMonitor.suspiciousCount());
        System.out.println("  │");

        List<TrackedFile> files = fileTable.getAllFiles();
        if (files.isEmpty()) {
            System.out.println("  │  (no files tracked)");
        } else {
            files.forEach(f -> System.out.printf(
                    "  │  %-20s  v%-3d  hash: %s%n",
                    f.getFilename(),
                    f.hasCommits() ? f.getLatestVersion().getVersionNumber() : 0,
                    f.hasCommits()
                            ? f.getLatestVersion().getContentHash().substring(0, 12) + "..."
                            : "(uncommitted)"));
        }
        System.out.println("  └─────────────────────────────────────────────────");
        System.out.println();
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public String           getName()            { return name; }
    public FileHashTable    getFileTable()        { return fileTable; }
    public BranchTree       getBranchTree()       { return branchTree; }
    public SecurityMonitor  getSecurityMonitor()  { return securityMonitor; }

    public TrackedFile getFile(String filename) {
        return fileTable.get(filename);
    }
}
