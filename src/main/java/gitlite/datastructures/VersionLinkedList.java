package gitlite.datastructures;

import gitlite.model.FileVersion;

import java.util.ArrayList;
import java.util.List;

/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║           VERSION HISTORY — DOUBLY LINKED LIST                  ║
 * ║                                                                  ║
 * ║  HEAD ←→ v1 ←→ v2 ←→ v3 ←→ ... ←→ vN ← TAIL                  ║
 * ║                                                                  ║
 * ║  • Each node = one committed FileVersion                         ║
 * ║  • Supports forward traversal (log) and backward traversal      ║
 * ║    (rollback / undo)                                             ║
 * ╚══════════════════════════════════════════════════════════════════╝
 *
 * Data Structure: Doubly Linked List
 * Time Complexity:
 *   - Append   : O(1)
 *   - Get by # : O(n)
 *   - Get by ID: O(n)
 *   - Size      : O(1)
 */
public class VersionLinkedList {

    private FileVersion head;
    private FileVersion tail;
    private int size;

    public VersionLinkedList() {
        head = null;
        tail = null;
        size = 0;
    }

    /**
     * Appends a new version to the end of the list — O(1).
     */
    public void append(FileVersion version) {
        if (head == null) {
            head = version;
            tail = version;
            version.prev = null;
            version.next = null;
        } else {
            tail.next    = version;
            version.prev = tail;
            version.next = null;
            tail         = version;
        }
        size++;
    }

    /**
     * Returns the latest (tail) version — O(1).
     */
    public FileVersion getLatest() {
        return tail;
    }

    /**
     * Returns the first (head) version — O(1).
     */
    public FileVersion getFirst() {
        return head;
    }

    /**
     * Retrieves a version by its 1-based version number — O(n).
     */
    public FileVersion getByVersionNumber(int number) {
        FileVersion current = head;
        while (current != null) {
            if (current.getVersionNumber() == number) return current;
            current = current.next;
        }
        return null;
    }

    /**
     * Retrieves a version by its ID prefix (first 8 chars) — O(n).
     */
    public FileVersion getByVersionId(String idPrefix) {
        FileVersion current = head;
        while (current != null) {
            if (current.getVersionId().startsWith(idPrefix)) return current;
            current = current.next;
        }
        return null;
    }

    /**
     * Returns all versions as an ordered list (oldest → newest) — O(n).
     */
    public List<FileVersion> getAllVersions() {
        List<FileVersion> versions = new ArrayList<>();
        FileVersion current = head;
        while (current != null) {
            versions.add(current);
            current = current.next;
        }
        return versions;
    }

    /**
     * Returns all versions in reverse order (newest → oldest) — O(n).
     */
    public List<FileVersion> getAllVersionsReversed() {
        List<FileVersion> versions = new ArrayList<>();
        FileVersion current = tail;
        while (current != null) {
            versions.add(current);
            current = current.prev;
        }
        return versions;
    }

    /**
     * Truncates history at a given version (for rollback).
     * Removes all versions AFTER the specified version number.
     * Returns the number of versions removed.
     */
    public int truncateAfter(int versionNumber) {
        FileVersion target = getByVersionNumber(versionNumber);
        if (target == null) return 0;

        int removed = 0;
        FileVersion current = tail;
        while (current != null && current.getVersionNumber() > versionNumber) {
            current = current.prev;
            removed++;
        }

        // Re-link
        if (current != null) {
            current.next = null;
            tail = current;
        } else {
            head = null;
            tail = null;
        }
        size -= removed;
        return removed;
    }

    /**
     * Returns the number of versions stored — O(1).
     */
    public int size() { return size; }

    /**
     * Returns true if no versions exist — O(1).
     */
    public boolean isEmpty() { return size == 0; }

    /**
     * Prints a compact visual diagram of the linked list.
     */
    public void printDiagram() {
        if (isEmpty()) {
            System.out.println("  [EMPTY — no commits yet]");
            return;
        }
        StringBuilder sb = new StringBuilder("  HEAD → ");
        FileVersion current = head;
        while (current != null) {
            sb.append(String.format("[v%d:%s]", current.getVersionNumber(),
                    current.getVersionId().substring(0, 6)));
            if (current.next != null) sb.append(" ↔ ");
            current = current.next;
        }
        sb.append(" ← TAIL");
        System.out.println(sb);
    }
}
