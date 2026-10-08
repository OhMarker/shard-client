package gg.shard.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Shard's UI typography. Inter (OFL-1.1, {@code assets/shard/font/}) is exposed as one
 * Minecraft TTF font per weight and size ({@code shard:ui-<weight>-<size>}, see
 * {@code assets/shard/font/*.json}); every provider falls back to {@code minecraft:default}
 * for glyphs Inter lacks (hearts, shields, arrows), so nothing ever renders as a box.
 *
 * <p>Sizes are design units (see {@link Scale}): the em of a size-13 font is 13 units tall. The
 * fonts are rasterised at {@link Scale#DESIGN_PX_PER_UNIT} pixels per unit ("oversample"), which
 * is exactly the on-screen density at 100% interface size, so edges stay smooth at every GUI
 * scale. Vanilla's {@code font.lineHeight} (9) is never used for layout; {@link #lineHeight},
 * {@link #capHeight} and {@link #baseline} come from Inter's own metrics.
 *
 * <p>With the "Vanilla" font option every call falls back to the bitmap font at 1x (2x for
 * titles), centred in the same line boxes, so layouts do not move.
 */
public final class Fonts {
    private Fonts() {}

    public enum Weight {
        REGULAR("regular"), MEDIUM("medium"), SEMIBOLD("semibold");

        final String id;

        Weight(String id) {
            this.id = id;
        }
    }

    /** Sizes that have a font definition; other sizes snap to the nearest one. */
    public static final int[] SIZES = {10, 11, 12, 13, 14, 18};
    /** Inter 4.1 metrics relative to the em. */
    static final double CAP_HEIGHT = 0.7275;
    static final double ASCENT = 0.9688;
    static final double DESCENT = 0.2412;
    private static final double LINE = 1.25;
    private static final String ELLIPSIS = "…";

    private static final Map<Integer, Style> STYLES = new HashMap<>();
    private static boolean smooth = true;

    /** Switches between Inter and the vanilla bitmap font (Settings → Appearance → Font). */
    public static void setSmooth(boolean value) {
        smooth = value;
    }

    public static boolean smooth() {
        return smooth;
    }

    public static int nearestSize(int size) {
        int best = SIZES[0];
        for (int s : SIZES) if (Math.abs(s - size) < Math.abs(best - size)) best = s;
        return best;
    }

    public static Identifier id(Weight weight, int size) {
        return Identifier.fromNamespaceAndPath("shard", "ui-" + weight.id + "-" + nearestSize(size));
    }

    public static Style style(Weight weight, int size) {
        int key = weight.ordinal() * 100 + nearestSize(size);
        return STYLES.computeIfAbsent(key, k -> Style.EMPTY.withFont(new FontDescription.Resource(id(weight, size))));
    }

    /** A component that draws in the requested weight and size (plain text in vanilla mode). */
    public static Component text(String text, Weight weight, int size) {
        String s = text == null ? "" : text;
        return smooth ? Component.literal(s).withStyle(style(weight, size)) : Component.literal(s);
    }

    private static Font font() {
        return Minecraft.getInstance().font;
    }

    /** Vanilla fallback draws the 8px bitmap font at 1x, or 2x for titles. */
    private static int vanillaScale(int size) {
        return size >= 16 ? 2 : 1;
    }

    // ---- metrics ------------------------------------------------------------------------------

    public static float width(String text, Weight weight, int size) {
        if (text == null || text.isEmpty()) return 0f;
        if (!smooth) return font().width(text) * vanillaScale(size);
        return font().getSplitter().stringWidth(FormattedText.of(text, style(weight, size)));
    }

    public static int widthInt(String text, Weight weight, int size) {
        return (int) Math.ceil(width(text, weight, size));
    }

    /** Height of one line box for this size (the layout grid uses this, never font.lineHeight). */
    public static int lineHeight(int size) {
        return smooth ? (int) Math.round(size * LINE) : 9 * vanillaScale(size) + 2;
    }

    public static int capHeight(int size) {
        return smooth ? (int) Math.round(size * CAP_HEIGHT) : 7 * vanillaScale(size);
    }

    /** Baseline offset from the top of the line box, placing capitals in the vertical centre. */
    public static int baseline(int size) {
        if (smooth) return (int) Math.round((lineHeight(size) + size * CAP_HEIGHT) / 2.0);
        int k = vanillaScale(size);
        return (lineHeight(size) - 8 * k) / 2 + 7 * k;
    }

    // ---- drawing (y is the top of a lineHeight(size) line box) ------------------------------

    public static void draw(GuiGraphics g, String text, Weight weight, int size, int x, int y, int color) {
        draw(g, text, weight, size, x, y, color, false);
    }

    /** HUD modules may ask for vanilla's drop shadow; the settings page never does. */
    public static void draw(GuiGraphics g, String text, Weight weight, int size, int x, int y, int color, boolean shadow) {
        if (text == null || text.isEmpty() || (color >>> 24) == 0) return;
        if (smooth) {
            // Vanilla places the baseline 7 units below the y it is given.
            g.drawString(font(), text(text, weight, size), x, y + baseline(size) - 7, color, shadow);
            return;
        }
        int k = vanillaScale(size);
        int top = y + (lineHeight(size) - 8 * k) / 2;
        if (k == 1) {
            g.drawString(font(), text, x, top, color, shadow);
            return;
        }
        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        pose.translate(x, top);
        pose.scale(k, k);
        g.drawString(font(), text, 0, 0, color, shadow);
        pose.popMatrix();
    }

    public static void drawCentered(GuiGraphics g, String text, Weight weight, int size, int centerX, int y, int color) {
        draw(g, text, weight, size, centerX - Math.round(width(text, weight, size) / 2f), y, color);
    }

    public static void drawRight(GuiGraphics g, String text, Weight weight, int size, int right, int y, int color) {
        draw(g, text, weight, size, right - Math.round(width(text, weight, size)), y, color);
    }

    /** Draws {@code text} trimmed with an ellipsis so it never exceeds {@code maxWidth}. */
    public static void drawClipped(GuiGraphics g, String text, Weight weight, int size, int x, int y, int maxWidth, int color) {
        if (maxWidth <= 0) return;
        draw(g, clip(text, weight, size, maxWidth), weight, size, x, y, color);
    }

    public static String clip(String text, Weight weight, int size, int maxWidth) {
        if (text == null) return "";
        if (width(text, weight, size) <= maxWidth) return text;
        int room = (int) Math.max(0, maxWidth - width(ELLIPSIS, weight, size));
        String head;
        if (!smooth) head = font().plainSubstrByWidth(text, room / vanillaScale(size));
        else head = font().getSplitter().plainHeadByWidth(text, room, style(weight, size));
        return head.stripTrailing() + ELLIPSIS;
    }

    /** Word-wraps plain text; paragraphs ("\n") are respected and long words are split by width. */
    public static List<String> wrap(String text, Weight weight, int size, int maxWidth) {
        List<String> out = new ArrayList<>();
        if (text == null || text.isEmpty() || maxWidth <= 0) return out;
        Style style = smooth ? style(weight, size) : Style.EMPTY;
        int width = smooth ? maxWidth : Math.max(1, maxWidth / vanillaScale(size));
        for (String paragraph : text.split("\n")) {
            if (paragraph.isBlank()) {
                out.add("");
                continue;
            }
            for (FormattedText line : font().getSplitter().splitLines(paragraph, width, style)) out.add(line.getString());
        }
        return out;
    }
}
