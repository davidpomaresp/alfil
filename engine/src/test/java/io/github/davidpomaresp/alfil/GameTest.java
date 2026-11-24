package io.github.davidpomaresp.alfil;

import static org.junit.jupiter.api.Assertions.*;

import io.github.davidpomaresp.alfil.Game.Status;
import org.junit.jupiter.api.Test;

class GameTest {

    @Test
    void newGameIsBeingPlayed() {
        assertEquals(Status.PLAYING, new Game().status());
    }

    @Test
    void rejectsIllegalMoves() {
        Game game = new Game();
        assertFalse(game.play("e2e5"));
        assertFalse(game.play("e7e5"));
        assertFalse(game.play("nonsense"));
        assertTrue(game.play("e2e4"));
    }

    @Test
    void foolsMate() {
        Game game = new Game();
        for (String move : new String[] {"f2f3", "e7e5", "g2g4", "d8h4"}) {
            assertTrue(game.play(move), move);
        }
        assertEquals(Status.BLACK_WINS_BY_CHECKMATE, game.status());
        assertFalse(game.play("a2a3"));
    }

    @Test
    void stalemate() {
        assertEquals(Status.DRAW_BY_STALEMATE, new Game("7k/5Q2/6K1/8/8/8/8/8 b - - 0 1").status());
    }

    @Test
    void threefoldRepetition() {
        Game game = new Game();
        String[] shuffle = {"g1f3", "g8f6", "f3g1", "f6g8"};
        for (int round = 0; round < 2; round++) {
            for (String move : shuffle) {
                assertEquals(Status.PLAYING, game.status());
                game.play(move);
            }
        }
        assertEquals(Status.DRAW_BY_REPETITION, game.status());
    }

    @Test
    void fiftyMoveRule() {
        assertEquals(Status.DRAW_BY_FIFTY_MOVES, new Game("4k3/8/8/8/8/8/R7/4K3 w - - 100 80").status());
    }

    @Test
    void insufficientMaterial() {
        assertEquals(Status.DRAW_BY_INSUFFICIENT_MATERIAL, new Game("4k3/8/8/8/8/8/8/3NK3 w - - 0 1").status());
    }

    @Test
    void promotionByUci() {
        Game game = new Game("4k3/1P6/8/8/8/8/8/4K3 w - - 0 1");
        assertTrue(game.play("b7b8n"));
        assertEquals(Piece.of(Piece.WHITE, Piece.KNIGHT), game.board().pieceAt("b8"));
    }

    @Test
    void engineFinishesAGameAgainstItself() {
        Game game = new Game();
        int plies = 0;
        while (!game.status().isOver() && plies < 300) {
            SearchResult result = game.think(2, 0);
            assertTrue(game.play(Move.toUci(result.bestMove())));
            plies++;
        }
        assertTrue(game.status().isOver() || plies == 300);
    }
}
