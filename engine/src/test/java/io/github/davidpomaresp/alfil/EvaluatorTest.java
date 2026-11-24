package io.github.davidpomaresp.alfil;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class EvaluatorTest {

    /** Same position with the board flipped vertically and the colours swapped. */
    static String mirror(String fen) {
        String[] f = fen.split(" ");
        String[] ranks = f[0].split("/");
        StringBuilder placement = new StringBuilder();
        for (int i = 7; i >= 0; i--) {
            placement.append(swapCase(ranks[i]));
            if (i > 0) {
                placement.append('/');
            }
        }
        String side = f[1].equals("w") ? "b" : "w";
        String castling = f[2].equals("-") ? "-" : sortCastling(swapCase(f[2]));
        String ep = f[3].equals("-") ? "-" : "" + f[3].charAt(0) + (char) ('1' + '8' - f[3].charAt(1));
        return placement + " " + side + " " + castling + " " + ep + " 0 1";
    }

    private static String swapCase(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            sb.append(Character.isUpperCase(c) ? Character.toLowerCase(c) : Character.toUpperCase(c));
        }
        return sb.toString();
    }

    private static String sortCastling(String c) {
        StringBuilder sb = new StringBuilder();
        for (char x : "KQkq".toCharArray()) {
            if (c.indexOf(x) >= 0) {
                sb.append(x);
            }
        }
        return sb.toString();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            Board.START_FEN,
            "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1",
            "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1",
            "r4rk1/1pp1qppp/p1np1n2/2b1p1B1/2B1P1b1/P1NP1N2/1PP1QPPP/R4RK1 w - - 0 10",
            "8/5pk1/6p1/7p/7P/6P1/5PK1/8 w - - 0 40",
            "2r3k1/pp3ppp/8/3P4/8/8/PP3PPP/2R3K1 w - - 0 1",
    })
    void evaluationIsSymmetric(String fen) {
        assertEquals(Evaluator.evaluate(Board.fromFen(fen)), Evaluator.evaluate(Board.fromFen(mirror(fen))), fen);
    }

    @Test
    void startPositionIsEqual() {
        assertEquals(0, Evaluator.evaluate(Board.startPosition()));
    }

    @Test
    void extraQueenIsWinning() {
        assertTrue(Evaluator.evaluate(Board.fromFen("4k3/8/8/8/8/8/8/3QK3 w - - 0 1")) > 800);
        assertTrue(Evaluator.evaluate(Board.fromFen("4k3/8/8/8/8/8/8/3QK3 b - - 0 1")) < -800);
    }

    @Test
    void advancedPassedPawnIsWorthMoreInTheEndgame() {
        int passed = Evaluator.evaluate(Board.fromFen("4k3/1P6/8/8/8/8/8/4K3 w - - 0 1"));
        int blocked = Evaluator.evaluate(Board.fromFen("1p2k3/1P6/8/8/8/8/8/4K3 w - - 0 1"));
        assertTrue(passed > 200, "passed pawn on b7 scored " + passed);
        assertTrue(passed - blocked > 150);
    }

    @Test
    void doubledAndIsolatedPawnsAreWorse() {
        int healthy = Evaluator.evaluate(Board.fromFen("4k3/pp6/8/8/8/8/PP6/4K3 w - - 0 1"));
        int doubled = Evaluator.evaluate(Board.fromFen("4k3/pp6/8/8/8/P7/P7/4K3 w - - 0 1"));
        assertTrue(healthy > doubled);
    }

    @Test
    void rookLikesOpenFiles() {
        int open = Evaluator.evaluate(Board.fromFen("4k3/pp4pp/8/8/8/8/PP4PP/3RK3 w - - 0 1"));
        int closed = Evaluator.evaluate(Board.fromFen("4k3/pp4pp/8/8/8/8/PP4PP/R3K3 w - - 0 1"));
        assertTrue(open > closed);
    }

    @Test
    void mirrorHelperWorks() {
        assertEquals("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR b KQkq - 0 1", mirror(Board.START_FEN));
    }
}
