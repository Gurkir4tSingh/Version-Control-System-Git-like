package gitlite.datastructures;

import java.util.*;

/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║              BRANCH SYSTEM — N-ARY TREE                         ║
 * ║                                                                  ║
 * ║            main                                                  ║
 * ║           /    \                                                 ║
 * ║       feature  hotfix                                            ║
 * ║       /    \                                                     ║
 * ║    auth   ui-redesign                                            ║
 * ║                                                                  ║
 * ║  Each node holds: branch name, parent ref, children list,       ║
 * ║  and the latest commit ID on that branch.                        ║
 * ╚══════════════════════════════════════════════════════════════════╝
 *
 * Data Structure: N-ary Tree (General Tree)
 * Time Complexity: createBranch O(1), getBranch O(n), printTree O(n)
 */
public class BranchTree {

    // ── Tree Node ─────────────────────────────────────────────────────────────
    public static class BranchNode {
        private final String       name;
        private       String       latestCommitId;
        private       BranchNode   parent;
        private final List<BranchNode> children;
        private final long         createdAt;

        public BranchNode(String name, BranchNode parent) {
            this.name          = name;
            this.parent        = parent;
            this.children      = new ArrayList<>();
            this.latestCommitId= null;
            this.createdAt     = System.currentTimeMillis();
        }

        public String          getName()           { return name; }
        public String          getLatestCommitId() { return latestCommitId; }
        public BranchNode      getParent()         { return parent; }
        public List<BranchNode>getChildren()       { return Collections.unmodifiableList(children); }

        public void setLatestCommitId(String id)   { this.latestCommitId = id; }
        public void addChild(BranchNode child)     { children.add(child); }
    }

    // ── Tree internals ────────────────────────────────────────────────────────
    private final BranchNode         root;
    private       BranchNode         currentBranch;
    private final Map<String, BranchNode> branchMap;   // name → node (O(1) lookup)

    public BranchTree() {
        root          = new BranchNode("main", null);
        currentBranch = root;
        branchMap     = new HashMap<>();
        branchMap.put("main", root);
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Creates a new branch forked from the current branch.
     *
     * @param branchName  name of the new branch
     * @return            the new BranchNode, or null if name already exists
     */
    public BranchNode createBranch(String branchName) {
        if (branchMap.containsKey(branchName)) return null;

        BranchNode newBranch = new BranchNode(branchName, currentBranch);
        currentBranch.addChild(newBranch);
        branchMap.put(branchName, newBranch);
        return newBranch;
    }

    /**
     * Switches the active branch.
     *
     * @return true if switch succeeded
     */
    public boolean switchBranch(String branchName) {
        BranchNode target = branchMap.get(branchName);
        if (target == null) return false;
        currentBranch = target;
        return true;
    }

    /**
     * Records the latest commit ID on the current branch.
     */
    public void updateCurrentBranchCommit(String commitId) {
        currentBranch.setLatestCommitId(commitId);
    }

    public BranchNode  getCurrentBranch()     { return currentBranch; }
    public String      getCurrentBranchName() { return currentBranch.getName(); }
    public BranchNode  getBranch(String name) { return branchMap.get(name); }
    public boolean     branchExists(String name){ return branchMap.containsKey(name); }
    public List<String>getAllBranchNames()     { return new ArrayList<>(branchMap.keySet()); }
    public int         getBranchCount()       { return branchMap.size(); }

    /**
     * Deletes a non-main, non-current branch.
     */
    public boolean deleteBranch(String branchName) {
        if ("main".equals(branchName)) return false;
        if (branchName.equals(currentBranch.getName())) return false;

        BranchNode node = branchMap.remove(branchName);
        if (node == null) return false;

        BranchNode parent = node.getParent();
        if (parent != null) parent.getChildren().remove(node);
        return true;
    }

    // ── Tree visualisation ────────────────────────────────────────────────────

    /**
     * Prints the branch tree using recursive DFS with indentation.
     */
    public void printTree() {
        System.out.println("  Branch Tree:");
        printTreeNode(root, "", true);
    }

    private void printTreeNode(BranchNode node, String indent, boolean isLast) {
        String connector = isLast ? "└── " : "├── ";
        String marker    = node.getName().equals(currentBranch.getName()) ? " ◄ (current)" : "";
        String commit    = node.getLatestCommitId() != null
                ? " [" + node.getLatestCommitId().substring(0, 8) + "]" : " [no commits]";

        System.out.println(indent + connector + node.getName() + commit + marker);

        String childIndent = indent + (isLast ? "    " : "│   ");
        List<BranchNode> children = node.getChildren();
        for (int i = 0; i < children.size(); i++) {
            printTreeNode(children.get(i), childIndent, i == children.size() - 1);
        }
    }

    /**
     * Returns the ancestry path from a branch back to root.
     */
    public List<String> getAncestryPath(String branchName) {
        List<String> path = new ArrayList<>();
        BranchNode   cur  = branchMap.get(branchName);
        while (cur != null) {
            path.add(0, cur.getName());
            cur = cur.getParent();
        }
        return path;
    }
}
