package chess.model;

public record Square(int row, int col) {
    public Square {
        if (row < 0 || row > 7 || col < 0 || col > 7) throw new IllegalArgumentException("Square outside board");
    }

    public static Square at(String notation) {
        if (notation == null || notation.length() != 2 || notation.charAt(0) < 'a' || notation.charAt(0) > 'h' || notation.charAt(1) < '1' || notation.charAt(1) > '8')
            throw new IllegalArgumentException("Invalid square: " + notation);
        return new Square(notation.charAt(1) - '1', notation.charAt(0) - 'a');
    }

    public static boolean inside(int row, int col) {
        return row >= 0 && row < 8 && col >= 0 && col < 8;
    }

    public String toString() {
        return "" + (char) ('a' + col) + (row + 1);
    }
}
