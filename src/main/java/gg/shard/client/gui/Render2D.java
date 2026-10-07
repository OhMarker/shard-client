package gg.shard.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** Small 2D drawing helpers on top of GuiGraphics (fills, 1px-radius corners, outlines, text). */
public final class Render2D {
    private Render2D() {}

    public static void fill(GuiGraphics g, int x, int y, int w, int h, int color) {
        if (w <= 0 || h <= 0) return;
        g.fill(x, y, x + w, y + h, color);
    }

    /** Rectangle with the four corner pixels dropped: reads as a soft radius at GUI scale. */
    public static void rounded(GuiGraphics g, int x, int y, int w, int h, int color) {
        if (w <= 2 || h <= 2) {
            fill(g, x, y, w, h, color);
            return;
        }
        g.fill(x + 1, y, x + w - 1, y + h, color);
        g.fill(x, y + 1, x + 1, y + h - 1, color);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    public static void outline(GuiGraphics g, int x, int y, int w, int h, int color) {
        if (w <= 0 || h <= 0) return;
        g.renderOutline(x, y, w, h, color);
    }

    /** Panel with a drop shadow, fill and 1px border. */
    public static void panel(GuiGraphics g, int x, int y, int w, int h, int fill, int border) {
        rounded(g, x + 1, y + 2, w, h, Theme.shadow());
        rounded(g, x, y, w, h, fill);
        outline(g, x, y, w, h, border);
    }

    public static void text(GuiGraphics g, Font font, String text, int x, int y, int color, boolean shadow) {
        g.drawString(font, text, x, y, color, shadow);
    }

    public static void textCentered(GuiGraphics g, Font font, String text, int cx, int y, int color, boolean shadow) {
        g.drawString(font, text, cx - font.width(text) / 2, y, color, shadow);
    }

    public static void textRight(GuiGraphics g, Font font, String text, int right, int y, int color, boolean shadow) {
        g.drawString(font, text, right - font.width(text), y, color, shadow);
    }

    /** Horizontal progress bar with a dim track. */
    public static void bar(GuiGraphics g, int x, int y, int w, int h, double fraction, int color) {
        rounded(g, x, y, w, h, 0x55000000);
        int fw = (int) Math.round(w * Math.max(0, Math.min(1, fraction)));
        if (fw > 0) rounded(g, x, y, fw, h, color);
    }

    public static boolean hovered(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    /** Frame-rate independent easing toward a target. */
    public static float approach(float current, float target, float speed, float deltaTicks) {
        float t = Math.min(1f, speed * Math.max(0f, deltaTicks));
        return current + (target - current) * t;
    }
}
