package io.github.davidpomaresp.alfil;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class UciTest {

    private static List<String> run(String... commands) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(bytes, true);
        Uci uci = new Uci(new BufferedReader(new StringReader("")), out);
        for (String command : commands) {
            uci.handle(command);
            uci.waitForSearch();
        }
        return Arrays.asList(bytes.toString().split("\\R"));
    }

    private static String bestMove(List<String> lines) {
        return lines.stream().filter(l -> l.startsWith("bestmove")).reduce((a, b) -> b).orElseThrow();
    }

    @Test
    void handshake() throws Exception {
        List<String> lines = run("uci", "isready");
        assertEquals("id name Alfil", lines.get(0));
        assertTrue(lines.contains("uciok"));
        assertEquals("readyok", lines.get(lines.size() - 1));
    }

    @Test
    void searchesFromStartPosition() throws Exception {
        List<String> lines = run("position startpos", "go depth 4");
        assertTrue(lines.stream().anyMatch(l -> l.startsWith("info depth 4 score cp")));
        assertTrue(bestMove(lines).matches("bestmove [a-h][1-8][a-h][1-8]"));
    }

    @Test
    void appliesMovesAndFindsMate() throws Exception {
        // After 1.f3 e5 2.g4, black mates with Qh4.
        List<String> lines = run("position startpos moves f2f3 e7e5 g2g4", "go depth 3");
        assertEquals("bestmove d8h4", bestMove(lines));
        assertTrue(lines.stream().anyMatch(l -> l.contains("score mate 1")));
    }

    @Test
    void readsFenPositions() throws Exception {
        List<String> lines = run("position fen 6k1/5ppp/8/8/8/8/8/R5K1 w - - 0 1", "go depth 3");
        assertEquals("bestmove a1a8", bestMove(lines));
    }

    @Test
    void respectsMoveTime() throws Exception {
        long start = System.currentTimeMillis();
        run("position startpos", "go movetime 300");
        assertTrue(System.currentTimeMillis() - start < 800);
    }

    @Test
    void stopEndsAnInfiniteSearch() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        Uci uci = new Uci(new BufferedReader(new StringReader("")), new PrintStream(bytes, true));
        uci.handle("position startpos");
        uci.handle("go infinite");
        Thread.sleep(200);
        uci.handle("stop");
        assertTrue(bytes.toString().contains("bestmove"));
    }

    @Test
    void reportsNoMoveWhenMated() throws Exception {
        List<String> lines = run("position startpos moves f2f3 e7e5 g2g4 d8h4", "go depth 2");
        assertEquals("bestmove 0000", bestMove(lines));
    }

    @Test
    void timeAllocation() {
        assertEquals(1950, Uci.allocateTime(60_000, 0, 0) );
        assertTrue(Uci.allocateTime(1_000, 0, 0) < 500);
        assertTrue(Uci.allocateTime(60_000, 2_000, 0) > 3_000);
        assertEquals(20, Uci.sizeBits(16));
    }
}
