package gg.shard.client.gui;

import gg.shard.client.util.Colors;

/**
 * Neutral grey palette (#141416 page, #1A1A1D tiles, #232326 lines) (docs/DESIGN.md) driven by one accent colour (the launcher's, defaulting to crystal cyan,
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

    /** Rows, cards and inputs. */
    public static int radius() {
        return 6;
    }

    /** Controls: buttons, fields, dropdowns, swatches. */
    public static int radiusSmall() {
        return 5;
    }

    /** The rail, the detail panel and popovers. */
    public static int radiusLarge() {
        return 8;
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
        return 0x80000000;
    }

    /** Sidebar and settings panel. */
    public static int surface() {
        return 0xFF141416;
    }

    /** Cards and list rows. */
    public static int surfaceRaised() {
        return 0xFF1A1A1D;
    }

    public static int surfaceHover() {
        return 0xFF1F1F22;
    }

    /** Inputs, switch tracks, dropdown buttons. */
    public static int control() {
        return 0xFF232326;
    }

    public static int controlHover() {
        return 0xFF2A2A2E;
    }

    /** Popovers (dropdown lists, colour picker, confirmations). */
    public static int popover() {
        return 0xFF1A1A1D;
    }

    /** Tinted square behind a module icon. */
    public static int iconWell() {
        return 0x14FFFFFF;
    }

    /** Text fields (the search box). */
    public static int input() {
        return 0xFF1C1C1F;
    }

    public static int inputLine() {
        return 0xFF26262A;
    }

    /** Text on the category tabs and secondary labels: between muted and text. */
    public static int soft() {
        return 0xFF8A8A90;
    }

    /** Module icons on tiles that are off. */
    public static int icon() {
        return 0xFFA9A9AF;
    }

    /** Hovered tile border. */
    public static int lineHover() {
        return 0xFF38383D;
    }

    public static int line() {
        return 0xFF232326;
    }

    public static int lineStrong() {
        return 0xFF2C2C30;
    }

    public static int shadow() {
        return 0x55000000;
    }

    // ---- text ---------------------------------------------------------------------------------

    public static int text() {
        return 0xFFEDEDED;
    }

    public static int muted() {
        return 0xFF7C7C82;
    }

    public static int subtle() {
        return 0xFF55555B;
    }

    public static int success() {
        return 0xFF4ADE80;
    }

    public static int warning() {
        return 0xFFFACC15;
    }

    public static int danger() {
        return 0xFFF87171;
    }

    // ---- legacy names kept for HUD modules ---------------------------------------------------

    public static int bg() {
        return 0xF00F1114;
    }

    public static int panel() {
        return surface();
    }

    public static int panelHover() {
        return surfaceHover();
    }

    public static int header() {
        return 0xF8131519;
    }
}
