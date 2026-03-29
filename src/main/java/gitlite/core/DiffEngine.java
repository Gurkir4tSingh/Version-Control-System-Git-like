package gitlite.core;

import java.util.ArrayList;
import java.util.List;

/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║                     DIFF ENGINE                                  ║
 * ║                                                                  ║
 * ║  Computes and displays the line-by-line difference between       ║
 * ║  two versions of a file — similar to `git diff`.                 ║
 * ║                                                                  ║
 * ║  Algorithm: Myers diff algorithm (simplified O(ND) variant)      ║
 * ║  using dynamic programming on the Longest Common Subsequence.    ║
 * ╚══════════════════════════════════════════════════════════════════╝
 */
public class DiffEngine {

    // ── Diff change types ─────────────────────────────────────────────────────
    public enum ChangeType { ADDED, REMOVED, UNCHANGED }

    public static class DiffLine {
        public final ChangeType type;
        public final String     text;
        public final int        lineNumber;   // line number in the relevant version

        DiffLine(ChangeType type, String text, int lineNumber) {
            this.type       = type;
            this.text       = text;
            this.lineNumber = lineNumber;
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Computes the diff between two content strings.
     *
     * @param oldContent  original (earlier) version
     * @param newContent  new (later) version
     * @return            ordered list of DiffLine objects
     */
    public static List<DiffLine> diff(String oldContent, String newContent) {
        String[] oldLines = splitLines(oldContent);
        String[] newLines = splitLines(newContent);

        int[][] lcs = computeLCS(oldLines, newLines);
        return buildDiff(oldLines, newLines, lcs);
    }

    /**
     * Renders the diff to stdout in a unified-diff style.
     *
     *  Lines added:   +  (green-style prefix)
     *  Lines removed: -  (red-style prefix)
     *  Unchanged:        (no prefix)
     *
     * @param filename   name shown in the diff header
     * @param oldVersion version label (e.g. "v2")
     * @param newVersion version label (e.g. "v3")
     * @param diffs      result from diff()
     */
    public static void printDiff(String filename, String oldVersion,
                                 String newVersion, List<DiffLine> diffs) {
        long added   = diffs.stream().filter(d -> d.type == ChangeType.ADDED).count();
        long removed = diffs.stream().filter(d -> d.type == ChangeType.REMOVED).count();

        System.out.println();
        System.out.println("  ╔══════════════════════════════════════════════════════╗");
        System.out.printf ("  ║  diff %s → %s  [%s]%n", padOrTrim(oldVersion, 6),
                padOrTrim(newVersion, 6), filename);
        System.out.printf ("  ║  +%d added line(s)  -%d removed line(s)%n", added, removed);
        System.out.println("  ╠══════════════════════════════════════════════════════╣");

        if (diffs.isEmpty()) {
            System.out.println("  ║  (no differences — files are identical)");
        }

        // Print in chunks: only show lines around changes (context = 2)
        int context = 2;
        boolean[] printLine = markContextLines(diffs, context);

        int  lastPrinted = -1;
        for (int i = 0; i < diffs.size(); i++) {
            if (!printLine[i]) continue;

            if (lastPrinted >= 0 && i > lastPrinted + 1) {
                System.out.println("  ║  ...");
            }

            DiffLine dl = diffs.get(i);
            String prefix = switch (dl.type) {
                case ADDED    -> "  + ";
                case REMOVED  -> "  - ";
                case UNCHANGED-> "    ";
            };
            System.out.printf("  ║%s%3d │ %s%n", prefix, dl.lineNumber, dl.text);
            lastPrinted = i;
        }

        System.out.println("  ╚══════════════════════════════════════════════════════╝");
        System.out.println();
    }

    /**
     * Quick summary: returns a one-line stats string.
     */
    public static String diffSummary(String oldContent, String newContent) {
        List<DiffLine> diffs = diff(oldContent, newContent);
        long added   = diffs.stream().filter(d -> d.type == ChangeType.ADDED).count();
        long removed = diffs.stream().filter(d -> d.type == ChangeType.REMOVED).count();
        if (added == 0 && removed == 0) return "(no changes)";
        return String.format("+%d / -%d lines", added, removed);
    }

    // ── LCS computation (O(m*n) DP) ──────────────────────────────────────────

    private static int[][] computeLCS(String[] a, String[] b) {
        int m = a.length;
        int n = b.length;
        int[][] dp = new int[m + 1][n + 1];

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (a[i - 1].equals(b[j - 1])) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }
        return dp;
    }

    // ── Backtrack through LCS table to build diff ─────────────────────────────

    private static List<DiffLine> buildDiff(String[] old, String[] neu, int[][] lcs) {
        List<DiffLine> result = new ArrayList<>();
        backtrack(old, neu, lcs, old.length, neu.length, result);
        return result;
    }

    private static void backtrack(String[] old, String[] neu, int[][] lcs,
                                   int i, int j, List<DiffLine> result) {
        if (i > 0 && j > 0 && old[i - 1].equals(neu[j - 1])) {
            backtrack(old, neu, lcs, i - 1, j - 1, result);
            result.add(new DiffLine(ChangeType.UNCHANGED, old[i - 1], i));
        } else if (j > 0 && (i == 0 || lcs[i][j - 1] >= lcs[i - 1][j])) {
            backtrack(old, neu, lcs, i, j - 1, result);
            result.add(new DiffLine(ChangeType.ADDED, neu[j - 1], j));
        } else if (i > 0 && (j == 0 || lcs[i][j - 1] < lcs[i - 1][j])) {
            backtrack(old, neu, lcs, i - 1, j, result);
            result.add(new DiffLine(ChangeType.REMOVED, old[i - 1], i));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String[] splitLines(String content) {
        if (content == null || content.isEmpty()) return new String[0];
        return content.split("\n", -1);
    }

    private static boolean[] markContextLines(List<DiffLine> diffs, int context) {
        boolean[] mark = new boolean[diffs.size()];
        for (int i = 0; i < diffs.size(); i++) {
            if (diffs.get(i).type != ChangeType.UNCHANGED) {
                for (int k = Math.max(0, i - context);
                     k <= Math.min(diffs.size() - 1, i + context); k++) {
                    mark[k] = true;
                }
            }
        }
        return mark;
    }

    private static String padOrTrim(String s, int len) {
        if (s.length() >= len) return s.substring(0, len);
        return s + " ".repeat(len - s.length());
    }
}
