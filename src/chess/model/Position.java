package chess.model;

import java.util.Objects;

public final class Position {
    private final Board board;
    private final Color turn;
    private final CastlingRights rights;
    private final Square enPassantTarget;
    private final int halfmoveClock;
    private final int fullmoveNumber;

    public Position(Board board, Color turn, CastlingRights rights, Square enPassantTarget, int halfmoveClock, int fullmoveNumber) {
        this.board = Objects.requireNonNull(board).copy();
        this.turn = Objects.requireNonNull(turn);
        this.rights = Objects.requireNonNull(rights).copy();
        this.enPassantTarget = enPassantTarget;
        this.halfmoveClock = halfmoveClock;
        this.fullmoveNumber = fullmoveNumber;
        if (halfmoveClock < 0 || fullmoveNumber < 1) throw new IllegalArgumentException("Invalid move counters");
    }

    public static Position initial() {
        return new Position(Board.initial(), Color.WHITE, CastlingRights.initial(), null, 0, 1);
    }

    public Board board() {
        return board.copy();
    }

    public Color turn() {
        return turn;
    }

    public CastlingRights rights() {
        return rights.copy();
    }

    public Square enPassantTarget() {
        return enPassantTarget;
    }

    public int halfmoveClock() {
        return halfmoveClock;
    }

    public int fullmoveNumber() {
        return fullmoveNumber;
    }

    // FEN is used to create isolated demonstration positions without a long setup game.
    public static Position fromFen(String fen) {
        String[] parts = fen.split(" ");
        if (parts.length != 6) throw new IllegalArgumentException("Expected six FEN fields");
        Board board = new Board();
        String[] ranks = parts[0].split("/");
        if (ranks.length != 8) throw new IllegalArgumentException("Expected eight ranks");
        for (int i = 0; i < 8; i++) {
            int col = 0;
            for (char ch : ranks[i].toCharArray()) {
                if (ch >= '1' && ch <= '8') {
                    col += ch - '0';
                    continue;
                }
                PieceType type = switch (Character.toLowerCase(ch)) {
                    case 'k' -> PieceType.KING;
                    case 'q' -> PieceType.QUEEN;
                    case 'r' -> PieceType.ROOK;
                    case 'b' -> PieceType.BISHOP;
                    case 'n' -> PieceType.KNIGHT;
                    case 'p' -> PieceType.PAWN;
                    default -> throw new IllegalArgumentException("Unknown piece: " + ch);
                };
                if (col >= 8) throw new IllegalArgumentException("Rank too long");
                board.put(new Square(7 - i, col++), new Piece(Character.isUpperCase(ch) ? Color.WHITE : Color.BLACK, type));
            }
            if (col != 8) throw new IllegalArgumentException("Rank does not contain eight squares");
        }
        CastlingRights rights = new CastlingRights(parts[2].contains("K"), parts[2].contains("Q"), parts[2].contains("k"), parts[2].contains("q"));
        return new Position(board, switch (parts[1]) {
            case "w" -> Color.WHITE;
            case "b" -> Color.BLACK;
            default -> throw new IllegalArgumentException("Invalid turn");
        }, rights, parts[3].equals("-") ? null : Square.at(parts[3]), Integer.parseInt(parts[4]), Integer.parseInt(parts[5]));
    }
}
