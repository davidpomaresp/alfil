package io.github.davidpomaresp.alfil.web;

import io.github.davidpomaresp.alfil.Game;
import io.github.davidpomaresp.alfil.Move;
import io.github.davidpomaresp.alfil.MoveList;
import io.github.davidpomaresp.alfil.MoveGenerator;
import io.github.davidpomaresp.alfil.SearchResult;
import org.teavm.jso.JSExport;

/**
 * Functions the web page can call once TeaVM has compiled the engine to JavaScript.
 * Strings are used for moves and positions to keep the boundary simple.
 */
public final class EngineExports {

    private static Game game = new Game();

    private EngineExports() {
    }

    public static void main(String[] args) {
    }

    @JSExport
    public static void newGame(String fen) {
        game = fen == null || fen.isEmpty() ? new Game() : new Game(fen);
    }

    @JSExport
    public static String fen() {
        return game.board().toFen();
    }

    /** Legal moves in UCI notation separated by spaces. */
    @JSExport
    public static String legalMoves() {
        MoveList moves = MoveGenerator.legalMoves(game.board());
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < moves.size(); i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(Move.toUci(moves.get(i)));
        }
        return sb.toString();
    }

    @JSExport
    public static boolean play(String uci) {
        return game.play(uci);
    }

    @JSExport
    public static String status() {
        return game.status().name();
    }

    @JSExport
    public static boolean inCheck() {
        return game.board().inCheck();
    }

    /** Searches and returns "bestmove score depth nodes millis". The move is not played. */
    @JSExport
    public static String think(int maxDepth, int millis) {
        SearchResult r = game.think(maxDepth, millis);
        String move = r.bestMove() == Move.NONE ? "none" : Move.toUci(r.bestMove());
        return move + " " + r.score() + " " + r.depth() + " " + r.nodes() + " " + r.millis();
    }
}
