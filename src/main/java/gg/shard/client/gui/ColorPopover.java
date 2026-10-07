package gg.shard.client.gui;

import gg.shard.client.module.setting.ColorSetting;
import gg.shard.client.util.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

/**
 * Colour picker: saturation/brightness square, hue bar, optional alpha bar, hex field and a
 * row of presets. The maths lives in {@link Colors#toHsb} / {@link Colors#hsba} so it is tested.
 */
final class ColorPopover extends Popover {
    private static final int SQUARE_W = 100;
    private static final int SQUARE_H = 60;
    private static final int HUE_W = 10;
    private static final int BAR_H = 8;
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

    ColorPopover(ColorSetting setting, int anchorX, int anchorBottom, int anchorTop, int screenW, int screenH) {
        this.setting = setting;
        double[] hsb = Colors.toHsb(setting.get());
        hue = hsb[0];
        sat = hsb[1];
        bri = hsb[2];
        alpha = setting.alpha();
        w = SQUARE_W + HUE_W + 30;
        int cursorY = 8 + SQUARE_H + 8;
        if (setting.allowAlpha()) {
            alphaY = cursorY;
            cursorY += BAR_H + 8;
        } else {
            alphaY = -1;
        }
        hexY = cursorY;
        cursorY += 16 + 8;
        presetY = cursorY;
        cursorY += 12 + 8;
        h = cursorY;
        x = anchorX + 0;
        y = anchorBottom + 2;
        clampTo(screenW, screenH, anchorTop);
        squareX = x + 8;
        squareY = y + 8;
        hueX = squareX + SQUARE_W + 6;
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

    @Override
    void render(GuiGraphics g, int mouseX, int mouseY, float dt) {
        fade = Render2D.approach(fade, 1f, 0.9f, dt);
        Font font = Minecraft.getInstance().font;
        int r = Theme.radius();
        Render2D.roundedRect(g, x + 1, y + 2, w, h, r, Theme.shadow());
        Render2D.panel(g, x, y, w, h, r, Theme.popover(), Theme.lineStrong());

        // Saturation / brightness square: white -> hue across, fading to black downwards.
        int hueColor = Colors.hsb(hue, 1, 1);
        for (int cx = 0; cx < SQUARE_W; cx++) {
            int top = Colors.mix(0xFFFFFFFF, hueColor, cx / (double) (SQUARE_W - 1));
            g.fillGradient(squareX + cx, squareY, squareX + cx + 1, squareY + SQUARE_H, top, 0xFF000000);
        }
        Render2D.roundedOutline(g, squareX - 1, squareY - 1, SQUARE_W + 2, SQUARE_H + 2, 2, Theme.lineStrong());
        int mx = squareX + (int) Math.round(sat * (SQUARE_W - 1));
        int my = squareY + (int) Math.round((1 - bri) * (SQUARE_H - 1));
        Render2D.circle(g, mx, my, 4, 0xFFFFFFFF);
        Render2D.circle(g, mx, my, 3, preview() | 0xFF000000);

        // Hue bar.
        for (int ry = 0; ry < SQUARE_H; ry++) {
            g.fill(hueX, squareY + ry, hueX + HUE_W, squareY + ry + 1, Colors.hsb(ry / (double) (SQUARE_H - 1), 1, 1));
        }
        Render2D.roundedOutline(g, hueX - 1, squareY - 1, HUE_W + 2, SQUARE_H + 2, 2, Theme.lineStrong());
        int hy = squareY + (int) Math.round(hue * (SQUARE_H - 1));
        g.fill(hueX - 2, hy - 1, hueX + HUE_W + 2, hy + 2, 0xFFFFFFFF);
        g.fill(hueX - 1, hy, hueX + HUE_W + 1, hy + 1, Colors.hsb(hue, 1, 1));

        // Alpha bar.
        if (alphaY >= 0) {
            int ax = squareX;
            int ay = y + alphaY;
            int aw = SQUARE_W + HUE_W + 6;
            Render2D.checker(g, ax, ay, aw, BAR_H, 4);
            int opaque = Colors.hsb(hue, sat, bri);
            for (int cx = 0; cx < aw; cx++) {
                int a = (int) Math.round(255.0 * cx / (aw - 1));
                g.fill(ax + cx, ay, ax + cx + 1, ay + BAR_H, Colors.withAlpha(opaque, a));
            }
            Render2D.roundedOutline(g, ax - 1, ay - 1, aw + 2, BAR_H + 2, 2, Theme.lineStrong());
            int kx = ax + (int) Math.round(alpha / 255.0 * (aw - 1));
            g.fill(kx - 1, ay - 2, kx + 2, ay + BAR_H + 2, 0xFFFFFFFF);
            g.fill(kx, ay - 1, kx + 1, ay + BAR_H + 1, Colors.withAlpha(opaque, alpha));
        }

        // Hex field and live swatch.
        int fy = y + hexY;
        hex.render(g, font, squareX, fy, 70, 16, hexFocused);
        int swX = squareX + 76;
        Render2D.checker(g, swX, fy, w - 8 - (swX - x), 16, 4);
        Render2D.roundedRect(g, swX, fy, w - 8 - (swX - x), 16, 3, preview());
        Render2D.roundedOutline(g, swX, fy, w - 8 - (swX - x), 16, 3, Theme.lineStrong());

        // Presets.
        int py = y + presetY;
        for (int i = 0; i < PRESETS.length; i++) {
            int px = squareX + i * 13;
            boolean hover = Render2D.hovered(mouseX, mouseY, px, py, 11, 11);
            Render2D.roundedRect(g, px, py, 11, 11, 3, PRESETS[i]);
            Render2D.roundedOutline(g, px, py, 11, 11, 3, hover ? Theme.accent() : Theme.lineStrong());
        }
    }

    private boolean inSquare(double mx, double my) {
        return Render2D.hovered(mx, my, squareX - 2, squareY - 2, SQUARE_W + 4, SQUARE_H + 4);
    }

    private boolean inHue(double mx, double my) {
        return Render2D.hovered(mx, my, hueX - 3, squareY - 2, HUE_W + 6, SQUARE_H + 4);
    }

    private boolean inAlpha(double mx, double my) {
        return alphaY >= 0 && Render2D.hovered(mx, my, squareX - 2, y + alphaY - 3, SQUARE_W + HUE_W + 10, BAR_H + 6);
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
                alpha = (int) Math.round(clamp((mx - squareX) / (SQUARE_W + HUE_W + 5)) * 255);
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
        if (Render2D.hovered(mx, my, squareX, y + hexY, 70, 16)) {
            hexFocused = true;
            hex.clickAt(Minecraft.getInstance().font, squareX, mx);
            return true;
        }
        for (int i = 0; i < PRESETS.length; i++) {
            if (Render2D.hovered(mx, my, squareX + i * 13, y + presetY, 11, 11)) {
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
