package gg.shard.client.modules.visual;

/**
 * Pure colour maths for the Sky module (tested): how much daylight the sun angle gives, mixing a
 * day and a night colour by it, dimming for rain and thunder, and blending with vanilla. Colours
 * are opaque ARGB; mixing is per channel in sRGB, which is what vanilla's sky gradient does too.
 */
public final class SkyPalette {
    private SkyPalette() {}

    /**
     * Daylight from the sun angle in radians (0 at noon), the same curve vanilla's sky darkening
     * has always used: full light for most of the day, a short fade at dusk and dawn, 0 at night.
     */
    public static float daylight(float sunAngle) {
        float d = (float) Math.cos(sunAngle) * 2f + 0.5f;
        return Math.max(0f, Math.min(1f, d));
    }

    /** {@code a} at t = 0, {@code b} at t = 1. */
    public static int lerp(float t, int a, int b) {
        float k = Math.max(0f, Math.min(1f, t));
        int r = Math.round(((a >> 16) & 0xFF) + (((b >> 16) & 0xFF) - ((a >> 16) & 0xFF)) * k);
        int g = Math.round(((a >> 8) & 0xFF) + (((b >> 8) & 0xFF) - ((a >> 8) & 0xFF)) * k);
        int bl = Math.round((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * k);
        return 0xFF000000 | r << 16 | g << 8 | bl;
    }

    /** Scales the RGB channels, keeping the result opaque. */
    public static int scale(int c, float f) {
        float k = Math.max(0f, f);
        int r = Math.min(255, Math.round(((c >> 16) & 0xFF) * k));
        int g = Math.min(255, Math.round(((c >> 8) & 0xFF) * k));
        int b = Math.min(255, Math.round((c & 0xFF) * k));
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    /** The colour for this much daylight, dimmed for rain (0..1) and thunder (0..1) like vanilla's sky. */
    public static int at(int day, int night, float daylight, float rain, float thunder) {
        int c = lerp(daylight, night, day);
        if (rain > 0) c = scale(c, 1f - rain * 0.5f);
        if (thunder > 0) c = scale(c, 1f - thunder * 0.5f);
        return c;
    }

    /** A night colour for a custom day colour: the same hue, much darker. */
    public static int nightOf(int day) {
        return scale(day, 0.14f);
    }
}
