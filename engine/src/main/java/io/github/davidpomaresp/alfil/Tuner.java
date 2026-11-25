package io.github.davidpomaresp.alfil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Texel tuning: adjusts the evaluation weights so the evaluation predicts game results
 * better. A score s (centipawns, white's view) is turned into an expected result with
 * 1 / (1 + 10^(-K*s/400)), and the tuner minimises the mean squared difference with the
 * real results of positions from self-play games, changing one weight at a time.
 *
 * Usage: java -cp alfil.jar io.github.davidpomaresp.alfil.Tuner data.txt [passes]
 */
public final class Tuner {

    private final Board[] boards;
    private final double[] results;
    private double k = 1.0;

    Tuner(List<String> lines) {
        boards = new Board[lines.size()];
        results = new double[lines.size()];
        for (int i = 0; i < lines.size(); i++) {
            String[] parts = lines.get(i).split(";");
            boards[i] = Board.fromFen(parts[0]);
            results[i] = Double.parseDouble(parts[1]);
        }
    }

    /** Mean squared error of the current weights over all positions. */
    double error() {
        int chunks = Runtime.getRuntime().availableProcessors();
        int size = (boards.length + chunks - 1) / chunks;
        return IntStream.range(0, chunks).parallel().mapToDouble(c -> {
            Evaluator evaluator = new Evaluator();
            double sum = 0;
            int end = Math.min(boards.length, (c + 1) * size);
            for (int i = c * size; i < end; i++) {
                int score = evaluator.score(boards[i]);
                if (boards[i].sideToMove() == Piece.BLACK) {
                    score = -score;
                }
                double expected = 1.0 / (1.0 + Math.pow(10, -k * score / 400.0));
                double diff = results[i] - expected;
                sum += diff * diff;
            }
            return sum;
        }).sum() / boards.length;
    }

    /** Picks K so that the starting weights fit the data as well as possible. */
    void fitK() {
        double best = Double.MAX_VALUE;
        double bestK = 1.0;
        for (double candidate = 0.2; candidate <= 2.5; candidate += 0.05) {
            k = candidate;
            double e = error();
            if (e < best) {
                best = e;
                bestK = candidate;
            }
        }
        k = bestK;
    }

    /** Coordinate descent: try each weight up and down by {@code step}, keep what helps. */
    int[] tune(int passes, int step) {
        int[] weights = Evaluator.weights();
        double best = error();
        for (int pass = 1; pass <= passes; pass++) {
            int changed = 0;
            for (int i = 0; i < weights.length; i++) {
                for (int delta : new int[] {step, -step}) {
                    weights[i] += delta;
                    Evaluator.setWeights(weights);
                    double e = error();
                    if (e < best) {
                        best = e;
                        changed++;
                        break;
                    }
                    weights[i] -= delta;
                    Evaluator.setWeights(weights);
                }
            }
            System.out.printf("pass %d (step %d): error %.6f, %d weights changed%n", pass, step, best, changed);
            if (changed == 0) {
                break;
            }
        }
        return weights;
    }

    public static void main(String[] args) throws Exception {
        List<String> lines = Files.readAllLines(Path.of(args[0]));
        int passes = args.length > 1 ? Integer.parseInt(args[1]) : 20;
        Tuner tuner = new Tuner(lines);
        System.out.printf("%d positions%n", lines.size());

        tuner.fitK();
        double start = tuner.error();
        System.out.printf("K = %.2f, starting error %.6f%n", tuner.k, start);

        int[] before = Evaluator.weights();
        tuner.tune(passes, 4);
        int[] after = tuner.tune(passes, 1);
        System.out.printf("final error %.6f (was %.6f)%n", tuner.error(), start);

        List<String> report = new ArrayList<>();
        for (int i = 0; i < after.length; i++) {
            String mark = after[i] != before[i] ? "   (was " + before[i] + ")" : "";
            report.add(String.format("%-24s %5d%s", EvalParams.name(i), after[i], mark));
        }
        report.forEach(System.out::println);
    }
}
