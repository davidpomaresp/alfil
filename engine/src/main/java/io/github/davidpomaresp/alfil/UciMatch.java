package io.github.davidpomaresp.alfil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Plays a match between any two UCI engines with a real chess clock: same openings,
 * colours swapped, base time plus increment. Our own Game class is the referee.
 *
 * Usage:
 *   java -cp alfil.jar io.github.davidpomaresp.alfil.UciMatch \
 *       "java -jar alfil.jar" "/usr/games/stockfish" 10000 100 3 \
 *       [-a Name=value ...] [-b Name=value ...]
 *
 * -a / -b set UCI options for the first or the second engine, e.g.
 * -b UCI_LimitStrength=true -b UCI_Elo=1800
 */
public final class UciMatch {

    private static final int MAX_PLIES = 400;

    private UciMatch() {
    }

    static final class Engine implements AutoCloseable {
        private final Process process;
        private final PrintWriter in;
        private final BufferedReader out;

        Engine(String command, List<String> options) throws IOException {
            process = new ProcessBuilder(command.split(" ")).redirectErrorStream(true).start();
            in = new PrintWriter(process.getOutputStream(), true);
            out = new BufferedReader(new InputStreamReader(process.getInputStream()));
            send("uci");
            waitFor("uciok");
            for (String option : options) {
                String[] kv = option.split("=", 2);
                send("setoption name " + kv[0] + " value " + kv[1]);
            }
            send("ucinewgame");
            send("isready");
            waitFor("readyok");
        }

        void send(String command) {
            in.println(command);
        }

        String waitFor(String prefix) throws IOException {
            String line;
            while ((line = out.readLine()) != null) {
                if (line.startsWith(prefix)) {
                    return line;
                }
            }
            throw new IOException("engine closed while waiting for " + prefix);
        }

        @Override
        public void close() {
            send("quit");
            process.destroy();
        }
    }

    /** Result for engine A: 1 win, 0.5 draw, 0 loss. */
    static double playGame(String opening, String cmdA, List<String> optsA, String cmdB, List<String> optsB,
                           boolean aIsWhite, long base, long inc) throws IOException {
        try (Engine a = new Engine(cmdA, optsA); Engine b = new Engine(cmdB, optsB)) {
            Game referee = new Game(opening);
            StringBuilder moves = new StringBuilder();
            long[] clock = {base, base};
            int plies = 0;
            while (!referee.status().isOver() && plies < MAX_PLIES) {
                int side = referee.board().sideToMove();
                boolean aToMove = (side == Piece.WHITE) == aIsWhite;
                Engine engine = aToMove ? a : b;
                engine.send("position fen " + opening + (moves.length() > 0 ? " moves" + moves : ""));
                engine.send("go wtime " + clock[0] + " btime " + clock[1] + " winc " + inc + " binc " + inc);
                long start = System.nanoTime();
                String move = engine.waitFor("bestmove").split(" ")[1];
                clock[side] -= (System.nanoTime() - start) / 1_000_000;
                if (clock[side] < 0) {
                    return aToMove ? 0.0 : 1.0; // lost on time
                }
                clock[side] += inc;
                if (!referee.play(move)) {
                    throw new IllegalStateException("illegal move " + move + " in " + referee.board().toFen());
                }
                moves.append(' ').append(move);
                plies++;
            }
            double white = switch (referee.status()) {
                case WHITE_WINS_BY_CHECKMATE -> 1.0;
                case BLACK_WINS_BY_CHECKMATE -> 0.0;
                default -> 0.5;
            };
            return aIsWhite ? white : 1.0 - white;
        }
    }

    public static void main(String[] args) throws Exception {
        String cmdA = args[0];
        String cmdB = args[1];
        long base = Long.parseLong(args[2]);
        long inc = Long.parseLong(args[3]);
        int threads = Integer.parseInt(args[4]);
        List<String> optsA = new ArrayList<>();
        List<String> optsB = new ArrayList<>();
        for (int i = 5; i + 1 < args.length; i += 2) {
            (args[i].equals("-a") ? optsA : optsB).add(args[i + 1]);
        }

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Future<Double>> results = new ArrayList<>();
        for (String opening : Match.OPENINGS) {
            results.add(pool.submit(() -> playGame(opening, cmdA, optsA, cmdB, optsB, true, base, inc)));
            results.add(pool.submit(() -> playGame(opening, cmdA, optsA, cmdB, optsB, false, base, inc)));
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
        System.out.printf("A: %s %s%nB: %s %s%n", cmdA, optsA, cmdB, optsB);
        System.out.printf("%d games, %d+%d ms%n", games, base, inc);
        System.out.printf("A: +%d =%d -%d  score %.1f%%%n", wins, draws, losses, score * 100);
        System.out.printf("Elo difference A-B: %+.0f +/- %.0f (95%%)%n",
                Match.elo(score), Match.eloMargin(wins, draws, losses));
    }
}
