package gitlite.datastructures;

import gitlite.model.FileVersion;

/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║              UNDO / REDO — STACK PAIR                           ║
 * ║                                                                  ║
 * ║   UNDO STACK        REDO STACK                                  ║
 * ║  ┌─────────┐       ┌─────────┐                                  ║
 * ║  │   vN    │ ←TOP  │   vK    │ ←TOP                             ║
 * ║  │  vN-1   │       │  vK+1   │                                  ║
 * ║  │   ...   │       │   ...   │                                  ║
 * ║  └─────────┘       └─────────┘                                  ║
 * ║                                                                  ║
 * ║  Undo: pop from UNDO → push to REDO                             ║
 * ║  Redo: pop from REDO → push to UNDO                             ║
 * ╚══════════════════════════════════════════════════════════════════╝
 *
 * Data Structure: Custom Generic Stack backed by a singly linked list
 * Time Complexity: Push O(1) | Pop O(1) | Peek O(1)
 */
public class UndoRedoStack {

    // ── Internal generic stack node ───────────────────────────────────────────
    private static class StackNode {
        FileVersion data;
        StackNode   below;

        StackNode(FileVersion data, StackNode below) {
            this.data  = data;
            this.below = below;
        }
    }

    // ── Internal generic stack ────────────────────────────────────────────────
    private static class Stack {
        private StackNode top;
        private int       count;
        private final int maxCapacity;

        Stack(int maxCapacity) {
            this.top         = null;
            this.count       = 0;
            this.maxCapacity = maxCapacity;
        }

        boolean push(FileVersion v) {
            if (count >= maxCapacity) return false;   // prevent unbounded growth
            top   = new StackNode(v, top);
            count++;
            return true;
        }

        FileVersion pop() {
            if (top == null) return null;
            FileVersion data = top.data;
            top   = top.below;
            count--;
            return data;
        }

        FileVersion peek() {
            return (top == null) ? null : top.data;
        }

        boolean isEmpty() { return top == null; }
        int     size()    { return count; }

        /** Print stack from top to bottom. */
        void print(String label) {
            System.out.printf("  %s Stack (size=%d):%n", label, count);
            StackNode cur = top;
            int i = count;
            while (cur != null) {
                System.out.printf("    [%d] v%d: %s — \"%s\"%n",
                        i--, cur.data.getVersionNumber(),
                        cur.data.getVersionId().substring(0, 8),
                        cur.data.getCommitMessage());
                cur = cur.below;
            }
            if (count == 0) System.out.println("    (empty)");
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    private static final int MAX_STACK_DEPTH = 50;

    private final Stack undoStack;
    private final Stack redoStack;

    public UndoRedoStack() {
        undoStack = new Stack(MAX_STACK_DEPTH);
        redoStack = new Stack(MAX_STACK_DEPTH);
    }

    /**
     * Records a new commit.  Clears the redo stack because a new
     * action invalidates the forward history.
     */
    public void recordCommit(FileVersion version) {
        undoStack.push(version);
        clearRedo();   // standard behaviour: new action wipes redo history
    }

    /**
     * Undo: returns the version to restore (the one BELOW current top).
     * Moves current top to redo stack.
     *
     * @return the FileVersion to restore, or null if nothing to undo
     */
    public FileVersion undo() {
        if (undoStack.size() <= 1) return null;   // need at least 2 entries
        FileVersion current = undoStack.pop();
        redoStack.push(current);
        return undoStack.peek();                  // the restored version
    }

    /**
     * Redo: moves top of redo stack back to undo stack.
     *
     * @return the FileVersion to restore, or null if nothing to redo
     */
    public FileVersion redo() {
        if (redoStack.isEmpty()) return null;
        FileVersion target = redoStack.pop();
        undoStack.push(target);
        return target;
    }

    /**
     * Returns the currently active version without popping.
     */
    public FileVersion current() {
        return undoStack.peek();
    }

    public boolean canUndo() { return undoStack.size() > 1; }
    public boolean canRedo() { return !redoStack.isEmpty(); }

    private void clearRedo() {
        while (!redoStack.isEmpty()) redoStack.pop();
    }

    /** Print both stacks for debugging / UI display. */
    public void printState() {
        undoStack.print("UNDO");
        redoStack.print("REDO");
    }

    public int undoDepth() { return undoStack.size(); }
    public int redoDepth() { return redoStack.size(); }
}
