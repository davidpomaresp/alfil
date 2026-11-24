package io.github.davidpomaresp.alfil;

/**
 * Static evaluation in centipawns (100 = one pawn), from the side to move's point of view.
 *
 * Score = material + a bonus for where each piece stands + bishop pair + pawn structure
 * + mobility + rooks on open files + pawns in front of the king. Every term has a
 * middlegame and an endgame value, blended by how much material is left ("tapered"
 * evaluation), so for example the king hides early and walks to the centre late.
 */
public final class Evaluator {

    static final int[] VALUE = {0, 100, 320, 330, 500, 900, 0};

    // How much each piece counts towards "middlegame-ness": 24 at the start, 0 with only pawns.
    private static final int[] PHASE = {0, 0, 1, 1, 2, 4, 0};
    private static final int MAX_PHASE = 24;
    private static final int BISHOP_PAIR = 30;

    // Passed pawn bonus by rank (from the pawn's own side), middlegame and endgame.
    private static final int[] PASSED_MG = {0, 5, 10, 15, 25, 40, 60, 0};
    private static final int[] PASSED_EG = {0, 10, 15, 25, 45, 70, 110, 0};
    private static final int DOUBLED_MG = -10;
    private static final int DOUBLED_EG = -20;
    private static final int ISOLATED_MG = -10;
    private static final int ISOLATED_EG = -15;
    private static final int ROOK_OPEN_FILE = 20;
    private static final int ROOK_SEMI_OPEN_FILE = 10;
    private static final int KING_SHIELD_PAWN = 12;
    // Points per reachable square, middlegame and endgame, for N, B, R, Q.
    private static final int[] MOBILITY_MG = {0, 0, 4, 5, 2, 1, 0};
    private static final int[] MOBILITY_EG = {0, 0, 4, 5, 4, 2, 0};
    // Typical number of reachable squares, so an average piece scores about 0.
    private static final int[] MOBILITY_BASE = {0, 0, 4, 6, 6, 12, 0};

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

    // Scratch arrays reused between calls; one Evaluator per search thread.
    private final int[] mg = new int[2];
    private final int[] eg = new int[2];
    private final int[] bishops = new int[2];
    private final int[][] pawnsOnFile = new int[2][8];
    private final int[][] frontmostPawn = new int[2][8];
    private final int[][] rearmostPawn = new int[2][8];

    /** 0 for the four centre squares, 1 for the ring around them, then 2, and 3 for the edge. */
    static int centerRing(int sq) {
        int fileDistance = Math.max(3 - Square.file(sq), Square.file(sq) - 4);
        int rankDistance = Math.max(3 - Square.rank(sq), Square.rank(sq) - 4);
        return Math.max(fileDistance, rankDistance);
    }

    /** Convenience for tests and one-off calls. */
    public static int evaluate(Board board) {
        return new Evaluator().score(board);
    }

    public int score(Board board) {
        mg[0] = mg[1] = eg[0] = eg[1] = bishops[0] = bishops[1] = 0;
        int phase = 0;

        // Pawns per file for each side, and the most and least advanced pawn per file.
        for (int f = 0; f < 8; f++) {
            pawnsOnFile[0][f] = 0;
            pawnsOnFile[1][f] = 0;
            frontmostPawn[0][f] = -1;
            frontmostPawn[1][f] = 8;
            rearmostPawn[0][f] = 8;
            rearmostPawn[1][f] = -1;
        }

        int[] squares = board.squares;
        for (int sq = 0; sq < 64; sq++) {
            int piece = squares[sq];
            if (piece == Piece.EMPTY) {
                continue;
            }
            int type = Piece.type(piece);
            int color = Piece.color(piece);
            int relative = color == Piece.WHITE ? sq : sq ^ 56;
            phase += PHASE[type];
            mg[color] += VALUE[type] + MIDGAME[type][relative];
            eg[color] += VALUE[type] + ENDGAME[type][relative];
            if (type == Piece.BISHOP) {
                bishops[color]++;
            } else if (type == Piece.PAWN) {
                int f = Square.file(sq);
                int r = Square.rank(sq);
                pawnsOnFile[color][f]++;
                if (color == Piece.WHITE) {
                    frontmostPawn[0][f] = Math.max(frontmostPawn[0][f], r);
                    rearmostPawn[0][f] = Math.min(rearmostPawn[0][f], r);
                } else {
                    frontmostPawn[1][f] = Math.min(frontmostPawn[1][f], r);
                    rearmostPawn[1][f] = Math.max(rearmostPawn[1][f], r);
                }
            }
        }

        for (int sq = 0; sq < 64; sq++) {
            int piece = squares[sq];
            int type = Piece.type(piece);
            if (piece == Piece.EMPTY || type == Piece.KING) {
                continue;
            }
            int color = Piece.color(piece);
            int them = color ^ 1;
            int f = Square.file(sq);
            if (type == Piece.PAWN) {
                pawnTerms(sq, color, pawnsOnFile, frontmostPawn, rearmostPawn, mg, eg);
                continue;
            }
            int mobility = mobility(squares, sq, type, color) - MOBILITY_BASE[type];
            mg[color] += mobility * MOBILITY_MG[type];
            eg[color] += mobility * MOBILITY_EG[type];
            if (type == Piece.ROOK && pawnsOnFile[color][f] == 0) {
                int bonus = pawnsOnFile[them][f] == 0 ? ROOK_OPEN_FILE : ROOK_SEMI_OPEN_FILE;
                mg[color] += bonus;
                eg[color] += bonus / 2;
            }
        }

        for (int color = 0; color < 2; color++) {
            if (bishops[color] >= 2) {
                mg[color] += BISHOP_PAIR;
                eg[color] += BISHOP_PAIR;
            }
            mg[color] += kingShield(squares, board.kingSquare(color), color);
        }

        int midgame = mg[0] - mg[1];
        int endgame = eg[0] - eg[1];
        phase = Math.min(phase, MAX_PHASE);
        int score = (midgame * phase + endgame * (MAX_PHASE - phase)) / MAX_PHASE;
        return board.sideToMove() == Piece.WHITE ? score : -score;
    }

