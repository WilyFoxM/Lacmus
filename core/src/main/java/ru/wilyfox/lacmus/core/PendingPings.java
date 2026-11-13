package ru.wilyfox.lacmus.core;

/**
 * Send times of ping requests still waiting for a pong, matched by the echoed token.
 *
 * <p>Single-threaded: a connection's ping requests leave and its pongs arrive on the same network
 * thread. A fixed number of slots; when they run out, the oldest request is forgotten.
 */
public final class PendingPings {
  private static final long EMPTY = Long.MIN_VALUE;

  private final long[] tokens;
  private final long[] sentNanos;
  private int next;

  public PendingPings(int slots) {
    if (slots <= 0) {
      throw new IllegalArgumentException("slots must be positive: " + slots);
    }
    tokens = new long[slots];
    sentNanos = new long[slots];
    java.util.Arrays.fill(tokens, EMPTY);
  }

  /** Remembers that a request with {@code token} left at {@code nanos}. */
  public void sent(long token, long nanos) {
    tokens[next] = token;
    sentNanos[next] = nanos;
    next = (next + 1) % tokens.length;
  }

  /**
   * Returns the round trip for a pong carrying {@code token} that arrived at {@code nanos}, or
   * {@code -1} when no request with that token is pending. A matched request is forgotten, so a
   * duplicate pong does not count twice.
   */
  public long received(long token, long nanos) {
    for (int i = 0; i < tokens.length; i++) {
      if (tokens[i] == token) {
        tokens[i] = EMPTY;
        return nanos - sentNanos[i];
      }
    }
    return -1;
  }
}
