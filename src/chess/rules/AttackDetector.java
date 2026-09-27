package chess.rules;

import chess.model.*;

public final class AttackDetector {
    public boolean isAttacked(Board board, Square target, Color by) {
        int pawnSourceRow = target.row() - (by == Color.WHITE ? 1 : -1);
        for (int dc : new int[]{-1, 1})
            if (matches(board, pawnSourceRow, target.col() + dc, by, PieceType.PAWN)) return true;
        int[][] knights = {{1, 2}, {2, 1}, {2, -1}, {1, -2}, {-1, -2}, {-2, -1}, {-2, 1}, {-1, 2}};
        for (int[] d : knights)
            if (matches(board, target.row() + d[0], target.col() + d[1], by, PieceType.KNIGHT)) return true;
        for (int dr = -1; dr <= 1; dr++)
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;
                if (matches(board, target.row() + dr, target.col() + dc, by, PieceType.KING)) return true;
                int row = target.row() + dr, col = target.col() + dc;
                while (Square.inside(row, col)) {
                    Piece piece = board.get(new Square(row, col));
                    if (piece != null) {
                        boolean diagonal = dr != 0 && dc != 0;
                        if (piece.color() == by && (piece.type() == PieceType.QUEEN || (diagonal && piece.type() == PieceType.BISHOP) || (!diagonal && piece.type() == PieceType.ROOK)))
                            return true;
                        break;
                    }
                    row += dr;
                    col += dc;
                }
            }
        return false;
    }

    private boolean matches(Board board, int row, int col, Color color, PieceType type) {
        if (!Square.inside(row, col)) return false;
        Piece piece = board.get(new Square(row, col));
        return piece != null && piece.color() == color && piece.type() == type;
    }

    public Square kingSquare(Board board, Color color) {
        for (int row = 0; row < 8; row++)
            for (int col = 0; col < 8; col++) {
                Square square = new Square(row, col);
                Piece piece = board.get(square);
                if (piece != null && piece.color() == color && piece.type() == PieceType.KING) return square;
            }
        throw new IllegalStateException("Missing " + color + " king");
    }

    public boolean inCheck(Board board, Color color) {
        return isAttacked(board, kingSquare(board, color), color.opposite());
    }
}
