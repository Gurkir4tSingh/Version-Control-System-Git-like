package gitlite;

import gitlite.core.Repository;
import gitlite.ui.Demo;
import gitlite.ui.VCSConsole;

/**
 * ╔═══════════════════════════════════════════════════════════════════╗
 * ║              GitLite VCS — Entry Point                            ║
 * ║                                                                   ║
 * ║  Usage:                                                           ║
 * ║    java -cp out gitlite.Main           → interactive shell        ║
 * ║    java -cp out gitlite.Main demo      → automated demo           ║
 * ╚═══════════════════════════════════════════════════════════════════╝
 *
 * Data Structures Used:
 *   • Doubly Linked List  — version history per file (VersionLinkedList)
 *   • Stack (pair)        — undo/redo support (UndoRedoStack)
 *   • Hash Table          — O(1) file lookup (FileHashTable)
 *   • N-ary Tree          — branching system (BranchTree)
 *   • Custom Hash (FNV-1a)— version ID generation (HashUtils)
 *   • SHA-256             — content integrity hashing (HashUtils)
 */
public class Main {

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("demo")) {
            // Run the automated demonstration
            Demo.run();
        } else {
            // Launch interactive shell
            Repository repo = new Repository("my-repo", "developer");
            VCSConsole console = new VCSConsole(repo);
            console.run();
        }
    }
}
