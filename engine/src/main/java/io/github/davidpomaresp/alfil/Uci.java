package io.github.davidpomaresp.alfil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;

/**
 * Universal Chess Interface: the text protocol chess GUIs and servers use to talk to
 * engines. The GUI writes commands to our standard input ("position ...", "go ...")
 * and we answer on standard output ("info ...", "bestmove e2e4").
 */
public final class Uci {

    static final String NAME = "Alfil";
    static final String AUTHOR = "David Pomares";
    private static final int DEFAULT_HASH_MB = 16;
    private static final long MOVE_OVERHEAD_MS = 50;

    private final BufferedReader in;
    private final PrintStream out;
    private TranspositionTable table = new TranspositionTable(sizeBits(DEFAULT_HASH_MB));
    private Game game = new Game(Board.START_FEN, table);
    private Search search;
    private Thread searchThread;

    public Uci(BufferedReader in, PrintStream out) {
        this.in = in;
        this.out = out;
    }

    public static void main(String[] args) throws IOException {
        new Uci(new BufferedReader(new InputStreamReader(System.in)), System.out).run();
    }

    public void run() throws IOException {
        String line;
        while ((line = in.readLine()) != null) {
            if (!handle(line.trim())) {
                break;
            }
        }
        stopSearch();
    }

    /** Handles one command; returns false on "quit". */
    boolean handle(String line) {
        String[] tokens = line.split("\\s+");
        switch (tokens[0]) {
            case "uci" -> {
                send("id name " + NAME);
                send("id author " + AUTHOR);
                send("option name Hash type spin default " + DEFAULT_HASH_MB + " min 1 max 1024");
                send("uciok");
            }
            case "isready" -> send("readyok");
            case "ucinewgame" -> {
                stopSearch();
                table.clear();
                game = new Game(Board.START_FEN, table);
            }
            case "setoption" -> setOption(tokens);
            case "position" -> {
                stopSearch();
                position(tokens);
            }
            case "go" -> {
                stopSearch();
                go(tokens);
            }
            case "stop" -> stopSearch();
            case "d" -> send(game.board().toString() + game.board().toFen());
            case "quit" -> {
                return false;
            }
            default -> {
                // Unknown commands are ignored, as the protocol asks.
            }
        }
        return true;
    }

    private void setOption(String[] tokens) {
        // setoption name Hash value 64
        if (tokens.length >= 5 && tokens[2].equalsIgnoreCase("Hash") && tokens[3].equals("value")) {
            int mb = Math.max(1, Math.min(1024, Integer.parseInt(tokens[4])));
            table = new TranspositionTable(sizeBits(mb));
            game = new Game(game.board().toFen(), table);
        }
    }

    /** Largest power of two of 16-byte entries that fits in the given megabytes. */
    static int sizeBits(int mb) {
        long entries = (long) mb * 1024 * 1024 / 16;
        return 63 - Long.numberOfLeadingZeros(entries);
    }

    private void position(String[] tokens) {
        int i = 1;
        String fen;
        if (tokens.length > 1 && tokens[1].equals("startpos")) {
            fen = Board.START_FEN;
            i = 2;
        } else if (tokens.length > 1 && tokens[1].equals("fen")) {
            StringBuilder sb = new StringBuilder();
            i = 2;
            while (i < tokens.length && !tokens[i].equals("moves")) {
                sb.append(tokens[i++]).append(' ');
            }
            fen = sb.toString().trim();
        } else {
            return;
        }
        Game next = new Game(fen, table);
        if (i < tokens.length && tokens[i].equals("moves")) {
            for (i++; i < tokens.length; i++) {
                if (!next.play(tokens[i])) {
                    send("info string illegal move " + tokens[i]);
                    break;
                }
            }
        }
        game = next;
    }

    private void go(String[] tokens) {
        long wtime = -1;
        long btime = -1;
        long winc = 0;
        long binc = 0;
        int movesToGo = 0;
        long moveTime = -1;
        int depth = Search.MAX_PLY - 1;
        boolean infinite = false;
        for (int i = 1; i < tokens.length; i++) {
            String value = i + 1 < tokens.length ? tokens[i + 1] : "0";
            switch (tokens[i]) {
                case "wtime" -> wtime = Long.parseLong(value);
                case "btime" -> btime = Long.parseLong(value);
                case "winc" -> winc = Long.parseLong(value);
                case "binc" -> binc = Long.parseLong(value);
                case "movestogo" -> movesToGo = Integer.parseInt(value);
                case "movetime" -> moveTime = Long.parseLong(value);
                case "depth" -> depth = Integer.parseInt(value);
                case "infinite" -> infinite = true;
                default -> {
                }
            }
        }

        boolean white = game.board().sideToMove() == Piece.WHITE;
        long time = white ? wtime : btime;
        long increment = white ? winc : binc;
        long millis;
        if (infinite) {
            millis = 0;
        } else if (moveTime > 0) {
            millis = Math.max(1, moveTime - MOVE_OVERHEAD_MS);
        } else if (time >= 0) {
            millis = allocateTime(time, increment, movesToGo);
        } else {
            millis = 0;
        }

        // Search on the game's own board, so it sees the earlier positions for repetitions.
        Search s = new Search(game.board(), table);
        s.setListener(this::sendInfo);
        search = s;
        int maxDepth = depth;
        long limit = millis;
        searchThread = new Thread(() -> {
            SearchResult result = s.search(maxDepth, limit);
            send("bestmove " + (result.bestMove() == Move.NONE ? "0000" : Move.toUci(result.bestMove())));
        }, "search");
        searchThread.start();
    }

    /**
     * Spends roughly 1/30 of the remaining time (or of the time until the next time
     * control) plus most of the increment, never more than half of what is left.
     */
    static long allocateTime(long time, long increment, int movesToGo) {
        int moves = movesToGo > 0 ? Math.min(movesToGo, 30) : 30;
        long budget = time / moves + increment * 3 / 4;
        budget = Math.min(budget, time / 2);
        return Math.max(1, budget - MOVE_OVERHEAD_MS);
    }

    private void sendInfo(SearchResult r) {
        StringBuilder sb = new StringBuilder("info depth ").append(r.depth());
        if (r.isMate()) {
            sb.append(" score mate ").append(r.mateIn());
        } else {
            sb.append(" score cp ").append(r.score());
        }
        long nps = r.millis() > 0 ? r.nodes() * 1000 / r.millis() : r.nodes();
        sb.append(" nodes ").append(r.nodes()).append(" nps ").append(nps).append(" time ").append(r.millis());
        sb.append(" pv");
        for (int move : r.pv()) {
            sb.append(' ').append(Move.toUci(move));
        }
        send(sb.toString());
    }

    private void stopSearch() {
        if (searchThread == null) {
            return;
        }
        search.stop();
        try {
            searchThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        searchThread = null;
        search = null;
    }

    /** Blocks until the current search, if any, has printed its bestmove. */
    void waitForSearch() throws InterruptedException {
        if (searchThread != null) {
            searchThread.join();
        }
    }

    private synchronized void send(String line) {
        out.println(line);
        out.flush();
    }
}
