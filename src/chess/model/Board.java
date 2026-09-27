package chess.model;

public final class Board {
    private final Piece[][] cells = new Piece[8][8];

    public Piece get(Square square) {
        return cells[square.row()][square.col()];
    }

    public void put(Square square, Piece piece) {
        cells[square.row()][square.col()] = piece;
    }

    public Board copy() {
        Board copy = new Board();
        for (int row = 0; row < 8; row++) System.arraycopy(cells[row], 0, copy.cells[row], 0, 8);
        return copy;
    }

    public static Board initial() {
        Board board = new Board();
        PieceType[] back = {PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP, PieceType.QUEEN, PieceType.KING, PieceType.BISHOP, PieceType.KNIGHT, PieceType.ROOK};
        for (int col = 0; col < 8; col++) {
            board.put(new Square(0, col), new Piece(Color.WHITE, back[col]));
            board.put(new Square(1, col), new Piece(Color.WHITE, PieceType.PAWN));
            board.put(new Square(6, col), new Piece(Color.BLACK, PieceType.PAWN));
            board.put(new Square(7, col), new Piece(Color.BLACK, back[col]));
        }
        return board;
    }

    public String layoutKey() {
        StringBuilder out = new StringBuilder(64);
        for (int row = 0; row < 8; row++)
            for (int col = 0; col < 8; col++) {
                Piece piece = cells[row][col];
                out.append(piece == null ? '.' : piece.symbol());
            }
        return out.toString();
    }

    public String toString() {
        StringBuilder out = new StringBuilder();
        for (int row = 7; row >= 0; row--) {
            out.append(row + 1).append(' ');
            for (int col = 0; col < 8; col++) {
                Piece piece = cells[row][col];
                out.append(piece == null ? '.' : piece.symbol()).append(' ');
            }
            out.append('\n');
        }
        return out.append("  a b c d e f g h").toString();
    }
}
