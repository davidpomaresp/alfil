package io.github.davidpomaresp.alfil;

/**
 * Random 64-bit keys used to give every position a (practically) unique number.
 * The hash of a position is the XOR of the keys of everything in it, so a move
 * only has to XOR out what changed and XOR in the new state.
 */
final class Zobrist {

    static final long[][] PIECE = new long[16][64];
    static final long[] CASTLING = new long[16];
    static final long[] EN_PASSANT_FILE = new long[8];
    static final long BLACK_TO_MOVE;

    private static long seed = 0x9E3779B97F4A7C15L;

    static {
        for (int piece = 0; piece < 16; piece++) {
            for (int sq = 0; sq < 64; sq++) {
                PIECE[piece][sq] = next();
            }
        }
        for (int i = 0; i < 16; i++) {
            CASTLING[i] = next();
        }
        for (int i = 0; i < 8; i++) {
            EN_PASSANT_FILE[i] = next();
        }
        BLACK_TO_MOVE = next();
    }

    private Zobrist() {
    }

    // xorshift64*, fixed seed so hashes are the same on every run
    private static long next() {
        seed ^= seed >>> 12;
        seed ^= seed << 25;
        seed ^= seed >>> 27;
        return seed * 0x2545F4914F6CDD1DL;
    }

    static long hash(Board board) {
        long h = 0;
        for (int sq = 0; sq < 64; sq++) {
            int piece = board.squares[sq];
            if (piece != Piece.EMPTY) {
                h ^= PIECE[piece][sq];
            }
        }
        h ^= CASTLING[board.castlingRights];
        if (board.enPassantSquare != Square.NONE) {
            h ^= EN_PASSANT_FILE[Square.file(board.enPassantSquare)];
        }
        if (board.sideToMove == Piece.BLACK) {
            h ^= BLACK_TO_MOVE;
        }
        return h;
    }
}
