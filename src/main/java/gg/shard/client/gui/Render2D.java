package gg.shard.client.gui;

import gg.shard.client.util.Colors;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

/**
 * 2D drawing helpers on top of GuiGraphics: real rounded rectangles (per-row fills, so they
 * work at any GUI scale), outlines, switches, sliders, checkerboards and text utilities.
 */
public final class Render2D {
    private Render2D() {}

    public static void fill(GuiGraphics g, int x, int y, int w, int h, int color) {
        if (w <= 0 || h <= 0 || Colors.alpha(color) == 0) return;
        g.fill(x, y, x + w, y + h, color);
    }

    /** Legacy 1px-radius rectangle used by HUD modules. */
    public static void rounded(GuiGraphics g, int x, int y, int w, int h, int color) {
        if (w <= 2 || h <= 2) {
            fill(g, x, y, w, h, color);
            return;
        }
        g.fill(x + 1, y, x + w - 1, y + h, color);
        g.fill(x, y + 1, x + 1, y + h - 1, color);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    /** Horizontal inset of a quarter-circle corner at row {@code i} (0 = outermost row). */
    static int cornerInset(int r, int i) {
        double d = r - 0.5 - i;
        double inside = r * r - d * d;
        if (inside <= 0) return r;
        return (int) Math.round(r - Math.sqrt(inside));
    }

    /** Filled rectangle with circular corners of radius {@code r}. */
    public static void roundedRect(GuiGraphics g, int x, int y, int w, int h, int r, int color) {
        if (w <= 0 || h <= 0 || Colors.alpha(color) == 0) return;
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        if (r <= 1) {
            if (r == 1) rounded(g, x, y, w, h, color);
            else fill(g, x, y, w, h, color);
            return;
        }
        for (int i = 0; i < r; i++) {
            int inset = cornerInset(r, i);
            g.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
            g.fill(x + inset, y + h - 1 - i, x + w - inset, y + h - i, color);
        }
        g.fill(x, y + r, x + w, y + h - r, color);
    }

    /** 1px outline following the same rounded shape. */
    public static void roundedOutline(GuiGraphics g, int x, int y, int w, int h, int r, int color) {
        if (w <= 0 || h <= 0 || Colors.alpha(color) == 0) return;
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        if (r <= 1) {
            g.renderOutline(x, y, w, h, color);
            return;
        }
        int prev = -1;
        for (int i = 0; i < r; i++) {
            int inset = cornerInset(r, i);
            int from = prev < 0 ? inset : Math.min(prev, inset);
            int to = prev < 0 ? inset : Math.max(prev, inset);
            if (i == 0) {
                g.fill(x + inset, y, x + w - inset, y + 1, color);
                g.fill(x + inset, y + h - 1, x + w - inset, y + h, color);
            } else if (to > from) {
                g.fill(x + from, y + i, x + to + 1, y + i + 1, color);
                g.fill(x + w - to - 1, y + i, x + w - from, y + i + 1, color);
                g.fill(x + from, y + h - 1 - i, x + to + 1, y + h - i, color);
                g.fill(x + w - to - 1, y + h - 1 - i, x + w - from, y + h - i, color);
            } else {
                g.fill(x + inset, y + i, x + inset + 1, y + i + 1, color);
                g.fill(x + w - inset - 1, y + i, x + w - inset, y + i + 1, color);
                g.fill(x + inset, y + h - 1 - i, x + inset + 1, y + h - i, color);
                g.fill(x + w - inset - 1, y + h - 1 - i, x + w - inset, y + h - i, color);
            }
            prev = inset;
        }
        g.fill(x, y + r, x + 1, y + h - r, color);
        g.fill(x + w - 1, y + r, x + w, y + h - r, color);
    }

    /** Rounded fill with a 1px border of {@code border}. */
    public static void panel(GuiGraphics g, int x, int y, int w, int h, int r, int fill, int border) {
        roundedRect(g, x, y, w, h, r, fill);
        roundedOutline(g, x, y, w, h, r, border);
    }

    /** Legacy panel with a shadow (HUD editor, tooltips). */
    public static void panel(GuiGraphics g, int x, int y, int w, int h, int fill, int border) {
        rounded(g, x + 1, y + 2, w, h, Theme.shadow());
        rounded(g, x, y, w, h, fill);
        outline(g, x, y, w, h, border);
    }

    public static void outline(GuiGraphics g, int x, int y, int w, int h, int color) {
        if (w <= 0 || h <= 0) return;
        g.renderOutline(x, y, w, h, color);
    }

    public static void circle(GuiGraphics g, int cx, int cy, int radius, int color) {
        roundedRect(g, cx - radius, cy - radius, radius * 2, radius * 2, radius, color);
    }

    /** Lunar-style toggle: pill track, round knob; {@code knob} is the animated 0..1 position. */
    public static void toggle(GuiGraphics g, int x, int y, int w, int h, float knob, boolean on, boolean focused) {
        int track = Colors.mix(Theme.control(), Theme.accent(), knob);
        roundedRect(g, x, y, w, h, h / 2, track);
        if (focused) roundedOutline(g, x - 1, y - 1, w + 2, h + 2, h / 2 + 1, Theme.accentAlpha(0x90));
        else roundedOutline(g, x, y, w, h, h / 2, on ? Theme.accentAlpha(0x60) : Theme.lineStrong());
        int kd = h - 4;
        int kx = x + 2 + Math.round(knob * (w - 4 - kd));
        roundedRect(g, kx, y + 2, kd, kd, kd / 2, on ? Theme.accentText() : Theme.muted());
    }

    /** Slider track with the filled portion in accent and a round knob. */
    public static void slider(GuiGraphics g, int x, int y, int w, double fraction, boolean active, boolean focused) {
        int trackH = 3;
        int ty = y + 4;
        roundedRect(g, x, ty, w, trackH, 1, Theme.control());
        int fw = (int) Math.round(w * Math.max(0, Math.min(1, fraction)));
        if (fw > 0) roundedRect(g, x, ty, fw, trackH, 1, Theme.accent());
        int knobD = 9;
        int kx = x + fw - knobD / 2;
        kx = Math.max(x - 1, Math.min(x + w - knobD + 1, kx));
        int ky = ty + trackH / 2 - knobD / 2;
        if (focused || active) circle(g, kx + knobD / 2, ky + knobD / 2, knobD / 2 + 1, Theme.accentAlpha(0x50));
        roundedRect(g, kx, ky, knobD, knobD, knobD / 2, active ? Theme.accentHover() : Theme.text());
    }

    /** Horizontal progress bar with a dim track (HUD modules). */
    public static void bar(GuiGraphics g, int x, int y, int w, int h, double fraction, int color) {
        rounded(g, x, y, w, h, 0x55000000);
        int fw = (int) Math.round(w * Math.max(0, Math.min(1, fraction)));
        if (fw > 0) rounded(g, x, y, fw, h, color);
    }

    /** Light/dark checkerboard for showing transparency. */
    public static void checker(GuiGraphics g, int x, int y, int w, int h, int cell) {
        for (int cy = 0; cy < h; cy += cell) {
            for (int cx = 0; cx < w; cx += cell) {
                boolean light = ((cx / cell) + (cy / cell)) % 2 == 0;
                g.fill(x + cx, y + cy, x + Math.min(cx + cell, w), y + Math.min(cy + cell, h), light ? 0xFF8A8A8A : 0xFF4A4A4A);
            }
        }
    }

    public static void gradientV(GuiGraphics g, int x, int y, int w, int h, int top, int bottom) {
        if (w <= 0 || h <= 0) return;
        g.fillGradient(x, y, x + w, y + h, top, bottom);
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

    /** Draws {@code text} trimmed with an ellipsis so it never exceeds {@code maxWidth}. */
    public static void textClipped(GuiGraphics g, Font font, String text, int x, int y, int maxWidth, int color, boolean shadow) {
        if (maxWidth <= 0) return;
        String shown = text;
        if (font.width(shown) > maxWidth) {
            int ell = font.width("…");
            shown = font.plainSubstrByWidth(text, Math.max(0, maxWidth - ell)) + "…";
        }
        g.drawString(font, shown, x, y, color, shadow);
    }

    /** Word-wraps plain text to {@code maxWidth}; long words are split by width. */
    public static List<String> wrap(Font font, String text, int maxWidth) {
        List<String> out = new ArrayList<>();
        if (text == null || text.isEmpty() || maxWidth <= 0) return out;
        for (String paragraph : text.split("\n")) {
            StringBuilder line = new StringBuilder();
            for (String word : paragraph.split(" ")) {
                if (word.isEmpty()) continue;
                String candidate = line.isEmpty() ? word : line + " " + word;
                if (font.width(candidate) <= maxWidth) {
                    line = new StringBuilder(candidate);
                    continue;
                }
                if (!line.isEmpty()) {
                    out.add(line.toString());
                    line = new StringBuilder();
                }
                String rest = word;
                while (font.width(rest) > maxWidth) {
                    String head = font.plainSubstrByWidth(rest, maxWidth);
                    if (head.isEmpty()) break;
                    out.add(head);
                    rest = rest.substring(head.length());
                }
                line = new StringBuilder(rest);
            }
            if (!line.isEmpty()) out.add(line.toString());
        }
        return out;
    }

    public static boolean hovered(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    /** Frame-rate independent easing toward a target. */
    public static float approach(float current, float target, float speed, float deltaTicks) {
        float t = Math.min(1f, speed * Math.max(0f, deltaTicks));
        float next = current + (target - current) * t;
        return Math.abs(target - next) < 0.002f ? target : next;
    }

    public static float easeOut(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return 1f - (float) Math.pow(1f - t, 3);
    }
}
