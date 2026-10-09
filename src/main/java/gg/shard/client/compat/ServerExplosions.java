package gg.shard.client.compat;

/**
 * Before 1.21.2 the client applies an explosion packet by finalizing an {@code Explosion}, which
 * plays the sound and adds the particle itself: ClientPacketListenerMixin marks that call here
 * (with whether the Anchor Optimizer already predicted it) and ExplosionMixin filters the sound
 * and particle as later versions do in handleExplosion. Render thread only.
 */
public final class ServerExplosions {
    private static boolean active;
    private static boolean anchorPredicted;

    private ServerExplosions() {}

    public static void begin(boolean predicted) {
        active = true;
        anchorPredicted = predicted;
    }

    public static void end() {
        active = false;
        anchorPredicted = false;
    }

    /** True while a server explosion packet is being applied. */
    public static boolean active() {
        return active;
    }

    public static boolean anchorPredicted() {
        return anchorPredicted;
    }
}
