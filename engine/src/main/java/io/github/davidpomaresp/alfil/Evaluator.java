package io.github.davidpomaresp.alfil;

/**
 * Static evaluation in centipawns (100 = one pawn), from the side to move's point of view.
 *
 * Score = material + a bonus for where each piece stands + bishop pair. Square bonuses
 * have a middlegame and an endgame value, blended by how much material is left
 * ("tapered" evaluation), so the king hides early and walks to the centre late.
 */
public final class Evaluator {

    static final int[] VALUE = {0, 100, 320, 330, 500, 900, 0};

    // How much each piece counts towards "middlegame-ness": 24 at the start, 0 with only pawns.
    private static final int[] PHASE = {0, 0, 1, 1, 2, 4, 0};
    private static final int MAX_PHASE = 24;
    private static final int BISHOP_PAIR = 30;

    // [type][square] for white; black uses the mirrored square.
    static final int[][] MIDGAME = new int[7][64];
    static final int[][] ENDGAME = new int[7][64];

    static {
        int[] pawnEndgame = {0, 0, 5, 10, 20, 35, 60, 0};
        for (int sq = 0; sq < 64; sq++) {
            int file = Square.file(sq);
            int rank = Square.rank(sq);
            int ring = centerRing(sq);

            boolean centralFile = file == 3 || file == 4;
            MIDGAME[Piece.PAWN][sq] = (rank - 1) * 3 + (centralFile && rank >= 3 ? 15 : 0);
            ENDGAME[Piece.PAWN][sq] = pawnEndgame[rank];

            int knight = new int[] {20, 10, 0, -25}[ring];
            MIDGAME[Piece.KNIGHT][sq] = knight;
            ENDGAME[Piece.KNIGHT][sq] = knight;

            int bishop = new int[] {10, 8, 0, -10}[ring];
            MIDGAME[Piece.BISHOP][sq] = bishop;
            ENDGAME[Piece.BISHOP][sq] = bishop;

            MIDGAME[Piece.ROOK][sq] = (rank == 6 ? 15 : 0) + (centralFile ? 5 : 0);
            ENDGAME[Piece.ROOK][sq] = rank == 6 ? 10 : 0;

            int queen = new int[] {5, 3, 0, -5}[ring];
            MIDGAME[Piece.QUEEN][sq] = queen;
            ENDGAME[Piece.QUEEN][sq] = queen;

            MIDGAME[Piece.KING][sq] = rank == 0 ? new int[] {15, 20, 10, 0, -5, 0, 25, 15}[file] : -20 * rank;
            ENDGAME[Piece.KING][sq] = new int[] {25, 12, 0, -20}[ring];
        }
    }

    private Evaluator() {
    }

    /** 0 for the four centre squares, 1 for the ring around them, then 2, and 3 for the edge. */
    static int centerRing(int sq) {
        int fileDistance = Math.max(3 - Square.file(sq), Square.file(sq) - 4);
        int rankDistance = Math.max(3 - Square.rank(sq), Square.rank(sq) - 4);
        return Math.max(fileDistance, rankDistance);
    }

    public static int evaluate(Board board) {
        int midgame = 0;
        int endgame = 0;
        int phase = 0;
        int whiteBishops = 0;
        int blackBishops = 0;

        for (int sq = 0; sq < 64; sq++) {
            int piece = board.squares[sq];
            if (piece == Piece.EMPTY) {
                continue;
            }
            int type = Piece.type(piece);
            phase += PHASE[type];
            if (Piece.color(piece) == Piece.WHITE) {
                midgame += VALUE[type] + MIDGAME[type][sq];
                endgame += VALUE[type] + ENDGAME[type][sq];
                if (type == Piece.BISHOP) {
                    whiteBishops++;
                }
            } else {
                int mirrored = sq ^ 56;
                midgame -= VALUE[type] + MIDGAME[type][mirrored];
                endgame -= VALUE[type] + ENDGAME[type][mirrored];
                if (type == Piece.BISHOP) {
                    blackBishops++;
                }
            }
        }

        int bishopPair = (whiteBishops >= 2 ? BISHOP_PAIR : 0) - (blackBishops >= 2 ? BISHOP_PAIR : 0);
        midgame += bishopPair;
        endgame += bishopPair;

        phase = Math.min(phase, MAX_PHASE);
        int score = (midgame * phase + endgame * (MAX_PHASE - phase)) / MAX_PHASE;
        return board.sideToMove() == Piece.WHITE ? score : -score;
    }
}
