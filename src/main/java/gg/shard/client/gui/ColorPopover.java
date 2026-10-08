package gg.shard.client.gui;

import gg.shard.client.module.setting.ColorSetting;
import gg.shard.client.util.Colors;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

/**
 * Colour picker: saturation/brightness square, hue bar, optional alpha bar, hex field, live
 * swatch and presets, laid out on the 4/8/12/16 grid with smooth round markers. The maths lives
 * in {@link Colors#toHsb} / {@link Colors#hsba} so it is tested.
 */
final class ColorPopover extends Popover {
    private static final int PAD = 16;
    private static final int GAP = 12;
    private static final int SQUARE_W = 168;
    private static final int SQUARE_H = 112;
    private static final int HUE_W = 16;
    private static final int BAR_H = 14;
    private static final int PRESET = 22;
    private static final int PRESET_GAP = 8;
    private static final int[] PRESETS = {0xFF22D3EE, 0xFFA78BFA, 0xFF34D399, 0xFFFB7185, 0xFFFBBF24, 0xFF93C5FD, 0xFFE879F9, 0xFFFFFFFF, 0xFF000000};

    private enum Drag { NONE, SQUARE, HUE, ALPHA }

    private final ColorSetting setting;
    private double hue;
    private double sat;
    private double bri;
    private int alpha;
    private Drag drag = Drag.NONE;
    private final TextInput hex = new TextInput(9);
    private boolean hexFocused;

    private final int squareX;
    private final int squareY;
    private final int hueX;
    private final int alphaY;
    private final int hexY;
    private final int presetY;
    private final int presetsPerRow;

    ColorPopover(ColorSetting setting, int anchorX, int anchorBottom, int anchorTop, int screenW, int screenH) {
        this.setting = setting;
        double[] hsb = Colors.toHsb(setting.get());
        hue = hsb[0];
        sat = hsb[1];
        bri = hsb[2];
        alpha = setting.alpha();
        w = PAD + SQUARE_W + GAP + HUE_W + PAD;
        int inner = w - PAD * 2;
        presetsPerRow = Math.max(1, (inner + PRESET_GAP) / (PRESET + PRESET_GAP));
        int presetRows = (PRESETS.length + presetsPerRow - 1) / presetsPerRow;
        int cursorY = PAD + SQUARE_H + GAP;
        if (setting.allowAlpha()) {
            alphaY = cursorY;
            cursorY += BAR_H + GAP;
        } else {
            alphaY = -1;
        }
        hexY = cursorY;
        cursorY += TextInput.HEIGHT + GAP;
        presetY = cursorY;
        cursorY += presetRows * PRESET + (presetRows - 1) * PRESET_GAP + PAD;
        h = cursorY;
        x = anchorX;
        y = anchorBottom + 4;
        clampTo(screenW, screenH, anchorTop);
        squareX = x + PAD;
        squareY = y + PAD;
        hueX = squareX + SQUARE_W + GAP;
        hex.sync(Colors.toHex(setting.get()));
        hex.onCommit(this::applyHex);
    }

    private int preview() {
        return Colors.hsba(hue, sat, bri, setting.allowAlpha() ? alpha : 0xFF);
    }

    private void apply() {
        setting.set(preview());
        if (!hexFocused) hex.sync(Colors.toHex(setting.get()));
    }

    private void applyHex() {
        Integer parsed = Colors.parseHex(hex.value());
        if (parsed == null) {
            hex.sync(Colors.toHex(setting.get()));
            return;
        }
        double[] hsb = Colors.toHsb(parsed);
        hue = hsb[0];
        sat = hsb[1];
        bri = hsb[2];
        alpha = Colors.alpha(parsed);
        apply();
    }

    private int barW() {
        return SQUARE_W + GAP + HUE_W;
    }

