package gg.shard.client.hud;

import gg.shard.client.ShardClient;
import gg.shard.client.gui.Render2D;
import gg.shard.client.gui.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/** Drag HUD elements around, scroll to scale, right-click to reset, arrows to nudge. */
public final class HudEditorScreen extends Screen {
    private static final int GRID = 4;
    private static final int SNAP = 6;

    private final Screen parent;
    private final HudManager hud;
    private HudModule dragging;
    private HudModule selected;
    private int dragOffsetX;
    private int dragOffsetY;

    public HudEditorScreen(Screen parent, HudManager hud) {
        super(Component.literal("Shard HUD editor"));
        this.parent = parent;
        this.hud = hud;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0x66000000);
        // Grid for alignment.
        for (int x = 0; x < width; x += GRID * 8) g.fill(x, 0, x + 1, height, 0x11FFFFFF);
        for (int y = 0; y < height; y += GRID * 8) g.fill(0, y, width, y + 1, 0x11FFFFFF);

        List<HudModule> list = hud.hudModules();
        HudModule hovered = at(mouseX, mouseY);
        for (HudModule m : list) {
            if (!m.isEnabled()) continue;
            HudManager.renderOne(g, minecraft.getDeltaTracker(), m);
            int x = m.pixelX(width);
            int y = m.pixelY(height);
            int w = m.scaledWidth();
            int h = m.scaledHeight();
            boolean active = m == selected || m == hovered;
            g.fill(x - 2, y - 2, x + w + 2, y + h + 2, active ? Theme.accentAlpha(0x30) : 0x18FFFFFF);
            Render2D.outline(g, x - 2, y - 2, w + 4, h + 4, active ? Theme.accent() : 0x55FFFFFF);
            if (active) {
                String label = m.name() + (m.scale() != 1.0 ? String.format("  %.2fx", m.scale()) : "");
                Render2D.text(g, font, label, x, y - 12, Theme.text(), true);
            }
        }

        String hint = "Drag to move  ·  Scroll to scale  ·  Right-click to reset  ·  Arrows to nudge  ·  Esc to finish";
        if (font.width(hint) > width - 16) hint = "Drag · Scroll = scale · Right-click = reset · Esc";
        Render2D.text(g, font, hint, (width - font.width(hint)) / 2, height - 14, Theme.muted(), true);
        String title = "HUD editor";
        Render2D.text(g, font, title, (width - font.width(title)) / 2, 6, Theme.text(), true);
    }

    private HudModule at(double mx, double my) {
        List<HudModule> list = hud.hudModules();
        for (int i = list.size() - 1; i >= 0; i--) {
            HudModule m = list.get(i);
            if (!m.isEnabled()) continue;
            int x = m.pixelX(width);
            int y = m.pixelY(height);
            if (mx >= x - 2 && mx <= x + m.scaledWidth() + 2 && my >= y - 2 && my <= y + m.scaledHeight() + 2) return m;
        }
        return null;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        HudModule m = at(event.x(), event.y());
        if (m == null) {
            selected = null;
            return super.mouseClicked(event, doubleClick);
        }
        selected = m;
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            m.resetPosition(0.01, 0.01);
            ShardClient.config().markDirty();
            return true;
        }
        dragging = m;
        dragOffsetX = (int) event.x() - m.pixelX(width);
        dragOffsetY = (int) event.y() - m.pixelY(height);
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (dragging == null) return super.mouseDragged(event, dx, dy);
        int nx = (int) event.x() - dragOffsetX;
        int ny = (int) event.y() - dragOffsetY;
        nx = Math.round((float) nx / GRID) * GRID;
        ny = Math.round((float) ny / GRID) * GRID;
        // Snap to screen edges and centre lines.
        int w = dragging.scaledWidth();
        int h = dragging.scaledHeight();
        if (Math.abs(nx) < SNAP) nx = 0;
        if (Math.abs(nx + w - width) < SNAP) nx = width - w;
        if (Math.abs(nx + w / 2 - width / 2) < SNAP) nx = width / 2 - w / 2;
        if (Math.abs(ny) < SNAP) ny = 0;
        if (Math.abs(ny + h - height) < SNAP) ny = height - h;
        if (Math.abs(ny + h / 2 - height / 2) < SNAP) ny = height / 2 - h / 2;
        dragging.setPixelPosition(nx, ny, width, height);
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging != null) {
            dragging = null;
            ShardClient.config().markDirty();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        HudModule m = at(mx, my);
        if (m == null) return super.mouseScrolled(mx, my, sx, sy);
        m.setScale(m.scale() + (sy > 0 ? 0.05 : -0.05));
        ShardClient.config().markDirty();
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (selected != null) {
            int dx = 0;
            int dy = 0;
            switch (event.key()) {
                case GLFW.GLFW_KEY_LEFT -> dx = -1;
                case GLFW.GLFW_KEY_RIGHT -> dx = 1;
                case GLFW.GLFW_KEY_UP -> dy = -1;
                case GLFW.GLFW_KEY_DOWN -> dy = 1;
                default -> {
                }
            }
            if (dx != 0 || dy != 0) {
                selected.setPixelPosition(selected.pixelX(width) + dx, selected.pixelY(height) + dy, width, height);
                ShardClient.config().markDirty();
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        ShardClient.config().save();
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
