package gg.shard.client.util;

import gg.shard.client.compat.KeyCodes;
//? if >=26.3 {
/*import gg.shard.client.compat.KeyCodes.GLFW;
*///?} else {
import org.lwjgl.glfw.GLFW;
//?}

import java.util.Locale;

/**
 * Game key code <-> human name, without touching Minecraft classes (unit-testable). The names are
 * defined on GLFW key codes; on versions with other codes (26.3: SDL scancodes) KeyCodes converts.
 */
public final class Keys {
    private Keys() {}

    public static final int NONE = -1;

    public static String name(int key) {
        if (key < 0) return "None";
        int glfw = KeyCodes.toGlfw(key);
        if (glfw == NONE) return "None";
        String name = glfw == KeyCodes.UNMAPPED ? null : glfwName(glfw);
        return name != null ? name : "Key " + key;
    }

    /** Inverse of {@link #name}; returns Integer.MIN_VALUE when unknown. */
    public static int fromName(String text) {
        int key = glfwFromName(text);
        String t = text.trim();
        // A typed number (more than one digit; "7" is the 7 key) is a raw code, kept as is.
        if (key == Integer.MIN_VALUE || key == NONE || t.length() > 1 && t.matches("-?\\d+")) return key;
        int game = KeyCodes.fromGlfw(key);
        return game == KeyCodes.UNMAPPED ? Integer.MIN_VALUE : game;
    }

    /** The name of a GLFW key code, or null when it has none. */
    private static String glfwName(int key) {
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
            default -> null;
        };
    }

    /** A GLFW key code for a name (or a typed number, returned as is). */
    private static int glfwFromName(String text) {
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
            String n = glfwName(key);
            if (n != null && n.equalsIgnoreCase(t)) return key;
        }
        try {
            return Integer.parseInt(t);
        } catch (NumberFormatException e) {
            return Integer.MIN_VALUE;
        }
    }
}
