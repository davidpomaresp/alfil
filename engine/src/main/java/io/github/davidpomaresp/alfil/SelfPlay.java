package io.github.davidpomaresp.alfil;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Plays the engine against itself and writes quiet positions with the final result,
 * one "fen;result" line each (result from white's side: 1, 0.5 or 0). This is the
 * training data for {@link Tuner}.
 *
 * Usage: java -cp alfil.jar io.github.davidpomaresp.alfil.SelfPlay out.txt games depth threads [seed]
 */
public final class SelfPlay {

    private static final int RANDOM_PLIES = 8;
    private static final int MAX_PLIES = 300;

    private SelfPlay() {
    }

    static List<String> playGame(long seed, int depth) {
        Random random = new Random(seed);
        Game game = new Game();
        TranspositionTable table = new TranspositionTable(16);

        // A few random moves first, so the games are different from each other.
        for (int i = 0; i < RANDOM_PLIES && !game.status().isOver(); i++) {
            MoveList moves = MoveGenerator.legalMoves(game.board());
            game.play(Move.toUci(moves.get(random.nextInt(moves.size()))));
        }

        List<String> positions = new ArrayList<>();
        int plies = 0;
        while (!game.status().isOver() && plies < MAX_PLIES) {
            Board board = game.board();
            Search search = new Search(board, table);
            if (!board.inCheck() && search.isQuiet()) {
                positions.add(board.toFen());
            }
            SearchResult result = search.search(depth, 0);
            // Very lopsided positions teach little; stop the game there and score it.
            if (Math.abs(result.score()) > 1500) {
                boolean whiteWins = (result.score() > 0) == (board.sideToMove() == Piece.WHITE);
                return label(positions, whiteWins ? "1" : "0");
            }
            game.play(Move.toUci(result.bestMove()));
            plies++;
        }
        String outcome = switch (game.status()) {
            case WHITE_WINS_BY_CHECKMATE -> "1";
            case BLACK_WINS_BY_CHECKMATE -> "0";
            default -> "0.5";
        };
        return label(positions, outcome);
    }

    private static List<String> label(List<String> positions, String result) {
        List<String> lines = new ArrayList<>(positions.size());
        for (String fen : positions) {
            lines.add(fen + ";" + result);
        }
        return lines;
    }

    public static void main(String[] args) throws Exception {
        Path out = Path.of(args[0]);
        int games = Integer.parseInt(args[1]);
        int depth = Integer.parseInt(args[2]);
        int threads = Integer.parseInt(args[3]);
        long seed = args.length > 4 ? Long.parseLong(args[4]) : 1;

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Future<List<String>>> futures = new ArrayList<>();
        for (int i = 0; i < games; i++) {
            long gameSeed = seed * 1_000_003 + i;
            futures.add(pool.submit(() -> playGame(gameSeed, depth)));
        }
        int written = 0;
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(out))) {
            for (int i = 0; i < futures.size(); i++) {
                for (String line : futures.get(i).get()) {
                    writer.println(line);
                    written++;
                }
                if ((i + 1) % 100 == 0) {
                    System.out.printf("%d/%d games, %d positions%n", i + 1, games, written);
                }
            }
        } catch (IOException e) {
            throw e;
        } finally {
            pool.shutdown();
        }
        System.out.printf("done: %d positions from %d games%n", written, games);
    }
}