    @Override
    void render(GuiGraphics g, int mouseX, int mouseY, float dtSeconds) {
        advance(dtSeconds);
        float a = alpha();
        int r = Theme.radius();
        Render2D.shadow(g, x, y, w, h, r, 0.6 * a);
        Render2D.panel(g, x, y, w, h, r, Colors.fade(Theme.popover(), a), Colors.fade(Theme.lineStrong(), a));

        // Saturation / brightness square: white -> hue across, fading to black downwards.
        int hueColor = Colors.hsb(hue, 1, 1);
        for (int cx = 0; cx < SQUARE_W; cx++) {
            int top = Colors.mix(0xFFFFFFFF, hueColor, cx / (double) (SQUARE_W - 1));
            g.fillGradient(squareX + cx, squareY, squareX + cx + 1, squareY + SQUARE_H, Colors.fade(top, a), Colors.fade(0xFF000000, a));
        }
        Render2D.roundedOutline(g, squareX - 1, squareY - 1, SQUARE_W + 2, SQUARE_H + 2, 4, Colors.fade(Theme.lineStrong(), a));
        int mx = squareX + (int) Math.round(sat * (SQUARE_W - 1));
        int my = squareY + (int) Math.round((1 - bri) * (SQUARE_H - 1));
        Render2D.circle(g, mx, my, 7, Colors.fade(0xFFFFFFFF, a));
        Render2D.circle(g, mx, my, 5, Colors.fade(preview() | 0xFF000000, a));

        // Hue bar.
        for (int ry = 0; ry < SQUARE_H; ry++) {
            g.fill(hueX, squareY + ry, hueX + HUE_W, squareY + ry + 1, Colors.fade(Colors.hsb(ry / (double) (SQUARE_H - 1), 1, 1), a));
        }
        Render2D.roundedOutline(g, hueX - 1, squareY - 1, HUE_W + 2, SQUARE_H + 2, 4, Colors.fade(Theme.lineStrong(), a));
        int hy = squareY + (int) Math.round(hue * (SQUARE_H - 1));
        Render2D.roundedRect(g, hueX - 3, hy - 4, HUE_W + 6, 8, 4, Colors.fade(0xFFFFFFFF, a));
        Render2D.roundedRect(g, hueX - 1, hy - 2, HUE_W + 2, 4, 2, Colors.fade(Colors.hsb(hue, 1, 1), a));

        // Alpha bar.
        if (alphaY >= 0) {
            int ax = squareX;
            int ay = y + alphaY;
            int aw = barW();
            Render2D.checker(g, ax, ay, aw, BAR_H, 7);
            int opaque = Colors.hsb(hue, sat, bri);
            for (int cx = 0; cx < aw; cx++) {
                int alphaAt = (int) Math.round(255.0 * cx / (aw - 1));
                g.fill(ax + cx, ay, ax + cx + 1, ay + BAR_H, Colors.fade(Colors.withAlpha(opaque, alphaAt), a));
            }
            Render2D.roundedOutline(g, ax - 1, ay - 1, aw + 2, BAR_H + 2, 4, Colors.fade(Theme.lineStrong(), a));
            int kx = ax + (int) Math.round(alpha / 255.0 * (aw - 1));
            Render2D.roundedRect(g, kx - 4, ay - 3, 8, BAR_H + 6, 4, Colors.fade(0xFFFFFFFF, a));
            Render2D.roundedRect(g, kx - 2, ay - 1, 4, BAR_H + 2, 2, Colors.fade(Colors.withAlpha(opaque, alpha), a));
        }

        // Hex field and live swatch.
        int fy = y + hexY;
        int fieldW = 112;
        hex.render(g, squareX, fy, fieldW, TextInput.HEIGHT, hexFocused);
        int swX = squareX + fieldW + GAP;
        int swW = x + w - PAD - swX;
        Render2D.checker(g, swX, fy, swW, TextInput.HEIGHT, 7);
        Render2D.roundedRect(g, swX, fy, swW, TextInput.HEIGHT, Theme.radiusSmall(), preview());
        Render2D.roundedOutline(g, swX, fy, swW, TextInput.HEIGHT, Theme.radiusSmall(), Colors.fade(Theme.lineStrong(), a));

        // Presets.
        for (int i = 0; i < PRESETS.length; i++) {
            int px = presetX(i);
            int py = presetY(i);
            boolean hover = Render2D.hovered(mouseX, mouseY, px, py, PRESET, PRESET);
            Render2D.roundedRect(g, px, py, PRESET, PRESET, 6, Colors.fade(PRESETS[i], a));
            Render2D.roundedOutline(g, px, py, PRESET, PRESET, 6, Colors.fade(hover ? Theme.accent() : Theme.lineStrong(), a));
        }
    }

