package io.github.davidpomaresp.alfil;

/**
 * Searches a fixed set of positions to a fixed depth and prints nodes and time.
 * Same depth, same positions: if a change lowers the node count, the search got
 * more efficient; if the best moves change, the change also affected the result.
 *
 * Usage: java -cp engine/target/classes io.github.davidpomaresp.alfil.Bench [depth]
 */
public final class Bench {

    static final String[] POSITIONS = {
            Board.START_FEN,
            "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1",
            "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1",
            "r4rk1/1pp1qppp/p1np1n2/2b1p1B1/2B1P1b1/P1NP1N2/1PP1QPPP/R4RK1 w - - 0 10",
            "rnbqkb1r/pp3ppp/4pn2/2pp4/3P4/2P1PN2/PP3PPP/RNBQKB1R w KQkq - 0 5",
            "r1bq1rk1/ppp2ppp/2np1n2/2b1p3/2B1P3/2NP1N2/PPP2PPP/R1BQ1RK1 w - - 0 7",
            "r2q1rk1/pp2bppp/2n1bn2/3p4/3P4/2NBBN2/PP3PPP/R2Q1RK1 w - - 4 11",
            "8/5pk1/6p1/7p/7P/6P1/5PK1/8 w - - 0 40",
    };

    private Bench() {
    }

    public static void main(String[] args) {
        int depth = args.length > 0 ? Integer.parseInt(args[0]) : 5;
        long totalNodes = 0;
        long totalMillis = 0;
        for (String fen : POSITIONS) {
            SearchResult r = new Search(Board.fromFen(fen)).search(depth, 0);
            totalNodes += r.nodes();
            totalMillis += r.millis();
            System.out.printf("%-8s %6d cp %12d nodes %7d ms  %s%n",
                    Move.toUci(r.bestMove()), r.score(), r.nodes(), r.millis(), fen);
        }
        System.out.printf("depth %d total: %d nodes, %d ms, %d nodes/s%n",
                depth, totalNodes, totalMillis, totalMillis == 0 ? 0 : totalNodes * 1000 / totalMillis);
    }
}
