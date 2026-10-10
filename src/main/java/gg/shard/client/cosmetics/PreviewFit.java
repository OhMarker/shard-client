package gg.shard.client.cosmetics;

/**
 * Fits a catalogue preview picture (any size, often odd like 450x720) into a transparent square,
 * centred and scaled down with an alpha-weighted area average, so it gets a full mip chain and
 * stays smooth at the few dozen pixels the in-game Cosmetics tab draws it. Pure, unit-tested.
 */
public final class PreviewFit {
    private PreviewFit() {}

    /** Side of the square previews are fitted into. */
    public static final int SIZE = 256;

    /** ARGB pixels of a {@code size}x{@code size} square holding the picture; never scaled up. */
    public static int[] fit(int[] argb, int width, int height, int size) {
        int[] out = new int[size * size];
        if (width <= 0 || height <= 0 || argb.length < width * height) return out;
        double scale = Math.min(1.0, Math.min((double) size / width, (double) size / height));
        int dw = Math.max(1, Math.min(size, (int) Math.round(width * scale)));
        int dh = Math.max(1, Math.min(size, (int) Math.round(height * scale)));
        int ox = (size - dw) / 2;
        int oy = (size - dh) / 2;
        double sx = (double) width / dw;
        double sy = (double) height / dh;
        for (int y = 0; y < dh; y++) {
            int y0 = (int) Math.floor(y * sy);
            int y1 = Math.min(height, Math.max(y0 + 1, (int) Math.floor((y + 1) * sy)));
            for (int x = 0; x < dw; x++) {
                int x0 = (int) Math.floor(x * sx);
                int x1 = Math.min(width, Math.max(x0 + 1, (int) Math.floor((x + 1) * sx)));
                long a = 0, r = 0, g = 0, b = 0;
                int n = 0;
                for (int yy = y0; yy < y1; yy++) {
                    for (int xx = x0; xx < x1; xx++) {
                        int c = argb[yy * width + xx];
                        int ca = c >>> 24;
                        a += ca;
                        r += (long) ((c >> 16) & 0xFF) * ca;
                        g += (long) ((c >> 8) & 0xFF) * ca;
                        b += (long) (c & 0xFF) * ca;
                        n++;
                    }
                }
                if (n == 0 || a == 0) continue;
                int pa = (int) Math.round((double) a / n);
                int pr = (int) Math.round((double) r / a);
                int pg = (int) Math.round((double) g / a);
                int pb = (int) Math.round((double) b / a);
                out[(oy + y) * size + ox + x] = pa << 24 | pr << 16 | pg << 8 | pb;
            }
        }
        return out;
    }
}
