package gg.shard.client.util;

/**
 * Server tick rate from the time packets every server sends once a second (pure, tested): game
 * ticks that passed between two packets divided by the real seconds between them, smoothed.
 * Lag shows up as fewer than 20 ticks per second.
 */
public final class TpsEstimator {
    private long lastGameTime = Long.MIN_VALUE;
    private long lastNanos;
    private double tps = 20.0;
    private int samples;

    public void onTimePacket(long gameTime, long nanos) {
        if (lastGameTime != Long.MIN_VALUE) {
            double seconds = (nanos - lastNanos) / 1e9;
            long ticks = gameTime - lastGameTime;
            if (seconds > 0.25 && ticks >= 0 && ticks < 2000) {
                double now = Math.min(20.0, ticks / seconds);
                tps = samples == 0 ? now : tps * 0.6 + now * 0.4;
                samples++;
            }
        }
        lastGameTime = gameTime;
        lastNanos = nanos;
    }

    /** Smoothed ticks per second, or 20 until two packets have arrived. */
    public double tps() {
        return tps;
    }

    public boolean known() {
        return samples > 0;
    }

    public void reset() {
        lastGameTime = Long.MIN_VALUE;
        tps = 20.0;
        samples = 0;
    }
}
