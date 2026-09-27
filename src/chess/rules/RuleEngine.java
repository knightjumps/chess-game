package chess.rules;

import chess.model.*;

import java.util.ArrayList;
import java.util.List;

public final class RuleEngine {
    private final AttackDetector attacks = new AttackDetector();
    private final MovementRules movements = new MovementRules();

    public record Applied(Position position, Piece moved, Piece captured, MoveType type) {
    }

    public Applied apply(Position original, MoveRequest request) {
        Board board = original.board();
        Square from = request.from(), to = request.to();
        Piece piece = board.get(from), target = board.get(to);
        if (piece == null || piece.color() != original.turn())
            throw new IllegalMoveException("No current player's piece at " + from);
        if (target != null && target.color() == piece.color())
            throw new IllegalMoveException("Destination has a friendly piece");
        if (target != null && target.type() == PieceType.KING)
            throw new IllegalMoveException("A king is never captured");
        boolean castle = piece.type() == PieceType.KING && from.row() == to.row() && Math.abs(to.col() - from.col()) == 2;
        boolean enPassant = piece.type() == PieceType.PAWN && from.col() != to.col() && target == null;
        boolean promotion = piece.type() == PieceType.PAWN && to.row() == (piece.color() == Color.WHITE ? 7 : 0);
        if (promotion != (request.promotion() != null))
            throw new IllegalMoveException("Promotion choice required only on final rank");
        if (promotion && (request.promotion() == PieceType.KING || request.promotion() == PieceType.PAWN))
            throw new IllegalMoveException("Promotion must be queen, rook, bishop, or knight");
        if (castle) validateCastle(original, board, piece, from, to);
        else if (enPassant) validateEnPassant(original, board, piece, from, to);
        else if (!movements.allows(board, piece, from, to))
            throw new IllegalMoveException("Piece cannot move from " + from + " to " + to);
        Piece captured = target;
        board.put(from, null);
        if (castle) {
            boolean kingSide = to.col() == 6;
            Square rookFrom = new Square(from.row(), kingSide ? 7 : 0);
            Square rookTo = new Square(from.row(), kingSide ? 5 : 3);
            board.put(rookTo, board.get(rookFrom));
            board.put(rookFrom, null);
        }
        if (enPassant) {
            Square capturedAt = new Square(from.row(), to.col());
            captured = board.get(capturedAt);
            board.put(capturedAt, null);
        }
        board.put(to, promotion ? new Piece(piece.color(), request.promotion()) : piece);
        if (attacks.inCheck(board, piece.color())) throw new IllegalMoveException("Move leaves your king in check");
        CastlingRights rights = original.rights();
        if (piece.type() == PieceType.KING) rights.revokeKing(piece.color());
        if (piece.type() == PieceType.ROOK) rights.revokeRookAt(from);
        if (captured != null && captured.type() == PieceType.ROOK)
            rights.revokeRookAt(enPassant ? new Square(from.row(), to.col()) : to);
        Square epTarget = piece.type() == PieceType.PAWN && Math.abs(to.row() - from.row()) == 2 ? new Square((from.row() + to.row()) / 2, from.col()) : null;
        int halfmove = piece.type() == PieceType.PAWN || captured != null ? 0 : original.halfmoveClock() + 1;
        Position next = new Position(board, piece.color().opposite(), rights, epTarget, halfmove, original.fullmoveNumber() + (piece.color() == Color.BLACK ? 1 : 0));
        MoveType type = castle ? (to.col() == 6 ? MoveType.CASTLE_KINGSIDE : MoveType.CASTLE_QUEENSIDE) : enPassant ? MoveType.EN_PASSANT : promotion ? MoveType.PROMOTION : captured != null ? MoveType.CAPTURE : MoveType.NORMAL;
        return new Applied(next, piece, captured, type);
    }

