package gg.shard.client.gui;

import net.minecraft.client.gui.GuiGraphics;

/**
 * A module that draws a live preview in its settings panel header (Low Fire shows the flames,
 * Crosshair shows the crosshair on sample backgrounds and doubles as a pixel editor).
 * Coordinates are design units; return the height used, or 0 to draw nothing.
 */
public interface PanelPreview {
    int renderPreview(GuiGraphics g, int x, int y, int width);

    /** Pointer position before each {@link #renderPreview}, for hover states. */
    default void previewMouse(int x, int y) {}

    /**
     * A click ({@code drag} false) or a drag step inside the preview, in design units. Returns a
     * short message to show as a toast, or null.
     */
    default String previewInput(int x, int y, int button, boolean drag) {
        return null;
    }
}
