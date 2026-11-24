package io.github.davidpomaresp.alfil;

import static io.github.davidpomaresp.alfil.Piece.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class FenTest {

    @Test
    void startPositionHasPiecesOnTheRightSquares() {
        Board board = Board.startPosition();

        assertEquals(of(WHITE, ROOK), board.pieceAt("a1"));
        assertEquals(of(WHITE, KNIGHT), board.pieceAt("b1"));
        assertEquals(of(WHITE, BISHOP), board.pieceAt("c1"));
        assertEquals(of(WHITE, QUEEN), board.pieceAt("d1"));
        assertEquals(of(WHITE, KING), board.pieceAt("e1"));
        assertEquals(of(BLACK, QUEEN), board.pieceAt("d8"));
        assertEquals(of(BLACK, KING), board.pieceAt("e8"));
        assertEquals(of(BLACK, ROOK), board.pieceAt("h8"));

        for (int file = 0; file < 8; file++) {
            assertEquals(of(WHITE, PAWN), board.pieceAt(Square.of(file, 1)));
            assertEquals(of(BLACK, PAWN), board.pieceAt(Square.of(file, 6)));
            for (int rank = 2; rank <= 5; rank++) {
                assertEquals(EMPTY, board.pieceAt(Square.of(file, rank)));
            }
        }
    }

    @Test
    void startPositionHasThirtyTwoPieces() {
        Board board = Board.startPosition();
        int count = 0;
        for (int sq = 0; sq < 64; sq++) {
            if (board.pieceAt(sq) != EMPTY) {
                count++;
            }
        }
        assertEquals(32, count);
    }

    @Test
    void startPositionStateFields() {
        Board board = Board.startPosition();

        assertEquals(WHITE, board.sideToMove());
        assertTrue(board.canCastle(Board.WHITE_KINGSIDE));
        assertTrue(board.canCastle(Board.WHITE_QUEENSIDE));
        assertTrue(board.canCastle(Board.BLACK_KINGSIDE));
        assertTrue(board.canCastle(Board.BLACK_QUEENSIDE));
        assertEquals(Square.NONE, board.enPassantSquare());
        assertEquals(0, board.halfmoveClock());
        assertEquals(1, board.fullmoveNumber());
    }

    @Test
    void readsSideCastlingAndEnPassant() {
        Board board = Board.fromFen("rnbqkbnr/ppp1pppp/8/8/3pP3/8/PPPP1PPP/RNBQKBNR b Kq e3 0 3");

        assertEquals(BLACK, board.sideToMove());
        assertTrue(board.canCastle(Board.WHITE_KINGSIDE));
        assertFalse(board.canCastle(Board.WHITE_QUEENSIDE));
        assertFalse(board.canCastle(Board.BLACK_KINGSIDE));
        assertTrue(board.canCastle(Board.BLACK_QUEENSIDE));
        assertEquals(Square.parse("e3"), board.enPassantSquare());
        assertEquals(3, board.fullmoveNumber());
    }

    @Test
    void noCastlingRights() {
        Board board = Board.fromFen("4k3/8/8/8/8/8/8/4K3 w - - 0 1");
        assertEquals(0, board.castlingRights());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            Board.START_FEN,
            "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1",
            "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1",
            "rnbqkbnr/ppp1pppp/8/8/3pP3/8/PPPP1PPP/RNBQKBNR b Kq e3 0 3",
    })
    void formatGivesBackTheSameFen(String fen) {
        assertEquals(fen, Board.fromFen(fen).toFen());
    }

    @Test
    void missingCountersDefaultToZeroAndOne() {
        Board board = Board.fromFen("4k3/8/8/8/8/8/8/4K3 w - -");
        assertEquals(0, board.halfmoveClock());
        assertEquals(1, board.fullmoveNumber());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP w KQkq - 0 1",
            "rnbqkbnr/pppppppp/9/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNX w KQkq - 0 1",
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR x KQkq - 0 1",
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkz - 0 1",
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq e9 0 1",
    })
    void rejectsBrokenFen(String fen) {
        assertThrows(IllegalArgumentException.class, () -> Board.fromFen(fen));
    }
}
