package io.github.davidpomaresp.alfil;

/**
 * Negamax with alpha-beta pruning, iterative deepening and a quiescence search.
 *
 * Negamax: the score of a position for the side to move is the best of the
 * negated scores of the positions after each move. Alpha-beta skips moves that
 * cannot change the result: alpha is what we are already guaranteed, beta is the
 * most the opponent will allow.
 *
 * On top of that, the usual ways to look at fewer positions: a transposition table,
 * good move ordering (hash move, captures, killers, history), principal variation
 * search, null-move pruning and late move reductions.
 */
public final class Search {

    public static final int MATE = 30000;
    public static final int MAX_PLY = 128;
    static final int INFINITY = 32000;

    private static final int HASH_MOVE_SCORE = 2_000_000;
    private static final int CAPTURE_SCORE = 1_000_000;
    private static final int KILLER_SCORE = 900_000;

    private final Board board;
    private final TranspositionTable table;
    private final Evaluator evaluator = new Evaluator();
    private final See see = new See();
    private final MoveList[] moveLists = new MoveList[MAX_PLY + 1];
    private final int[][] moveScores = new int[MAX_PLY + 1][256];
    private final int[][] pvTable = new int[MAX_PLY + 1][MAX_PLY + 1];
    private final int[] pvLength = new int[MAX_PLY + 1];
    private final int[][] killers = new int[MAX_PLY + 1][2];
    private final int[][] history = new int[64][64];

    private boolean moveOrdering = true;
    private long nodes;
    private long startTime;
    private long deadline;
    private boolean stopped;
    private volatile boolean stopRequested;
    private SearchListener listener;

    public interface SearchListener {
        void onIteration(SearchResult result);
    }

    public Search(Board board) {
        this(board, new TranspositionTable(20));
    }

    public Search(Board board, TranspositionTable table) {
        this.board = board;
        this.table = table;
        for (int i = 0; i < moveLists.length; i++) {
            moveLists[i] = new MoveList();
        }
    }

    public void setListener(SearchListener listener) {
        this.listener = listener;
    }

    /** Turns move ordering in the main search on or off, to measure what it is worth. */
    public void setMoveOrdering(boolean enabled) {
        this.moveOrdering = enabled;
    }

    /** True if no capture changes the static evaluation (quiescence score equals it). */
    public boolean isQuiet() {
        nodes = 1;
        stopped = false;
        deadline = Long.MAX_VALUE;
        return quiescence(-INFINITY, INFINITY, 0) == evaluator.score(board);
    }

    /** Score of the quiescence search alone from the current position (for tests). */
    int quiescenceScore() {
        nodes = 1;
        stopped = false;
        deadline = Long.MAX_VALUE;
        return quiescence(-INFINITY, INFINITY, 0);
    }

    /** Asks a running search to stop as soon as possible (e.g. from another thread). */
    public void stop() {
        stopRequested = true;
    }

    /**
     * Searches depth 1, 2, 3... until {@code maxDepth} or until {@code millis} run out,
     * and returns the best move of the last depth that finished.
     */
    public SearchResult search(int maxDepth, long millis) {
        nodes = 0;
        stopped = false;
        startTime = System.currentTimeMillis();
        deadline = millis > 0 ? startTime + millis : Long.MAX_VALUE;
        maxDepth = Math.min(Math.max(maxDepth, 1), MAX_PLY - 1);
        for (int[] k : killers) {
            k[0] = Move.NONE;
            k[1] = Move.NONE;
        }
        for (int[] h : history) {
            java.util.Arrays.fill(h, 0);
        }

        MoveList rootMoves = MoveGenerator.legalMoves(board);
        if (rootMoves.size() == 0) {
            return new SearchResult(Move.NONE, board.inCheck() ? -MATE : 0, 0, 0, 0, new int[0]);
        }

        SearchResult best = new SearchResult(rootMoves.get(0), 0, 0, 0, 0, new int[] {rootMoves.get(0)});
        for (int depth = 1; depth <= maxDepth; depth++) {
            int score = aspirationSearch(depth, best.score());
            if (stopped && depth > 1) {
                break;
            }
            if (pvLength[0] > 0) {
                int[] pv = java.util.Arrays.copyOf(pvTable[0], pvLength[0]);
                best = new SearchResult(pv[0], score, depth, nodes, System.currentTimeMillis() - startTime, pv);
                if (listener != null) {
                    listener.onIteration(best);
                }
            }
            if (stopped || Math.abs(score) >= MATE - depth) {
                break;
            }
            // A new depth takes several times longer than the last one; don't start
            // one we almost certainly can't finish.
            long elapsed = System.currentTimeMillis() - startTime;
            if (millis > 0 && elapsed > millis / 2) {
                break;
            }
        }
        return new SearchResult(best.bestMove(), best.score(), best.depth(), nodes,
                System.currentTimeMillis() - startTime, best.pv());
    }

