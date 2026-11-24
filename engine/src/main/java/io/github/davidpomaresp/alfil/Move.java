package io.github.davidpomaresp.alfil;

/**
 * A move packed into an int: bits 0-5 from, bits 6-11 to, bits 12-15 flags.
 */
public final class Move {

    public static final int NONE = 0;

    public static final int QUIET = 0;
    public static final int DOUBLE_PAWN_PUSH = 1;
    public static final int KING_CASTLE = 2;
    public static final int QUEEN_CASTLE = 3;
    public static final int CAPTURE = 4;
    public static final int EN_PASSANT = 5;
    public static final int PROMOTION = 8;

    private Move() {
    }

    public static int of(int from, int to, int flags) {
        return from | (to << 6) | (flags << 12);
    }

    public static int promotion(int from, int to, int pieceType, boolean capture) {
        int flags = PROMOTION | (pieceType - Piece.KNIGHT) | (capture ? CAPTURE : 0);
        return of(from, to, flags);
    }

    public static int from(int move) {
        return move & 63;
    }

    public static int to(int move) {
        return (move >> 6) & 63;
    }

    public static int flags(int move) {
        return move >> 12;
    }

    public static boolean isCapture(int move) {
        return (flags(move) & CAPTURE) != 0;
    }

    public static boolean isPromotion(int move) {
        return (flags(move) & PROMOTION) != 0;
    }

    /** Piece type the pawn becomes, or {@link Piece#EMPTY} if this is not a promotion. */
    public static int promotionType(int move) {
        return isPromotion(move) ? (flags(move) & 3) + Piece.KNIGHT : Piece.EMPTY;
    }

    public static String toUci(int move) {
        String s = Square.name(from(move)) + Square.name(to(move));
        if (isPromotion(move)) {
            s += Character.toLowerCase(Piece.toChar(promotionType(move)));
        }
        return s;
    }
}
