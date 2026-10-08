package gg.shard.client.gui;

import net.minecraft.client.gui.GuiGraphics;

/**
 * A floating layer drawn above the settings page (dropdown lists, the colour picker, confirm
 * dialogs). Coordinates are design units. The screen routes input here first while one is
 * open and closes it on an outside click or Esc. Fades in over 150 ms unless motion is reduced.
 */
abstract class Popover {
    static final float FADE_MS = 150f;

    protected int x;
    protected int y;
    protected int w;
    protected int h;
    protected float fade;
    private boolean closeRequested;

    boolean contains(double mx, double my) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    void requestClose() {
        closeRequested = true;
    }

    boolean wantsClose() {
        return closeRequested;
    }

    /** True while the popover is modal: outside clicks are swallowed instead of closing it. */
    boolean modal() {
        return false;
    }

    /** Keeps the popover inside the screen, flipping above the anchor when needed. */
    protected void clampTo(int screenW, int screenH, int anchorTop) {
        if (x + w > screenW - 8) x = Math.max(8, screenW - 8 - w);
        if (x < 8) x = 8;
        if (y + h > screenH - 8) y = Math.max(8, anchorTop - h - 4);
    }

    protected void advance(float dtSeconds) {
        fade = Render2D.step(fade, 1f, dtSeconds, FADE_MS);
    }

    protected float alpha() {
        return Render2D.easeOut(fade);
    }

    abstract void render(GuiGraphics g, int mouseX, int mouseY, float dtSeconds);

    boolean mouseClicked(double mx, double my, int button) {
        return false;
    }

    boolean mouseDragged(double mx, double my) {
        return false;
    }

    boolean mouseReleased() {
        return false;
    }

    boolean mouseScrolled(double mx, double my, double delta) {
        return false;
    }

    boolean keyPressed(int key, int modifiers) {
        return false;
    }

    boolean charTyped(String ch) {
        return false;
    }
}