    /**
     * Aspiration windows: the score rarely changes much from one depth to the next, so
     * search with a narrow window around the last one (more cutoffs) and widen it only
     * if the real score falls outside.
     */
    private int aspirationSearch(int depth, int previous) {
        if (depth < 4 || Math.abs(previous) >= MATE - MAX_PLY) {
            return negamax(depth, -INFINITY, INFINITY, 0, true);
        }
        int delta = 25;
        int alpha = previous - delta;
        int beta = previous + delta;
        while (true) {
            int score = negamax(depth, alpha, beta, 0, true);
            if (stopped) {
                return score;
            }
            if (score <= alpha) {
                alpha = Math.max(-INFINITY, alpha - delta);
            } else if (score >= beta) {
                beta = Math.min(INFINITY, beta + delta);
            } else {
                return score;
            }
            delta *= 2;
            if (delta > 1000) {
                alpha = -INFINITY;
                beta = INFINITY;
            }
        }
    }

    private int negamax(int depth, int alpha, int beta, int ply, boolean allowNull) {
        pvLength[ply] = 0;
        if (shouldStop()) {
            return 0;
        }
        boolean root = ply == 0;
        if (!root && isDraw()) {
            return 0;
        }
        if (ply >= MAX_PLY) {
            return evaluator.score(board);
        }

        boolean inCheck = board.inCheck();
        if (inCheck) {
            depth++;
        }
        if (depth <= 0) {
            return quiescence(alpha, beta, ply);
        }
        nodes++;

        boolean pvNode = beta - alpha > 1;
        long entry = table.probe(board.hash());
        int hashMove = Move.NONE;
        if (entry != 0) {
            hashMove = TranspositionTable.move(entry);
            if (!pvNode && TranspositionTable.depth(entry) >= depth) {
                int score = fromTable(TranspositionTable.score(entry), ply);
                int bound = TranspositionTable.bound(entry);
                if (bound == TranspositionTable.EXACT
                        || (bound == TranspositionTable.LOWER && score >= beta)
                        || (bound == TranspositionTable.UPPER && score <= alpha)) {
                    return score;
                }
            }
        }

        // Null move: if passing the turn still leaves us above beta, a real move almost
        // surely does too, so cut without searching. Not in check (passing would be
        // illegal) and not with only pawns left (zugzwang is common there).
        int us = board.sideToMove();
        if (allowNull && !pvNode && !inCheck && depth >= 3 && board.hasNonPawnMaterial(us)
                && evaluator.score(board) >= beta) {
            int reduction = 2 + depth / 4;
            board.makeNullMove();
            int score = -negamax(depth - 1 - reduction, -beta, -beta + 1, ply + 1, false);
            board.unmakeNullMove();
            if (stopped) {
                return 0;
            }
            if (score >= beta && score < MATE - MAX_PLY) {
                return beta;
            }
        }

        MoveList moves = moveLists[ply];
        MoveGenerator.generatePseudoLegal(board, moves);
        int[] scores = moveScores[ply];
        if (moveOrdering) {
            scoreMoves(moves, scores, hashMove, ply);
        }

        int originalAlpha = alpha;
        int bestScore = -INFINITY;
        int bestMove = Move.NONE;
        int legal = 0;
        for (int i = 0; i < moves.size(); i++) {
            if (moveOrdering) {
                pickBest(moves, scores, i);
            }
            int move = moves.get(i);
            board.makeMove(move);
            if (board.isAttacked(board.kingSquare(us), us ^ 1)) {
                board.unmakeMove(move);
                continue;
            }
            legal++;
            boolean quiet = !Move.isCapture(move) && !Move.isPromotion(move);

            int score;
            if (legal == 1) {
                score = -negamax(depth - 1, -beta, -alpha, ply + 1, true);
            } else {
                // Late move reductions: quiet moves late in a well-ordered list are
                // rarely best, so search them shallower first and only fully if they surprise us.
                int reduction = 0;
                if (moveOrdering && depth >= 3 && legal > 3 && quiet && !inCheck && !board.inCheck()) {
                    reduction = legal > 8 ? 2 : 1;
                }
                // Principal variation search: prove the move is worse with a null window.
                score = -negamax(depth - 1 - reduction, -alpha - 1, -alpha, ply + 1, true);
                if (score > alpha && reduction > 0) {
                    score = -negamax(depth - 1, -alpha - 1, -alpha, ply + 1, true);
                }
                if (score > alpha && score < beta) {
                    score = -negamax(depth - 1, -beta, -alpha, ply + 1, true);
                }
            }
            board.unmakeMove(move);
            if (stopped) {
                return 0;
            }

            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
                if (score > alpha) {
                    alpha = score;
                    updatePv(ply, move);
                    if (alpha >= beta) {
                        if (quiet) {
                            rememberQuietCutoff(move, depth, ply);
                        }
                        break;
                    }
                }
            }
        }

