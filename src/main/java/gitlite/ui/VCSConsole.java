package gitlite.ui;

import gitlite.core.Repository;
import gitlite.core.TrackedFile;
import gitlite.model.FileVersion;

import java.util.Scanner;

/**
 * Interactive command-line shell for GitLite VCS.
 *
 * Supported commands:
 *   add <file>                     Add a file to tracking
 *   edit <file> <content>          Set working-copy content
 *   commit <file> <message>        Commit a file
 *   log <file>                     Show commit history
 *   diff <file> <vA> <vB>          Diff two versions
 *   undo <file>                    Undo last commit
 *   redo <file>                    Redo last undone commit
 *   rollback <file> <version>      Hard rollback to version
 *   verify <file>                  Integrity check
 *   branch <name>                  Create a branch
 *   checkout <name>                Switch branch
 *   branches                       Show branch tree
 *   user <name>                    Switch current user
 *   status                         Show repo status
 *   hashtable                      Show file hash table
 *   logs                           Show all activity logs
 *   suspicious                     Show suspicious logs only
 *   simulate-tamper <file> <text>  Simulate a tampering attack
 *   simulate-rollback <file>       Simulate a rollback attack
 *   help                           Show this help
 *   exit                           Quit
 */
public class VCSConsole {

    private final Repository repo;
    private final Scanner    scanner;

    public VCSConsole(Repository repo) {
        this.repo    = repo;
        this.scanner = new Scanner(System.in);
    }