    private static void pawnTerms(int sq, int color, int[][] pawnsOnFile, int[][] frontmost, int[][] rearmost,
                                  int[] mg, int[] eg) {
        int them = color ^ 1;
        int f = Square.file(sq);
        int r = Square.rank(sq);
        int relativeRank = color == Piece.WHITE ? r : 7 - r;

        boolean isolated = (f == 0 || pawnsOnFile[color][f - 1] == 0) && (f == 7 || pawnsOnFile[color][f + 1] == 0);
        if (isolated) {
            mg[color] += ISOLATED_MG;
            eg[color] += ISOLATED_EG;
        }
        // Count doubling once, on the rear pawn of the file.
        if (pawnsOnFile[color][f] > 1 && r == rearmost[color][f]) {
            mg[color] += DOUBLED_MG;
            eg[color] += DOUBLED_EG;
        }

        // Passed: no enemy pawn in front of it on its own or the neighbouring files.
        boolean passed = true;
        for (int file = Math.max(0, f - 1); file <= Math.min(7, f + 1) && passed; file++) {
            if (pawnsOnFile[them][file] == 0) {
                continue;
            }
            // For white, the enemy's rearmost (lowest) black pawn on that file must be behind us.
            if (color == Piece.WHITE ? rearmost[them][file] > r : rearmost[them][file] < r) {
                passed = false;
            }
        }
        // Only the front pawn of a doubled pair counts as passed.
        if (passed && r == frontmost[color][f]) {
            mg[color] += PASSED_MG[relativeRank];
            eg[color] += PASSED_EG[relativeRank];
        }
    }

    /** Number of squares a piece can move to (empty or enemy), ignoring pins. */
    private static int mobility(int[] squares, int sq, int type, int color) {
        int count = 0;
        if (type == Piece.KNIGHT) {
            for (int to : Attacks.KNIGHT[sq]) {
                int target = squares[to];
                if (target == Piece.EMPTY || Piece.color(target) != color) {
                    count++;
                }
            }
            return count;
        }
        int first = type == Piece.BISHOP ? 4 : 0;
        int last = type == Piece.ROOK ? 4 : 8;
        for (int d = first; d < last; d++) {
            for (int to : Attacks.RAYS[sq][d]) {
                int target = squares[to];
                if (target == Piece.EMPTY) {
                    count++;
                    continue;
                }
                if (Piece.color(target) != color) {
                    count++;
                }
                break;
            }
        }
        return count;
    }

    /** Own pawns on the three squares in front of a king on its first two ranks. */
    private static int kingShield(int[] squares, int king, int color) {
        int rank = Square.rank(king);
        int relativeRank = color == Piece.WHITE ? rank : 7 - rank;
        if (relativeRank > 1) {
            return 0;
        }
        int forward = color == Piece.WHITE ? 8 : -8;
        int pawn = Piece.of(color, Piece.PAWN);
        int file = Square.file(king);
        int bonus = 0;
        for (int f = Math.max(0, file - 1); f <= Math.min(7, file + 1); f++) {
            int front = Square.of(f, rank) + forward;
            if (squares[front] == pawn) {
                bonus += KING_SHIELD_PAWN;
            } else if (squares[front + forward] == pawn) {
                bonus += KING_SHIELD_PAWN / 2;
            }
        }
        return bonus;
    }
}
