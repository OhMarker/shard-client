package gg.shard.client.gui;

import gg.shard.client.util.Colors;

/**
 * Dark, calm palette driven by one accent colour (the launcher's, defaulting to crystal cyan).
 * Surfaces are near-opaque so they read cleanly over the blurred world; lines are 1px and
 * low-contrast; the accent only ever appears on the thing that is "on" or focused.
 */
public final class Theme {
    private Theme() {}

    private static int accent = 0xFF22D3EE;
    private static int guiScale = 2;

    public static void setAccent(int argb) {
        accent = 0xFF000000 | (argb & 0xFFFFFF);
    }

    /** The screen tells the theme the current GUI scale so radii stay around 12-16 screen px. */
    public static void setGuiScale(int scale) {
        guiScale = Math.max(1, scale);
    }

    public static int guiScale() {
        return guiScale;
    }

    /** Corner radius in GUI units: about 14 screen pixels, never below 3 or above 7. */
    public static int radius() {
        return Math.max(3, Math.min(7, Math.round(14f / guiScale)));
    }

    /** Smaller radius for controls (switches, buttons, fields). */
    public static int radiusSmall() {
        return Math.max(2, Math.min(5, Math.round(8f / guiScale)));
    }

    public static int accent() {
        return accent;
    }

    public static int accentAlpha(int alpha) {
        return Colors.withAlpha(accent, alpha);
    }

    public static int accentHover() {
        return Colors.lighten(accent, 0.12);
    }

    public static int accentText() {
        return Colors.contrastText(accent);
    }

    // ---- surfaces ---------------------------------------------------------------------------

    /** Full-screen tint behind the settings page (drawn over the blur). */
    public static int overlay() {
        return 0xB8070A12;
    }

    /** Sidebar and settings panel. */
    public static int surface() {
        return 0xF50E131C;
    }

    /** Cards and list rows. */
    public static int surfaceRaised() {
        return 0xF8131927;
    }

    public static int surfaceHover() {
        return 0xFC19202F;
    }

    /** Inputs, switch tracks, dropdown buttons. */
    public static int control() {
        return 0xFF1C2433;
    }

    public static int controlHover() {
        return 0xFF232C3D;
    }

    /** Popovers (dropdown lists, colour picker). */
    public static int popover() {
        return 0xFF161D2A;
    }

    public static int line() {
        return 0x1EFFFFFF;
    }

    public static int lineStrong() {
        return 0x38FFFFFF;
    }

    public static int shadow() {
        return 0x55000000;
    }

    // ---- text -------------------------------------------------------------------------------

    public static int text() {
        return 0xFFE8ECF4;
    }

    public static int muted() {
        return 0xFF97A0B3;
    }

    public static int subtle() {
        return 0xFF5F6779;
    }

    public static int success() {
        return 0xFF34D399;
    }

    public static int warning() {
        return 0xFFFBBF24;
    }

    public static int danger() {
        return 0xFFFB7185;
    }

    // ---- legacy names kept for HUD modules ------------------------------------------------

    public static int bg() {
        return 0xF00B0F18;
    }

    public static int panel() {
        return surface();
    }

    public static int panelHover() {
        return surfaceHover();
    }

    public static int header() {
        return 0xF8101624;
    }
}
