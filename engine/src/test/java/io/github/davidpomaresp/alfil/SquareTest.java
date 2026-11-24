package io.github.davidpomaresp.alfil;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SquareTest {

    @Test
    void cornersHaveTheExpectedIndexes() {
        assertEquals(0, Square.parse("a1"));
        assertEquals(7, Square.parse("h1"));
        assertEquals(56, Square.parse("a8"));
        assertEquals(63, Square.parse("h8"));
    }

    @Test
    void nameAndParseAreInverse() {
        for (int sq = 0; sq < 64; sq++) {
            assertEquals(sq, Square.parse(Square.name(sq)));
        }
    }

    @Test
    void fileAndRank() {
        int e4 = Square.parse("e4");
        assertEquals(4, Square.file(e4));
        assertEquals(3, Square.rank(e4));
    }
}
