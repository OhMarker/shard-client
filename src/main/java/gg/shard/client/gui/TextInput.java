package gg.shard.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

/** A single-line text field drawn in the Shard theme: cursor, placeholder, paste, home/end. */
final class TextInput {
    private String value = "";
    private int cursor;
    private final int maxLength;
    private String placeholder = "";
    private Consumer<String> onChange = v -> {};
    private Runnable onCommit = () -> {};

    TextInput(int maxLength) {
        this.maxLength = maxLength;
    }

    TextInput placeholder(String text) {
        this.placeholder = text;
        return this;
    }

    TextInput onChange(Consumer<String> listener) {
        this.onChange = listener;
        return this;
    }

    TextInput onCommit(Runnable listener) {
        this.onCommit = listener;
        return this;
    }

    String value() {
        return value;
    }

    void setValue(String text) {
        value = text == null ? "" : text.length() > maxLength ? text.substring(0, maxLength) : text;
        cursor = Math.min(cursor, value.length());
    }

    /** Replaces the text without firing listeners (used to mirror a setting into the field). */
    void sync(String text) {
        if (!value.equals(text)) {
            value = text == null ? "" : text;
            cursor = value.length();
        }
    }

    boolean isEmpty() {
        return value.isEmpty();
    }

    void cursorToEnd() {
        cursor = value.length();
    }

    void render(GuiGraphics g, Font font, int x, int y, int w, int h, boolean focused) {
        int r = Theme.radiusSmall();
        Render2D.roundedRect(g, x, y, w, h, r, Theme.control());
        Render2D.roundedOutline(g, x, y, w, h, r, focused ? Theme.accentAlpha(0xB0) : Theme.line());
        int textX = x + 5;
        int textY = y + (h - 8) / 2;
        int maxW = w - 10;
        if (value.isEmpty() && !focused) {
            Render2D.textClipped(g, font, placeholder, textX, textY, maxW, Theme.subtle(), false);
            return;
        }
        // Keep the cursor visible by scrolling the text horizontally.
        String beforeCursor = value.substring(0, cursor);
        int offset = Math.max(0, font.width(beforeCursor) - maxW + 2);
        g.enableScissor(x + 3, y, x + w - 3, y + h);
        Render2D.text(g, font, value, textX - offset, textY, Theme.text(), false);
        if (focused && (System.currentTimeMillis() / 500) % 2 == 0) {
            int cx = textX - offset + font.width(beforeCursor);
            g.fill(cx, textY - 1, cx + 1, textY + 9, Theme.accent());
        }
        g.disableScissor();
    }

    /** Places the cursor at the clicked x position. */
    void clickAt(Font font, int fieldX, double mouseX) {
        int textX = fieldX + 5;
        int best = value.length();
        for (int i = 0; i <= value.length(); i++) {
            if (textX + font.width(value.substring(0, i)) > mouseX) {
                best = Math.max(0, i - 1);
                break;
            }
        }
        cursor = best;
    }

    /** Returns true when the key was consumed. Enter fires onCommit; Escape is left to the caller. */
    boolean keyPressed(int key, int modifiers) {
        boolean ctrl = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0 || (modifiers & GLFW.GLFW_MOD_SUPER) != 0;
        switch (key) {
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (cursor > 0) {
                    int from = ctrl ? wordStart() : cursor - 1;
                    value = value.substring(0, from) + value.substring(cursor);
                    cursor = from;
                    onChange.accept(value);
                }
                return true;
            }
            case GLFW.GLFW_KEY_DELETE -> {
                if (cursor < value.length()) {
                    value = value.substring(0, cursor) + value.substring(cursor + 1);
                    onChange.accept(value);
                }
                return true;
            }
            case GLFW.GLFW_KEY_LEFT -> {
                cursor = ctrl ? wordStart() : Math.max(0, cursor - 1);
                return true;
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                cursor = Math.min(value.length(), cursor + 1);
                return true;
            }
            case GLFW.GLFW_KEY_HOME -> {
                cursor = 0;
                return true;
            }
            case GLFW.GLFW_KEY_END -> {
                cursor = value.length();
                return true;
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                onCommit.run();
                return true;
            }
            case GLFW.GLFW_KEY_V -> {
                if (ctrl) {
                    insert(Minecraft.getInstance().keyboardHandler.getClipboard());
                    return true;
                }
                return false;
            }
            case GLFW.GLFW_KEY_A -> {
                if (ctrl) {
                    cursor = value.length();
                    return true;
                }
                return false;
            }
            default -> {
                return false;
            }
        }
    }

    private int wordStart() {
        int i = cursor;
        while (i > 0 && value.charAt(i - 1) == ' ') i--;
        while (i > 0 && value.charAt(i - 1) != ' ') i--;
        return i;
    }

    boolean charTyped(String ch) {
        if (ch == null || ch.isEmpty()) return false;
        char c = ch.charAt(0);
        if (c < 32 || c == 127) return false;
        insert(ch);
        return true;
    }

    private void insert(String text) {
        if (text == null || text.isEmpty()) return;
        String clean = text.replace("\r", "").replace("\n", " ");
        String next = value.substring(0, cursor) + clean + value.substring(cursor);
        if (next.length() > maxLength) next = next.substring(0, maxLength);
        int added = next.length() - value.length();
        value = next;
        cursor = Math.min(next.length(), cursor + Math.max(0, added));
        onChange.accept(value);
    }
}
