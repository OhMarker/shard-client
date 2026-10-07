package gg.shard.client.gui;

import gg.shard.client.util.Colors;

/** Dark glass palette driven by the launcher accent (defaults to crystal cyan). */
public final class Theme {
    private Theme() {}

    private static int accent = 0xFF22D3EE;

    public static void setAccent(int argb) {
        accent = 0xFF000000 | (argb & 0xFFFFFF);
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

    public static int bg() {
        return 0xF00B0F18;
    }

    public static int panel() {
        return 0xE60F1420;
    }

    public static int panelHover() {
        return 0xF0161C2B;
    }

    public static int header() {
        return 0xF8101624;
    }

    public static int line() {
        return 0x2EFFFFFF;
    }

    public static int lineStrong() {
        return 0x47FFFFFF;
    }

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

    public static int shadow() {
        return 0x66000000;
    }
}