        if (legal == 0) {
            // Prefer the quickest mate and the slowest loss.
            return inCheck ? -MATE + ply : 0;
        }

        int bound = bestScore >= beta ? TranspositionTable.LOWER
                : bestScore > originalAlpha ? TranspositionTable.EXACT
                : TranspositionTable.UPPER;
        table.store(board.hash(), bestMove, toTable(bestScore, ply), depth, bound);
        return bestScore;
    }

    /**
     * Keeps searching captures and promotions until the position is quiet, so the
     * evaluation is never taken in the middle of an exchange (the "horizon effect").
     */
    private int quiescence(int alpha, int beta, int ply) {
        nodes++;
        pvLength[ply] = 0;
        if (shouldStop()) {
            return 0;
        }
        if (board.inCheck() && ply < MAX_PLY) {
            return evasions(alpha, beta, ply);
        }
        int standPat = evaluator.score(board);
        if (ply >= MAX_PLY || standPat >= beta) {
            return standPat;
        }
        if (standPat > alpha) {
            alpha = standPat;
        }

        MoveList moves = moveLists[ply];
        MoveGenerator.generateCaptures(board, moves);
        int[] scores = moveScores[ply];
        for (int i = 0; i < moves.size(); i++) {
            scores[i] = captureScore(moves.get(i));
        }

        int us = board.sideToMove();
        int bestScore = standPat;
        for (int i = 0; i < moves.size(); i++) {
            pickBest(moves, scores, i);
            int move = moves.get(i);
            // Skip captures that lose material once the exchange is played out.
            if (!Move.isPromotion(move) && see.evaluate(board, move) < 0) {
                continue;
            }
            board.makeMove(move);
            if (board.isAttacked(board.kingSquare(us), us ^ 1)) {
                board.unmakeMove(move);
                continue;
            }
            int score = -quiescence(-beta, -alpha, ply + 1);
            board.unmakeMove(move);
            if (stopped) {
                return 0;
            }
            if (score > bestScore) {
                bestScore = score;
                if (score > alpha) {
                    alpha = score;
                    if (alpha >= beta) {
                        break;
                    }
                }
            }
        }
        return bestScore;
    }

    /**
     * In check there is no "stand pat": the side to move has to get out of check,
     * so every legal move is searched, quiet ones too, and no moves means mate.
     */
    private int evasions(int alpha, int beta, int ply) {
        MoveList moves = moveLists[ply];
        MoveGenerator.generateLegal(board, moves);
        if (moves.size() == 0) {
            return -MATE + ply;
        }
        int[] scores = moveScores[ply];
        for (int i = 0; i < moves.size(); i++) {
            int move = moves.get(i);
            scores[i] = Move.isCapture(move) ? captureScore(move) : -1_000_000;
        }
        int bestScore = -INFINITY;
        for (int i = 0; i < moves.size(); i++) {
            pickBest(moves, scores, i);
            int move = moves.get(i);
            board.makeMove(move);
            int score = -quiescence(-beta, -alpha, ply + 1);
            board.unmakeMove(move);
            if (stopped) {
                return 0;
            }
            if (score > bestScore) {
                bestScore = score;
                if (score > alpha) {
                    alpha = score;
                    if (alpha >= beta) {
                        break;
                    }
                }
            }
        }
        return bestScore;
    }

    /**
     * Order: the move the transposition table remembers, then captures by MVV-LVA
     * (most valuable victim, least valuable attacker), then killer moves, then
     * quiet moves by how often they caused cutoffs before (history).
     */
    private void scoreMoves(MoveList moves, int[] scores, int hashMove, int ply) {
        for (int i = 0; i < moves.size(); i++) {
            int move = moves.get(i);
            if (move == hashMove) {
                scores[i] = HASH_MOVE_SCORE;
            } else if (Move.isCapture(move) || Move.isPromotion(move)) {
                scores[i] = CAPTURE_SCORE + captureScore(move);
            } else if (move == killers[ply][0]) {
                scores[i] = KILLER_SCORE;
            } else if (move == killers[ply][1]) {
                scores[i] = KILLER_SCORE - 1;
            } else {
                scores[i] = history[Move.from(move)][Move.to(move)];
            }
        }
    }

    /** Swaps the highest-scored remaining move into position {@code index}. */
    private static void pickBest(MoveList moves, int[] scores, int index) {
        int best = index;
        for (int i = index + 1; i < moves.size(); i++) {
            if (scores[i] > scores[best]) {
                best = i;
            }
        }
        if (best != index) {
            int move = moves.get(index);
            moves.set(index, moves.get(best));
            moves.set(best, move);
            int score = scores[index];
            scores[index] = scores[best];
            scores[best] = score;
        }
    }

    private void rememberQuietCutoff(int move, int depth, int ply) {
        if (killers[ply][0] != move) {
            killers[ply][1] = killers[ply][0];
            killers[ply][0] = move;
        }
        int[] row = history[Move.from(move)];
        row[Move.to(move)] = Math.min(row[Move.to(move)] + depth * depth, KILLER_SCORE - 2);
    }

    private int captureScore(int move) {
        int victim = Move.flags(move) == Move.EN_PASSANT
                ? Piece.PAWN
                : Piece.type(board.pieceAt(Move.to(move)));
        int attacker = Piece.type(board.pieceAt(Move.from(move)));
        return Evaluator.VALUE[victim] * 10 - attacker + Evaluator.VALUE[Move.promotionType(move)];
    }

    // Mate scores are stored relative to the node, not the root, so they stay correct
    // when the same position is reached at a different ply.
    private static int toTable(int score, int ply) {
        if (score >= MATE - MAX_PLY) {
            return score + ply;
        }
        if (score <= -MATE + MAX_PLY) {
            return score - ply;
        }
        return score;
    }

    private static int fromTable(int score, int ply) {
        if (score >= MATE - MAX_PLY) {
            return score - ply;
        }
        if (score <= -MATE + MAX_PLY) {
            return score + ply;
        }
        return score;
    }

    private boolean isDraw() {
        return board.halfmoveClock() >= 100 || board.isRepetition() || board.isInsufficientMaterial();
    }

    private void updatePv(int ply, int move) {
        pvTable[ply][0] = move;
        System.arraycopy(pvTable[ply + 1], 0, pvTable[ply], 1, pvLength[ply + 1]);
        pvLength[ply] = pvLength[ply + 1] + 1;
    }

    private boolean shouldStop() {
        if (stopped) {
            return true;
        }
        // Reading the clock is slow, so only do it every 1024 nodes.
        if ((nodes & 1023) == 0 && (stopRequested || System.currentTimeMillis() >= deadline)) {
            stopped = true;
        }
        return stopped;
    }
}
