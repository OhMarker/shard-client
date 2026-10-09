package gg.shard.client.gui;

import gg.shard.client.util.Colors;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * 2D drawing helpers on top of GuiGraphics. Rounded rectangles and outlines are drawn from
 * anti-aliased corner textures ({@link RoundedTextures}) plus plain fills for the straight
 * parts; the old per-row fill path stays available as a fallback ({@link #setTexturedCorners}).
 * Everything is in design units; {@link #setPixelsPerUnit} tells the helpers how many physical
 * pixels a unit is so textures are generated at exactly the on-screen resolution.
 */
public final class Render2D {
    private Render2D() {}

    private static double pixelsPerUnit = Scale.DESIGN_PX_PER_UNIT;
    private static boolean texturedCorners = true;

    public static void setPixelsPerUnit(double value) {
        pixelsPerUnit = Math.max(0.25, value);
    }

    public static double pixelsPerUnit() {
        return pixelsPerUnit;
    }

    /** False switches every rounded shape to the per-row fill fallback (Settings → Appearance). */
    public static void setTexturedCorners(boolean value) {
        texturedCorners = value;
    }

    public static boolean texturedCorners() {
        return texturedCorners;
    }

    static int px(double units) {
        return Scale.pixels(units, pixelsPerUnit);
    }

    // ---- fills ----------------------------------------------------------------------------------

    public static void fill(GuiGraphics g, int x, int y, int w, int h, int color) {
        if (w <= 0 || h <= 0 || Colors.alpha(color) == 0) return;
        g.fill(x, y, x + w, y + h, color);
    }

    /** Legacy 1-unit-radius rectangle used by HUD modules. */
    public static void rounded(GuiGraphics g, int x, int y, int w, int h, int color) {
        if (w <= 2 || h <= 2) {
            fill(g, x, y, w, h, color);
            return;
        }
        g.fill(x + 1, y, x + w - 1, y + h, color);
        g.fill(x, y + 1, x + 1, y + h - 1, color);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    /** Horizontal inset of a quarter-circle corner at row {@code i} (0 = outermost row); fallback path. */
    static int cornerInset(int r, int i) {
        double d = r - 0.5 - i;
        double inside = r * r - d * d;
        if (inside <= 0) return r;
        return (int) Math.round(r - Math.sqrt(inside));
    }

    /** Filled rectangle with circular corners of radius {@code r} (design units). */
    public static void roundedRect(GuiGraphics g, int x, int y, int w, int h, int r, int color) {
        if (w <= 0 || h <= 0 || Colors.alpha(color) == 0) return;
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        if (r <= 1) {
            if (r == 1) rounded(g, x, y, w, h, color);
            else fill(g, x, y, w, h, color);
            return;
        }
        if (!texturedCorners) {
            roundedRectFallback(g, x, y, w, h, r, color);
            return;
        }
        int radiusPx = px(r);
        int wPx = px(w);
        int hPx = px(h);
        if (wPx <= 256 && hPx <= 256) {
            // Small boxes (every HUD element, rows, buttons) are one cached texture: one draw.
            Identifier box = RoundedTextures.box(wPx, hPx, radiusPx);
            g.blit(RenderPipelines.GUI_TEXTURED, box, x, y, 0f, 0f, w, h, wPx, hPx, wPx, hPx, color);
            return;
        }
        Identifier tex = RoundedTextures.disc(radiusPx);
        corners(g, tex, x, y, w, h, r, radiusPx, color);
        fill(g, x + r, y, w - 2 * r, r, color);
        fill(g, x, y + r, w, h - 2 * r, color);
        fill(g, x + r, y + h - r, w - 2 * r, r, color);
    }

    /** 1-unit outline following the same rounded shape. */
    public static void roundedOutline(GuiGraphics g, int x, int y, int w, int h, int r, int color) {
        if (w <= 0 || h <= 0 || Colors.alpha(color) == 0) return;
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        if (r <= 1) {
            g.renderOutline(x, y, w, h, color);
            return;
        }
        if (!texturedCorners) {
            roundedOutlineFallback(g, x, y, w, h, r, color);
            return;
        }
        int radiusPx = px(r);
        Identifier tex = RoundedTextures.ring(radiusPx, px(1));
        corners(g, tex, x, y, w, h, r, radiusPx, color);
        fill(g, x + r, y, w - 2 * r, 1, color);
        fill(g, x + r, y + h - 1, w - 2 * r, 1, color);
        fill(g, x, y + r, 1, h - 2 * r, color);
        fill(g, x + w - 1, y + r, 1, h - 2 * r, color);
    }

    /** Blits the four quadrants of a {@code 2 * radiusPx} square texture as the corners. */
    private static void corners(GuiGraphics g, Identifier tex, int x, int y, int w, int h, int r, int radiusPx, int color) {
        int size = radiusPx * 2;
        corner(g, tex, x, y, 0, 0, r, radiusPx, size, color);
        corner(g, tex, x + w - r, y, radiusPx, 0, r, radiusPx, size, color);
        corner(g, tex, x, y + h - r, 0, radiusPx, r, radiusPx, size, color);
        corner(g, tex, x + w - r, y + h - r, radiusPx, radiusPx, r, radiusPx, size, color);
    }

    private static void corner(GuiGraphics g, Identifier tex, int x, int y, int u, int v, int r, int radiusPx, int size, int color) {
        g.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, (float) u, (float) v, r, r, radiusPx, radiusPx, size, size, color);
    }

    static void roundedRectFallback(GuiGraphics g, int x, int y, int w, int h, int r, int color) {
        for (int i = 0; i < r; i++) {
            int inset = cornerInset(r, i);
            g.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
            g.fill(x + inset, y + h - 1 - i, x + w - inset, y + h - i, color);
        }
        g.fill(x, y + r, x + w, y + h - r, color);
    }

    static void roundedOutlineFallback(GuiGraphics g, int x, int y, int w, int h, int r, int color) {
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

    /** Rounded fill with a 1-unit border of {@code border}. */
    public static void panel(GuiGraphics g, int x, int y, int w, int h, int r, int fill, int border) {
        roundedRect(g, x, y, w, h, r, fill);
        roundedOutline(g, x, y, w, h, r, border);
    }

    /** Legacy panel with a shadow (tooltips). */
    public static void panel(GuiGraphics g, int x, int y, int w, int h, int fill, int border) {
        rounded(g, x + 1, y + 2, w, h, Theme.shadow());
        rounded(g, x, y, w, h, fill);
        outline(g, x, y, w, h, border);
    }

    /** Soft drop shadow under a rounded shape. */
    public static void shadow(GuiGraphics g, int x, int y, int w, int h, int r, double strength) {
        roundedRect(g, x + 1, y + 3, w, h, r, Colors.fade(Theme.shadow(), strength));
    }

    public static void outline(GuiGraphics g, int x, int y, int w, int h, int color) {
        if (w <= 0 || h <= 0) return;
        g.renderOutline(x, y, w, h, color);
    }

    public static void circle(GuiGraphics g, int cx, int cy, int radius, int color) {
        roundedRect(g, cx - radius, cy - radius, radius * 2, radius * 2, radius, color);
    }

    // ---- controls -------------------------------------------------------------------------------

    /**
     * Toggle switch (docs/DESIGN.md): quiet neutral track when off, accent track when on, round
     * knob; {@code knob} is the animated 0..1 position. Never the loudest thing in a row.
     */
    public static void toggle(GuiGraphics g, int x, int y, int w, int h, float knob, boolean on, boolean focused) {
        int track = Colors.mix(Theme.control(), Theme.accent(), knob);
        roundedRect(g, x, y, w, h, h / 2, track);
        if (focused) roundedOutline(g, x - 2, y - 2, w + 4, h + 4, h / 2 + 2, Theme.accentAlpha(0xA0));
        else if (knob < 0.5f) roundedOutline(g, x, y, w, h, h / 2, Theme.line());
        int inset = 2;
        int kd = h - inset * 2;
        int kx = x + inset + Math.round(knob * (w - inset * 2 - kd));
        roundedRect(g, kx, y + inset, kd, kd, kd / 2, Colors.mix(Theme.subtle(), Theme.accentText(), knob));
    }

    /** Slider track with the filled portion in accent and a round knob, centred in a box {@code h} tall. */
    public static void slider(GuiGraphics g, int x, int y, int w, int h, double fraction, boolean active, boolean focused) {
        int trackH = 2;
        int ty = y + (h - trackH) / 2;
        roundedRect(g, x, ty, w, trackH, trackH / 2, Theme.control());
        int fw = (int) Math.round(w * Math.max(0, Math.min(1, fraction)));
        if (fw > 0) roundedRect(g, x, ty, fw, trackH, trackH / 2, Theme.accent());
        int knobD = 12;
        int kx = x + fw - knobD / 2;
        kx = Math.max(x - 2, Math.min(x + w - knobD + 2, kx));
        int ky = ty + trackH / 2 - knobD / 2;
        if (focused || active) circle(g, kx + knobD / 2, ky + knobD / 2, knobD / 2 + 3, 0x30FFFFFF);
        roundedRect(g, kx, ky, knobD, knobD, knobD / 2, active ? 0xFFFFFFFF : Theme.text());
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

    // ---- legacy vanilla-font text helpers (HUD fallbacks) --------------------------------------

    public static void text(GuiGraphics g, Font font, String text, int x, int y, int color, boolean shadow) {
        g.drawString(font, text, x, y, color, shadow);
    }

    public static void textCentered(GuiGraphics g, Font font, String text, int cx, int y, int color, boolean shadow) {
        g.drawString(font, text, cx - font.width(text) / 2, y, color, shadow);
    }

    public static void textRight(GuiGraphics g, Font font, String text, int right, int y, int color, boolean shadow) {
        g.drawString(font, text, right - font.width(text), y, color, shadow);
    }

    public static void textClipped(GuiGraphics g, Font font, String text, int x, int y, int maxWidth, int color, boolean shadow) {
        if (maxWidth <= 0) return;
        String shown = text;
        if (font.width(shown) > maxWidth) {
            int ell = font.width("…");
            shown = font.plainSubstrByWidth(text, Math.max(0, maxWidth - ell)) + "…";
        }
        g.drawString(font, shown, x, y, color, shadow);
    }

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

    // ---- geometry and motion --------------------------------------------------------------------

    public static boolean hovered(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    /** Frame-rate independent easing toward a target (legacy, tick-based). */
    public static float approach(float current, float target, float speed, float deltaTicks) {
        float t = Math.min(1f, speed * Math.max(0f, deltaTicks));
        float next = current + (target - current) * t;
        return Math.abs(target - next) < 0.002f ? target : next;
    }

    /**
     * Time-based motion: moves {@code current} toward {@code target} so a full 0..1 trip takes
     * {@code durationMs}; snaps when "Reduce motion" is on. Apply {@link #easeOut} or
     * {@link #easeInOut} to the result for the curve.
     */
    public static float step(float current, float target, float dtSeconds, float durationMs) {
        if (Theme.reduceMotion() || durationMs <= 0f) return target;
        float rate = Math.max(0f, dtSeconds) * 1000f / durationMs;
        float delta = target - current;
        if (Math.abs(delta) <= rate) return target;
        return current + Math.signum(delta) * rate;
    }

    public static float easeOut(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return 1f - (float) Math.pow(1f - t, 3);
    }

    public static float easeInOut(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return t * t * (3f - 2f * t);
    }
}
