package io.github.davidpomaresp.alfil;

/**
 * Static exchange evaluation: the material balance of the whole sequence of captures
 * on one square, each side always recapturing with its least valuable piece and
 * free to stop when continuing would lose. Used to skip captures that lose material.
 */
final class See {

    private final int[] squares = new int[64];
    private final int[] gain = new int[32];

    /** Material won (positive) or lost (negative) by {@code move}, in centipawns. */
    int evaluate(Board board, int move) {
        System.arraycopy(board.squares, 0, squares, 0, 64);
        int from = Move.from(move);
        int to = Move.to(move);
        int captured = Move.flags(move) == Move.EN_PASSANT ? Piece.PAWN : Piece.type(squares[to]);
        if (Move.flags(move) == Move.EN_PASSANT) {
            squares[to + (board.sideToMove() == Piece.WHITE ? -8 : 8)] = Piece.EMPTY;
        }

        int depth = 0;
        gain[0] = Evaluator.VALUE[captured];
        int onSquare = Piece.type(squares[from]);
        squares[to] = squares[from];
        squares[from] = Piece.EMPTY;
        int side = board.sideToMove() ^ 1;

        while (depth < gain.length - 1) {
            int attacker = leastValuableAttacker(to, side);
            if (attacker < 0) {
                break;
            }
            depth++;
            // If the piece now on the square is taken, this side wins its value.
            gain[depth] = Evaluator.VALUE[onSquare] - gain[depth - 1];
            onSquare = Piece.type(squares[attacker]);
            if (onSquare == Piece.KING && leastValuableAttacker(to, side ^ 1) >= 0) {
                depth--; // the king cannot capture into an attacked square
                break;
            }
            squares[to] = squares[attacker];
            squares[attacker] = Piece.EMPTY;
            side ^= 1;
        }
        // Walk back: each side picks the better of capturing or stopping.
        while (depth > 0) {
            gain[depth - 1] = -Math.max(-gain[depth - 1], gain[depth]);
            depth--;
        }
        return gain[0];
    }

    /** Square of the cheapest piece of {@code color} attacking {@code target}, or -1. */
    private int leastValuableAttacker(int target, int color) {
        int pawn = Piece.of(color, Piece.PAWN);
        int file = Square.file(target);
        int behind = color == Piece.WHITE ? -8 : 8;
        int left = target + behind - 1;
        int right = target + behind + 1;
        if (file > 0 && left >= 0 && left < 64 && squares[left] == pawn) {
            return left;
        }
        if (file < 7 && right >= 0 && right < 64 && squares[right] == pawn) {
            return right;
        }
        int knight = Piece.of(color, Piece.KNIGHT);
        for (int sq : Attacks.KNIGHT[target]) {
            if (squares[sq] == knight) {
                return sq;
            }
        }
        int bishopSquare = -1;
        int rookSquare = -1;
        int queenSquare = -1;
        for (int d = 0; d < 8; d++) {
            for (int sq : Attacks.RAYS[target][d]) {
                int piece = squares[sq];
                if (piece == Piece.EMPTY) {
                    continue;
                }
                if (Piece.color(piece) == color) {
                    int type = Piece.type(piece);
                    if (d >= 4 && type == Piece.BISHOP && bishopSquare < 0) {
                        bishopSquare = sq;
                    } else if (d < 4 && type == Piece.ROOK && rookSquare < 0) {
                        rookSquare = sq;
                    } else if (type == Piece.QUEEN && queenSquare < 0) {
                        queenSquare = sq;
                    }
                }
                break;
            }
        }
        if (bishopSquare >= 0) {
            return bishopSquare;
        }
        if (rookSquare >= 0) {
            return rookSquare;
        }
        if (queenSquare >= 0) {
            return queenSquare;
        }
        int king = Piece.of(color, Piece.KING);
        for (int sq : Attacks.KING[target]) {
            if (squares[sq] == king) {
                return sq;
            }
        }
        return -1;
    }
}
