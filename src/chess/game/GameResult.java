package chess.game;

import chess.model.Color;

public record GameResult(Color winner, Reason reason) {
    public enum Reason {CHECKMATE, STALEMATE, DEAD_POSITION, THREEFOLD_REPETITION, FIVEFOLD_REPETITION, FIFTY_MOVE_RULE, SEVENTY_FIVE_MOVE_RULE, AGREEMENT, RESIGNATION, FORFEIT}

    public boolean draw() {
        return winner == null;
    }

    public String toString() {
        return (draw() ? "DRAW" : winner + " WINS") + " by " + reason;
    }
}
