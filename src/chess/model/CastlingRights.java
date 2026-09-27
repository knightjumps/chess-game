package chess.model;

public final class CastlingRights {
    private boolean wk, wq, bk, bq;

    public CastlingRights(boolean wk, boolean wq, boolean bk, boolean bq) {
        this.wk = wk;
        this.wq = wq;
        this.bk = bk;
        this.bq = bq;
    }

    public static CastlingRights initial() {
        return new CastlingRights(true, true, true, true);
    }

    public CastlingRights copy() {
        return new CastlingRights(wk, wq, bk, bq);
    }

    public boolean allowed(Color color, boolean kingSide) {
        return color == Color.WHITE ? (kingSide ? wk : wq) : (kingSide ? bk : bq);
    }

    public void revokeKing(Color color) {
        if (color == Color.WHITE) {
            wk = false;
            wq = false;
        } else {
            bk = false;
            bq = false;
        }
    }

    public void revokeRookAt(Square square) {
        if (square.equals(Square.at("h1"))) wk = false;
        if (square.equals(Square.at("a1"))) wq = false;
        if (square.equals(Square.at("h8"))) bk = false;
        if (square.equals(Square.at("a8"))) bq = false;
    }

    public String toString() {
        String value = (wk ? "K" : "") + (wq ? "Q" : "") + (bk ? "k" : "") + (bq ? "q" : "");
        return value.isEmpty() ? "-" : value;
    }
}
