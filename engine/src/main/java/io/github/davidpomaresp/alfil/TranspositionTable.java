package io.github.davidpomaresp.alfil;

/**
 * Remembers results of positions already searched, indexed by their Zobrist hash.
 * The same position is often reached by different move orders (transpositions), and
 * the stored best move is also the best guess for which move to try first next time.
 *
 * Each entry packs into one long: move (16 bits), score (16), depth (8), bound (2).
 */
public final class TranspositionTable {

    static final int EXACT = 1;
    static final int LOWER = 2; // score >= stored value (the search failed high)
    static final int UPPER = 3; // score <= stored value (no move raised alpha)

    private final long[] keys;
    private final long[] entries;
    private final int mask;

    /** A table with 2^sizeBits entries of 16 bytes each (20 bits = 16 MB). */
    public TranspositionTable(int sizeBits) {
        keys = new long[1 << sizeBits];
        entries = new long[1 << sizeBits];
        mask = (1 << sizeBits) - 1;
    }

    public void clear() {
        java.util.Arrays.fill(keys, 0);
        java.util.Arrays.fill(entries, 0);
    }

    /** Returns the packed entry for this hash, or 0 if there is none. */
    long probe(long hash) {
        int index = (int) hash & mask;
        return keys[index] == hash ? entries[index] : 0;
    }

    void store(long hash, int move, int score, int depth, int bound) {
        int index = (int) hash & mask;
        // Keep a deeper result for the same position unless the new one is exact.
        if (keys[index] == hash && depth(entries[index]) > depth && bound != EXACT) {
            return;
        }
        keys[index] = hash;
        entries[index] = (move & 0xFFFFL)
                | ((score & 0xFFFFL) << 16)
                | ((long) (depth & 0xFF) << 32)
                | ((long) bound << 40);
    }

    static int move(long entry) {
        return (int) (entry & 0xFFFF);
    }

    static int score(long entry) {
        return (short) (entry >>> 16);
    }

    static int depth(long entry) {
        return (int) ((entry >>> 32) & 0xFF);
    }

    static int bound(long entry) {
        return (int) ((entry >>> 40) & 3);
    }
}
