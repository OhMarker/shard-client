package gg.shard.client.gui;

import gg.shard.client.module.setting.EnumSetting;
import gg.shard.client.util.Colors;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

/** The list that opens under a dropdown control; 12 units of padding, current value highlighted. */
final class DropdownPopover extends Popover {
    static final int ITEM_H = 32;
    static final int PAD = 12;
    private static final int MAX_VISIBLE = 7;
    private static final Fonts.Weight WEIGHT = Fonts.Weight.MEDIUM;
    private static final int SIZE = 13;

    private final EnumSetting<?> setting;
    private final Enum<?>[] values;
    private int highlight;
    private int scroll;

    DropdownPopover(EnumSetting<?> setting, int anchorX, int anchorBottom, int anchorTop, int minWidth, int screenW, int screenH) {
        this.setting = setting;
        this.values = setting.values();
        int widest = minWidth;
        for (Enum<?> v : values) widest = Math.max(widest, Fonts.widthInt(EnumSetting.pretty(v), WEIGHT, SIZE) + PAD * 2 + 36);
        this.w = widest;
        this.h = Math.min(values.length, MAX_VISIBLE) * ITEM_H + PAD * 2;
        this.x = anchorX;
        this.y = anchorBottom + 4;
        this.highlight = setting.get().ordinal();
        clampTo(screenW, screenH, anchorTop);
        ensureVisible();
    }

    private void ensureVisible() {
        if (highlight < scroll) scroll = highlight;
        if (highlight >= scroll + MAX_VISIBLE) scroll = highlight - MAX_VISIBLE + 1;
    }

    private int indexAt(double my) {
        int i = (int) Math.floor((my - y - PAD) / ITEM_H) + scroll;
        return i >= 0 && i < values.length && i < scroll + MAX_VISIBLE && my >= y + PAD ? i : -1;
    }

    @Override
    void render(GuiGraphics g, int mouseX, int mouseY, float dtSeconds) {
        advance(dtSeconds);
        float a = alpha();
        int r = Theme.radius();
        Render2D.shadow(g, x, y, w, h, r, 0.6 * a);
        Render2D.panel(g, x, y, w, h, r, Colors.fade(Theme.popover(), a), Colors.fade(Theme.lineStrong(), a));
        int hover = contains(mouseX, mouseY) ? indexAt(mouseY) : -1;
        if (hover >= 0) highlight = hover;
        g.enableScissor(x, y + PAD, x + w, y + h - PAD);
        int lineH = Fonts.lineHeight(SIZE);
        for (int i = scroll; i < values.length && i < scroll + MAX_VISIBLE; i++) {
            int iy = y + PAD + (i - scroll) * ITEM_H;
            boolean current = values[i] == setting.get();
            if (i == highlight) Render2D.roundedRect(g, x + PAD / 2, iy, w - PAD, ITEM_H, Theme.radiusSmall(), Colors.fade(Theme.controlHover(), a));
            int color = current ? Theme.accent() : Theme.text();
            Fonts.draw(g, EnumSetting.pretty(values[i]), WEIGHT, SIZE, x + PAD + 4, iy + (ITEM_H - lineH) / 2, Colors.fade(color, a));
            if (current) Glyphs.draw(g, "check", x + w - PAD - 20, iy + (ITEM_H - 16) / 2, Colors.fade(Theme.accent(), a));
        }
        g.disableScissor();
        if (values.length > MAX_VISIBLE) {
            // Scroll thumb.
            int trackH = h - PAD * 2;
            int thumbH = Math.max(16, trackH * MAX_VISIBLE / values.length);
            int thumbY = y + PAD + (int) ((trackH - thumbH) * (scroll / (double) (values.length - MAX_VISIBLE)));
            Render2D.roundedRect(g, x + w - 5, thumbY, 3, thumbH, 1, Colors.fade(Theme.lineStrong(), a));
        }
    }

    @Override
    boolean mouseClicked(double mx, double my, int button) {
        int i = indexAt(my);
        if (i >= 0) {
            select(i);
            return true;
        }
        return contains(mx, my);
    }

    private void select(int i) {
        selectValue(setting, values[i]);
        requestClose();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void selectValue(EnumSetting setting, Enum<?> value) {
        setting.set(value);
    }

    @Override
    boolean mouseScrolled(double mx, double my, double delta) {
        int max = Math.max(0, values.length - MAX_VISIBLE);
        scroll = Math.max(0, Math.min(max, scroll - (int) Math.signum(delta)));
        return true;
    }

    @Override
    boolean keyPressed(int key, int modifiers) {
        switch (key) {
            case GLFW.GLFW_KEY_UP -> highlight = Math.floorMod(highlight - 1, values.length);
            case GLFW.GLFW_KEY_DOWN -> highlight = Math.floorMod(highlight + 1, values.length);
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER, GLFW.GLFW_KEY_SPACE -> {
                select(highlight);
                return true;
            }
            case GLFW.GLFW_KEY_ESCAPE -> {
                requestClose();
                return true;
            }
            default -> {
                return false;
            }
        }
        ensureVisible();
        return true;
    }
}
