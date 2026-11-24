package io.github.davidpomaresp.alfil;

/**
 * A game in progress: a board plus the rules for when it is over.
 * Moves go in and out as UCI strings ("e2e4", "e7e8q"), which is what the web page and
 * other programs use.
 */
public final class Game {

    public enum Status {
        PLAYING,
        WHITE_WINS_BY_CHECKMATE,
        BLACK_WINS_BY_CHECKMATE,
        DRAW_BY_STALEMATE,
        DRAW_BY_REPETITION,
        DRAW_BY_FIFTY_MOVES,
        DRAW_BY_INSUFFICIENT_MATERIAL;

        public boolean isOver() {
            return this != PLAYING;
        }
    }

    private final Board board;
    private final TranspositionTable table;

    public Game() {
        this(Board.START_FEN);
    }

    public Game(String fen) {
        this(fen, new TranspositionTable(20));
    }

    /** Games can share a table so what was learned in one search helps the next. */
    public Game(String fen, TranspositionTable table) {
        this.board = Board.fromFen(fen);
        this.table = table;
    }

    public Board board() {
        return board;
    }

    /** Finds the legal move matching a UCI string, or {@link Move#NONE}. */
    public int findMove(String uci) {
        MoveList moves = MoveGenerator.legalMoves(board);
        for (int i = 0; i < moves.size(); i++) {
            if (Move.toUci(moves.get(i)).equals(uci)) {
                return moves.get(i);
            }
        }
        return Move.NONE;
    }

    /** Plays a move if it is legal; returns false otherwise. */
    public boolean play(String uci) {
        if (status().isOver()) {
            return false;
        }
        int move = findMove(uci);
        if (move == Move.NONE) {
            return false;
        }
        board.makeMove(move);
        return true;
    }

    public Status status() {
        if (MoveGenerator.legalMoves(board).size() == 0) {
            if (!board.inCheck()) {
                return Status.DRAW_BY_STALEMATE;
            }
            return board.sideToMove() == Piece.WHITE ? Status.BLACK_WINS_BY_CHECKMATE : Status.WHITE_WINS_BY_CHECKMATE;
        }
        if (board.repetitionCount() >= 2) {
            return Status.DRAW_BY_REPETITION;
        }
        if (board.halfmoveClock() >= 100) {
            return Status.DRAW_BY_FIFTY_MOVES;
        }
        if (board.isInsufficientMaterial()) {
            return Status.DRAW_BY_INSUFFICIENT_MATERIAL;
        }
        return Status.PLAYING;
    }

    public SearchResult think(int maxDepth, long millis) {
        return new Search(board, table).search(maxDepth, millis);
    }
}
