package io.github.davidpomaresp.alfil;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class BoardTest {

    private static void play(Board board, String... ucis) {
        for (String uci : ucis) {
            MoveList moves = MoveGenerator.legalMoves(board);
            int found = Move.NONE;
            for (int i = 0; i < moves.size(); i++) {
                if (Move.toUci(moves.get(i)).equals(uci)) {
                    found = moves.get(i);
                }
            }
            assertNotEquals(Move.NONE, found, uci);
            board.makeMove(found);
        }
    }

    /** Walks the move tree and checks the incremental hash against a full recomputation. */
    private static void checkHashes(Board board, int depth) {
        assertEquals(Zobrist.hash(board), board.hash(), board.toFen());
        if (depth == 0) {
            return;
        }
        MoveList moves = MoveGenerator.legalMoves(board);
        for (int i = 0; i < moves.size(); i++) {
            long before = board.hash();
            board.makeMove(moves.get(i));
            checkHashes(board, depth - 1);
            board.unmakeMove(moves.get(i));
            assertEquals(before, board.hash());
        }
    }

    @Test
    void incrementalHashMatchesFullHash() {
        checkHashes(Board.fromFen("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1"), 3);
        checkHashes(Board.fromFen("8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1"), 4);
        checkHashes(Board.fromFen("rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8"), 3);
    }

    @Test
    void sameMovesInDifferentOrderGiveSameHash() {
        Board a = Board.startPosition();
        Board b = Board.startPosition();
        play(a, "g1f3", "g8f6", "b1c3");
        play(b, "b1c3", "g8f6", "g1f3");
        assertEquals(a.hash(), b.hash());
    }

    @Test
    void detectsRepetition() {
        Board board = Board.startPosition();
        play(board, "g1f3", "g8f6", "f3g1", "f6g8");
        assertTrue(board.isRepetition());
        assertEquals(1, board.repetitionCount());
        play(board, "g1f3", "g8f6", "f3g1", "f6g8");
        assertEquals(2, board.repetitionCount());
    }

    @Test
    void pawnMoveResetsRepetitionHistory() {
        Board board = Board.startPosition();
        play(board, "g1f3", "g8f6", "f3g1", "f6g8", "e2e4");
        assertFalse(board.isRepetition());
    }

    @Test
    void insufficientMaterial() {
        assertTrue(Board.fromFen("8/8/4k3/8/8/3K4/8/8 w - - 0 1").isInsufficientMaterial());
        assertTrue(Board.fromFen("8/8/4k3/8/8/3KN3/8/8 w - - 0 1").isInsufficientMaterial());
        assertTrue(Board.fromFen("8/8/4kb2/8/8/3KB3/8/8 w - - 0 1").isInsufficientMaterial());
        assertFalse(Board.fromFen("8/8/4k3/8/8/3KBB2/8/8 w - - 0 1").isInsufficientMaterial());
        assertFalse(Board.fromFen("8/8/4k3/8/8/3KP3/8/8 w - - 0 1").isInsufficientMaterial());
        assertFalse(Board.fromFen("8/8/4kn2/8/8/3KN3/8/8 w - - 0 1").isInsufficientMaterial());
    }
}
