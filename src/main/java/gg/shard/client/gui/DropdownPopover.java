package gg.shard.client.gui;

import gg.shard.client.module.setting.EnumSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

/** The list that opens under a dropdown control; keyboard and mouse both select. */
final class DropdownPopover extends Popover {
    private static final int ITEM_H = 14;
    private static final int MAX_VISIBLE = 8;

    private final EnumSetting<?> setting;
    private final Enum<?>[] values;
    private int highlight;
    private int scroll;

    DropdownPopover(EnumSetting<?> setting, int anchorX, int anchorBottom, int anchorTop, int minWidth, int screenW, int screenH) {
        this.setting = setting;
        this.values = setting.values();
        Font font = Minecraft.getInstance().font;
        int widest = minWidth;
        for (Enum<?> v : values) widest = Math.max(widest, font.width(EnumSetting.pretty(v)) + 22);
        this.w = widest;
        this.h = Math.min(values.length, MAX_VISIBLE) * ITEM_H + 8;
        this.x = anchorX;
        this.y = anchorBottom + 2;
        this.highlight = setting.get().ordinal();
        clampTo(screenW, screenH, anchorTop);
        ensureVisible();
    }

    private void ensureVisible() {
        if (highlight < scroll) scroll = highlight;
        if (highlight >= scroll + MAX_VISIBLE) scroll = highlight - MAX_VISIBLE + 1;
    }

    private int indexAt(double my) {
        int i = (int) ((my - y - 4) / ITEM_H) + scroll;
        return i >= 0 && i < values.length && i < scroll + MAX_VISIBLE ? i : -1;
    }

    @Override
    void render(GuiGraphics g, int mouseX, int mouseY, float dt) {
        fade = Render2D.approach(fade, 1f, 0.9f, dt);
        Font font = Minecraft.getInstance().font;
        int r = Theme.radiusSmall();
        Render2D.roundedRect(g, x + 1, y + 2, w, h, r, Theme.shadow());
        Render2D.panel(g, x, y, w, h, r, Theme.popover(), Theme.lineStrong());
        int hover = contains(mouseX, mouseY) ? indexAt(mouseY) : -1;
        if (hover >= 0) highlight = hover;
        g.enableScissor(x, y + 4, x + w, y + h - 4);
        for (int i = scroll; i < values.length && i < scroll + MAX_VISIBLE; i++) {
            int iy = y + 4 + (i - scroll) * ITEM_H;
            boolean current = values[i] == setting.get();
            if (i == highlight) Render2D.roundedRect(g, x + 3, iy, w - 6, ITEM_H, r - 1, Theme.controlHover());
            Render2D.text(g, font, EnumSetting.pretty(values[i]), x + 9, iy + 3, current ? Theme.accent() : Theme.text(), false);
            if (current) Glyphs.draw(g, "check", x + w - 20, iy - 1, Theme.accent());
        }
        g.disableScissor();
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
