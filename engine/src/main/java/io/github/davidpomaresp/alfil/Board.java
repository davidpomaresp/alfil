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
    final int[] kingSquare = {Square.NONE, Square.NONE};

    private static final int MAX_PLY = 1024;
    private final int[] undoCaptured = new int[MAX_PLY];
    private final int[] undoCastling = new int[MAX_PLY];
    private final int[] undoEnPassant = new int[MAX_PLY];
    private final int[] undoHalfmove = new int[MAX_PLY];
    private int ply;

    // Castling rights survive a move only if neither square is listed here.
    private static final int[] CASTLING_MASK = new int[64];

    static {
        java.util.Arrays.fill(CASTLING_MASK, 15);
        CASTLING_MASK[Square.parse("a1")] &= ~WHITE_QUEENSIDE;
        CASTLING_MASK[Square.parse("h1")] &= ~WHITE_KINGSIDE;
        CASTLING_MASK[Square.parse("e1")] &= ~(WHITE_KINGSIDE | WHITE_QUEENSIDE);
        CASTLING_MASK[Square.parse("a8")] &= ~BLACK_QUEENSIDE;
        CASTLING_MASK[Square.parse("h8")] &= ~BLACK_KINGSIDE;
        CASTLING_MASK[Square.parse("e8")] &= ~(BLACK_KINGSIDE | BLACK_QUEENSIDE);
    }

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

    public int kingSquare(int color) {
        return kingSquare[color];
    }

    public boolean inCheck() {
        return isAttacked(kingSquare[sideToMove], sideToMove ^ 1);
    }

    /** True if a piece of {@code byColor} attacks {@code square}. */
    public boolean isAttacked(int square, int byColor) {
        int pawn = Piece.of(byColor, Piece.PAWN);
        int file = Square.file(square);
        // A white pawn attacks upwards, so it would sit one rank below the square.
        int pawnRow = byColor == Piece.WHITE ? -8 : 8;
        int left = square + pawnRow - 1;
        int right = square + pawnRow + 1;
        if (file > 0 && left >= 0 && left < 64 && squares[left] == pawn) {
            return true;
        }
        if (file < 7 && right >= 0 && right < 64 && squares[right] == pawn) {
            return true;
        }

        int knight = Piece.of(byColor, Piece.KNIGHT);
        for (int target : Attacks.KNIGHT[square]) {
            if (squares[target] == knight) {
                return true;
            }
        }
        int king = Piece.of(byColor, Piece.KING);
        for (int target : Attacks.KING[square]) {
            if (squares[target] == king) {
                return true;
            }
        }

        int queen = Piece.of(byColor, Piece.QUEEN);
        int rook = Piece.of(byColor, Piece.ROOK);
        int bishop = Piece.of(byColor, Piece.BISHOP);
        for (int d = 0; d < 8; d++) {
            int slider = d < 4 ? rook : bishop;
            for (int target : Attacks.RAYS[square][d]) {
                int piece = squares[target];
                if (piece == Piece.EMPTY) {
                    continue;
                }
                if (piece == slider || piece == queen) {
                    return true;
                }
                break;
            }
        }
        return false;
    }

    public void makeMove(int move) {
        int from = Move.from(move);
        int to = Move.to(move);
        int flags = Move.flags(move);
        int us = sideToMove;
        int piece = squares[from];

        undoCastling[ply] = castlingRights;
        undoEnPassant[ply] = enPassantSquare;
        undoHalfmove[ply] = halfmoveClock;

        int captured;
        if (flags == Move.EN_PASSANT) {
            int capturedSquare = to + (us == Piece.WHITE ? -8 : 8);
            captured = squares[capturedSquare];
            squares[capturedSquare] = Piece.EMPTY;
        } else {
            captured = squares[to];
        }
        undoCaptured[ply] = captured;
        ply++;

        squares[to] = Move.isPromotion(move) ? Piece.of(us, Move.promotionType(move)) : piece;
        squares[from] = Piece.EMPTY;

        if (flags == Move.KING_CASTLE) {
            squares[to - 1] = squares[to + 1];
            squares[to + 1] = Piece.EMPTY;
        } else if (flags == Move.QUEEN_CASTLE) {
            squares[to + 1] = squares[to - 2];
            squares[to - 2] = Piece.EMPTY;
        }

        if (Piece.type(piece) == Piece.KING) {
            kingSquare[us] = to;
        }

        enPassantSquare = flags == Move.DOUBLE_PAWN_PUSH ? (from + to) / 2 : Square.NONE;
        castlingRights &= CASTLING_MASK[from] & CASTLING_MASK[to];
        halfmoveClock = (Piece.type(piece) == Piece.PAWN || captured != Piece.EMPTY) ? 0 : halfmoveClock + 1;
        if (us == Piece.BLACK) {
            fullmoveNumber++;
        }
        sideToMove = us ^ 1;
    }

    public void unmakeMove(int move) {
        int from = Move.from(move);
        int to = Move.to(move);
        int flags = Move.flags(move);
        sideToMove ^= 1;
        int us = sideToMove;
        if (us == Piece.BLACK) {
            fullmoveNumber--;
        }

        ply--;
        int captured = undoCaptured[ply];
        castlingRights = undoCastling[ply];
        enPassantSquare = undoEnPassant[ply];
        halfmoveClock = undoHalfmove[ply];

        int piece = Move.isPromotion(move) ? Piece.of(us, Piece.PAWN) : squares[to];
        squares[from] = piece;
        if (flags == Move.EN_PASSANT) {
            squares[to] = Piece.EMPTY;
            squares[to + (us == Piece.WHITE ? -8 : 8)] = captured;
        } else {
            squares[to] = captured;
        }

        if (flags == Move.KING_CASTLE) {
            squares[to + 1] = squares[to - 1];
            squares[to - 1] = Piece.EMPTY;
        } else if (flags == Move.QUEEN_CASTLE) {
            squares[to - 2] = squares[to + 1];
            squares[to + 1] = Piece.EMPTY;
        }

        if (Piece.type(piece) == Piece.KING) {
            kingSquare[us] = from;
        }
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
