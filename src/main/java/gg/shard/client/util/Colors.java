package gg.shard.client.util;

import java.util.Locale;

/** ARGB int colour helpers. No Minecraft dependencies. */
public final class Colors {
    private Colors() {}

    public static int argb(int a, int r, int g, int b) {
        return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    public static int alpha(int c) {
        return (c >>> 24) & 0xFF;
    }

    public static int red(int c) {
        return (c >>> 16) & 0xFF;
    }

    public static int green(int c) {
        return (c >>> 8) & 0xFF;
    }

    public static int blue(int c) {
        return c & 0xFF;
    }

    public static int withAlpha(int c, int alpha) {
        return ((alpha & 0xFF) << 24) | (c & 0xFFFFFF);
    }

    /** Multiplies the existing alpha by {@code factor} (0..1). */
    public static int fade(int c, double factor) {
        int a = (int) Math.round(alpha(c) * Math.max(0, Math.min(1, factor)));
        return withAlpha(c, a);
    }

    public static int mix(int a, int b, double t) {
        t = Math.max(0, Math.min(1, t));
        return argb(
                (int) Math.round(alpha(a) + (alpha(b) - alpha(a)) * t),
                (int) Math.round(red(a) + (red(b) - red(a)) * t),
                (int) Math.round(green(a) + (green(b) - green(a)) * t),
                (int) Math.round(blue(a) + (blue(b) - blue(a)) * t)
        );
    }

    public static int lighten(int c, double amount) {
        return mix(c, withAlpha(0xFFFFFF, alpha(c)), amount);
    }

    public static int darken(int c, double amount) {
        return mix(c, withAlpha(0x000000, alpha(c)), amount);
    }

    /** Relative luminance 0..1 (sRGB). */
    public static double luminance(int c) {
        return (0.2126 * lin(red(c)) + 0.7152 * lin(green(c)) + 0.0722 * lin(blue(c)));
    }

    private static double lin(int channel) {
        double s = channel / 255.0;
        return s <= 0.03928 ? s / 12.92 : Math.pow((s + 0.055) / 1.055, 2.4);
    }

    /** Readable text colour on top of {@code background}. */
    public static int contrastText(int background) {
        return luminance(background) > 0.45 ? 0xFF06070B : 0xFFFFFFFF;
    }

    /** #RRGGBB (alpha 255) or #AARRGGBB; also accepts 0x prefix. Null when invalid. */
    public static Integer parseHex(String text) {
        if (text == null) return null;
        String t = text.trim();
        if (t.startsWith("#")) t = t.substring(1);
        else if (t.startsWith("0x") || t.startsWith("0X")) t = t.substring(2);
        if (!t.matches("[0-9a-fA-F]{6}|[0-9a-fA-F]{8}")) return null;
        long v = Long.parseLong(t, 16);
        if (t.length() == 6) v |= 0xFF000000L;
        return (int) v;
    }

    public static String toHex(int argb) {
        return alpha(argb) == 0xFF
                ? String.format(Locale.ROOT, "#%06X", argb & 0xFFFFFF)
                : String.format(Locale.ROOT, "#%08X", argb);
    }

    /** HSB -> ARGB with full alpha. h in 0..1. Implemented locally to avoid java.desktop. */
    public static int hsb(double h, double s, double b) {
        h = h - Math.floor(h);
        s = Math.max(0, Math.min(1, s));
        b = Math.max(0, Math.min(1, b));
        double c = b * s;
        double x = c * (1 - Math.abs((h * 6) % 2 - 1));
        double m = b - c;
        double r, g, bl;
        int sector = (int) Math.floor(h * 6) % 6;
        switch (sector) {
            case 0 -> { r = c; g = x; bl = 0; }
            case 1 -> { r = x; g = c; bl = 0; }
            case 2 -> { r = 0; g = c; bl = x; }
            case 3 -> { r = 0; g = x; bl = c; }
            case 4 -> { r = x; g = 0; bl = c; }
            default -> { r = c; g = 0; bl = x; }
        }
        return argb(255, (int) Math.round((r + m) * 255), (int) Math.round((g + m) * 255), (int) Math.round((bl + m) * 255));
    }

    /** Health-style gradient: green at 1.0, yellow mid, red at 0. */
    public static int health(double fraction) {
        fraction = Math.max(0, Math.min(1, fraction));
        return hsb(fraction * 0.33, 0.85, 0.95);
    }
}
