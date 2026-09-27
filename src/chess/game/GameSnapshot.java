package chess.game;

import chess.model.*;

import java.util.List;

public record GameSnapshot(Board board, Color turn, GameResult result, List<Move> history, long version) {
    public GameSnapshot {
        board = board.copy();
        history = List.copyOf(history);
    }

    @Override
    public Board board() {
        return board.copy();
    }
}
