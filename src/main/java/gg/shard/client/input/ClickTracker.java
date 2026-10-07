package gg.shard.client.input;

import java.util.ArrayDeque;
import java.util.Deque;

/** Counts mouse clicks per second from the attack/use hooks. No Minecraft dependencies. */
public final class ClickTracker {
    private ClickTracker() {}

    private static final Deque<Long> LEFT = new ArrayDeque<>();
    private static final Deque<Long> RIGHT = new ArrayDeque<>();
    private static final long WINDOW_MS = 1000;

    public static void recordLeft() {
        record(LEFT, System.currentTimeMillis());
    }

    public static void recordRight() {
        record(RIGHT, System.currentTimeMillis());
    }

    public static int left() {
        return count(LEFT, System.currentTimeMillis());
    }

    public static int right() {
        return count(RIGHT, System.currentTimeMillis());
    }

    static void record(Deque<Long> deque, long now) {
        deque.addLast(now);
        prune(deque, now);
    }

    static int count(Deque<Long> deque, long now) {
        prune(deque, now);
        return deque.size();
    }

    private static void prune(Deque<Long> deque, long now) {
        while (!deque.isEmpty() && now - deque.peekFirst() > WINDOW_MS) deque.pollFirst();
    }

    public static void reset() {
        LEFT.clear();
        RIGHT.clear();
    }
}
