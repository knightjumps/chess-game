package chess.game;

import chess.model.*;
import chess.rules.*;

import java.util.*;
import java.util.random.RandomGenerator;

public final class ChessGame {
    private final Player white;
    private final Player black;
    private final RuleEngine rules = new RuleEngine();
    private final List<Move> history = new ArrayList<>();
    private final Map<String, Integer> repetitionCounts = new HashMap<>();
    private Position position;
    private GameResult result;
    private String drawOfferedBy;
    private long version;

    private ChessGame(Player white, Player black, Position position) {
        if (white.id().equals(black.id())) throw new IllegalArgumentException("Players must differ");
        this.white = white;
        this.black = black;
        this.position = position;
        repetitionCounts.put(rules.repetitionKey(position), 1);
        evaluateEnding();
    }

    public static ChessGame start(Player first, Player second, RandomGenerator random) {
        Objects.requireNonNull(random);
        return random.nextBoolean() ? new ChessGame(first, second, Position.initial()) : new ChessGame(second, first, Position.initial());
    }

    public static ChessGame fromPositionForDemo(Player white, Player black, Position position) {
        return new ChessGame(white, black, position);
    }

    public Player white() {
        return white;
    }

    public Player black() {
        return black;
    }

    public synchronized GameSnapshot snapshot() {
        return new GameSnapshot(position.board(), position.turn(), result, history, version);
    }

    public synchronized Move makeMove(String playerId, MoveRequest request, long expectedVersion) {
        requireActive();
        requireVersion(expectedVersion);
        requireTurn(playerId);
        RuleEngine.Applied applied = rules.apply(position, request);
        Position next = applied.position();
        Move move = new Move(history.size() + 1, position.turn(), request, applied.moved(), applied.captured(), applied.type());
        position = next;
        history.add(move);
        version++;
        drawOfferedBy = null;
        repetitionCounts.merge(rules.repetitionKey(position), 1, Integer::sum);
        evaluateEnding();
        return move;
    }

    public synchronized void resign(String playerId) {
        finishByPlayer(playerId, GameResult.Reason.RESIGNATION);
    }

    public synchronized void forfeit(String playerId) {
        finishByPlayer(playerId, GameResult.Reason.FORFEIT);
    }

    private void finishByPlayer(String playerId, GameResult.Reason reason) {
        requireActive();
        Color loser = colorOf(playerId);
        result = new GameResult(loser.opposite(), reason);
        drawOfferedBy = null;
        version++;
    }

    public synchronized void offerDraw(String playerId) {
        requireActive();
        colorOf(playerId);
        if (history.size() < 2)
            throw new IllegalStateException("Both players must have moved before agreeing to a draw");
        drawOfferedBy = playerId;
        version++;
    }

    public synchronized void acceptDraw(String playerId) {
        requireActive();
        colorOf(playerId);
        if (drawOfferedBy == null || drawOfferedBy.equals(playerId))
            throw new IllegalStateException("No opponent draw offer");
        result = new GameResult(null, GameResult.Reason.AGREEMENT);
        drawOfferedBy = null;
        version++;
    }

    public synchronized void claimDraw(String playerId) {
        requireActive();
        requireTurn(playerId);
        int repeats = repetitionCounts.getOrDefault(rules.repetitionKey(position), 0);
        if (repeats >= 3) result = new GameResult(null, GameResult.Reason.THREEFOLD_REPETITION);
        else if (position.halfmoveClock() >= 100) result = new GameResult(null, GameResult.Reason.FIFTY_MOVE_RULE);
        else throw new IllegalStateException("No claimable draw in current position");
        version++;
    }

    private void evaluateEnding() {
        if (!rules.hasLegalMove(position)) {
            result = rules.inCheck(position) ? new GameResult(position.turn().opposite(), GameResult.Reason.CHECKMATE) : new GameResult(null, GameResult.Reason.STALEMATE);
        } else if (rules.isDeadPosition(position)) result = new GameResult(null, GameResult.Reason.DEAD_POSITION);
        else if (repetitionCounts.getOrDefault(rules.repetitionKey(position), 0) >= 5)
            result = new GameResult(null, GameResult.Reason.FIVEFOLD_REPETITION);
        else if (position.halfmoveClock() >= 150)
            result = new GameResult(null, GameResult.Reason.SEVENTY_FIVE_MOVE_RULE);
    }

    private void requireActive() {
        if (result != null) throw new IllegalStateException("Game already finished: " + result);
    }

    private void requireVersion(long expected) {
        if (expected != version)
            throw new IllegalStateException("Stale game version: expected " + version + ", got " + expected);
    }

    private void requireTurn(String playerId) {
        if (colorOf(playerId) != position.turn()) throw new IllegalMoveException("Not this player's turn");
    }

    private Color colorOf(String playerId) {
        if (white.id().equals(playerId)) return Color.WHITE;
        if (black.id().equals(playerId)) return Color.BLACK;
        throw new IllegalArgumentException("Unknown player: " + playerId);
    }
}
