package io.github.davidpomaresp.alfil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Plays two engine settings against each other: every opening twice, swapping colours,
 * with the same time per move. Prints the score and the Elo difference with a 95% margin.
 *
 * Usage: java -cp engine/target/classes io.github.davidpomaresp.alfil.Match [millisPerMove] [threads]
 */
public final class Match {

    // Short, balanced openings (after 2-4 moves) so the games do not all repeat.
    static final String[] OPENINGS = {
            "rnbqkbnr/pppp1ppp/8/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R b KQkq - 1 2",
            "rnbqkbnr/pp1ppppp/8/2p5/4P3/8/PPPP1PPP/RNBQKBNR w KQkq c6 0 2",
            "rnbqkbnr/pppp1ppp/4p3/8/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2",
            "rnbqkbnr/pp1ppppp/2p5/8/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2",
            "rnbqkbnr/ppp1pppp/8/3p4/3P4/8/PPP1PPPP/RNBQKBNR w KQkq d6 0 2",
            "rnbqkb1r/pppppppp/5n2/8/3P4/8/PPP1PPPP/RNBQKBNR w KQkq - 1 2",
            "rnbqkbnr/pppppppp/8/8/2P5/8/PP1PPPPP/RNBQKBNR b KQkq c3 0 1",
            "rnbqkbnr/pppppppp/8/8/8/5N2/PPPPPPPP/RNBQKB1R b KQkq - 1 1",
            "r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3",
            "rnbqkb1r/pppp1ppp/5n2/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3",
            "rnbqkbnr/pp2pppp/3p4/2p5/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 0 3",
            "rnbqkbnr/pp1ppppp/8/2p5/4P3/2N5/PPPP1PPP/R1BQKBNR b KQkq - 1 2",
            "rnbqkbnr/ppp2ppp/4p3/3p4/3PP3/8/PPP2PPP/RNBQKBNR w KQkq d6 0 3",
            "rnbqkbnr/pp2pppp/2p5/3p4/3PP3/8/PPP2PPP/RNBQKBNR w KQkq d6 0 3",
            "rnbqkbnr/ppp1pppp/8/3p4/2PP4/8/PP2PPPP/RNBQKBNR b KQkq c3 0 2",
            "rnbqkb1r/pppppp1p/5np1/8/2PP4/8/PP2PPPP/RNBQKBNR w KQkq - 0 3",
            "rnbqkb1r/pppp1ppp/4pn2/8/2PP4/8/PP2PPPP/RNBQKBNR w KQkq - 0 3",
            "rnbqkbnr/ppp1pppp/8/3p4/3P4/5N2/PPP1PPPP/RNBQKB1R b KQkq - 1 2",
            "rnbqkbnr/pppp1ppp/8/4p3/2P5/8/PP1PPPPP/RNBQKBNR w KQkq e6 0 2",
            "rnbqkbnr/ppppp1pp/8/5p2/3P4/8/PPP1PPPP/RNBQKBNR w KQkq f6 0 2",
            "rnbqkbnr/pppp1ppp/8/4p3/4P3/2N5/PPPP1PPP/R1BQKBNR b KQkq - 1 2",
            "rnbqkbnr/pppp1ppp/8/4p3/3PP3/8/PPP2PPP/RNBQKBNR b KQkq d3 0 2",
            "rnbqkbnr/ppp1pppp/8/3p4/4P3/8/PPPP1PPP/RNBQKBNR w KQkq d6 0 2",
            "rnbqkbnr/pppppp1p/6p1/8/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2",
            "rnbqkb1r/pppppppp/5n2/8/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 1 2",
    };

    // 25 more openings, written as moves from the start position.
    private static final String[] MORE_OPENINGS = {
            "e2e4 e7e5 g1f3 b8c6 f1b5",
            "e2e4 e7e5 g1f3 b8c6 f1c4 f8c5",
            "e2e4 c7c5 g1f3 d7d6 d2d4",
            "e2e4 c7c5 b1c3 b8c6 g2g3",
            "e2e4 e7e6 d2d4 d7d5 b1c3",
            "e2e4 c7c6 d2d4 d7d5 e4e5",
            "e2e4 d7d5 e4d5 d8d5 b1c3",
            "e2e4 d7d6 d2d4 g8f6 b1c3",
            "e2e4 g8f6 e4e5 f6d5 d2d4",
            "d2d4 d7d5 c2c4 e7e6 b1c3",
            "d2d4 d7d5 c2c4 c7c6 g1f3",
            "d2d4 d7d5 c2c4 d5c4 g1f3",
            "d2d4 g8f6 c2c4 g7g6 b1c3 f8g7",
            "d2d4 g8f6 c2c4 e7e6 b1c3 f8b4",
            "d2d4 g8f6 c2c4 e7e6 g1f3 b7b6",
            "d2d4 g8f6 c2c4 c7c5 d4d5",
            "d2d4 f7f5 g2g3 g8f6 f1g2",
            "d2d4 g8f6 g1f3 e7e6 c1g5",
            "c2c4 e7e5 b1c3 g8f6 g2g3",
            "c2c4 c7c5 g1f3 g8f6 b1c3",
            "g1f3 d7d5 g2g3 g8f6 f1g2",
            "g1f3 g8f6 c2c4 g7g6 g2g3",
            "b2b3 e7e5 c1b2 b8c6 e2e3",
            "f2f4 d7d5 g1f3 g8f6 e2e3",
            "e2e4 e7e5 f2f4 e5f4 g1f3",
    };

