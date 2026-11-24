package io.github.davidpomaresp.alfil;

/**
 * Reads and writes positions in Forsyth-Edwards Notation.
 */
public final class Fen {

    private static final String CASTLING_CHARS = "KQkq";

    private Fen() {
    }

    public static Board parse(String fen) {
        String[] fields = fen.trim().split("\\s+");
        if (fields.length < 4 || fields.length > 6) {
            throw new IllegalArgumentException("FEN needs 4 to 6 fields: " + fen);
        }
        Board board = new Board();
        parsePlacement(board, fields[0]);
        board.sideToMove = parseSide(fields[1]);
        board.castlingRights = parseCastling(fields[2]);
        board.enPassantSquare = fields[3].equals("-") ? Square.NONE : Square.parse(fields[3]);
        board.halfmoveClock = fields.length > 4 ? parseNumber(fields[4], 0) : 0;
        board.fullmoveNumber = fields.length > 5 ? parseNumber(fields[5], 1) : 1;
        return board;
    }

    private static void parsePlacement(Board board, String placement) {
        String[] ranks = placement.split("/");
        if (ranks.length != 8) {
            throw new IllegalArgumentException("Placement needs 8 ranks: " + placement);
        }
        for (int i = 0; i < 8; i++) {
            int rank = 7 - i;
            int file = 0;
            for (char c : ranks[i].toCharArray()) {
                if (c >= '1' && c <= '8') {
                    file += c - '0';
                } else {
                    if (file > 7) {
                        throw new IllegalArgumentException("Rank too long: " + ranks[i]);
                    }
                    board.squares[Square.of(file, rank)] = Piece.fromChar(c);
                    file++;
                }
            }
            if (file != 8) {
                throw new IllegalArgumentException("Rank must cover 8 squares: " + ranks[i]);
            }
        }
    }

    private static int parseSide(String side) {
        return switch (side) {
            case "w" -> Piece.WHITE;
            case "b" -> Piece.BLACK;
            default -> throw new IllegalArgumentException("Side must be w or b: " + side);
        };
    }

    private static int parseCastling(String castling) {
        if (castling.equals("-")) {
            return 0;
        }
        int rights = 0;
        for (char c : castling.toCharArray()) {
            int index = CASTLING_CHARS.indexOf(c);
            if (index < 0) {
                throw new IllegalArgumentException("Bad castling field: " + castling);
            }
            rights |= 1 << index;
        }
        return rights;
    }

    private static int parseNumber(String field, int min) {
        try {
            int n = Integer.parseInt(field);
            if (n < min) {
                throw new IllegalArgumentException("Number out of range: " + field);
            }
            return n;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Not a number: " + field, e);
        }
    }

    public static String format(Board board) {
        StringBuilder sb = new StringBuilder();
        for (int rank = 7; rank >= 0; rank--) {
            int empty = 0;
            for (int file = 0; file < 8; file++) {
                int piece = board.squares[Square.of(file, rank)];
                if (piece == Piece.EMPTY) {
                    empty++;
                    continue;
                }
                if (empty > 0) {
                    sb.append(empty);
                    empty = 0;
                }
                sb.append(Piece.toChar(piece));
            }
            if (empty > 0) {
                sb.append(empty);
            }
            if (rank > 0) {
                sb.append('/');
            }
        }
        sb.append(board.sideToMove == Piece.WHITE ? " w " : " b ");
        if (board.castlingRights == 0) {
            sb.append('-');
        } else {
            for (int i = 0; i < 4; i++) {
                if ((board.castlingRights & (1 << i)) != 0) {
                    sb.append(CASTLING_CHARS.charAt(i));
                }
            }
        }
        sb.append(' ')
                .append(board.enPassantSquare == Square.NONE ? "-" : Square.name(board.enPassantSquare))
                .append(' ').append(board.halfmoveClock)
                .append(' ').append(board.fullmoveNumber);
        return sb.toString();
    }
}
