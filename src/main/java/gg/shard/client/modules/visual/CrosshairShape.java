package gg.shard.client.modules.visual;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

/**
 * Pure crosshair geometry (tested): every style becomes a square pixel mask centred on the
 * crosshair, the outline is the mask grown by the outline width, and drawing merges each row
 * into runs. One crosshair pixel is one GUI unit, like vanilla's crosshair, so it scales with
 * the GUI scale. Also the custom pixel grid and the share code.
 */
public final class CrosshairShape {
    private CrosshairShape() {}

    /** Side of the custom pixel grid. */
    public static final int CUSTOM = 15;

    public enum Kind { CROSS, DOT, CROSS_DOT, CIRCLE, PLUS, T_SHAPE, X, CUSTOM }

    /** A mask of side {@code size}, pixel (x, y) at {@code bits[y * size + x]}, centre at size / 2. */
    public record Mask(int size, boolean[] bits) {
        public boolean at(int x, int y) {
            return x >= 0 && y >= 0 && x < size && y < size && bits[y * size + x];
        }

        public int center() {
            return size / 2;
        }
    }

    /** One horizontal run of pixels, relative to the centre. */
    public record Run(int x, int y, int w) {}

    public static Mask mask(Kind kind, int armLength, int gap, int thickness, boolean[] custom) {
        int t = Math.max(1, thickness);
        int s = Math.max(1, armLength);
        int g = Math.max(0, gap);
        int reach = switch (kind) {
            case CUSTOM -> CUSTOM / 2;
            case CIRCLE -> g + s / 2 + 1 + t;
            case X -> g + s + t;
            default -> g + s + t;
        };
        int size = reach * 2 + 1;
        int c = size / 2;
        boolean[] bits = new boolean[size * size];
        int lo = -(t - 1) / 2;
        int hi = lo + t - 1;
        switch (kind) {
            case DOT -> square(bits, size, c, lo, hi);
            case PLUS -> arms(bits, size, c, 0, s + g, lo, hi, true, true);
            case CROSS -> arms(bits, size, c, g + 1, s, lo, hi, true, true);
            case CROSS_DOT -> {
                arms(bits, size, c, g + 1, s, lo, hi, true, true);
                square(bits, size, c, lo, hi);
            }
            case T_SHAPE -> arms(bits, size, c, g + 1, s, lo, hi, false, true);
            case X -> {
                for (int d = g + 1; d <= g + s; d++) {
                    for (int k = lo; k <= hi; k++) {
                        set(bits, size, c + d + k, c + d);
                        set(bits, size, c - d + k, c + d);
                        set(bits, size, c + d + k, c - d);
                        set(bits, size, c - d + k, c - d);
                    }
                }
            }
            case CIRCLE -> {
                double r = g + s / 2.0 + 1;
                for (int y = 0; y < size; y++) {
                    for (int x = 0; x < size; x++) {
                        double d = Math.hypot(x - c, y - c);
                        if (Math.abs(d - r) <= t / 2.0) bits[y * size + x] = true;
                    }
                }
                square(bits, size, c, lo, hi);
            }
            case CUSTOM -> {
                for (int i = 0; i < CUSTOM * CUSTOM && custom != null && i < custom.length; i++) bits[i] = custom[i];
            }
        }
        return new Mask(size, bits);
    }

    /** The mask grown by {@code r} pixels in every direction (square brush), as a larger mask. */
    public static Mask grow(Mask m, int r) {
        if (r <= 0) return m;
        int size = m.size() + 2 * r;
        boolean[] bits = new boolean[size * size];
        for (int y = 0; y < m.size(); y++) {
            for (int x = 0; x < m.size(); x++) {
                if (!m.at(x, y)) continue;
                for (int dy = -r; dy <= r; dy++) for (int dx = -r; dx <= r; dx++) bits[(y + r + dy) * size + (x + r + dx)] = true;
            }
        }
        return new Mask(size, bits);
    }