    /** All openings as FEN strings. */
    static final String[] ALL_OPENINGS = allOpenings();

    private static String[] allOpenings() {
        String[] all = java.util.Arrays.copyOf(OPENINGS, OPENINGS.length + MORE_OPENINGS.length);
        for (int i = 0; i < MORE_OPENINGS.length; i++) {
            Game game = new Game();
            for (String move : MORE_OPENINGS[i].split(" ")) {
                if (!game.play(move)) {
                    throw new IllegalStateException("bad opening move " + move + " in " + MORE_OPENINGS[i]);
                }
            }
            all[OPENINGS.length + i] = game.board().toFen();
        }
        return all;
    }

    private static final int MAX_PLIES = 300;

    private Match() {
    }

    /** One engine setting. Add fields here when a new experiment needs them. */
    public record Player(String name, boolean moveOrdering) {
    }

    /** Result from the point of view of {@code first}: 1 win, 0.5 draw, 0 loss. */
    static double playGame(String opening, Player white, Player black, Player first, long millis) {
        Game game = new Game(opening);
        TranspositionTable whiteTable = new TranspositionTable(20);
        TranspositionTable blackTable = new TranspositionTable(20);
        int plies = 0;
        while (!game.status().isOver() && plies < MAX_PLIES) {
            Player toMove = game.board().sideToMove() == Piece.WHITE ? white : black;
            Search search = new Search(game.board(), toMove == white ? whiteTable : blackTable);
            search.setMoveOrdering(toMove.moveOrdering());
            int move = search.search(Search.MAX_PLY - 1, millis).bestMove();
            game.play(Move.toUci(move));
            plies++;
        }
        double whiteScore = switch (game.status()) {
            case WHITE_WINS_BY_CHECKMATE -> 1.0;
            case BLACK_WINS_BY_CHECKMATE -> 0.0;
            default -> 0.5;
        };
        return first == white ? whiteScore : 1.0 - whiteScore;
    }

    public static void main(String[] args) throws Exception {
        long millis = args.length > 0 ? Long.parseLong(args[0]) : 100;
        int threads = args.length > 1 ? Integer.parseInt(args[1]) : 1;
        Player a = new Player("ordering", true);
        Player b = new Player("no-ordering", false);

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Future<Double>> results = new ArrayList<>();
        for (String opening : OPENINGS) {
            results.add(pool.submit(() -> playGame(opening, a, b, a, millis)));
            results.add(pool.submit(() -> playGame(opening, b, a, a, millis)));
        }
        int wins = 0;
        int draws = 0;
        int losses = 0;
        for (Future<Double> f : results) {
            double r = f.get();
            if (r == 1.0) {
                wins++;
            } else if (r == 0.5) {
                draws++;
            } else {
                losses++;
            }
        }
        pool.shutdown();

        int games = wins + draws + losses;
        double score = (wins + 0.5 * draws) / games;
        System.out.printf("%s vs %s, %d ms per move, %d games%n", a.name(), b.name(), millis, games);
        System.out.printf("+%d =%d -%d  score %.1f%%%n", wins, draws, losses, score * 100);
        System.out.printf("Elo %+.0f +/- %.0f (95%%)%n", elo(score), eloMargin(wins, draws, losses));
    }

    static double elo(double score) {
        if (score <= 0) {
            return Double.NEGATIVE_INFINITY;
        }
        if (score >= 1) {
            return Double.POSITIVE_INFINITY;
        }
        return -400 * Math.log10(1 / score - 1);
    }

    /** 95% margin from the variance of the per-game results. */
    static double eloMargin(int wins, int draws, int losses) {
        int n = wins + draws + losses;
        double mean = (wins + 0.5 * draws) / n;
        double variance = (wins * Math.pow(1 - mean, 2) + draws * Math.pow(0.5 - mean, 2)
                + losses * Math.pow(mean, 2)) / n;
        double stdError = Math.sqrt(variance / n);
        double high = Math.min(mean + 1.96 * stdError, 0.999);
        double low = Math.max(mean - 1.96 * stdError, 0.001);
        return (elo(high) - elo(low)) / 2;
    }
}
