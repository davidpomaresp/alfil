package io.github.davidpomaresp.alfil;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SeeTest {

    @ParameterizedTest(name = "{1}: {2}")
    @CsvSource(delimiter = ';', value = {
            // Free pawn.
            "4k3/8/8/3p4/8/8/8/3RK3 w - - 0 1; d1d5; 100",
            // Pawn defended by a pawn: rook for pawn.
            "4k3/8/4p3/3p4/8/8/8/3RK3 w - - 0 1; d1d5; -400",
            // Pawn takes a defended knight: wins knight for pawn.
            "4k3/8/4p3/3n4/4P3/8/8/4K3 w - - 0 1; e4d5; 220",
            // Knight takes, pawn recaptures, rook takes back: knight for two pawns.
            "4k3/8/4p3/3p4/8/2N5/8/3RK3 w - - 0 1; c3d5; -120",
            // Queen takes a pawn defended by a rook: loses the queen.
            "3rk3/8/8/3p4/8/8/8/3QK3 w - - 0 1; d1d5; -800",
            // X-ray: the rook behind the queen recaptures, so it's queen for rook and pawn, not -800.
            "3rk3/8/8/3p4/8/8/3Q4/3RK3 w - - 0 1; d2d5; -300",
            // Rook first with the queen behind it: wins the pawn.
            "3rk3/8/8/3p4/8/8/3R4/3QK3 w - - 0 1; d2d5; 100",
    })
    void exchanges(String fen, String uci, int expected) {
        Board board = Board.fromFen(fen);
        int move = new Game(fen).findMove(uci);
        assertEquals(expected, new See().evaluate(board, move));
        assertEquals(fen, board.toFen());
    }
}