    private int presetX(int i) {
        return squareX + (i % presetsPerRow) * (PRESET + PRESET_GAP);
    }

    private int presetY(int i) {
        return y + presetY + (i / presetsPerRow) * (PRESET + PRESET_GAP);
    }

    private boolean inSquare(double mx, double my) {
        return Render2D.hovered(mx, my, squareX - 4, squareY - 4, SQUARE_W + 8, SQUARE_H + 8);
    }

    private boolean inHue(double mx, double my) {
        return Render2D.hovered(mx, my, hueX - 6, squareY - 4, HUE_W + 12, SQUARE_H + 8);
    }

    private boolean inAlpha(double mx, double my) {
        return alphaY >= 0 && Render2D.hovered(mx, my, squareX - 4, y + alphaY - 6, barW() + 8, BAR_H + 12);
    }

    private void dragTo(double mx, double my) {
        switch (drag) {
            case SQUARE -> {
                sat = clamp((mx - squareX) / (SQUARE_W - 1));
                bri = 1 - clamp((my - squareY) / (SQUARE_H - 1));
                apply();
            }
            case HUE -> {
                hue = clamp((my - squareY) / (SQUARE_H - 1));
                apply();
            }
            case ALPHA -> {
                alpha = (int) Math.round(clamp((mx - squareX) / (barW() - 1)) * 255);
                apply();
            }
            default -> {
            }
        }
    }

    private static double clamp(double v) {
        return Math.max(0, Math.min(1, v));
    }

    @Override
    boolean mouseClicked(double mx, double my, int button) {
        hexFocused = false;
        if (inSquare(mx, my)) drag = Drag.SQUARE;
        else if (inHue(mx, my)) drag = Drag.HUE;
        else if (inAlpha(mx, my)) drag = Drag.ALPHA;
        else drag = Drag.NONE;
        if (drag != Drag.NONE) {
            dragTo(mx, my);
            return true;
        }
        if (Render2D.hovered(mx, my, squareX, y + hexY, 112, TextInput.HEIGHT)) {
            hexFocused = true;
            hex.clickAt(squareX, 112, mx);
            return true;
        }
        for (int i = 0; i < PRESETS.length; i++) {
            if (Render2D.hovered(mx, my, presetX(i), presetY(i), PRESET, PRESET)) {
                double[] hsb = Colors.toHsb(PRESETS[i]);
                hue = hsb[0];
                sat = hsb[1];
                bri = hsb[2];
                apply();
                return true;
            }
        }
        return contains(mx, my);
    }

    @Override
    boolean mouseDragged(double mx, double my) {
        if (drag == Drag.NONE) return false;
        dragTo(mx, my);
        return true;
    }

    @Override
    boolean mouseReleased() {
        boolean was = drag != Drag.NONE;
        drag = Drag.NONE;
        return was;
    }

    @Override
    boolean keyPressed(int key, int modifiers) {
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (hexFocused) {
                hexFocused = false;
                hex.sync(Colors.toHex(setting.get()));
            } else requestClose();
            return true;
        }
        if (hexFocused) return hex.keyPressed(key, modifiers);
        return false;
    }

    @Override
    boolean charTyped(String ch) {
        return hexFocused && hex.charTyped(ch);
    }
}
