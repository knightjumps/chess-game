package chess.model;

import java.util.Objects;

public record Piece(Color color, PieceType type) {
    public Piece {
        Objects.requireNonNull(color);
        Objects.requireNonNull(type);
    }

    public char symbol() {
        char value = switch (type) {
            case KING -> 'k';
            case QUEEN -> 'q';
            case ROOK -> 'r';
            case BISHOP -> 'b';
            case KNIGHT -> 'n';
            case PAWN -> 'p';
        };
        return color == Color.WHITE ? Character.toUpperCase(value) : value;
    }
}