    public void run() {
        printBanner();
        repo.status();

        while (true) {
            System.out.printf("gitlite (%s) [%s] > ",
                    repo.getCurrentBranch(), repo.getCurrentUser());
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) continue;

            String[] parts = line.split("\\s+", 3);
            String   cmd   = parts[0].toLowerCase();

            try {
                switch (cmd) {
                    case "add"        -> cmdAdd(parts);
                    case "edit"       -> cmdEdit(parts);
                    case "commit"     -> cmdCommit(parts);
                    case "log"        -> cmdLog(parts);
                    case "diff"       -> cmdDiff(parts);
                    case "undo"       -> cmdUndo(parts);
                    case "redo"       -> cmdRedo(parts);
                    case "rollback"   -> cmdRollback(parts);
                    case "verify"     -> cmdVerify(parts);
                    case "branch"     -> cmdBranch(parts);
                    case "checkout"   -> cmdCheckout(parts);
                    case "branches"   -> repo.printBranches();
                    case "user"       -> cmdUser(parts);
                    case "status"     -> repo.status();
                    case "hashtable"  -> repo.getFileTable().printTable();
                    case "logs"       -> repo.getSecurityMonitor().printAllLogs();
                    case "suspicious" -> repo.getSecurityMonitor().printSuspiciousLogs();
                    case "simulate-tamper"    -> cmdSimulateTamper(parts);
                    case "simulate-rollback"  -> cmdSimulateRollback(parts);
                    case "cat"        -> cmdCat(parts);
                    case "show"       -> cmdShow(parts);
                    case "export"     -> cmdExport(parts);
                    case "files"      -> cmdFiles();
                    case "rm", "delete", "remove" -> cmdDelete(parts);
                    case "view"       -> cmdView(parts);
                    case "history"    -> cmdHistory(parts);
                    case "help"       -> printHelp();
                    case "exit", "quit" -> {
                        System.out.println("  Goodbye!");
                        return;
                    }
                    default -> System.out.println("  Unknown command. Type 'help' for help.");
                }
            } catch (Exception e) {
                System.out.println("  Error: " + e.getMessage());
            }
        }
    }

    // ── Command handlers ──────────────────────────────────────────────────────

    private void cmdAdd(String[] p) {
        require(p, 2, "Usage: add <filename>");
        TrackedFile f = repo.addFile(p[1]);
        System.out.println("  ✓ Tracking file: " + f.getFilename());
    }

    private void cmdEdit(String[] p) {
        require(p, 3, "Usage: edit <filename> <content>");
        boolean ok = repo.editFile(p[1], p[2]);
        if (ok) System.out.println("  ✓ Working copy updated for: " + p[1]);
    }

    private void cmdCommit(String[] p) {
        require(p, 3, "Usage: commit <filename> <message>");
        FileVersion v = repo.commit(p[1], p[2]);
        if (v != null) {
            System.out.println("  ✓ Committed:");
            System.out.println("  " + v);
        }
    }

    private void cmdLog(String[] p) {
        require(p, 2, "Usage: log <filename>");
        repo.log(p[1]);
    }

    private void cmdDiff(String[] p) {
        if (p.length < 4) { System.out.println("  Usage: diff <filename> <vA> <vB>"); return; }
        int va = Integer.parseInt(p[2]);
        int vb = Integer.parseInt(p[3]);
        repo.diff(p[1], va, vb);
    }

    private void cmdUndo(String[] p) {
        require(p, 2, "Usage: undo <filename>");
        FileVersion v = repo.undo(p[1]);
        if (v != null)
            System.out.printf("  ✓ Undone — now at v%d: \"%s\"%n",
                    v.getVersionNumber(), v.getCommitMessage());
    }

    private void cmdRedo(String[] p) {
        require(p, 2, "Usage: redo <filename>");
        FileVersion v = repo.redo(p[1]);
        if (v != null)
            System.out.printf("  ✓ Redone — now at v%d: \"%s\"%n",
                    v.getVersionNumber(), v.getCommitMessage());
    }

    private void cmdRollback(String[] p) {
        if (p.length < 3) { System.out.println("  Usage: rollback <filename> <version>"); return; }
        int target = Integer.parseInt(p[2]);
        repo.rollback(p[1], target);
    }

    private void cmdVerify(String[] p) {
        require(p, 2, "Usage: verify <filename>");
        repo.verifyIntegrity(p[1]);
    }

    private void cmdBranch(String[] p) {
        require(p, 2, "Usage: branch <name>");
        boolean ok = repo.createBranch(p[1]);
        System.out.println(ok ? "  ✓ Branch created: " + p[1]
                              : "  ✗ Branch already exists: " + p[1]);
    }

    private void cmdCheckout(String[] p) {
        require(p, 2, "Usage: checkout <branch>");
        boolean ok = repo.switchBranch(p[1]);
        System.out.println(ok ? "  ✓ Switched to branch: " + p[1]
                              : "  ✗ Branch not found: " + p[1]);
    }

    private void cmdUser(String[] p) {
        require(p, 2, "Usage: user <username>");
        repo.setCurrentUser(p[1]);
        System.out.println("  ✓ Switched to user: " + p[1]);
    }

    private void cmdSimulateTamper(String[] p) {
        require(p, 3, "Usage: simulate-tamper <filename> <fake-content>");
        repo.simulateTampering(p[1], 1, p[2]);
    }

    private void cmdSimulateRollback(String[] p) {
        require(p, 2, "Usage: simulate-rollback <filename>");
        TrackedFile file = repo.getFile(p[1]);
        if (file == null) { System.out.println("  File not found."); return; }
        int current = file.hasCommits() ? file.getLatestVersion().getVersionNumber() : 0;
        if (current < 2) { System.out.println("  Need at least 2 commits to simulate rollback attack."); return; }
        // Attempt rollback to version 1 from version current (will trigger security alert)
        System.out.println("  [SIMULATION] Attempting rollback attack from v" + current + " → v1 ...");
        repo.rollback(p[1], 1);
    }

    /**
     * cat <file> — print the current working-copy content of a tracked file.
     */
    private void cmdCat(String[] p) {
        require(p, 2, "Usage: cat <filename>");
        TrackedFile file = repo.getFile(p[1]);
        if (file == null) { System.out.println("  File not found: " + p[1]); return; }

        String content = file.getContent();
        if (content == null || content.isEmpty()) {
            System.out.println("  (file is empty — use 'edit' to add content)");
            return;
        }
        System.out.println();
        System.out.println("  ┌── " + p[1] + " (working copy) " + "─".repeat(Math.max(0, 44 - p[1].length())) + "┐");
        String[] lines = content.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            System.out.printf("  │  %3d │ %s%n", i + 1, lines[i]);
        }
        System.out.println("  └" + "─".repeat(55) + "┘");
        System.out.println();
    }

    /**
     * show <file> <version> — print the content of a specific committed version.
     */
    private void cmdShow(String[] p) {
        if (p.length < 3) { System.out.println("  Usage: show <filename> <version>"); return; }
        TrackedFile file = repo.getFile(p[1]);
        if (file == null) { System.out.println("  File not found: " + p[1]); return; }

        int vNum;
        try { vNum = Integer.parseInt(p[2]); }
        catch (NumberFormatException e) { System.out.println("  Version must be a number."); return; }

        gitlite.model.FileVersion v = file.getVersion(vNum);
        if (v == null) { System.out.println("  Version v" + vNum + " not found."); return; }

        System.out.println();
        System.out.println("  " + v);   // prints the full version metadata box
        System.out.println();
        System.out.println("  ┌── Content ─────────────────────────────────────────────┐");
        String[] lines = v.getContent().split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            System.out.printf("  │  %3d │ %s%n", i + 1, lines[i]);
        }
        System.out.println("  └" + "─".repeat(55) + "┘");
        System.out.println();
    }

    /**
     * export <file> — write the latest committed version to a real file on disk.
     */
    private void cmdExport(String[] p) {
        require(p, 2, "Usage: export <filename>");
        TrackedFile file = repo.getFile(p[1]);
        if (file == null) { System.out.println("  File not found: " + p[1]); return; }
        if (!file.hasCommits()) { System.out.println("  No commits yet — nothing to export."); return; }

        String content = file.getLatestVersion().getContent();
        java.nio.file.Path outPath = java.nio.file.Paths.get(p[1]);
        try {
            java.nio.file.Files.writeString(outPath, content);
            System.out.println("  ✓ Exported to: " + outPath.toAbsolutePath());
            System.out.println("  You can now compile it with:");
            System.out.println("    javac " + p[1]);
            // Derive class name from filename (strip .java)
            String className = p[1].endsWith(".java") ? p[1].replace(".java", "") : p[1];
            System.out.println("    java  " + className);
        } catch (java.io.IOException e) {
            System.out.println("  ✗ Export failed: " + e.getMessage());
        }
    }

    /**
     * files — list all tracked files with version count and latest hash.
     */
    private void cmdFiles() {
        java.util.List<TrackedFile> all = repo.getFileTable().getAllFiles();
        if (all.isEmpty()) {
            System.out.println("  No files tracked yet. Use 'add <filename>' to start.");
            return;
        }
        System.out.println();
        System.out.println("  Tracked files (" + all.size() + "):");
        System.out.println("  " + "─".repeat(60));
        System.out.printf("  %-22s  %-6s  %-14s  %s%n", "Filename", "Vers.", "Hash (latest)", "Branch");
        System.out.println("  " + "─".repeat(60));
        for (TrackedFile f : all) {
            String hash   = f.hasCommits() ? f.getLatestVersion().getContentHash().substring(0, 12) + "..." : "(uncommitted)";
            String branch = f.hasCommits() ? f.getLatestVersion().getBranchName() : "-";
            int    ver    = f.hasCommits() ? f.getLatestVersion().getVersionNumber() : 0;
            System.out.printf("  %-22s  v%-5d  %-14s  %s%n", f.getFilename(), ver, hash, branch);
        }
        System.out.println("  " + "─".repeat(60));
        System.out.println();
    }

    /**
     * rm / delete / remove <file> — stop tracking a file and wipe its history.
     * Asks for confirmation before deleting.
     */
    private void cmdDelete(String[] p) {
        require(p, 2, "Usage: rm <filename>");
        String filename = p[1];
        gitlite.core.TrackedFile file = repo.getFile(filename);
        if (file == null) {
            System.out.println("  File not found: " + filename);
            return;
        }
        int versions = file.getVersionCount();
        System.out.printf("  ⚠  This will permanently delete '%s' and all %d version(s).%n", filename, versions);
        System.out.print("  Type the filename to confirm, or 'n' to cancel: ");
        String confirm = scanner.nextLine().trim();
        if (!confirm.equals(filename)) {
            System.out.println("  Cancelled — nothing deleted.");
            return;
        }
        boolean ok = repo.deleteFile(filename);
        if (ok) {
            System.out.println("  ✓ Deleted: " + filename);
        } else {
            System.out.println("  ✗ Delete failed.");
        }
    }

    /**
     * view <file> — interactive viewer: lists all versions and lets you
     * pick one to read, or press Enter to read the working copy.
     */
    private void cmdView(String[] p) {
        require(p, 2, "Usage: view <filename>");
        String filename = p[1];
        gitlite.core.TrackedFile file = repo.getFile(filename);
        if (file == null) {
            System.out.println("  File not found: " + filename);
            return;
        }

        java.util.List<gitlite.model.FileVersion> versions = file.getAllVersions();

        System.out.println();
        System.out.println("  ┌── " + filename + " ─────────────────────────────────────────");
        System.out.printf ("  │  Versions: %d  |  Working copy: %s%n",
                versions.size(),
                file.getContent().isEmpty() ? "(empty)" : file.getContent().length() + " chars");
        System.out.println("  ├────────────────────────────────────────────────────────");

        if (versions.isEmpty()) {
            System.out.println("  │  (no committed versions yet)");
        } else {
            for (gitlite.model.FileVersion v : versions) {
                System.out.printf("  │  [v%d] %-10s  by %-12s  \"%s\"%n",
                        v.getVersionNumber(),
                        v.getVersionId().substring(0, 8),
                        v.getAuthor(),
                        v.getCommitMessage());
            }
        }
        System.out.println("  └────────────────────────────────────────────────────────");
        System.out.println();
        System.out.print("  Enter version number to view (or Enter for working copy, 'q' to quit): ");
        String input = scanner.nextLine().trim();

        if (input.equals("q") || input.equals("Q")) return;

        if (input.isEmpty()) {
            // Show working copy
            String wc = file.getContent();
            if (wc == null || wc.isEmpty()) {
                System.out.println("  Working copy is empty.");
                return;
            }
            printContentBox(filename + " (working copy)", wc);
        } else {
            try {
                int vNum = Integer.parseInt(input);
                gitlite.model.FileVersion v = file.getVersion(vNum);
                if (v == null) {
                    System.out.println("  Version v" + vNum + " not found.");
                    return;
                }
                printContentBox(filename + "  v" + vNum + " by " + v.getAuthor(), v.getContent());
            } catch (NumberFormatException e) {
                System.out.println("  Invalid version number.");
            }
        }
    }

    /**
     * history <file> — rich version history table with content preview.
     */
    private void cmdHistory(String[] p) {
        require(p, 2, "Usage: history <filename>");
        gitlite.core.TrackedFile file = repo.getFile(p[1]);
        if (file == null) { System.out.println("  File not found: " + p[1]); return; }

        java.util.List<gitlite.model.FileVersion> versions = file.getAllVersions();
        if (versions.isEmpty()) {
            System.out.println("  No versions committed yet for: " + p[1]);
            return;
        }

        System.out.println();
        System.out.println("  Version history — " + p[1]);
        System.out.println("  " + "═".repeat(72));
        System.out.printf("  %-4s  %-10s  %-12s  %-8s  %-6s  %s%n",
                "Ver", "ID", "Author", "Branch", "Lines", "Message");
        System.out.println("  " + "─".repeat(72));

        for (gitlite.model.FileVersion v : versions) {
            int lines = v.getContent().isEmpty() ? 0 : v.getContent().split("\n", -1).length;
            System.out.printf("  v%-3d  %-10s  %-12s  %-8s  %-6d  \"%s\"%n",
                    v.getVersionNumber(),
                    v.getVersionId().substring(0, 8),
                    v.getAuthor(),
                    v.getBranchName(),
                    lines,
                    v.getCommitMessage());
        }
        System.out.println("  " + "═".repeat(72));

        // Show linked list diagram
        System.out.println();
        file.getHistory().printDiagram();
        System.out.println();
    }

    /** Shared helper — prints content inside a nice box with line numbers. */
    private void printContentBox(String title, String content) {
        System.out.println();
        int boxWidth = 55;
        String header = "  ┌── " + title + " ";
        System.out.println(header + "─".repeat(Math.max(0, boxWidth - header.length() + 2)) + "┐");
        String[] lines = content.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            // Wrap long lines
            if (line.length() > 48) {
                System.out.printf("  │  %3d │ %s%n", i + 1, line.substring(0, 48));
                System.out.printf("  │       │ %s%n", line.substring(48));
            } else {
                System.out.printf("  │  %3d │ %s%n", i + 1, line);
            }
        }
        System.out.println("  └" + "─".repeat(boxWidth) + "┘");
        System.out.println();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void require(String[] parts, int minLen, String usage) {
        if (parts.length < minLen)
            throw new IllegalArgumentException(usage);
    }

    // ── Banner & Help ─────────────────────────────────────────────────────────

    private void printBanner() {
        System.out.println();
        System.out.println("  ╔═════════════════════════════════════════════════════════╗");
        System.out.println("  ║          GitLite VCS  —  Version Control System         ║");
        System.out.println("  ║    Data Structures: Linked List · Stack · Hash Table    ║");
        System.out.println("  ║                     Tree · Custom Hash                  ║");
        System.out.println("  ╚═════════════════════════════════════════════════════════╝");
        System.out.println();
    }

    private void printHelp() {
        System.out.println();
        System.out.println("  ┌── Commands ─────────────────────────────────────────────────┐");
        System.out.println("  │  FILE OPERATIONS                                             │");
        System.out.println("  │    add <file>                  Track a new file              │");
        System.out.println("  │    edit <file> <content>       Set working copy content      │");
        System.out.println("  │    commit <file> <message>     Save a new version            │");
        System.out.println("  │    cat <file>                  Print working copy content    │");
        System.out.println("  │    show <file> <version>       Print a specific version      │");
        System.out.println("  │    files                       List all tracked files        │");
        System.out.println("  │    export <file>               Write latest version to disk  │");
        System.out.println("  │    view <file>                 Interactive version viewer    │");
        System.out.println("  │    history <file>              Rich history table + diagram  │");
        System.out.println("  │    rm <file>                   Delete file + all versions    │");
        System.out.println("  │                                                              │");
        System.out.println("  │  HISTORY                                                     │");
        System.out.println("  │    log <file>                  Show all commits              │");
        System.out.println("  │    diff <file> <vA> <vB>       Compare two versions          │");
        System.out.println("  │    undo <file>                 Undo last commit              │");
        System.out.println("  │    redo <file>                 Redo undone commit            │");
        System.out.println("  │    rollback <file> <version>   Hard rollback to version      │");
        System.out.println("  │                                                              │");
        System.out.println("  │  INTEGRITY                                                   │");
        System.out.println("  │    verify <file>               Check all version hashes      │");
        System.out.println("  │                                                              │");
        System.out.println("  │  BRANCHES                                                    │");
        System.out.println("  │    branch <name>               Create branch                 │");
        System.out.println("  │    checkout <name>             Switch branch                 │");
        System.out.println("  │    branches                    Show branch tree              │");
        System.out.println("  │                                                              │");
        System.out.println("  │  SECURITY                                                    │");
        System.out.println("  │    logs                        All activity logs             │");
        System.out.println("  │    suspicious                  Suspicious logs only          │");
        System.out.println("  │    simulate-tamper <f> <txt>   Simulate hash tampering       │");
        System.out.println("  │    simulate-rollback <f>       Simulate rollback attack      │");
        System.out.println("  │                                                              │");
        System.out.println("  │  GENERAL                                                     │");
        System.out.println("  │    user <name>                 Switch user                   │");
        System.out.println("  │    status                      Repo overview                 │");
        System.out.println("  │    hashtable                   Show file hash table          │");
        System.out.println("  │    help                        This help                     │");
        System.out.println("  │    exit                        Quit                          │");
        System.out.println("  └──────────────────────────────────────────────────────────────┘");
        System.out.println();
    }
}
