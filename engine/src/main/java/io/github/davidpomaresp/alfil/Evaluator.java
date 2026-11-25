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

    /** Plain piece values, used to order captures. The evaluation uses its own (tunable) ones. */
    static final int[] VALUE = {0, 100, 320, 330, 500, 900, 0};

    // How much each piece counts towards "middlegame-ness": 24 at the start, 0 with only pawns.
    private static final int[] PHASE = {0, 0, 1, 1, 2, 4, 0};
    static final int MAX_PHASE = 24;
    // Typical number of reachable squares, so an average piece gets about 0 for mobility.
    private static final int[] MOBILITY_BASE = {0, 0, 4, 6, 6, 12, 0};

    private static int[] w = EvalParams.defaults();
    // Material plus square bonus, [type][square] from white's side; black mirrors the square.
    private static final int[][] MIDGAME = new int[7][64];
    private static final int[][] ENDGAME = new int[7][64];

    static {
        rebuildTables();
    }

    /** Replaces the weights (used by the tuner) and recomputes the square tables. */
    static void setWeights(int[] weights) {
        w = weights.clone();
        rebuildTables();
    }

    static int[] weights() {
        return w.clone();
    }

    private static void rebuildTables() {
        for (int sq = 0; sq < 64; sq++) {
            int file = Square.file(sq);
            int rank = Square.rank(sq);
            int ring = centerRing(sq);
            boolean centralFile = file == 3 || file == 4;

            for (int type = Piece.PAWN; type <= Piece.KING; type++) {
                int material = type == Piece.KING ? 0 : w[EvalParams.MATERIAL_MG + type - 1];
                MIDGAME[type][sq] = material;
                ENDGAME[type][sq] = type == Piece.KING ? 0 : w[EvalParams.MATERIAL_EG + type - 1];
            }

            if (rank >= 1 && rank <= 6) {
                MIDGAME[Piece.PAWN][sq] += (rank - 1) * w[EvalParams.PAWN_ADVANCE_MG]
                        + (centralFile && rank >= 3 ? w[EvalParams.PAWN_CENTER_MG] : 0);
                ENDGAME[Piece.PAWN][sq] += w[EvalParams.PAWN_RANK_EG + rank - 1];
            }
            MIDGAME[Piece.KNIGHT][sq] += w[EvalParams.KNIGHT_RING + ring];
            ENDGAME[Piece.KNIGHT][sq] += w[EvalParams.KNIGHT_RING + ring];
            MIDGAME[Piece.BISHOP][sq] += w[EvalParams.BISHOP_RING + ring];
            ENDGAME[Piece.BISHOP][sq] += w[EvalParams.BISHOP_RING + ring];
            MIDGAME[Piece.ROOK][sq] += (rank == 6 ? w[EvalParams.ROOK_SEVENTH_MG] : 0)
                    + (centralFile ? w[EvalParams.ROOK_CENTER_FILE_MG] : 0);
            ENDGAME[Piece.ROOK][sq] += rank == 6 ? w[EvalParams.ROOK_SEVENTH_EG] : 0;
            MIDGAME[Piece.QUEEN][sq] += w[EvalParams.QUEEN_RING + ring];
            ENDGAME[Piece.QUEEN][sq] += w[EvalParams.QUEEN_RING + ring];
            MIDGAME[Piece.KING][sq] += rank == 0 ? w[EvalParams.KING_FILE_MG + file] : w[EvalParams.KING_RANK_MG] * rank;
            ENDGAME[Piece.KING][sq] += w[EvalParams.KING_RING_EG + ring];
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
            mg[color] += MIDGAME[type][relative];
            eg[color] += ENDGAME[type][relative];
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
            mg[color] += mobility * w[EvalParams.MOBILITY_MG + type - Piece.KNIGHT];
            eg[color] += mobility * w[EvalParams.MOBILITY_EG + type - Piece.KNIGHT];
            if (type == Piece.ROOK && pawnsOnFile[color][f] == 0) {
                boolean open = pawnsOnFile[them][f] == 0;
                mg[color] += w[open ? EvalParams.ROOK_OPEN_MG : EvalParams.ROOK_SEMI_OPEN_MG];
                eg[color] += w[open ? EvalParams.ROOK_OPEN_EG : EvalParams.ROOK_SEMI_OPEN_EG];
            }
        }

        for (int color = 0; color < 2; color++) {
            if (bishops[color] >= 2) {
                mg[color] += w[EvalParams.BISHOP_PAIR_MG];
                eg[color] += w[EvalParams.BISHOP_PAIR_EG];
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
            mg[color] += w[EvalParams.ISOLATED_MG];
            eg[color] += w[EvalParams.ISOLATED_EG];
        }
        // Count doubling once, on the rear pawn of the file.
        if (pawnsOnFile[color][f] > 1 && r == rearmost[color][f]) {
            mg[color] += w[EvalParams.DOUBLED_MG];
            eg[color] += w[EvalParams.DOUBLED_EG];
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
            mg[color] += w[EvalParams.PASSED_MG + relativeRank - 1];
            eg[color] += w[EvalParams.PASSED_EG + relativeRank - 1];
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
                bonus += w[EvalParams.KING_SHIELD_MG];
            } else if (squares[front + forward] == pawn) {
                bonus += w[EvalParams.KING_SHIELD_MG] / 2;
            }
        }
        return bonus;
    }
}
