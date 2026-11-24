package io.github.davidpomaresp.alfil;

/**
 * Generates moves in two steps: first every pseudo-legal move (each piece moving by
 * its own rules), then only those that do not leave our king in check.
 */
public final class MoveGenerator {

    private static final int E1 = 4;
    private static final int E8 = 60;

    private MoveGenerator() {
    }

    public static MoveList legalMoves(Board board) {
        MoveList moves = new MoveList();
        generateLegal(board, moves);
        return moves;
    }

    public static void generateLegal(Board board, MoveList out) {
        MoveList pseudo = new MoveList();
        generatePseudoLegal(board, pseudo);
        out.clear();
        int us = board.sideToMove();
        for (int i = 0; i < pseudo.size(); i++) {
            int move = pseudo.get(i);
            board.makeMove(move);
            if (!board.isAttacked(board.kingSquare(us), us ^ 1)) {
                out.add(move);
            }
            board.unmakeMove(move);
        }
    }

    public static void generatePseudoLegal(Board board, MoveList out) {
        int us = board.sideToMove();
        for (int sq = 0; sq < 64; sq++) {
            int piece = board.squares[sq];
            if (piece == Piece.EMPTY || Piece.color(piece) != us) {
                continue;
            }
            switch (Piece.type(piece)) {
                case Piece.PAWN -> pawnMoves(board, sq, us, out);
                case Piece.KNIGHT -> stepMoves(board, sq, us, Attacks.KNIGHT[sq], out);
                case Piece.BISHOP -> slidingMoves(board, sq, us, 4, 8, out);
                case Piece.ROOK -> slidingMoves(board, sq, us, 0, 4, out);
                case Piece.QUEEN -> slidingMoves(board, sq, us, 0, 8, out);
                case Piece.KING -> {
                    stepMoves(board, sq, us, Attacks.KING[sq], out);
                    castlingMoves(board, us, out);
                }
                default -> throw new IllegalStateException("Bad piece " + piece);
            }
        }
    }

    private static void pawnMoves(Board board, int from, int us, MoveList out) {
        int forward = us == Piece.WHITE ? 8 : -8;
        int startRank = us == Piece.WHITE ? 1 : 6;
        int lastRank = us == Piece.WHITE ? 7 : 0;
        int file = Square.file(from);

        int one = from + forward;
        if (board.squares[one] == Piece.EMPTY) {
            addPawnMove(from, one, false, lastRank, out);
            int two = one + forward;
            if (Square.rank(from) == startRank && board.squares[two] == Piece.EMPTY) {
                out.add(Move.of(from, two, Move.DOUBLE_PAWN_PUSH));
            }
        }

        for (int side = -1; side <= 1; side += 2) {
            if (file + side < 0 || file + side > 7) {
                continue;
            }
            int to = one + side;
            int target = board.squares[to];
            if (target != Piece.EMPTY && Piece.color(target) != us) {
                addPawnMove(from, to, true, lastRank, out);
            } else if (to == board.enPassantSquare()) {
                out.add(Move.of(from, to, Move.EN_PASSANT));
            }
        }
    }

    private static void addPawnMove(int from, int to, boolean capture, int lastRank, MoveList out) {
        if (Square.rank(to) == lastRank) {
            for (int type = Piece.QUEEN; type >= Piece.KNIGHT; type--) {
                out.add(Move.promotion(from, to, type, capture));
            }
        } else {
            out.add(Move.of(from, to, capture ? Move.CAPTURE : Move.QUIET));
        }
    }

    private static void stepMoves(Board board, int from, int us, int[] targets, MoveList out) {
        for (int to : targets) {
            int target = board.squares[to];
            if (target == Piece.EMPTY) {
                out.add(Move.of(from, to, Move.QUIET));
            } else if (Piece.color(target) != us) {
                out.add(Move.of(from, to, Move.CAPTURE));
            }
        }
    }

    private static void slidingMoves(Board board, int from, int us, int firstDir, int lastDir, MoveList out) {
        for (int d = firstDir; d < lastDir; d++) {
            for (int to : Attacks.RAYS[from][d]) {
                int target = board.squares[to];
                if (target == Piece.EMPTY) {
                    out.add(Move.of(from, to, Move.QUIET));
                    continue;
                }
                if (Piece.color(target) != us) {
                    out.add(Move.of(from, to, Move.CAPTURE));
                }
                break;
            }
        }
    }

    /**
     * The king may not castle out of, through or into check. Landing in check is caught
     * later by the legality filter; the other two are checked here.
     */
    private static void castlingMoves(Board board, int us, MoveList out) {
        int king = us == Piece.WHITE ? E1 : E8;
        int kingside = us == Piece.WHITE ? Board.WHITE_KINGSIDE : Board.BLACK_KINGSIDE;
        int queenside = us == Piece.WHITE ? Board.WHITE_QUEENSIDE : Board.BLACK_QUEENSIDE;
        int them = us ^ 1;
        int[] sq = board.squares;

        if (board.kingSquare(us) != king || (!board.canCastle(kingside) && !board.canCastle(queenside))) {
            return;
        }
        if (board.isAttacked(king, them)) {
            return;
        }
        if (board.canCastle(kingside)
                && sq[king + 1] == Piece.EMPTY && sq[king + 2] == Piece.EMPTY
                && sq[king + 3] == Piece.of(us, Piece.ROOK)
                && !board.isAttacked(king + 1, them)) {
            out.add(Move.of(king, king + 2, Move.KING_CASTLE));
        }
        if (board.canCastle(queenside)
                && sq[king - 1] == Piece.EMPTY && sq[king - 2] == Piece.EMPTY && sq[king - 3] == Piece.EMPTY
                && sq[king - 4] == Piece.of(us, Piece.ROOK)
                && !board.isAttacked(king - 1, them)) {
            out.add(Move.of(king, king - 2, Move.QUEEN_CASTLE));
        }
    }
}
