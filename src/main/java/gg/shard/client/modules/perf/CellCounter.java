package gg.shard.client.modules.perf;

import java.util.Arrays;

/**
 * Counts entities per block cell during one frame without allocating (tested). An open-addressed
 * table of packed block positions; {@link #reset} starts a new frame by bumping a generation
 * stamp instead of clearing the arrays. When the table is three-quarters full, further new cells
 * are admitted untracked rather than growing, so a pathological frame never allocates.
 */
public final class CellCounter {
    private final long[] keys;
    private final int[] counts;
    private final int[] stamps;
    private final int mask;
    private final int limit;
    private int generation = 1;
    private int used;

    public CellCounter(int capacity) {
        int cap = Integer.highestOneBit(Math.max(16, capacity));
        keys = new long[cap];
        counts = new int[cap];
        stamps = new int[cap];
        mask = cap - 1;
        limit = cap * 3 / 4;
    }

    /** Forgets every count (O(1) except once every 2^31 frames). */
    public void reset() {
        used = 0;
        if (++generation == 0) {
            Arrays.fill(stamps, 0);
            generation = 1;
        }
    }

    /** Adds one to {@code cell} and returns its new count, or 1 when the table is full. */
    public int increment(long cell) {
        int i = mix(cell) & mask;
        while (true) {
            if (stamps[i] != generation) {
                if (used >= limit) return 1;
                stamps[i] = generation;
                keys[i] = cell;
                counts[i] = 1;
                used++;
                return 1;
            }
            if (keys[i] == cell) return ++counts[i];
            i = (i + 1) & mask;
        }
    }

    private static int mix(long v) {
        long h = v * 0x9E3779B97F4A7C15L;
        return (int) (h ^ (h >>> 32));
    }
}
