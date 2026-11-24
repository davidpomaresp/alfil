package io.github.davidpomaresp.alfil;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Reference counts from the Chess Programming Wiki "Perft Results" page.
 */
class PerftTest {

    private static final String KIWIPETE = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1";
    private static final String POSITION_3 = "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1";
    private static final String POSITION_4 = "r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1";
    private static final String POSITION_4_MIRRORED = "r2q1rk1/pP1p2pp/Q4n2/bbp1p3/Np6/1B3NBn/pPPP1PPP/R3K2R b KQ - 0 1";
    private static final String POSITION_5 = "rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8";
    private static final String POSITION_6 = "r4rk1/1pp1qppp/p1np1n2/2b1p1B1/2B1P1b1/P1NP1N2/1PP1QPPP/R4RK1 w - - 0 10";

    @ParameterizedTest(name = "start position depth {0}")
    @CsvSource({"1, 20", "2, 400", "3, 8902", "4, 197281", "5, 4865609"})
    void startPosition(int depth, long expected) {
        assertEquals(expected, Perft.count(Board.startPosition(), depth));
    }

    @ParameterizedTest(name = "kiwipete depth {0}")
    @CsvSource({"1, 48", "2, 2039", "3, 97862", "4, 4085603"})
    void kiwipete(int depth, long expected) {
        assertEquals(expected, Perft.count(Board.fromFen(KIWIPETE), depth));
    }

    @ParameterizedTest(name = "position 3 depth {0}")
    @CsvSource({"1, 14", "2, 191", "3, 2812", "4, 43238", "5, 674624"})
    void position3(int depth, long expected) {
        assertEquals(expected, Perft.count(Board.fromFen(POSITION_3), depth));
    }

    @ParameterizedTest(name = "position 4 depth {0}")
    @CsvSource({"1, 6", "2, 264", "3, 9467", "4, 422333"})
    void position4(int depth, long expected) {
        assertEquals(expected, Perft.count(Board.fromFen(POSITION_4), depth));
        assertEquals(expected, Perft.count(Board.fromFen(POSITION_4_MIRRORED), depth));
    }

    @ParameterizedTest(name = "position 5 depth {0}")
    @CsvSource({"1, 44", "2, 1486", "3, 62379", "4, 2103487"})
    void position5(int depth, long expected) {
        assertEquals(expected, Perft.count(Board.fromFen(POSITION_5), depth));
    }

    @ParameterizedTest(name = "position 6 depth {0}")
    @CsvSource({"1, 46", "2, 2079", "3, 89890", "4, 3894594"})
    void position6(int depth, long expected) {
        assertEquals(expected, Perft.count(Board.fromFen(POSITION_6), depth));
    }
}
