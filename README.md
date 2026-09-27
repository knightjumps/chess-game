# Chess Game LLD (Java 25)

Run in IntelliJ with `src/Main.java`, or from the project root:

```sh
javac -d out $(rg --files src -g '*.java')
java -cp out Main
```

## Design

| Package | Responsibility | Why it is separate |
|---|---|---|
| `chess.model` | Value objects and board position | The rules and game lifecycle share one clear vocabulary. `Piece` and `Square` are immutable values. |
| `chess.rules` | Movement, attack detection, full move validation, ending helpers | Piece geometry is different from king-safety and historical rules. A move can have valid geometry but still expose its king. |
| `chess.game` | Players, match lifecycle, move history, outcomes, versioning, match registry | The game owns its board and is the only route for committed moves. The registry holds many independent games; a process-wide board Singleton would break concurrent matches. |
| `chess.demo` | Executable scenarios | Demonstrations do not become production rules. |

`MatchRegistry` provides thread-safe lookup by match ID for a future HTTP/WebSocket adapter. `ChessGame.makeMove` is synchronized and checks an expected version. This makes one process's move transition atomic and rejects requests based on an old state. A network service would persist the position, move, result, and version in one database transaction, then broadcast the committed snapshot. The in-memory implementation does not supply persistence, authentication, matchmaking, clocks, or networking.

The rule engine creates a candidate board for each requested move. It verifies piece movement, occupancy, special-move prerequisites, and whether the mover's king remains safe. Only then does `ChessGame` replace its position, record the move, advance the version, and evaluate the result. Rejected moves leave the game unchanged. `GameSnapshot` returns a board copy and immutable history, so a caller cannot edit a live match.

The position stores castling rights, en passant target, turn, and move counters in addition to the pieces. Repetition comparisons normalize an en passant target unless a legal en passant capture is available. Threefold repetition and the 50-move threshold are player claims; fivefold repetition and the 75-move threshold end the match automatically. The dead-position check deliberately recognizes only clear cases rather than making a false draw declaration for ambiguous material.

The driver demonstrates initial setup, color assignment, invalid turns, blocked moves, Scholar's Mate, castling, en passant, knight promotion, stalemate, resignation, forfeit, draw agreement, claimable and automatic draws, king safety, castling safety, and stale request rejection. It uses FEN only to create short isolated demonstration positions; ordinary games start from the standard setup.

## Scope and follow-up work

This is an interview-scale rules engine, not a tournament platform. It does not implement chess clocks, draw claims based on an *intended next move*, move notation (SAN/PGN), persistence, transport, or arbiter policies. `fromPositionForDemo` accepts a FEN fixture and assumes the caller supplies a coherent legal position. A production import path should validate king counts, side-to-move consistency, castling rights, and en passant metadata.
