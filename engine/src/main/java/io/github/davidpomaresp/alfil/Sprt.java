package io.github.davidpomaresp.alfil;

/**
 * Sequential probability ratio test for engine matches. Compares two hypotheses,
 * "the change is worth elo0" (H0) and "it is worth elo1" (H1), and returns the log
 * likelihood ratio of the results so far. Playing stops once it crosses
 * ln(beta / (1 - alpha)) or ln((1 - beta) / alpha).
 *
 * Uses the usual normal approximation of the score with the variance measured from
 * the games (wins, draws and losses).
 */
final class Sprt {

    private Sprt() {
    }

    static double expectedScore(double elo) {
        return 1.0 / (1.0 + Math.pow(10, -elo / 400.0));
    }

    static double llr(int wins, int draws, int losses, double elo0, double elo1) {
        int n = wins + draws + losses;
        if (wins == 0 || losses == 0) {
            return 0; // not enough information yet for a variance
        }
        double mean = (wins + 0.5 * draws) / n;
        double variance = (wins * Math.pow(1 - mean, 2) + draws * Math.pow(0.5 - mean, 2)
                + losses * Math.pow(mean, 2)) / n;
        double s0 = expectedScore(elo0);
        double s1 = expectedScore(elo1);
        return n * (s1 - s0) * (2 * mean - s0 - s1) / (2 * variance);
    }
}
