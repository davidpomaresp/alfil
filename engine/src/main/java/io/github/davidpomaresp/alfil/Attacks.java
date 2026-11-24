package io.github.davidpomaresp.alfil;

/**
 * Precomputed target squares for knights and kings, and rays for sliding pieces.
 * Computing them once avoids checking the board edges on every move.
 */
final class Attacks {

    // Rook directions first (0-3), then bishop directions (4-7).
    static final int[][] DIRECTIONS = {
            {0, 1}, {0, -1}, {1, 0}, {-1, 0},
            {1, 1}, {-1, 1}, {1, -1}, {-1, -1}
    };

    static final int[][] KNIGHT = new int[64][];
    static final int[][] KING = new int[64][];
    static final int[][][] RAYS = new int[64][8][];

    private static final int[][] KNIGHT_JUMPS = {
            {1, 2}, {2, 1}, {2, -1}, {1, -2}, {-1, -2}, {-2, -1}, {-2, 1}, {-1, 2}
    };

    static {
        for (int sq = 0; sq < 64; sq++) {
            KNIGHT[sq] = steps(sq, KNIGHT_JUMPS);
            KING[sq] = steps(sq, DIRECTIONS);
            for (int d = 0; d < 8; d++) {
                RAYS[sq][d] = ray(sq, DIRECTIONS[d][0], DIRECTIONS[d][1]);
            }
        }
    }

    private Attacks() {
    }

    private static int[] steps(int sq, int[][] deltas) {
        int[] buffer = new int[deltas.length];
        int n = 0;
        for (int[] d : deltas) {
            int file = Square.file(sq) + d[0];
            int rank = Square.rank(sq) + d[1];
            if (onBoard(file, rank)) {
                buffer[n++] = Square.of(file, rank);
            }
        }
        return java.util.Arrays.copyOf(buffer, n);
    }

    private static int[] ray(int sq, int df, int dr) {
        int[] buffer = new int[7];
        int n = 0;
        int file = Square.file(sq) + df;
        int rank = Square.rank(sq) + dr;
        while (onBoard(file, rank)) {
            buffer[n++] = Square.of(file, rank);
            file += df;
            rank += dr;
        }
        return java.util.Arrays.copyOf(buffer, n);
    }

    private static boolean onBoard(int file, int rank) {
        return file >= 0 && file < 8 && rank >= 0 && rank < 8;
    }
}