    private void validateCastle(Position position, Board board, Piece king, Square from, Square to) {
        int home = king.color() == Color.WHITE ? 0 : 7;
        boolean kingSide = to.col() == 6;
        if (from.row() != home || from.col() != 4 || to.row() != home || (!kingSide && to.col() != 2))
            throw new IllegalMoveException("Invalid castling squares");
        if (!position.rights().allowed(king.color(), kingSide))
            throw new IllegalMoveException("Castling right has been lost");
        Square rookAt = new Square(home, kingSide ? 7 : 0);
        Piece rook = board.get(rookAt);
        if (rook == null || rook.color() != king.color() || rook.type() != PieceType.ROOK)
            throw new IllegalMoveException("Required rook is missing");
        int step = kingSide ? 1 : -1;
        for (int col = 4 + step; col != rookAt.col(); col += step)
            if (board.get(new Square(home, col)) != null) throw new IllegalMoveException("Pieces block castling");
        if (attacks.isAttacked(board, from, king.color().opposite()))
            throw new IllegalMoveException("King cannot castle out of check");
        // Test each crossed square with the king removed from e-file. Its original
        // location could otherwise hide a sliding attack on the transit square.
        for (int col = 4 + step; col != to.col() + step; col += step) {
            Board transit = board.copy();
            transit.put(from, null);
            Square crossed = new Square(home, col);
            transit.put(crossed, king);
            if (attacks.isAttacked(transit, crossed, king.color().opposite()))
                throw new IllegalMoveException("King cannot castle through check");
        }
    }

    private void validateEnPassant(Position position, Board board, Piece pawn, Square from, Square to) {
        int direction = pawn.color() == Color.WHITE ? 1 : -1;
        if (to.row() - from.row() != direction || Math.abs(to.col() - from.col()) != 1 || !to.equals(position.enPassantTarget()))
            throw new IllegalMoveException("En passant is unavailable");
        Piece victim = board.get(new Square(from.row(), to.col()));
        if (victim == null || victim.color() == pawn.color() || victim.type() != PieceType.PAWN)
            throw new IllegalMoveException("No pawn to capture en passant");
    }

    public boolean hasLegalMove(Position position) {
        Board board = position.board();
        for (int row = 0; row < 8; row++)
            for (int col = 0; col < 8; col++) {
                Square from = new Square(row, col);
                Piece piece = board.get(from);
                if (piece == null || piece.color() != position.turn()) continue;
                for (int r = 0; r < 8; r++)
                    for (int c = 0; c < 8; c++) {
                        Square to = new Square(r, c);
                        PieceType promotion = piece.type() == PieceType.PAWN && r == (piece.color() == Color.WHITE ? 7 : 0) ? PieceType.QUEEN : null;
                        try {
                            apply(position, new MoveRequest(from, to, promotion));
                            return true;
                        } catch (IllegalMoveException ignored) {
                        }
                    }
            }
        return false;
    }

    public boolean inCheck(Position position) {
        return attacks.inCheck(position.board(), position.turn());
    }

    public String repetitionKey(Position position) {
        String ep = "-";
        Square target = position.enPassantTarget();
        if (target != null) {
            int sourceRow = target.row() - (position.turn() == Color.WHITE ? 1 : -1);
            for (int dc : new int[]{-1, 1}) {
                int sourceCol = target.col() + dc;
                if (!Square.inside(sourceRow, sourceCol)) continue;
                try {
                    if (apply(position, new MoveRequest(new Square(sourceRow, sourceCol), target, null)).type() == MoveType.EN_PASSANT) {
                        ep = target.toString();
                        break;
                    }
                } catch (IllegalMoveException ignored) {
                }
            }
        }
        return position.board().layoutKey() + "|" + position.turn() + "|" + position.rights() + "|" + ep;
    }

    public boolean isDeadPosition(Position position) {
        Board board = position.board();
        List<Piece> nonKings = new ArrayList<>();
        List<Square> bishopSquares = new ArrayList<>();
        for (int row = 0; row < 8; row++)
            for (int col = 0; col < 8; col++) {
                Piece piece = board.get(new Square(row, col));
                if (piece != null && piece.type() != PieceType.KING) {
                    nonKings.add(piece);
                    if (piece.type() == PieceType.BISHOP) bishopSquares.add(new Square(row, col));
                }
            }
        if (nonKings.isEmpty()) return true;
        if (nonKings.size() == 1)
            return nonKings.getFirst().type() == PieceType.BISHOP || nonKings.getFirst().type() == PieceType.KNIGHT;
        if (nonKings.stream().allMatch(p -> p.type() == PieceType.BISHOP)) {
            int parity = (bishopSquares.getFirst().row() + bishopSquares.getFirst().col()) % 2;
            return bishopSquares.stream().allMatch(s -> (s.row() + s.col()) % 2 == parity);
        }
        return false; // Conservative: never invent a draw for a position that might permit mate.
    }
}
