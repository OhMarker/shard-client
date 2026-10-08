package gg.shard.client.gui;

import gg.shard.client.util.Colors;

/**
 * Dark, calm palette driven by one accent colour (the launcher's, defaulting to crystal cyan,
 * optionally overridden in Settings → Appearance). Surfaces are near-opaque so they read
 * cleanly over the blurred world; lines are 1 design unit and low-contrast; the accent only
 * ever appears on the active switch, the focused control, the selected category and the
 * primary button.
 *
 * <p>All sizes are design units (see {@link Scale}), so they are the same at every GUI scale.
 */
public final class Theme {
    private Theme() {}

    private static int launcherAccent = 0xFF22D3EE;
    private static Integer accentOverride;
    private static boolean reduceMotion;

    public static void setAccent(int argb) {
        launcherAccent = 0xFF000000 | (argb & 0xFFFFFF);
    }

    /** The launcher's accent (or the default) before any user override. */
    public static int launcherAccent() {
        return launcherAccent;
    }

    /** Settings → Appearance can replace the launcher's accent; null restores it. */
    public static void setAccentOverride(Integer argb) {
        accentOverride = argb == null ? null : 0xFF000000 | (argb & 0xFFFFFF);
    }

    public static void setReduceMotion(boolean value) {
        reduceMotion = value;
    }

    public static boolean reduceMotion() {
        return reduceMotion;
    }

    // ---- shape --------------------------------------------------------------------------------

    /** Cards and popovers. */
    public static int radius() {
        return 12;
    }

    /** Controls: buttons, fields, dropdowns, swatches. */
    public static int radiusSmall() {
        return 6;
    }

    /** The sidebar and the settings panel. */
    public static int radiusLarge() {
        return 16;
    }

    // ---- accent -------------------------------------------------------------------------------

    public static int accent() {
        return accentOverride != null ? accentOverride : launcherAccent;
    }

    public static int accentAlpha(int alpha) {
        return Colors.withAlpha(accent(), alpha);
    }

    public static int accentHover() {
        return Colors.lighten(accent(), 0.12);
    }

    public static int accentText() {
        return Colors.contrastText(accent());
    }

    // ---- surfaces -----------------------------------------------------------------------------

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

    /** Popovers (dropdown lists, colour picker, confirmations). */
    public static int popover() {
        return 0xFF161D2A;
    }

    /** Tinted square behind a module icon. */
    public static int iconWell() {
        return 0x14FFFFFF;
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

    // ---- text ---------------------------------------------------------------------------------

    public static int text() {
        return 0xFFE8ECF4;
    }

    public static int muted() {
        return 0xFF9AA3B5;
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

    // ---- legacy names kept for HUD modules ---------------------------------------------------

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
