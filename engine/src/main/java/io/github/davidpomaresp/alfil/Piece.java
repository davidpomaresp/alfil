package io.github.davidpomaresp.alfil;

/**
 * Pieces are stored as small ints: the low 3 bits hold the type and bit 3 holds the colour.
 * 0 means an empty square.
 */
public final class Piece {

    public static final int EMPTY = 0;

    public static final int PAWN = 1;
    public static final int KNIGHT = 2;
    public static final int BISHOP = 3;
    public static final int ROOK = 4;
    public static final int QUEEN = 5;
    public static final int KING = 6;

    public static final int WHITE = 0;
    public static final int BLACK = 1;

    private static final String SYMBOLS = ".PNBRQK";

    private Piece() {
    }

    public static int of(int color, int type) {
        return (color << 3) | type;
    }

    public static int type(int piece) {
        return piece & 7;
    }

    public static int color(int piece) {
        return piece >> 3;
    }

    /** FEN letter: uppercase for white, lowercase for black. */
    public static char toChar(int piece) {
        if (piece == EMPTY) {
            return '.';
        }
        char c = SYMBOLS.charAt(type(piece));
        return color(piece) == WHITE ? c : Character.toLowerCase(c);
    }

    public static int fromChar(char c) {
        int type = SYMBOLS.indexOf(Character.toUpperCase(c));
        if (type <= 0) {
            throw new IllegalArgumentException("Unknown piece: " + c);
        }
        return of(Character.isUpperCase(c) ? WHITE : BLACK, type);
    }
}
