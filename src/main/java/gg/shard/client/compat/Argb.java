package gg.shard.client.compat;

/**
 * ARGB's float channel getters, which arrived in 1.21.4 (stonecutter rule: ARGB.redFloat and
 * friends become these before that).
 */
public final class Argb {
    private Argb() {}

    public static float alphaFloat(int argb) {
        return (argb >>> 24) / 255f;
    }

    public static float redFloat(int argb) {
        return ((argb >> 16) & 0xFF) / 255f;
    }

    public static float greenFloat(int argb) {
        return ((argb >> 8) & 0xFF) / 255f;
    }

    public static float blueFloat(int argb) {
        return (argb & 0xFF) / 255f;
    }
}
