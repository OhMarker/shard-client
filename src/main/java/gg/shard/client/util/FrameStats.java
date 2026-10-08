package gg.shard.client.util;

import java.util.Arrays;

/**
 * Rolling frame times for the FPS readout (pure, tested). Average FPS hides stutters, so the HUD
 * can also show the 1% low: the frame rate of the slowest 1% of recent frames, which is what a
 * player feels as a hitch in a crystal fight.
 */
public final class FrameStats {
    private final float[] ms;
    private int next;
    private int count;

    public FrameStats(int capacity) {
        this.ms = new float[Math.max(2, capacity)];
    }

    public void add(float frameMs) {
        if (!(frameMs > 0) || frameMs > 1000) return; // pauses and alt-tabs are not frames
        ms[next] = frameMs;
        next = (next + 1) % ms.length;
        if (count < ms.length) count++;
    }

    public int count() {
        return count;
    }

    /** The {@code i}-th most recent frame time (0 = newest), for graphs. */
    public float recent(int i) {
        if (i < 0 || i >= count) return 0;
        return ms[Math.floorMod(next - 1 - i, ms.length)];
    }

    /** Frames per second from the average frame time, or 0 with no data. */
    public int averageFps() {
        if (count == 0) return 0;
        double sum = 0;
        for (int i = 0; i < count; i++) sum += recent(i);
        return (int) Math.round(1000.0 * count / sum);
    }

    /** FPS of the slowest {@code percent}% of frames (1 = "1% low"); 0 with too little data. */
    public int lowFps(double percent) {
        if (count < 20) return 0;
        float[] sorted = new float[count];
        for (int i = 0; i < count; i++) sorted[i] = recent(i);
        Arrays.sort(sorted);
        int n = Math.max(1, (int) Math.round(count * percent / 100.0));
        double sum = 0;
        for (int i = count - n; i < count; i++) sum += sorted[i];
        return (int) Math.round(1000.0 * n / sum);
    }

    public void clear() {
        count = 0;
        next = 0;
    }
}
