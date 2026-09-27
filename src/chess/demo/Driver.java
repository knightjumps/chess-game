package chess.demo;

import chess.game.*;
import chess.model.*;

import java.util.Random;

public final class Driver {
    private static final Player ALICE = new Player("alice", "Alice", Player.Kind.HUMAN);
    private static final Player BOB = new Player("bob", "Bob", Player.Kind.HUMAN);
    private static final MatchRegistry MATCHES = new MatchRegistry();

    private static ChessGame fresh() {
        return MATCHES.get(MATCHES.create(ALICE, BOB, new Random(1)));
    }

    private static ChessGame position(String fen) {
        return ChessGame.fromPositionForDemo(ALICE, BOB, Position.fromFen(fen));
    }

    private static void move(ChessGame game, Color color, String from, String to) {
        move(game, color, new MoveRequest(Square.at(from), Square.at(to), null));
    }

    private static void move(ChessGame game, Color color, MoveRequest request) {
        String id = (color == Color.WHITE ? game.white() : game.black()).id();
        Move accepted = game.makeMove(id, request, game.snapshot().version());
        System.out.println("  " + accepted);
    }

    private static void rejected(ChessGame game, Color color, String from, String to) {
        long before = game.snapshot().version();
        try {
            move(game, color, from, to);
            throw new AssertionError("Expected rejection");
        } catch (RuntimeException expected) {
            System.out.println("  Rejected " + from + "-" + to + ": " + expected.getMessage());
        }
        if (game.snapshot().version() != before) throw new AssertionError("Invalid move changed game");
    }

    private static void expectResult(ChessGame game, Color winner, GameResult.Reason reason) {
        GameResult result = game.snapshot().result();
        if (result == null || result.winner() != winner || result.reason() != reason)
            throw new AssertionError("Expected " + winner + "/" + reason + ", got " + result);
    }

