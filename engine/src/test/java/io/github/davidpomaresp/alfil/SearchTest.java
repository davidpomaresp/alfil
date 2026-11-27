package io.github.davidpomaresp.alfil;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SearchTest {

    private static SearchResult search(String fen, int depth) {
        return new Search(Board.fromFen(fen)).search(depth, 0);
    }

    @Test
    void findsMateInOne() {
        // Back-rank mate: Ra8#.
        SearchResult result = search("6k1/5ppp/8/8/8/8/8/R5K1 w - - 0 1", 3);
        assertEquals("a1a8", Move.toUci(result.bestMove()));
        assertTrue(result.isMate());
        assertEquals(1, result.mateIn());
    }

    @Test
    void findsMateInTwo() {
        // 1.Qb8+ Rxb8 2.Rxb8#
        SearchResult result = search("r5k1/5ppp/8/8/8/8/1Q3PPP/1R4K1 w - - 0 1", 5);
        assertTrue(result.isMate());
        assertEquals(2, result.mateIn());
    }

    @Test
    void knowsWhenItIsGettingMated() {
        // Same position after 1.Qb8+: black's only move is Rxb8, then Rxb8#.
        SearchResult result = search("rQ4k1/5ppp/8/8/8/8/5PPP/1R4K1 b - - 1 1", 4);
        assertTrue(result.isMate());
        assertEquals(-1, result.mateIn());
    }

    @Test
    void takesAHangingQueen() {
        SearchResult result = search("4k3/8/8/3q4/8/8/8/3RK3 w - - 0 1", 3);
        assertEquals("d1d5", Move.toUci(result.bestMove()));
    }

    @Test
    void doesNotTakeADefendedPawnWithTheQueen() {
        // Qxd5 loses the queen to exd5.
        SearchResult result = search("4k3/8/4p3/3p4/8/8/8/3QK3 w - - 0 1", 4);
        assertNotEquals("d1d5", Move.toUci(result.bestMove()));
    }

    @Test
    void avoidsStalemateWhenWinning() {
        // Qg6 would stalemate; any sensible move keeps winning chances.
        SearchResult result = search("7k/8/5K2/8/8/8/8/6Q1 w - - 0 1", 4);
        Board board = Board.fromFen("7k/8/5K2/8/8/8/8/6Q1 w - - 0 1");
        board.makeMove(result.bestMove());
        assertFalse(MoveGenerator.legalMoves(board).size() == 0 && !board.inCheck());
        assertTrue(result.score() > 500);
    }

    @Test
    void stopsWithinTheTimeLimit() {
        long start = System.currentTimeMillis();
        SearchResult result = new Search(Board.startPosition()).search(64, 300);
        long elapsed = System.currentTimeMillis() - start;
        assertTrue(elapsed < 600, "took " + elapsed + " ms");
        assertNotEquals(Move.NONE, result.bestMove());
        assertTrue(result.depth() >= 1);
    }

    @Test
    void reportsNoMoveWhenMated() {
        SearchResult result = search("rnb1kbnr/pppp1ppp/8/4p3/6Pq/5P2/PPPPP2P/RNBQKBNR w KQkq - 1 3", 3);
        assertEquals(Move.NONE, result.bestMove());
        assertEquals(-Search.MATE, result.score());
    }

    @Test
    void quiescenceSeesMateAfterACapture() {
        // Rxa8 is a capture and also mate on the back rank.
        Search search = new Search(Board.fromFen("r5k1/5ppp/8/8/8/8/8/R5K1 w - - 0 1"));
        assertTrue(search.quiescenceScore() >= Search.MATE - Search.MAX_PLY);
        Search mated = new Search(Board.fromFen("R5k1/5ppp/8/8/8/8/8/6K1 b - - 0 1"));
        assertEquals(-Search.MATE, mated.quiescenceScore());
    }

    @Test
    void quiescenceSearchesQuietEvasions() {
        // Black is in check and can only escape with a quiet king move: it must not mate itself.
        Search search = new Search(Board.fromFen("R5k1/8/8/8/8/8/8/6K1 b - - 0 1"));
        assertTrue(Math.abs(search.quiescenceScore()) < Search.MATE - Search.MAX_PLY);
    }
}
