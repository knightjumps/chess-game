package chess.game;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.random.RandomGenerator;

/**
 * In-memory match lookup for an HTTP/WebSocket adapter; each match owns its own board.
 */
public final class MatchRegistry {
    private final ConcurrentHashMap<UUID, ChessGame> matches = new ConcurrentHashMap<>();

    public UUID create(Player first, Player second, RandomGenerator random) {
        ChessGame game = ChessGame.start(first, second, random);
        UUID id = UUID.randomUUID();
        matches.put(id, game);
        return id;
    }

    public ChessGame get(UUID id) {
        ChessGame game = matches.get(id);
        if (game == null) throw new IllegalArgumentException("Unknown match: " + id);
        return game;
    }
}
