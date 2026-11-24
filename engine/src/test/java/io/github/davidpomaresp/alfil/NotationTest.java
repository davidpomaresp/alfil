package io.github.davidpomaresp.alfil;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class NotationTest {

    @ParameterizedTest(name = "{1} -> {2}")
    @CsvSource(delimiter = ';', value = {
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1; e2e4; e4",
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1; g1f3; Nf3",
            "rnbqkbnr/ppp1pppp/8/3p4/4P3/8/PPPP1PPP/RNBQKBNR w KQkq d6 0 2; e4d5; exd5",
            "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1; e1g1; O-O",
            "r3k2r/8/8/8/8/8/8/R3K2R b KQkq - 0 1; e8c8; O-O-O",
            "4k3/1P6/8/8/8/8/8/4K3 w - - 0 1; b7b8q; b8=Q+",
            "4k3/1P6/8/8/8/8/8/4K3 w - - 0 1; b7b8n; b8=N",
            "6k1/5ppp/8/8/8/8/8/R5K1 w - - 0 1; a1a8; Ra8#",
            "4k3/8/8/8/8/8/4K3/R6R w - - 0 1; a1d1; Rad1",
            "4k3/8/8/8/8/8/4K3/R6R w - - 0 1; h1d1; Rhd1",
            "4k3/8/8/R7/8/8/8/R3K3 w - - 0 1; a1a3; R1a3",
            "7k/2N5/8/8/8/2N1N3/8/4K3 w - - 0 1; c3d5; Nc3d5",
            "7k/8/8/8/8/2N1N3/8/4K3 w - - 0 1; c3d5; Ncd5",
            "rnbqkbnr/ppp1p1pp/8/3pPp2/8/8/PPPP1PPP/RNBQKBNR w KQkq f6 0 3; e5f6; exf6",
    })
    void san(String fen, String uci, String expected) {
        Board board = Board.fromFen(fen);
        assertEquals(expected, Notation.toSan(board, new Game(fen).findMove(uci)));
        assertEquals(fen, board.toFen());
    }
}
