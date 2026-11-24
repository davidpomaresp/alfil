package io.github.davidpomaresp.alfil;

/** What a finished search reports: the move to play and some numbers about how it got there. */
public record SearchResult(int bestMove, int score, int depth, long nodes, long millis, int[] pv) {

    public boolean isMate() {
        return Math.abs(score) >= Search.MATE - Search.MAX_PLY;
    }

    /** Moves until mate, positive if we mate and negative if we get mated. */
    public int mateIn() {
        int plies = Search.MATE - Math.abs(score);
        int moves = (plies + 1) / 2;
        return score > 0 ? moves : -moves;
    }
}
