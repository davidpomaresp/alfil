package io.github.davidpomaresp.alfil;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MoveGeneratorTest {

    private static boolean hasMove(Board board, String uci) {
        MoveList moves = MoveGenerator.legalMoves(board);
        for (int i = 0; i < moves.size(); i++) {
            if (Move.toUci(moves.get(i)).equals(uci)) {
                return true;
            }
        }
        return false;
    }

    /** Positions that each target one special rule, with their published perft counts. */
    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = ';', value = {
            "illegal en passant 1; 3k4/3p4/8/K1P4r/8/8/8/8 b - - 0 1; 6; 1134888",
            "illegal en passant 2; 8/8/4k3/8/2p5/8/B2P2K1/8 w - - 0 1; 6; 1015133",
            "en passant gives check; 8/8/1k6/2b5/2pP4/8/5K2/8 b - d3 0 1; 6; 1440467",
            "short castling gives check; 5k2/8/8/8/8/8/8/4K2R w K - 0 1; 6; 661072",
            "long castling gives check; 3k4/8/8/8/8/8/8/R3K3 w Q - 0 1; 6; 803711",
            "castling rights; r3k2r/1b4bq/8/8/8/8/7B/R3K2R w KQkq - 0 1; 4; 1274206",
            "castling prevented; r3k2r/8/3Q4/8/8/5q2/8/R3K2R b KQkq - 0 1; 4; 1720476",
            "promote out of check; 2K2r2/4P3/8/8/8/8/8/3k4 w - - 0 1; 6; 3821001",
            "discovered check; 8/8/1P2K3/8/2n5/1q6/8/5k2 b - - 0 1; 5; 1004658",
            "promote to give check; 4k3/1P6/8/8/8/8/K7/8 w - - 0 1; 6; 217342",
            "underpromote to give check; 8/P1k5/K7/8/8/8/8/8 w - - 0 1; 6; 92683",
            "self stalemate; K1k5/8/P7/8/8/8/8/8 w - - 0 1; 6; 2217",
            "stalemate and checkmate 1; 8/k1P5/8/1K6/8/8/8/8 w - - 0 1; 7; 567584",
            "stalemate and checkmate 2; 8/8/2k5/5q2/5n2/8/5K2/8 b - - 0 1; 4; 23527",
    })
    void specialRules(String name, String fen, int depth, long expected) {
        assertEquals(expected, Perft.count(Board.fromFen(fen), depth));
    }

    @Test
    void enPassantThatExposesTheKingIsNotAllowed() {
        // Taking on d3 would remove both pawns from the fourth rank and open the queen's line.
        Board board = Board.fromFen("8/8/8/8/k2Pp2Q/8/8/3K4 b - d3 0 1");
        assertFalse(hasMove(board, "e4d3"));
    }

    @Test
    void enPassantIsGeneratedRightAfterTheDoublePush() {
        Board board = Board.fromFen("rnbqkbnr/ppp1pppp/8/8/3pP3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 3");
        assertTrue(hasMove(board, "d4e3"));
    }

    @Test
    void cannotCastleThroughAnAttackedSquare() {
        // The black rook on f8 covers f1.
        Board board = Board.fromFen("4kr2/8/8/8/8/8/8/R3K2R w KQ - 0 1");
        assertFalse(hasMove(board, "e1g1"));
        assertTrue(hasMove(board, "e1c1"));
    }

    @Test
    void cannotCastleOutOfCheck() {
        Board board = Board.fromFen("4r1k1/8/8/8/8/8/8/R3K2R w KQ - 0 1");
        assertTrue(board.inCheck());
        assertFalse(hasMove(board, "e1g1"));
        assertFalse(hasMove(board, "e1c1"));
    }

    @Test
    void queensideCastlingOnlyNeedsTheB1SquareEmpty() {
        // b1 is attacked, but the king never crosses it.
        Board board = Board.fromFen("1r2k3/8/8/8/8/8/8/R3K3 w Q - 0 1");
        assertTrue(hasMove(board, "e1c1"));
    }

    @Test
    void pawnPromotesToAllFourPieces() {
        Board board = Board.fromFen("4k3/1P6/8/8/8/8/8/4K3 w - - 0 1");
        for (String uci : new String[] {"b7b8q", "b7b8r", "b7b8b", "b7b8n"}) {
            assertTrue(hasMove(board, uci), uci);
        }
    }

    @Test
    void checkmateHasNoMoves() {
        Board board = Board.fromFen("rnb1kbnr/pppp1ppp/8/4p3/6Pq/5P2/PPPPP2P/RNBQKBNR w KQkq - 1 3");
        assertTrue(board.inCheck());
        assertEquals(0, MoveGenerator.legalMoves(board).size());
    }

    @Test
    void stalemateHasNoMovesAndNoCheck() {
        Board board = Board.fromFen("7k/5Q2/6K1/8/8/8/8/8 b - - 0 1");
        assertFalse(board.inCheck());
        assertEquals(0, MoveGenerator.legalMoves(board).size());
    }

    @Test
    void makeAndUnmakeRestoreThePosition() {
        Board board = Board.fromFen("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1");
        String before = board.toFen();
        MoveList moves = MoveGenerator.legalMoves(board);
        for (int i = 0; i < moves.size(); i++) {
            board.makeMove(moves.get(i));
            board.unmakeMove(moves.get(i));
            assertEquals(before, board.toFen(), Move.toUci(moves.get(i)));
        }
    }

    @Test
    void movingTheRookRemovesItsCastlingRight() {
        Board board = Board.fromFen("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1");
        board.makeMove(Move.of(Square.parse("h1"), Square.parse("h2"), Move.QUIET));
        assertFalse(board.canCastle(Board.WHITE_KINGSIDE));
        assertTrue(board.canCastle(Board.WHITE_QUEENSIDE));
    }

    @Test
    void capturingARookRemovesTheOpponentsCastlingRight() {
        Board board = Board.fromFen("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1");
        board.makeMove(Move.of(Square.parse("a1"), Square.parse("a8"), Move.CAPTURE));
        assertFalse(board.canCastle(Board.BLACK_QUEENSIDE));
        assertFalse(board.canCastle(Board.WHITE_QUEENSIDE));
        assertTrue(board.canCastle(Board.BLACK_KINGSIDE));
    }
}
