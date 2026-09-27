package chess.model;

import java.util.Objects;

public record MoveRequest(Square from, Square to, PieceType promotion) {
    public MoveRequest {
        Objects.requireNonNull(from);
        Objects.requireNonNull(to);
    }

    public static MoveRequest of(String from, String to) {
        return new MoveRequest(Square.at(from), Square.at(to), null);
    }

    public static MoveRequest promote(String from, String to, PieceType type) {
        return new MoveRequest(Square.at(from), Square.at(to), type);
    }

    public String toString() {
        return from + "-" + to + (promotion == null ? "" : "=" + promotion);
    }
}
