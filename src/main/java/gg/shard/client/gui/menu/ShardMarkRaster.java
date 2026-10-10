package gg.shard.client.gui.menu;

import com.mojang.blaze3d.platform.NativeImage;

/**
 * Shard Launcher's crystal mark (launcher src/renderer/src/components/brand/Logo.tsx, a 64x64 SVG
 * of five faceted polygons tilted -12°) rasterised in software with 4x4 supersampling, so the title
 * screen shows the same logo as the launcher in the same accent. {@link #pixels} is pure.
 */
final class ShardMarkRaster {
    private ShardMarkRaster() {}

    /** Gradient: colour/alpha at t=0 and t=1 along (gx0,gy0)→(gx1,gy1) of the polygon's bounding box. */
    private record Face(float[] xy, int rgb0, float a0, int rgb1, float a1, float gx0, float gy0, float gx1, float gy1) {}

    private static final int WHITE = 0xFFFFFF;

    private static Face[] faces(int accent) {
        int a = accent & 0xFFFFFF;
        return new Face[]{
                new Face(new float[]{32, 6, 23, 23, 26, 50, 32, 58}, a, 1f, a, 0.55f, 0, 0, 1, 1),
                new Face(new float[]{32, 6, 26, 50, 32, 58, 32, 37}, a, 0.9f, a, 0.9f, 0, 0, 0, 1),
                new Face(new float[]{32, 6, 32, 37, 32, 58, 38, 50}, WHITE, 0.95f, a, 1f, 0, 0, 0, 1),
                new Face(new float[]{32, 6, 41, 23, 38, 50, 32, 58}, a, 0.6f, a, 0.2f, 1, 0, 0, 1),
                new Face(new float[]{32, 6, 23, 23, 32, 18}, WHITE, 0.85f, WHITE, 0.85f, 0, 0, 0, 1),
        };
    }

    private static final float[] OUTLINE = {32, 6, 23, 23, 26, 50, 32, 58, 38, 50, 41, 23};

    static NativeImage render(int size, int accent) {
        int[] px = pixels(size, accent);
        NativeImage image = new NativeImage(size, size, false);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) image.setPixel(x, y, px[y * size + x]);
        }
        return image;
    }

    /** ARGB pixels, straight (not premultiplied) alpha. */
    static int[] pixels(int size, int accent) {
        SPLIT.clear();
        Face[] faces = faces(accent);
        int[] out = new int[size * size];
        double cos = Math.cos(Math.toRadians(12));
        double sin = Math.sin(Math.toRadians(12));
        float scale = 64f / size;
        int ss = 4;
        float edgeHalf = 0.6f; // stroke width 1.2 in SVG units
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float r = 0, gr = 0, b = 0, al = 0; // premultiplied sums
                for (int j = 0; j < ss; j++) {
                    for (int k = 0; k < ss; k++) {
                        float sx = (x + (k + 0.5f) / ss) * scale;
                        float sy = (y + (j + 0.5f) / ss) * scale;
                        // Undo the -12° rotation about (32, 32): rotate the sample by +12°.
                        float dx = sx - 32, dy = sy - 32;
                        float ux = (float) (dx * cos - dy * sin) + 32;
                        float uy = (float) (dx * sin + dy * cos) + 32;
                        float cr = 0, cg = 0, cb = 0, ca = 0;
                        for (Face f : faces) {
                            if (!inside(f.xy, ux, uy)) continue;
                            float t = gradientT(f, ux, uy);
                            float fa = f.a0 + (f.a1 - f.a0) * t;
                            int c = mix(f.rgb0, f.rgb1, t);
                            float fr = (c >> 16 & 0xFF) / 255f, fg = (c >> 8 & 0xFF) / 255f, fb = (c & 0xFF) / 255f;
                            cr = fr * fa + cr * (1 - fa);
                            cg = fg * fa + cg * (1 - fa);
                            cb = fb * fa + cb * (1 - fa);
                            ca = fa + ca * (1 - fa);
                        }
                        if (ShardBackdrop.edgeDistance(xs(OUTLINE), ys(OUTLINE), ux, uy) < edgeHalf) {
                            float sa = 0.55f;
                            cr = sa + cr * (1 - sa);
                            cg = sa + cg * (1 - sa);
                            cb = sa + cb * (1 - sa);
                            ca = sa + ca * (1 - sa);
                        }
                        r += cr;
                        gr += cg;
                        b += cb;
                        al += ca;
                    }
                }
                int n = ss * ss;
                al /= n;
                if (al <= 0) continue;
                int ai = Math.round(al * 255);
                int ri = Math.round(Math.min(1f, r / n / al) * 255);
                int gi = Math.round(Math.min(1f, gr / n / al) * 255);
                int bi = Math.round(Math.min(1f, b / n / al) * 255);
                out[y * size + x] = ai << 24 | ri << 16 | gi << 8 | bi;
            }
        }
        return out;
    }

    private static float gradientT(Face f, float x, float y) {
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (int i = 0; i < f.xy.length; i += 2) {
            minX = Math.min(minX, f.xy[i]);
            maxX = Math.max(maxX, f.xy[i]);
            minY = Math.min(minY, f.xy[i + 1]);
            maxY = Math.max(maxY, f.xy[i + 1]);
        }
        // objectBoundingBox units: the box maps to the unit square.
        float bx = maxX > minX ? (x - minX) / (maxX - minX) : 0;
        float by = maxY > minY ? (y - minY) / (maxY - minY) : 0;
        float dx = f.gx1 - f.gx0, dy = f.gy1 - f.gy0;
        float len = dx * dx + dy * dy;
        if (len == 0) return 0;
        return Math.max(0, Math.min(1, ((bx - f.gx0) * dx + (by - f.gy0) * dy) / len));
    }

    private static boolean inside(float[] xy, float x, float y) {
        return ShardBackdrop.inside(xs(xy), ys(xy), x, y);
    }

    private static final java.util.Map<float[], float[][]> SPLIT = new java.util.IdentityHashMap<>();

    private static float[] xs(float[] xy) {
        return split(xy)[0];
    }

    private static float[] ys(float[] xy) {
        return split(xy)[1];
    }

    private static float[][] split(float[] xy) {
        return SPLIT.computeIfAbsent(xy, k -> {
            float[] xs = new float[k.length / 2];
            float[] ys = new float[k.length / 2];
            for (int i = 0; i < xs.length; i++) {
                xs[i] = k[2 * i];
                ys[i] = k[2 * i + 1];
            }
            return new float[][]{xs, ys};
        });
    }

    private static int mix(int a, int b, float t) {
        int r = Math.round((a >> 16 & 0xFF) + ((b >> 16 & 0xFF) - (a >> 16 & 0xFF)) * t);
        int g = Math.round((a >> 8 & 0xFF) + ((b >> 8 & 0xFF) - (a >> 8 & 0xFF)) * t);
        int bl = Math.round((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * t);
        return r << 16 | g << 8 | bl;
    }
}
