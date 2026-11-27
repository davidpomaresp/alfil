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
    // Allocated on the first move, so a board that is only looked at (e.g. by the tuner) stays small.
    private int[] undoCaptured;
    private int[] undoCastling;
    private int[] undoEnPassant;
    private int[] undoHalfmove;
    private long[] hashHistory = new long[0];
    private int ply;
    long hash;

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

    public long hash() {
        return hash;
    }

    /**
     * True if the current position already happened earlier. Only positions since the
     * last capture or pawn move can repeat, and only with the same side to move.
     */
    public boolean isRepetition() {
        int stop = Math.max(0, ply - halfmoveClock);
        for (int i = ply - 2; i >= stop; i -= 2) {
            if (hashHistory[i] == hash) {
                return true;
            }
        }
        return false;
    }

    /** Number of earlier occurrences of the current position. */
    public int repetitionCount() {
        int count = 0;
        int stop = Math.max(0, ply - halfmoveClock);
        for (int i = ply - 2; i >= stop; i -= 2) {
            if (hashHistory[i] == hash) {
                count++;
            }
        }
        return count;
    }

    /** Neither side can possibly mate: K v K, K+minor v K, or K+B v K+B with same-coloured bishops. */
    public boolean isInsufficientMaterial() {
        int minors = 0;
        int bishopSquareColors = 0;
        int bishops = 0;
        for (int sq = 0; sq < 64; sq++) {
            int type = Piece.type(squares[sq]);
            switch (type) {
                case Piece.EMPTY, Piece.KING -> {
                }
                case Piece.KNIGHT -> minors++;
                case Piece.BISHOP -> {
                    minors++;
                    bishops++;
                    bishopSquareColors |= 1 << ((Square.file(sq) + Square.rank(sq)) & 1);
                }
                default -> {
                    return false;
                }
            }
        }
        if (minors <= 1) {
            return true;
        }
        return minors == bishops && bishopSquareColors != 3;
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

        ensureHistory();
        undoCastling[ply] = castlingRights;
        undoEnPassant[ply] = enPassantSquare;
        undoHalfmove[ply] = halfmoveClock;
        hashHistory[ply] = hash;

        long h = hash ^ Zobrist.CASTLING[castlingRights] ^ Zobrist.BLACK_TO_MOVE;
        if (enPassantSquare != Square.NONE) {
            h ^= Zobrist.EN_PASSANT_FILE[Square.file(enPassantSquare)];
        }

        int captured;
        if (flags == Move.EN_PASSANT) {
            int capturedSquare = to + (us == Piece.WHITE ? -8 : 8);
            captured = squares[capturedSquare];
            squares[capturedSquare] = Piece.EMPTY;
            h ^= Zobrist.PIECE[captured][capturedSquare];
        } else {
            captured = squares[to];
            if (captured != Piece.EMPTY) {
                h ^= Zobrist.PIECE[captured][to];
            }
        }
        undoCaptured[ply] = captured;
        ply++;

        int placed = Move.isPromotion(move) ? Piece.of(us, Move.promotionType(move)) : piece;
        squares[to] = placed;
        squares[from] = Piece.EMPTY;
        h ^= Zobrist.PIECE[piece][from] ^ Zobrist.PIECE[placed][to];

        if (flags == Move.KING_CASTLE) {
            int rook = squares[to + 1];
            squares[to - 1] = rook;
            squares[to + 1] = Piece.EMPTY;
            h ^= Zobrist.PIECE[rook][to + 1] ^ Zobrist.PIECE[rook][to - 1];
        } else if (flags == Move.QUEEN_CASTLE) {
            int rook = squares[to - 2];
            squares[to + 1] = rook;
            squares[to - 2] = Piece.EMPTY;
            h ^= Zobrist.PIECE[rook][to - 2] ^ Zobrist.PIECE[rook][to + 1];
        }

        if (Piece.type(piece) == Piece.KING) {
            kingSquare[us] = to;
        }

        enPassantSquare = flags == Move.DOUBLE_PAWN_PUSH && canCaptureEnPassant(to, us ^ 1)
                ? (from + to) / 2 : Square.NONE;
        castlingRights &= CASTLING_MASK[from] & CASTLING_MASK[to];
        h ^= Zobrist.CASTLING[castlingRights];
        if (enPassantSquare != Square.NONE) {
            h ^= Zobrist.EN_PASSANT_FILE[Square.file(enPassantSquare)];
        }
        hash = h;
        halfmoveClock = (Piece.type(piece) == Piece.PAWN || captured != Piece.EMPTY) ? 0 : halfmoveClock + 1;
        if (us == Piece.BLACK) {
            fullmoveNumber++;
        }
        sideToMove = us ^ 1;
    }

    /**
     * Whether a pawn of {@code capturer} stands next to the pawn that just moved two
     * squares. If none does, the en passant square is left out, so that the position
     * (and its hash) is the same as when it is reached any other way.
     */
    boolean canCaptureEnPassant(int pawnSquare, int capturer) {
        int pawn = Piece.of(capturer, Piece.PAWN);
        int file = Square.file(pawnSquare);
        return (file > 0 && squares[pawnSquare - 1] == pawn) || (file < 7 && squares[pawnSquare + 1] == pawn);
    }

    /** Passes the turn without moving. Only used by the search (null-move pruning). */
    public void makeNullMove() {
        ensureHistory();
        undoCaptured[ply] = Piece.EMPTY;
        undoCastling[ply] = castlingRights;
        undoEnPassant[ply] = enPassantSquare;
        undoHalfmove[ply] = halfmoveClock;
        hashHistory[ply] = hash;
        ply++;
        if (enPassantSquare != Square.NONE) {
            hash ^= Zobrist.EN_PASSANT_FILE[Square.file(enPassantSquare)];
            enPassantSquare = Square.NONE;
        }
        hash ^= Zobrist.BLACK_TO_MOVE;
        halfmoveClock++;
        sideToMove ^= 1;
    }

    public void unmakeNullMove() {
        sideToMove ^= 1;
        ply--;
        enPassantSquare = undoEnPassant[ply];
        halfmoveClock = undoHalfmove[ply];
        hash = hashHistory[ply];
    }

    /** True if the side to move has something other than king and pawns. */
    public boolean hasNonPawnMaterial(int color) {
        for (int sq = 0; sq < 64; sq++) {
            int piece = squares[sq];
            int type = Piece.type(piece);
            if (piece != Piece.EMPTY && Piece.color(piece) == color && type != Piece.PAWN && type != Piece.KING) {
                return true;
            }
        }
        return false;
    }

    private void ensureHistory() {
        if (undoCaptured == null) {
            undoCaptured = new int[MAX_PLY];
            undoCastling = new int[MAX_PLY];
            undoEnPassant = new int[MAX_PLY];
            undoHalfmove = new int[MAX_PLY];
            hashHistory = new long[MAX_PLY];
        }
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
        hash = hashHistory[ply];

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
