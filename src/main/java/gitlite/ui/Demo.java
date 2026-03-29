package gitlite.ui;

import gitlite.core.Repository;
import gitlite.model.FileVersion;

/**
 * Automated demonstration that exercises every feature of GitLite VCS.
 *
 * Run this class to see all features in action without typing commands.
 * Each section is clearly labelled with the data structure being demonstrated.
 */
public class Demo {

    public static void run() {
        sep("GITLITE VCS — FULL FEATURE DEMO");

        // ── 1. Setup ──────────────────────────────────────────────────────────
        sep("1. Repository Initialisation");
        Repository repo = new Repository("university-project", "alice");
        repo.status();

        // ── 2. Hash Table — file registry ─────────────────────────────────────
        sep("2. Hash Table — File Registry (add files)");
        repo.addFile("Main.java");
        repo.addFile("Utils.java");
        repo.addFile("README.md");
        System.out.println("  Hash table after adding 3 files:");
        repo.getFileTable().printTable();

        // ── 3. Linked List — version history via commits ──────────────────────
        sep("3. Linked List — Version History (commits)");

        repo.editAndCommit("Main.java",
                "public class Main {\n    public static void main(String[] args) {\n        System.out.println(\"Hello\");\n    }\n}",
                "Initial commit");

        repo.editAndCommit("Main.java",
                "public class Main {\n    public static void main(String[] args) {\n        System.out.println(\"Hello, World!\");\n    }\n}",
                "Improved greeting message");

        repo.editAndCommit("Main.java",
                "public class Main {\n    // Entry point\n    public static void main(String[] args) {\n        System.out.println(\"Hello, World!\");\n        System.out.println(\"GitLite VCS running.\");\n    }\n}",
                "Added VCS status message");

        repo.editAndCommit("Main.java",
                "public class Main {\n    // Entry point\n    public static void main(String[] args) {\n        System.out.println(\"Hello, World!\");\n        System.out.println(\"GitLite VCS running.\");\n        System.out.println(\"Version: 1.0\");\n    }\n}",
                "Added version number output");

        repo.log("Main.java");

        // ── 4. Diff Engine ────────────────────────────────────────────────────
        sep("4. Diff Engine — Show Differences Between Versions");
        repo.diff("Main.java", 1, 3);
        repo.diff("Main.java", 2, 4);

        // ── 5. Stack — Undo / Redo ────────────────────────────────────────────
        sep("5. Stack — Undo / Redo Operations");
        System.out.println("  Current undo/redo stack state:");
        repo.getFile("Main.java").getUndoRedoStack().printState();

        System.out.println("\n  Performing UNDO...");
        FileVersion undone = repo.undo("Main.java");
        if (undone != null)
            System.out.println("  → Restored to: " + undone.getSummary());

        System.out.println("\n  Performing UNDO again...");
        undone = repo.undo("Main.java");
        if (undone != null)
            System.out.println("  → Restored to: " + undone.getSummary());

        System.out.println("\n  Stack after 2 undos:");
        repo.getFile("Main.java").getUndoRedoStack().printState();

        System.out.println("\n  Performing REDO...");
        FileVersion redone = repo.redo("Main.java");
        if (redone != null)
            System.out.println("  → Redone to: " + redone.getSummary());

        // ── 6. Hashing — Integrity Verification ──────────────────────────────
        sep("6. SHA-256 Hashing — Integrity Verification");
        repo.verifyIntegrity("Main.java");

        // ── 7. Tamper simulation ──────────────────────────────────────────────
        sep("7. Security — Hash Mismatch / Tampering Simulation");
        repo.simulateTampering("Main.java", 1, "malicious content injected");

        // ── 8. Rollback Attack Simulation ─────────────────────────────────────
        sep("8. Security — Rollback Attack Simulation");

        // Add more commits to Utils.java so rollback delta is large enough
        repo.editAndCommit("Utils.java", "// Utils v1\npublic class Utils {}", "Initial utils");
        repo.editAndCommit("Utils.java", "// Utils v2\npublic class Utils { int x; }", "Added field");
        repo.editAndCommit("Utils.java", "// Utils v3\npublic class Utils { int x; int y; }", "Added second field");
        repo.editAndCommit("Utils.java", "// Utils v4\npublic class Utils { int x; int y; void foo(){} }", "Added method");
        repo.editAndCommit("Utils.java", "// Utils v5\npublic class Utils { int x; int y; void foo(){} void bar(){} }", "Added bar method");

        System.out.println("  [SIM] Attempting aggressive rollback from v5 → v1 (delta=4)...");
        repo.rollback("Utils.java", 1);   // delta=4 > threshold=3, triggers alert

        // ── 9. Branch Tree ────────────────────────────────────────────────────
        sep("9. Tree / Graph — Branch System");
        repo.createBranch("feature/login");
        repo.createBranch("hotfix/null-check");
        repo.switchBranch("feature/login");
        repo.createBranch("feature/login-ui");   // child of feature/login
        repo.switchBranch("main");

        System.out.println("  Branch tree:");
        repo.printBranches();

        System.out.println("  Ancestry path of 'feature/login-ui':");
        repo.getBranchTree().getAncestryPath("feature/login-ui")
                .forEach(b -> System.out.println("    → " + b));

        // ── 10. Rapid-commit detection ────────────────────────────────────────
        sep("10. Security — Rapid Commit Detection");
        repo.setCurrentUser("suspicious-bot");
        repo.addFile("injected.java");
        System.out.println("  Simulating 6 rapid commits from 'suspicious-bot'...");
        for (int i = 1; i <= 6; i++) {
            repo.editAndCommit("injected.java",
                    "// injected content v" + i,
                    "rapid commit #" + i);
        }

        // ── 11. Multi-user log ────────────────────────────────────────────────
        sep("11. User Logs — Full Activity Log");
        repo.setCurrentUser("alice");
        repo.editAndCommit("README.md",
                "# University Project\nGitLite VCS built for Data Structures course.",
                "README first draft");
        repo.setCurrentUser("bob");
        repo.editAndCommit("README.md",
                "# University Project\nGitLite VCS — a Git-like system in Java.",
                "README updated by bob");

        System.out.println();
        repo.getSecurityMonitor().printAllLogs();

        // ── 12. Suspicious log summary ────────────────────────────────────────
        sep("12. Suspicious Activity Summary");
        repo.getSecurityMonitor().printSuspiciousLogs();

        // ── 13. Final status ──────────────────────────────────────────────────
        sep("13. Final Repository Status");
        repo.setCurrentUser("alice");
        repo.status();

        sep("DEMO COMPLETE");
    }

    private static void sep(String title) {
        System.out.println();
        System.out.println("  ══════════════════════════════════════════════════════════");
        System.out.println("  ▶  " + title);
        System.out.println("  ══════════════════════════════════════════════════════════");
    }
}
