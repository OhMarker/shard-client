package gg.shard.client.gui;

import net.minecraft.client.gui.GuiGraphics;

/**
 * A module that can draw a live preview in its settings panel header (Low Fire shows the
 * flames at the configured height and opacity). Coordinates are design units; return the
 * height used, or 0 to draw nothing.
 */
public interface PanelPreview {
    int renderPreview(GuiGraphics g, int x, int y, int width);
}
