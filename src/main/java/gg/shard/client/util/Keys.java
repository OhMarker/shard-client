package gg.shard.client.util;

import org.lwjgl.glfw.GLFW;

import java.util.Locale;

/** GLFW key code <-> human name, without touching Minecraft classes (unit-testable). */
public final class Keys {
    private Keys() {}

    public static final int NONE = -1;

    public static String name(int key) {
        if (key < 0) return "None";
        if (key >= GLFW.GLFW_KEY_A && key <= GLFW.GLFW_KEY_Z) return String.valueOf((char) key);
        if (key >= GLFW.GLFW_KEY_0 && key <= GLFW.GLFW_KEY_9) return String.valueOf((char) key);
        if (key >= GLFW.GLFW_KEY_F1 && key <= GLFW.GLFW_KEY_F25) return "F" + (key - GLFW.GLFW_KEY_F1 + 1);
        if (key >= GLFW.GLFW_KEY_KP_0 && key <= GLFW.GLFW_KEY_KP_9) return "Num " + (key - GLFW.GLFW_KEY_KP_0);
        return switch (key) {
            case GLFW.GLFW_KEY_SPACE -> "Space";
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "LShift";
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "RShift";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "LCtrl";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "RCtrl";
            case GLFW.GLFW_KEY_LEFT_ALT -> "LAlt";
            case GLFW.GLFW_KEY_RIGHT_ALT -> "RAlt";
            case GLFW.GLFW_KEY_TAB -> "Tab";
            case GLFW.GLFW_KEY_CAPS_LOCK -> "Caps";
            case GLFW.GLFW_KEY_ENTER -> "Enter";
            case GLFW.GLFW_KEY_BACKSPACE -> "Backspace";
            case GLFW.GLFW_KEY_DELETE -> "Delete";
            case GLFW.GLFW_KEY_INSERT -> "Insert";
            case GLFW.GLFW_KEY_HOME -> "Home";
            case GLFW.GLFW_KEY_END -> "End";
            case GLFW.GLFW_KEY_PAGE_UP -> "PgUp";
            case GLFW.GLFW_KEY_PAGE_DOWN -> "PgDn";
            case GLFW.GLFW_KEY_UP -> "Up";
            case GLFW.GLFW_KEY_DOWN -> "Down";
            case GLFW.GLFW_KEY_LEFT -> "Left";
            case GLFW.GLFW_KEY_RIGHT -> "Right";
            case GLFW.GLFW_KEY_GRAVE_ACCENT -> "`";
            case GLFW.GLFW_KEY_MINUS -> "-";
            case GLFW.GLFW_KEY_EQUAL -> "=";
            case GLFW.GLFW_KEY_LEFT_BRACKET -> "[";
            case GLFW.GLFW_KEY_RIGHT_BRACKET -> "]";
            case GLFW.GLFW_KEY_BACKSLASH -> "\\";
            case GLFW.GLFW_KEY_SEMICOLON -> ";";
            case GLFW.GLFW_KEY_APOSTROPHE -> "'";
            case GLFW.GLFW_KEY_COMMA -> ",";
            case GLFW.GLFW_KEY_PERIOD -> ".";
            case GLFW.GLFW_KEY_SLASH -> "/";
            case GLFW.GLFW_KEY_ESCAPE -> "Esc";
            default -> "Key " + key;
        };
    }

    /** Inverse of {@link #name}; returns Integer.MIN_VALUE when unknown. */
    public static int fromName(String text) {
        String t = text.trim();
        if (t.isEmpty()) return Integer.MIN_VALUE;
        if (t.equalsIgnoreCase("none") || t.equalsIgnoreCase("unbind")) return NONE;
        if (t.length() == 1) {
            char c = Character.toUpperCase(t.charAt(0));
            if (c >= 'A' && c <= 'Z') return c;
            if (c >= '0' && c <= '9') return c;
        }
        String lower = t.toLowerCase(Locale.ROOT);
        if (lower.matches("f\\d{1,2}")) {
            int n = Integer.parseInt(lower.substring(1));
            if (n >= 1 && n <= 25) return GLFW.GLFW_KEY_F1 + n - 1;
        }
        for (int key = 0; key <= GLFW.GLFW_KEY_LAST; key++) {
            String n = name(key);
            if (!n.startsWith("Key ") && n.equalsIgnoreCase(t)) return key;
        }
        try {
            return Integer.parseInt(t);
        } catch (NumberFormatException e) {
            return Integer.MIN_VALUE;
        }
    }
}
