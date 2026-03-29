package gitlite.datastructures;

import gitlite.core.TrackedFile;

import java.util.ArrayList;
import java.util.List;

/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║               FILE REGISTRY — HASH TABLE                        ║
 * ║                                                                  ║
 * ║  Index  Bucket                                                   ║
 * ║  ─────  ──────────────────────────────────────────────          ║
 * ║   [0]   NULL                                                     ║
 * ║   [1]   "main.java" → TrackedFile ──→ NULL                      ║
 * ║   [2]   "README.md" → TrackedFile ──→ NULL                      ║
 * ║   [3]   "util.java" → TrackedFile → "App.java" → NULL  (chain)  ║
 * ║   ...                                                            ║
 * ║                                                                  ║
 * ║  Collision resolution: Separate Chaining (linked list)          ║
 * ╚══════════════════════════════════════════════════════════════════╝
 *
 * Data Structure: Hash Table with Separate Chaining
 * Time Complexity: Insert/Lookup/Delete = O(1) avg, O(n) worst
 */
public class FileHashTable {

    // ── Chaining node ─────────────────────────────────────────────────────────
    private static class Entry {
        String      key;       // filename
        TrackedFile value;
        Entry       next;

        Entry(String key, TrackedFile value) {
            this.key   = key;
            this.value = value;
            this.next  = null;
        }
    }

    // ── Table internals ───────────────────────────────────────────────────────
    private static final int INITIAL_CAPACITY  = 16;
    private static final double LOAD_THRESHOLD = 0.75;

    private Entry[] buckets;
    private int     count;
    private int     capacity;

    public FileHashTable() {
        capacity = INITIAL_CAPACITY;
        buckets  = new Entry[capacity];
        count    = 0;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Inserts or updates a file entry — O(1) avg.
     */
    public void put(String filename, TrackedFile file) {
        if ((double) count / capacity >= LOAD_THRESHOLD) resize();

        int index = hash(filename);
        Entry cur = buckets[index];

        // Update if key already exists
        while (cur != null) {
            if (cur.key.equals(filename)) {
                cur.value = file;
                return;
            }
            cur = cur.next;
        }

        // Insert at front of chain (O(1))
        Entry newEntry  = new Entry(filename, file);
        newEntry.next   = buckets[index];
        buckets[index]  = newEntry;
        count++;
    }

    /**
     * Retrieves a tracked file by filename — O(1) avg.
     */
    public TrackedFile get(String filename) {
        int   index = hash(filename);
        Entry cur   = buckets[index];
        while (cur != null) {
            if (cur.key.equals(filename)) return cur.value;
            cur = cur.next;
        }
        return null;
    }

    /**
     * Removes a file entry — O(1) avg.
     */
    public boolean remove(String filename) {
        int   index = hash(filename);
        Entry cur   = buckets[index];
        Entry prev  = null;
        while (cur != null) {
            if (cur.key.equals(filename)) {
                if (prev == null) buckets[index] = cur.next;
                else              prev.next = cur.next;
                count--;
                return true;
            }
            prev = cur;
            cur  = cur.next;
        }
        return false;
    }

    /**
     * Returns true if the filename is tracked.
     */
    public boolean contains(String filename) {
        return get(filename) != null;
    }

    /**
     * Returns all tracked files.
     */
    public List<TrackedFile> getAllFiles() {
        List<TrackedFile> files = new ArrayList<>();
        for (Entry bucket : buckets) {
            Entry cur = bucket;
            while (cur != null) {
                files.add(cur.value);
                cur = cur.next;
            }
        }
        return files;
    }

    /**
     * Returns all filenames.
     */
    public List<String> getAllFilenames() {
        List<String> names = new ArrayList<>();
        for (Entry bucket : buckets) {
            Entry cur = bucket;
            while (cur != null) {
                names.add(cur.key);
                cur = cur.next;
            }
        }
        return names;
    }

    public int size() { return count; }

    // ── Hash function ─────────────────────────────────────────────────────────

    /**
     * Polynomial rolling hash (djb2-style) — maps filename to bucket index.
     */
    private int hash(String key) {
        int hash = 5381;
        for (char c : key.toCharArray()) {
            hash = ((hash << 5) + hash) + c;   // hash * 33 + c
        }
        return Math.abs(hash) % capacity;
    }

    // ── Resize ────────────────────────────────────────────────────────────────

    /** Doubles capacity and rehashes all entries when load factor exceeded. */
    private void resize() {
        int     newCapacity = capacity * 2;
        Entry[] newBuckets  = new Entry[newCapacity];
        int     oldCapacity = capacity;

        capacity = newCapacity;   // update BEFORE rehashing

        for (int i = 0; i < oldCapacity; i++) {
            Entry cur = buckets[i];
            while (cur != null) {
                Entry next     = cur.next;
                int newIndex   = hash(cur.key);
                cur.next       = newBuckets[newIndex];
                newBuckets[newIndex] = cur;
                cur            = next;
            }
        }
        buckets = newBuckets;
    }

    /** Prints a visual overview of the hash table buckets. */
    public void printTable() {
        System.out.printf("  Hash Table (capacity=%d, size=%d, load=%.2f)%n",
                capacity, count, (double) count / capacity);
        for (int i = 0; i < capacity; i++) {
            if (buckets[i] == null) continue;
            System.out.printf("  [%3d] ", i);
            Entry cur = buckets[i];
            while (cur != null) {
                System.out.printf("→ \"%s\" ", cur.key);
                cur = cur.next;
            }
            System.out.println("→ NULL");
        }
    }
}
