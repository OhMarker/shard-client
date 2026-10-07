package gg.shard.client.gui;

import net.minecraft.client.gui.GuiGraphics;

/**
 * A floating layer drawn above the settings page (dropdown lists, the colour picker). The
 * screen routes input here first while one is open and closes it on an outside click or Esc.
 */
abstract class Popover {
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

    /** Keeps the popover inside the screen, flipping above the anchor when needed. */
    protected void clampTo(int screenW, int screenH, int anchorTop) {
        if (x + w > screenW - 4) x = Math.max(4, screenW - 4 - w);
        if (x < 4) x = 4;
        if (y + h > screenH - 4) y = Math.max(4, anchorTop - h - 2);
    }

    abstract void render(GuiGraphics g, int mouseX, int mouseY, float dt);

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
