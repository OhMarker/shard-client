package gg.shard.client.gui;

import gg.shard.client.util.Colors;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/** A modal confirmation card centred on the page: title, message, Cancel and one action. */
final class ConfirmPopover extends Popover {
    private static final int PAD = 20;
    private static final int BUTTON_H = 32;

    private final String title;
    private final List<String> lines;
    private final String confirmLabel;
    private final boolean danger;
    private final Runnable onConfirm;
    private final int messageW;

    ConfirmPopover(String title, String message, String confirmLabel, boolean danger, Runnable onConfirm, int screenW, int screenH) {
        this.title = title;
        this.confirmLabel = confirmLabel;
        this.danger = danger;
        this.onConfirm = onConfirm;
        this.w = Math.min(Math.max(240, screenW - 48), 420);
        this.messageW = w - PAD * 2;
        this.lines = Fonts.wrap(message, Fonts.Weight.REGULAR, 12, messageW);
        int textH = Fonts.lineHeight(14) + 6 + lines.size() * Fonts.lineHeight(12);
        this.h = PAD + textH + 20 + BUTTON_H + PAD;
        this.x = (screenW - w) / 2;
        this.y = Math.max(16, (screenH - h) / 2);
    }

    @Override
    boolean modal() {
        return true;
    }

    private int buttonsY() {
        return y + h - PAD - BUTTON_H;
    }

    private int cancelX() {
        return x + w - PAD - buttonW() * 2 - 8;
    }

    private int confirmX() {
        return x + w - PAD - buttonW();
    }

    private int buttonW() {
        int widest = Math.max(Fonts.widthInt("Cancel", Fonts.Weight.MEDIUM, 13), Fonts.widthInt(confirmLabel, Fonts.Weight.MEDIUM, 13));
        return Math.max(96, widest + 32);
    }

    @Override
    void render(GuiGraphics g, int mouseX, int mouseY, float dtSeconds) {
        advance(dtSeconds);
        float a = alpha();
        int r = Theme.radius();
        Render2D.shadow(g, x, y, w, h, r, 0.7 * a);
        Render2D.panel(g, x, y, w, h, r, Colors.fade(Theme.popover(), a), Colors.fade(Theme.lineStrong(), a));
        int ty = y + PAD;
        Fonts.draw(g, title, Fonts.Weight.SEMIBOLD, 14, x + PAD, ty, Colors.fade(Theme.text(), a));
        ty += Fonts.lineHeight(14) + 6;
        for (String line : lines) {
            Fonts.draw(g, line, Fonts.Weight.REGULAR, 12, x + PAD, ty, Colors.fade(Theme.muted(), a));
            ty += Fonts.lineHeight(12);
        }
        button(g, cancelX(), buttonsY(), buttonW(), "Cancel", false, mouseX, mouseY, a);
        button(g, confirmX(), buttonsY(), buttonW(), confirmLabel, true, mouseX, mouseY, a);
    }

    private void button(GuiGraphics g, int bx, int by, int bw, String label, boolean primary, int mouseX, int mouseY, float a) {
        boolean hover = Render2D.hovered(mouseX, mouseY, bx, by, bw, BUTTON_H);
        int fill;
        int text;
        if (primary) {
            int base = danger ? Theme.danger() : Theme.accent();
            fill = hover ? Colors.lighten(base, 0.12) : base;
            text = Colors.contrastText(base);
        } else {
            fill = hover ? Theme.controlHover() : Theme.control();
            text = Theme.text();
        }
        Render2D.roundedRect(g, bx, by, bw, BUTTON_H, Theme.radiusSmall(), Colors.fade(fill, a));
        if (!primary) Render2D.roundedOutline(g, bx, by, bw, BUTTON_H, Theme.radiusSmall(), Colors.fade(Theme.line(), a));
        Fonts.drawCentered(g, label, Fonts.Weight.MEDIUM, 13, bx + bw / 2, by + (BUTTON_H - Fonts.lineHeight(13)) / 2, Colors.fade(text, a));
    }

    @Override
    boolean mouseClicked(double mx, double my, int button) {
        if (Render2D.hovered(mx, my, cancelX(), buttonsY(), buttonW(), BUTTON_H)) {
            requestClose();
            return true;
        }
        if (Render2D.hovered(mx, my, confirmX(), buttonsY(), buttonW(), BUTTON_H)) {
            onConfirm.run();
            requestClose();
            return true;
        }
        return contains(mx, my);
    }

    @Override
    boolean keyPressed(int key, int modifiers) {
        switch (key) {
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                onConfirm.run();
                requestClose();
                return true;
            }
            case GLFW.GLFW_KEY_ESCAPE -> {
                requestClose();
                return true;
            }
            default -> {
                return true;
            }
        }
    }
}
