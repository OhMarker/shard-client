package gg.shard.client.gui;

/**
 * Pure maths for drawing a screen at its own GUI scale (Inventory scale), unit-tested. The
 * screen is laid out in a smaller (or larger) virtual size, drawn with a pose scale of
 * {@code factor}, and mouse positions are divided by the same factor, so a click lands on the
 * slot drawn under it at every combination of game and inventory scale.
 */
public final class ScreenScale {
    private ScreenScale() {}

    /**
     * Pose factor for showing a screen at {@code wanted} GUI scale while the game runs at
     * {@code guiScale}; {@code wanted} is clamped to {@code maxScale} (what fits the window) and
     * 0 means "same as the game".
     */
    public static double factor(int wanted, int guiScale, int maxScale) {
        if (wanted <= 0 || guiScale <= 0) return 1.0;
        int target = Math.max(1, Math.min(wanted, Math.max(1, maxScale)));
        return (double) target / guiScale;
    }

    /** Virtual size the screen is laid out in. */
    public static int virtualSize(int guiSize, double factor) {
        return Math.max(1, (int) Math.floor(guiSize / factor));
    }

    /** GUI mouse coordinate to the screen's own coordinate. */
    public static double toScreen(double gui, double factor) {
        return gui / factor;
    }

    /** The screen's coordinate back to physical pixels, for tests and the smoke harness. */
    public static double toPhysical(double screen, double factor, int guiScale) {
        return screen * factor * guiScale;
    }

    /** Pose translation that scales an element by {@code s} around the anchor point {@code a}. */
    public static double anchorShift(double anchor, double s) {
        return anchor * (1 - s);
    }
}
