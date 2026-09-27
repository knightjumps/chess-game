package chess.model;

public record Move(int number, Color player, MoveRequest request, Piece moved, Piece captured, MoveType type) {
    public String toString() {
        return number + ". " + player + " " + request + " [" + type + "]" + (captured == null ? "" : " captures " + captured.type());
    }
}
