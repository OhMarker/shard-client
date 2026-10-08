package gg.shard.client.cosmetics;

import java.util.ArrayList;
import java.util.List;

/**
 * Mip levels for a high-resolution cape. Each level halves the previous one with a 2x2 box filter
 * weighted by alpha, so the transparent parts of the cape layout never darken the edges of the
 * painted parts. Pixels are ARGB ints (NativeImage.getPixels order).
 */
public final class MipChain {
    private MipChain() {}

    public record Level(int width, int height, int[] argb) {}

    /** Number of levels from {@code width} down to {@code minWidth} (inclusive), at least 1. */
    public static int levelCount(int width, int height, int minWidth) {
        int n = 1;
        while (width / 2 >= Math.max(1, minWidth) && height / 2 >= 1 && width % 2 == 0 && height % 2 == 0) {
            width /= 2;
            height /= 2;
            n++;
        }
        return n;
    }

    /** Level 0 is the input itself; the result has {@link #levelCount} levels. */
    public static List<Level> build(int[] argb, int width, int height, int minWidth) {
        List<Level> out = new ArrayList<>();
        Level level = new Level(width, height, argb);
        out.add(level);
        int count = levelCount(width, height, minWidth);
        for (int i = 1; i < count; i++) {
            level = halve(level);
            out.add(level);
        }
        return out;
    }

    static Level halve(Level in) {
        int w = in.width() / 2;
        int h = in.height() / 2;
        int[] src = in.argb();
        int[] dst = new int[w * h];
        int sw = in.width();
        for (int y = 0; y < h; y++) {
            int row0 = (2 * y) * sw;
            int row1 = row0 + sw;
            for (int x = 0; x < w; x++) {
                int a = src[row0 + 2 * x];
                int b = src[row0 + 2 * x + 1];
                int c = src[row1 + 2 * x];
                int d = src[row1 + 2 * x + 1];
                dst[y * w + x] = average(a, b, c, d);
            }
        }
        return new Level(w, h, dst);
    }

    /** Alpha-weighted mean of four ARGB pixels; fully transparent input stays transparent black. */
    static int average(int a, int b, int c, int d) {
        int[] px = {a, b, c, d};
        long alpha = 0;
        long r = 0;
        long g = 0;
        long bl = 0;
        for (int p : px) {
            int pa = p >>> 24;
            alpha += pa;
            r += (long) ((p >> 16) & 0xFF) * pa;
            g += (long) ((p >> 8) & 0xFF) * pa;
            bl += (long) (p & 0xFF) * pa;
        }
        if (alpha == 0) return 0;
        int outA = (int) ((alpha + 2) / 4);
        int outR = (int) ((r + alpha / 2) / alpha);
        int outG = (int) ((g + alpha / 2) / alpha);
        int outB = (int) ((bl + alpha / 2) / alpha);
        return (outA << 24) | (outR << 16) | (outG << 8) | outB;
    }
}
