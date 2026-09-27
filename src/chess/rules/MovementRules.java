package chess.rules;

import chess.model.*;

public final class MovementRules {
    public boolean allows(Board board, Piece piece, Square from, Square to) {
        int dr = to.row() - from.row(), dc = to.col() - from.col();
        if (dr == 0 && dc == 0) return false;
        return switch (piece.type()) {
            case KNIGHT -> (Math.abs(dr) == 2 && Math.abs(dc) == 1) || (Math.abs(dr) == 1 && Math.abs(dc) == 2);
            case KING -> Math.max(Math.abs(dr), Math.abs(dc)) == 1;
            case BISHOP -> Math.abs(dr) == Math.abs(dc) && clear(board, from, to);
            case ROOK -> (dr == 0 || dc == 0) && clear(board, from, to);
            case QUEEN -> (dr == 0 || dc == 0 || Math.abs(dr) == Math.abs(dc)) && clear(board, from, to);
            case PAWN -> pawnAllows(board, piece, from, to);
        };
    }

    private boolean clear(Board board, Square from, Square to) {
        int dr = Integer.signum(to.row() - from.row()), dc = Integer.signum(to.col() - from.col());
        for (int row = from.row() + dr, col = from.col() + dc; row != to.row() || col != to.col(); row += dr, col += dc)
            if (board.get(new Square(row, col)) != null) return false;
        return true;
    }

    private boolean pawnAllows(Board board, Piece pawn, Square from, Square to) {
        int direction = pawn.color() == Color.WHITE ? 1 : -1;
        int dr = to.row() - from.row(), dc = Math.abs(to.col() - from.col());
        if (dc == 0) {
            if (board.get(to) != null) return false;
            if (dr == direction) return true;
            int startRow = pawn.color() == Color.WHITE ? 1 : 6;
            return dr == 2 * direction && from.row() == startRow && board.get(new Square(from.row() + direction, from.col())) == null;
        }
        return dc == 1 && dr == direction && board.get(to) != null && board.get(to).color() != pawn.color();
    }
}
