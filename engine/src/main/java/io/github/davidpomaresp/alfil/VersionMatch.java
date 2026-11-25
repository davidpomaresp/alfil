package io.github.davidpomaresp.alfil;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Plays two builds of the engine against each other, each loaded from its own jar,
 * with the same openings, alternating colours and the same time per move.
 *
 * Usage: java -cp engine/target/classes io.github.davidpomaresp.alfil.VersionMatch new.jar old.jar [millis] [threads]
 */
public final class VersionMatch {

    private static final int MAX_PLIES = 300;

    private VersionMatch() {
    }

    /** One build of the engine, driven through its public Game API. */
    static final class Engine {
        final String name;
        private final Constructor<?> newGame;
        private final Method play;
        private final Method think;
        private final Method bestMove;
        private final Method toUci;

        Engine(String jar) throws Exception {
            name = new File(jar).getName();
            ClassLoader loader = new URLClassLoader(new URL[] {new File(jar).toURI().toURL()},
                    ClassLoader.getPlatformClassLoader());
            Class<?> game = loader.loadClass("io.github.davidpomaresp.alfil.Game");
            newGame = game.getConstructor(String.class);
            play = game.getMethod("play", String.class);
            think = game.getMethod("think", int.class, long.class);
            bestMove = loader.loadClass("io.github.davidpomaresp.alfil.SearchResult").getMethod("bestMove");
            toUci = loader.loadClass("io.github.davidpomaresp.alfil.Move").getMethod("toUci", int.class);
        }

        Object start(String fen) throws Exception {
            return newGame.newInstance(fen);
        }

        String think(Object game, long millis) throws Exception {
            Object result = think.invoke(game, 64, millis);
            return (String) toUci.invoke(null, (int) bestMove.invoke(result));
        }

        void play(Object game, String uci) throws Exception {
            play.invoke(game, uci);
        }
    }

    /** Result for engine {@code a}: 1 win, 0.5 draw, 0 loss. */
    static double playGame(String opening, Engine a, Engine b, boolean aIsWhite, long millis) throws Exception {
        Game referee = new Game(opening);
        Object gameA = a.start(opening);
        Object gameB = b.start(opening);
        int plies = 0;
        while (!referee.status().isOver() && plies < MAX_PLIES) {
            boolean whiteToMove = referee.board().sideToMove() == Piece.WHITE;
            boolean aToMove = whiteToMove == aIsWhite;
            String move = aToMove ? a.think(gameA, millis) : b.think(gameB, millis);
            if (!referee.play(move)) {
                throw new IllegalStateException((aToMove ? a.name : b.name) + " played illegal " + move);
            }
            a.play(gameA, move);
            b.play(gameB, move);
            plies++;
        }
        double white = switch (referee.status()) {
            case WHITE_WINS_BY_CHECKMATE -> 1.0;
            case BLACK_WINS_BY_CHECKMATE -> 0.0;
            default -> 0.5;
        };
        return aIsWhite ? white : 1.0 - white;
    }

    public static void main(String[] args) throws Exception {
        Engine a = new Engine(args[0]);
        Engine b = new Engine(args[1]);
        long millis = args.length > 2 ? Long.parseLong(args[2]) : 100;
        int threads = args.length > 3 ? Integer.parseInt(args[3]) : 1;

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Future<Double>> results = new ArrayList<>();
        for (String opening : Match.ALL_OPENINGS) {
            results.add(pool.submit(() -> playGame(opening, a, b, true, millis)));
            results.add(pool.submit(() -> playGame(opening, a, b, false, millis)));
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
        System.out.printf("%s vs %s, %d ms per move, %d games%n", a.name, b.name, millis, games);
        System.out.printf("+%d =%d -%d  score %.1f%%%n", wins, draws, losses, score * 100);
        System.out.printf("Elo %+.0f +/- %.0f (95%%)%n", Match.elo(score), Match.eloMargin(wins, draws, losses));
    }
}
