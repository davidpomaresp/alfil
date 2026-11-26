package io.github.davidpomaresp.alfil;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SprtTest {

    @Test
    void evenScoreFavoursTheNullHypothesis() {
        assertTrue(Sprt.llr(300, 400, 300, 0, 10) < 0);
    }

    @Test
    void clearWinAcceptsTheChange() {
        assertTrue(Sprt.llr(400, 400, 200, 0, 10) > Math.log(0.95 / 0.05));
    }

    @Test
    void moreGamesMeanMoreEvidence() {
        double small = Sprt.llr(60, 80, 50, 0, 10);
        double large = Sprt.llr(600, 800, 500, 0, 10);
        assertTrue(large > small);
    }

    @Test
    void expectedScoreOfZeroEloIsHalf() {
        assertEquals(0.5, Sprt.expectedScore(0), 1e-9);
        assertEquals(0.64, Sprt.expectedScore(100), 0.01);
    }
}
