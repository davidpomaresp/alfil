package io.github.davidpomaresp.alfil;

/**
 * Negamax with alpha-beta pruning, iterative deepening and a quiescence search.
 *
 * Negamax: the score of a position for the side to move is the best of the
 * negated scores of the positions after each move. Alpha-beta skips moves that
 * cannot change the result: alpha is what we are already guaranteed, beta is the
 * most the opponent will allow.
 */
public final class Search {

    public static final int MATE = 30000;
    public static final int MAX_PLY = 128;
    static final int INFINITY = 32000;

    private final Board board;
    private final MoveList[] moveLists = new MoveList[MAX_PLY + 1];
    private final int[][] captureScores = new int[MAX_PLY + 1][256];
    private final int[][] pvTable = new int[MAX_PLY + 1][MAX_PLY + 1];
    private final int[] pvLength = new int[MAX_PLY + 1];

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
        this.board = board;
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
        stopRequested = false;
        startTime = System.currentTimeMillis();
        deadline = millis > 0 ? startTime + millis : Long.MAX_VALUE;
        maxDepth = Math.min(Math.max(maxDepth, 1), MAX_PLY - 1);

        MoveList rootMoves = MoveGenerator.legalMoves(board);
        if (rootMoves.size() == 0) {
            return new SearchResult(Move.NONE, board.inCheck() ? -MATE : 0, 0, 0, 0, new int[0]);
        }

        SearchResult best = new SearchResult(rootMoves.get(0), 0, 0, 0, 0, new int[] {rootMoves.get(0)});
        for (int depth = 1; depth <= maxDepth; depth++) {
            int score = negamax(depth, -INFINITY, INFINITY, 0);
            if (stopped) {
                break;
            }
            int[] pv = java.util.Arrays.copyOf(pvTable[0], pvLength[0]);
            best = new SearchResult(pv[0], score, depth, nodes, System.currentTimeMillis() - startTime, pv);
            if (listener != null) {
                listener.onIteration(best);
            }
            if (Math.abs(score) >= MATE - depth) {
                break;
            }
        }
        return new SearchResult(best.bestMove(), best.score(), best.depth(), nodes,
                System.currentTimeMillis() - startTime, best.pv());
    }

    private int negamax(int depth, int alpha, int beta, int ply) {
        pvLength[ply] = 0;
        if (shouldStop()) {
            return 0;
        }
        if (ply > 0 && isDraw()) {
            return 0;
        }
        if (depth <= 0 || ply >= MAX_PLY) {
            return quiescence(alpha, beta, ply);
        }
        nodes++;

        MoveList moves = moveLists[ply];
        MoveGenerator.generateLegal(board, moves);
        if (moves.size() == 0) {
            // Prefer the quickest mate and the slowest loss.
            return board.inCheck() ? -MATE + ply : 0;
        }

        if (moveOrdering) {
            sortCapturesFirst(moves, captureScores[ply]);
        }

        int bestScore = -INFINITY;
        for (int i = 0; i < moves.size(); i++) {
            int move = moves.get(i);
            board.makeMove(move);
            int score = -negamax(depth - 1, -beta, -alpha, ply + 1);
            board.unmakeMove(move);
            if (stopped) {
                return 0;
            }
            if (score > bestScore) {
                bestScore = score;
                if (score > alpha) {
                    alpha = score;
                    updatePv(ply, move);
                    if (alpha >= beta) {
                        break;
                    }
                }
            }
        }
        return bestScore;
    }

    /**
     * Keeps searching captures and promotions until the position is quiet, so the
     * evaluation is never taken in the middle of an exchange (the "horizon effect").
     */
    private int quiescence(int alpha, int beta, int ply) {
        nodes++;
        if (shouldStop()) {
            return 0;
        }
        int standPat = Evaluator.evaluate(board);
        if (ply >= MAX_PLY) {
            return standPat;
        }
        if (standPat >= beta) {
            return standPat;
        }
        if (standPat > alpha) {
            alpha = standPat;
        }

        MoveList moves = moveLists[ply];
        MoveGenerator.generateLegal(board, moves);
        int count = keepCapturesInOrder(moves, captureScores[ply]);
        int bestScore = standPat;
        for (int i = 0; i < count; i++) {
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
     * Keeps only captures and promotions, most valuable victim first and, among equal
     * victims, least valuable attacker first ("MVV-LVA"). Without this the quiescence
     * search can explode in positions with many possible captures.
     */
    private int keepCapturesInOrder(MoveList moves, int[] scores) {
        int count = 0;
        for (int i = 0; i < moves.size(); i++) {
            int move = moves.get(i);
            if (Move.isCapture(move) || Move.isPromotion(move)) {
                moves.set(count, move);
                scores[count++] = captureScore(move);
            }
        }
        sort(moves, scores, count);
        return count;
    }

    /**
     * Tries captures first (MVV-LVA), then quiet moves. Good moves first means alpha
     * rises early, so later moves get cut off sooner.
     */
    private void sortCapturesFirst(MoveList moves, int[] scores) {
        for (int i = 0; i < moves.size(); i++) {
            int move = moves.get(i);
            scores[i] = Move.isCapture(move) || Move.isPromotion(move) ? 100_000 + captureScore(move) : 0;
        }
        sort(moves, scores, moves.size());
    }

    private static void sort(MoveList moves, int[] scores, int count) {
        for (int i = 1; i < count; i++) {
            int move = moves.get(i);
            int score = scores[i];
            int j = i - 1;
            while (j >= 0 && scores[j] < score) {
                moves.set(j + 1, moves.get(j));
                scores[j + 1] = scores[j];
                j--;
            }
            moves.set(j + 1, move);
            scores[j + 1] = score;
        }
    }

    private int captureScore(int move) {
        int victim = Move.flags(move) == Move.EN_PASSANT
                ? Piece.PAWN
                : Piece.type(board.pieceAt(Move.to(move)));
        int attacker = Piece.type(board.pieceAt(Move.from(move)));
        return Evaluator.VALUE[victim] * 10 - attacker + Evaluator.VALUE[Move.promotionType(move)];
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
