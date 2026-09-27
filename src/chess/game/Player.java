package chess.game;

import java.util.Objects;

public record Player(String id, String name, Kind kind) {
    public enum Kind {HUMAN, COMPUTER}

    public Player {
        Objects.requireNonNull(id);
        Objects.requireNonNull(name);
        Objects.requireNonNull(kind);
        if (id.isBlank() || name.isBlank()) throw new IllegalArgumentException("Player identity is blank");
    }
}
