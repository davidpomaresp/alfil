package io.github.davidpomaresp.alfil;

/**
 * Game state: what is on each square plus the extra information a FEN string carries.
 */
public final class Board {

    public static final String START_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    public static final int WHITE_KINGSIDE = 1;
    public static final int WHITE_QUEENSIDE = 2;
    public static final int BLACK_KINGSIDE = 4;
    public static final int BLACK_QUEENSIDE = 8;

    final int[] squares = new int[64];
    int sideToMove = Piece.WHITE;
    int castlingRights;
    int enPassantSquare = Square.NONE;
    int halfmoveClock;
    int fullmoveNumber = 1;

    Board() {
    }

    public static Board startPosition() {
        return Fen.parse(START_FEN);
    }

    public static Board fromFen(String fen) {
        return Fen.parse(fen);
    }

    public String toFen() {
        return Fen.format(this);
    }

    public int pieceAt(int square) {
        return squares[square];
    }

    public int pieceAt(String square) {
        return squares[Square.parse(square)];
    }

    public int sideToMove() {
        return sideToMove;
    }

    public int castlingRights() {
        return castlingRights;
    }

    public boolean canCastle(int right) {
        return (castlingRights & right) != 0;
    }

    public int enPassantSquare() {
        return enPassantSquare;
    }

    public int halfmoveClock() {
        return halfmoveClock;
    }

    public int fullmoveNumber() {
        return fullmoveNumber;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int rank = 7; rank >= 0; rank--) {
            sb.append(rank + 1).append(' ');
            for (int file = 0; file < 8; file++) {
                sb.append(' ').append(Piece.toChar(squares[Square.of(file, rank)]));
            }
            sb.append('\n');
        }
        sb.append("   a b c d e f g h\n");
        return sb.toString();
    }
}
