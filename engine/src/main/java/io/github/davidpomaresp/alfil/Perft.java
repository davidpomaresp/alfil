package io.github.davidpomaresp.alfil;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Counts the leaf nodes of the legal move tree to a fixed depth. Comparing the counts
 * with published values is the standard way to check a move generator.
 */
public final class Perft {

    private Perft() {
    }

    public static long count(Board board, int depth) {
        if (depth == 0) {
            return 1;
        }
        MoveList moves = MoveGenerator.legalMoves(board);
        if (depth == 1) {
            return moves.size();
        }
        long nodes = 0;
        for (int i = 0; i < moves.size(); i++) {
            int move = moves.get(i);
            board.makeMove(move);
            nodes += count(board, depth - 1);
            board.unmakeMove(move);
        }
        return nodes;
    }

    /** Node count below each root move; useful to find which move a bug hides behind. */
    public static Map<String, Long> divide(Board board, int depth) {
        Map<String, Long> result = new LinkedHashMap<>();
        MoveList moves = MoveGenerator.legalMoves(board);
        for (int i = 0; i < moves.size(); i++) {
            int move = moves.get(i);
            board.makeMove(move);
            result.put(Move.toUci(move), count(board, depth - 1));
            board.unmakeMove(move);
        }
        return result;
    }

    public static void main(String[] args) {
        String fen = args.length > 1 ? args[1] : Board.START_FEN;
        int depth = args.length > 0 ? Integer.parseInt(args[0]) : 5;
        Board board = Board.fromFen(fen);
        long start = System.nanoTime();
        long nodes = count(board, depth);
        double seconds = (System.nanoTime() - start) / 1e9;
        System.out.printf("depth %d: %d nodes in %.2f s (%.0f nodes/s)%n", depth, nodes, seconds, nodes / seconds);
    }
}