    /** Rows of the mask merged into runs, relative to the centre pixel. */
    public static List<Run> runs(Mask m) {
        List<Run> out = new ArrayList<>();
        int c = m.center();
        for (int y = 0; y < m.size(); y++) {
            int x = 0;
            while (x < m.size()) {
                if (!m.at(x, y)) {
                    x++;
                    continue;
                }
                int start = x;
                while (x < m.size() && m.at(x, y)) x++;
                out.add(new Run(start - c, y - c, x - start));
            }
        }
        return out;
    }

    private static void arms(boolean[] bits, int size, int c, int from, int length, int lo, int hi, boolean top, boolean rest) {
        for (int d = from; d < from + length; d++) {
            for (int k = lo; k <= hi; k++) {
                if (top) set(bits, size, c + k, c - d);
                set(bits, size, c + k, c + d);
                set(bits, size, c - d, c + k);
                set(bits, size, c + d, c + k);
            }
        }
        if (from == 0) square(bits, size, c, lo, hi);
    }

    private static void square(boolean[] bits, int size, int c, int lo, int hi) {
        for (int y = lo; y <= hi; y++) for (int x = lo; x <= hi; x++) set(bits, size, c + x, c + y);
    }

    private static void set(boolean[] bits, int size, int x, int y) {
        if (x >= 0 && y >= 0 && x < size && y < size) bits[y * size + x] = true;
    }

    // ---- custom grid as text ------------------------------------------------------------------

    public static String encodePixels(boolean[] px) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < CUSTOM * CUSTOM; i += 4) {
            int v = 0;
            for (int b = 0; b < 4; b++) if (i + b < CUSTOM * CUSTOM && px != null && i + b < px.length && px[i + b]) v |= 8 >> b;
            sb.append(Integer.toHexString(v));
        }
        return sb.toString();
    }

    public static boolean[] decodePixels(String text) {
        boolean[] px = new boolean[CUSTOM * CUSTOM];
        if (text == null) return px;
        for (int n = 0; n < text.length(); n++) {
            int v = Character.digit(text.charAt(n), 16);
            if (v < 0) continue;
            for (int b = 0; b < 4; b++) {
                int i = n * 4 + b;
                if (i < px.length) px[i] = (v & (8 >> b)) != 0;
            }
        }
        return px;
    }

    // ---- share code ---------------------------------------------------------------------------

    /** Everything that defines how a crosshair looks. Colours are ARGB. */
    public record Spec(Kind kind, int size, int gap, int thickness, int color, boolean outline, int outlineColor, int outlineWidth, String pixels) {}

    private static final String PREFIX = "SHARD-X1-";

    public static String encode(Spec s) {
        String raw = String.join(";", s.kind().name(), String.valueOf(s.size()), String.valueOf(s.gap()), String.valueOf(s.thickness()),
                Integer.toHexString(s.color()), s.outline() ? "1" : "0", Integer.toHexString(s.outlineColor()), String.valueOf(s.outlineWidth()),
                s.pixels() == null ? "" : s.pixels());
        return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /** Parses a share code, or returns null when it is not one. */
    public static Spec decode(String code) {
        if (code == null) return null;
        String c = code.trim();
        if (!c.toUpperCase(Locale.ROOT).startsWith(PREFIX)) return null;
        try {
            String raw = new String(Base64.getUrlDecoder().decode(c.substring(PREFIX.length())), java.nio.charset.StandardCharsets.UTF_8);
            String[] f = raw.split(";", -1);
            if (f.length < 8) return null;
            return new Spec(Kind.valueOf(f[0]), Integer.parseInt(f[1]), Integer.parseInt(f[2]), Integer.parseInt(f[3]),
                    (int) Long.parseLong(f[4], 16), "1".equals(f[5]), (int) Long.parseLong(f[6], 16), Integer.parseInt(f[7]),
                    f.length > 8 ? f[8] : "");
        } catch (RuntimeException e) {
            return null;
        }
    }
}
