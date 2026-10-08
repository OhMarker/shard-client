package gg.shard.client.gui;

/** Supersampled coverage maths behind {@link RoundedTextures}; pure Java so it is unit-tested. */
public final class CornerMask {
    private CornerMask() {}

    public static final int SUPERSAMPLE = 4;

    /**
     * Alpha (0..255) of pixel (x, y) of a {@code 2r} square holding a disc of radius {@code r}
     * centred on it; with {@code ring > 0} only the outer {@code ring} pixels of the disc count.
     */
    public static int coverage(int x, int y, int r, int ring) {
        int inside = 0;
        double inner = ring > 0 ? r - ring : -1;
        for (int j = 0; j < SUPERSAMPLE; j++) {
            for (int i = 0; i < SUPERSAMPLE; i++) {
                double sx = x + (i + 0.5) / SUPERSAMPLE - r;
                double sy = y + (j + 0.5) / SUPERSAMPLE - r;
                double d = Math.sqrt(sx * sx + sy * sy);
                if (d <= r && d >= inner) inside++;
            }
        }
        return inside * 255 / (SUPERSAMPLE * SUPERSAMPLE);
    }

    /** The whole mask as ARGB white pixels, row-major, {@code 2r} wide and tall. */
    public static int[] mask(int r, int ring) {
        int size = r * 2;
        int[] out = new int[size * size];
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) out[y * size + x] = (coverage(x, y, r, ring) << 24) | 0xFFFFFF;
        }
        return out;
    }
}