    public static void main(String[] args) {
        ChessGame mate = fresh();
        System.out.println("1. Initial setup and checkmate (White: " + mate.white().name() + ")");
        System.out.println(mate.snapshot().board());
        rejected(mate, Color.BLACK, "e7", "e5");
        rejected(mate, Color.WHITE, "a1", "a4");
        move(mate, Color.WHITE, "e2", "e4");
        move(mate, Color.BLACK, "e7", "e5");
        move(mate, Color.WHITE, "d1", "h5");
        move(mate, Color.BLACK, "b8", "c6");
        move(mate, Color.WHITE, "f1", "c4");
        move(mate, Color.BLACK, "g8", "f6");
        move(mate, Color.WHITE, "h5", "f7");
        expectResult(mate, Color.WHITE, GameResult.Reason.CHECKMATE);
        System.out.println("  Result: " + mate.snapshot().result());
        System.out.println("  Recorded moves: " + mate.snapshot().history().size());
        ChessGame castle = fresh();
        System.out.println("\n2. Kingside castling");
        move(castle, Color.WHITE, "e2", "e4");
        move(castle, Color.BLACK, "e7", "e5");
        move(castle, Color.WHITE, "g1", "f3");
        move(castle, Color.BLACK, "b8", "c6");
        move(castle, Color.WHITE, "f1", "c4");
        move(castle, Color.BLACK, "f8", "c5");
        move(castle, Color.WHITE, "e1", "g1");
        if (castle.snapshot().board().get(Square.at("f1")).type() != PieceType.ROOK)
            throw new AssertionError("Rook did not castle");
        ChessGame ep = fresh();
        System.out.println("\n3. En passant");
        move(ep, Color.WHITE, "e2", "e4");
        move(ep, Color.BLACK, "a7", "a6");
        move(ep, Color.WHITE, "e4", "e5");
        move(ep, Color.BLACK, "d7", "d5");
        move(ep, Color.WHITE, "e5", "d6");
        if (ep.snapshot().board().get(Square.at("d5")) != null) throw new AssertionError("Captured pawn remained");
        ChessGame promotion = position("7k/P7/8/8/8/8/8/4K3 w - - 0 1");
        System.out.println("\n4. Underpromotion");
        move(promotion, Color.WHITE, MoveRequest.promote("a7", "a8", PieceType.KNIGHT));
        if (promotion.snapshot().board().get(Square.at("a8")).type() != PieceType.KNIGHT)
            throw new AssertionError("Promotion failed");
        ChessGame stalemate = position("7k/8/6K1/5Q2/8/8/8/8 w - - 0 1");
        System.out.println("\n5. Stalemate");
        move(stalemate, Color.WHITE, "f5", "f7");
        expectResult(stalemate, null, GameResult.Reason.STALEMATE);
        System.out.println("  Result: " + stalemate.snapshot().result());
        ChessGame resigned = fresh();
        System.out.println("\n6. Resignation and draw agreement");
        resigned.resign(resigned.black().id());
        System.out.println("  Result: " + resigned.snapshot().result());
        expectResult(resigned, Color.WHITE, GameResult.Reason.RESIGNATION);
        ChessGame agreed = fresh();
        move(agreed, Color.WHITE, "e2", "e4");
        move(agreed, Color.BLACK, "e7", "e5");
        agreed.offerDraw(agreed.white().id());
        agreed.acceptDraw(agreed.black().id());
        expectResult(agreed, null, GameResult.Reason.AGREEMENT);
        System.out.println("  Result: " + agreed.snapshot().result());
        ChessGame claim = position("7k/8/8/8/8/8/4K2R/8 w - - 100 1");
        claim.claimDraw(claim.white().id());
        System.out.println("\n7. Fifty-move claim: " + claim.snapshot().result());
        expectResult(claim, null, GameResult.Reason.FIFTY_MOVE_RULE);
        ChessGame repetition = fresh();
        System.out.println("\n8. Threefold repetition claim");
        for (int i = 0; i < 2; i++) {
            move(repetition, Color.WHITE, "g1", "f3");
            move(repetition, Color.BLACK, "g8", "f6");
            move(repetition, Color.WHITE, "f3", "g1");
            move(repetition, Color.BLACK, "f6", "g8");
        }
        repetition.claimDraw(repetition.white().id());
        expectResult(repetition, null, GameResult.Reason.THREEFOLD_REPETITION);
        System.out.println("  Result: " + repetition.snapshot().result());
        ChessGame seventyFive = position("7k/8/8/8/8/8/4K2R/8 w - - 149 1");
        System.out.println("\n9. Automatic seventy-five-move draw");
        move(seventyFive, Color.WHITE, "h2", "h3");
        expectResult(seventyFive, null, GameResult.Reason.SEVENTY_FIVE_MOVE_RULE);
        System.out.println("  Result: " + seventyFive.snapshot().result());
        System.out.println("\n10. King safety, castling safety, and stale request");
        rejected(position("4r2k/8/8/8/8/8/4R3/4K3 w - - 0 1"), Color.WHITE, "e2", "f2");
        rejected(position("5r1k/8/8/8/8/8/8/4K2R w K - 0 1"), Color.WHITE, "e1", "g1");
        ChessGame stale = fresh();
        long oldVersion = stale.snapshot().version();
        move(stale, Color.WHITE, "e2", "e4");
        try {
            stale.makeMove(stale.black().id(), MoveRequest.of("e7", "e5"), oldVersion);
            throw new AssertionError("Stale move accepted");
        } catch (IllegalStateException expected) {
            System.out.println("  Rejected stale request: " + expected.getMessage());
        }
        ChessGame forfeited = fresh();
        forfeited.forfeit(forfeited.white().id());
        expectResult(forfeited, Color.BLACK, GameResult.Reason.FORFEIT);
        System.out.println("\n11. Forfeit: " + forfeited.snapshot().result());
        ChessGame independent = fresh();
        if (independent.snapshot().history().size() != 0 || independent.snapshot().board().get(Square.at("e2")).type() != PieceType.PAWN)
            throw new AssertionError("Separate matches shared board state");
        System.out.println("\n12. Separate match retained its initial board");
        System.out.println("\nAll driver scenarios passed.");
    }
}
