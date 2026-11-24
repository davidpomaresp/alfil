package io.github.davidpomaresp.alfil;

/** Standard algebraic notation (SAN), the "Nf3", "exd5", "O-O", "e8=Q+" people read. */
public final class Notation {

    private static final String LETTERS = " PNBRQK";

    private Notation() {
    }

    /** SAN for a legal move in the given position. The board is left unchanged. */
    public static String toSan(Board board, int move) {
        int flags = Move.flags(move);
        String san;
        if (flags == Move.KING_CASTLE) {
            san = "O-O";
        } else if (flags == Move.QUEEN_CASTLE) {
            san = "O-O-O";
        } else {
            san = pieceAndSquare(board, move);
        }

        board.makeMove(move);
        if (board.inCheck()) {
            san += MoveGenerator.legalMoves(board).size() == 0 ? "#" : "+";
        }
        board.unmakeMove(move);
        return san;
    }

    private static String pieceAndSquare(Board board, int move) {
        int from = Move.from(move);
        int to = Move.to(move);
        int type = Piece.type(board.pieceAt(from));
        StringBuilder sb = new StringBuilder();

        if (type == Piece.PAWN) {
            if (Move.isCapture(move)) {
                sb.append((char) ('a' + Square.file(from))).append('x');
            }
            sb.append(Square.name(to));
            if (Move.isPromotion(move)) {
                sb.append('=').append(LETTERS.charAt(Move.promotionType(move)));
            }
            return sb.toString();
        }

        sb.append(LETTERS.charAt(type));
        sb.append(disambiguation(board, move, type));
        if (Move.isCapture(move)) {
            sb.append('x');
        }
        sb.append(Square.name(to));
        return sb.toString();
    }

    /** When two pieces of the same kind can reach the square, add the file, the rank or both. */
    private static String disambiguation(Board board, int move, int type) {
        int from = Move.from(move);
        boolean ambiguous = false;
        boolean sameFile = false;
        boolean sameRank = false;
        MoveList moves = MoveGenerator.legalMoves(board);
        for (int i = 0; i < moves.size(); i++) {
            int other = moves.get(i);
            int otherFrom = Move.from(other);
            if (other == move || Move.to(other) != Move.to(move) || otherFrom == from
                    || Piece.type(board.pieceAt(otherFrom)) != type) {
                continue;
            }
            ambiguous = true;
            sameFile |= Square.file(otherFrom) == Square.file(from);
            sameRank |= Square.rank(otherFrom) == Square.rank(from);
        }
        if (!ambiguous) {
            return "";
        }
        String square = Square.name(from);
        if (!sameFile) {
            return square.substring(0, 1);
        }
        if (!sameRank) {
            return square.substring(1);
        }
        return square;
    }
}
