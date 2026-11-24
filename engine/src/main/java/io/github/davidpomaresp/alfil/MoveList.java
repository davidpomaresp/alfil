package io.github.davidpomaresp.alfil;

/** Fixed-size buffer of moves, reused to avoid allocating during search. */
public final class MoveList {

    // No legal chess position has more than 218 moves.
    private final int[] moves = new int[256];
    private int size;

    public void add(int move) {
        moves[size++] = move;
    }

    public int get(int index) {
        return moves[index];
    }

    public void set(int index, int move) {
        moves[index] = move;
    }

    public int size() {
        return size;
    }

    public void clear() {
        size = 0;
    }

    public boolean contains(int move) {
        for (int i = 0; i < size; i++) {
            if (moves[i] == move) {
                return true;
            }
        }
        return false;
    }
}
