package gg.shard.client.compat;

//? if >=26.3 {
/*import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.PointerBuffer;
import org.lwjgl.sdl.SDLMouse;
import org.lwjgl.sdl.SDLStdinc;
import org.lwjgl.sdl.SDLVideo;
import org.lwjgl.sdl.SDL_Rect;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

/^*
 * 26.3 runs on SDL3, and LWJGL's GLFW is not on the classpath. This stands in for the parts of
 * org.lwjgl.glfw.GLFW Shard uses (the import is rewritten in stonecutter.gradle.kts): key, mouse
 * button and modifier constants carry Minecraft's own values (SDL scancodes, SDL buttons and
 * modifiers, from InputConstants), so they compare with key and mouse events as before; window,
 * cursor and monitor calls go to SDL. Monitors are SDL display ids, and a "video mode" is the
 * display's bounds in the same coordinates as window positions and sizes.
 ^/
public final class GLFW {
    private GLFW() {}

    public static final int GLFW_TRUE = 1, GLFW_FALSE = 0, GLFW_DONT_CARE = -1;
    /^* SDL_SCANCODE_UNKNOWN (InputConstants.UNKNOWN), an unbound key mapping. ^/
    public static final int GLFW_KEY_UNKNOWN = 0;
    public static final int GLFW_KEY_SPACE = InputConstants.KEY_SPACE, GLFW_KEY_APOSTROPHE = InputConstants.KEY_APOSTROPHE,
            GLFW_KEY_COMMA = InputConstants.KEY_COMMA, GLFW_KEY_MINUS = InputConstants.KEY_MINUS, GLFW_KEY_PERIOD = InputConstants.KEY_PERIOD,
            GLFW_KEY_SLASH = InputConstants.KEY_SLASH, GLFW_KEY_SEMICOLON = InputConstants.KEY_SEMICOLON, GLFW_KEY_EQUAL = InputConstants.KEY_EQUALS,
            GLFW_KEY_LEFT_BRACKET = InputConstants.KEY_LBRACKET, GLFW_KEY_BACKSLASH = InputConstants.KEY_BACKSLASH,
            GLFW_KEY_RIGHT_BRACKET = InputConstants.KEY_RBRACKET, GLFW_KEY_GRAVE_ACCENT = InputConstants.KEY_GRAVE;
    public static final int GLFW_KEY_0 = InputConstants.KEY_0, GLFW_KEY_7 = InputConstants.KEY_7, GLFW_KEY_9 = InputConstants.KEY_9;
    public static final int GLFW_KEY_A = InputConstants.KEY_A, GLFW_KEY_C = InputConstants.KEY_C, GLFW_KEY_F = InputConstants.KEY_F,
            GLFW_KEY_G = InputConstants.KEY_G, GLFW_KEY_R = InputConstants.KEY_R, GLFW_KEY_V = InputConstants.KEY_V,
            GLFW_KEY_Y = InputConstants.KEY_Y, GLFW_KEY_Z = InputConstants.KEY_Z;
    public static final int GLFW_KEY_ESCAPE = InputConstants.KEY_ESCAPE, GLFW_KEY_ENTER = InputConstants.KEY_RETURN,
            GLFW_KEY_TAB = InputConstants.KEY_TAB, GLFW_KEY_BACKSPACE = InputConstants.KEY_BACKSPACE, GLFW_KEY_INSERT = InputConstants.KEY_INSERT,
            GLFW_KEY_DELETE = InputConstants.KEY_DELETE, GLFW_KEY_RIGHT = InputConstants.KEY_RIGHT, GLFW_KEY_LEFT = InputConstants.KEY_LEFT,
            GLFW_KEY_DOWN = InputConstants.KEY_DOWN, GLFW_KEY_UP = InputConstants.KEY_UP, GLFW_KEY_PAGE_UP = InputConstants.KEY_PAGEUP,
            GLFW_KEY_PAGE_DOWN = InputConstants.KEY_PAGEDOWN, GLFW_KEY_HOME = InputConstants.KEY_HOME, GLFW_KEY_END = InputConstants.KEY_END,
            GLFW_KEY_CAPS_LOCK = InputConstants.KEY_CAPSLOCK;
    public static final int GLFW_KEY_F1 = InputConstants.KEY_F1, GLFW_KEY_F5 = InputConstants.KEY_F5;
    /^* SDL has no F25; an unused scancode so the constant stays distinct. ^/
    public static final int GLFW_KEY_F25 = 0x1FF;
    public static final int GLFW_KEY_KP_0 = InputConstants.KEY_NUMPAD0, GLFW_KEY_KP_9 = InputConstants.KEY_NUMPAD9,
            GLFW_KEY_KP_ENTER = InputConstants.KEY_NUMPADENTER;
    public static final int GLFW_KEY_LEFT_SHIFT = InputConstants.KEY_LSHIFT, GLFW_KEY_LEFT_CONTROL = InputConstants.KEY_LCONTROL,
            GLFW_KEY_LEFT_ALT = InputConstants.KEY_LALT, GLFW_KEY_RIGHT_SHIFT = InputConstants.KEY_RSHIFT,
            GLFW_KEY_RIGHT_CONTROL = InputConstants.KEY_RCONTROL, GLFW_KEY_RIGHT_ALT = InputConstants.KEY_RALT;
    /^* The highest scancode (SDL_SCANCODE_COUNT - 1). ^/
    public static final int GLFW_KEY_LAST = 511;

    public static final int GLFW_MOUSE_BUTTON_LEFT = InputConstants.MOUSE_BUTTON_LEFT, GLFW_MOUSE_BUTTON_RIGHT = InputConstants.MOUSE_BUTTON_RIGHT,
            GLFW_MOUSE_BUTTON_MIDDLE = InputConstants.MOUSE_BUTTON_MIDDLE;
    public static final int GLFW_MOD_SHIFT = InputConstants.MOD_SHIFT, GLFW_MOD_CONTROL = InputConstants.MOD_CONTROL,
            GLFW_MOD_ALT = InputConstants.MOD_ALT, GLFW_MOD_SUPER = InputConstants.MOD_SUPER;

    // Window attributes and input modes (GLFW's values; only these are understood here).
    public static final int GLFW_FOCUSED = 0x20001, GLFW_ICONIFIED = 0x20002, GLFW_VISIBLE = 0x20004, GLFW_DECORATED = 0x20005;
    public static final int GLFW_CURSOR = 0x33001, GLFW_CURSOR_NORMAL = 0x34001, GLFW_CURSOR_DISABLED = 0x34003;

    /^* A display's size (its bounds, in window coordinates). ^/
    public record GLFWVidMode(int width, int height) {}

    public static void glfwSetCursorPos(long window, double x, double y) {
        SDLMouse.SDL_WarpMouseInWindow(window, (float) x, (float) y);
    }

    public static int glfwGetInputMode(long window, int mode) {
        if (mode != GLFW_CURSOR) return 0;
        return SDLMouse.SDL_GetWindowRelativeMouseMode(window) || !SDLMouse.SDL_CursorVisible() ? GLFW_CURSOR_DISABLED : GLFW_CURSOR_NORMAL;
    }

    public static void glfwSetInputMode(long window, int mode, int value) {
        if (mode != GLFW_CURSOR || value != GLFW_CURSOR_NORMAL) return;
        SDLMouse.SDL_SetWindowRelativeMouseMode(window, false);
        SDLMouse.SDL_ShowCursor();
    }

    public static void glfwRestoreWindow(long window) {
        SDLVideo.SDL_RestoreWindow(window);
    }

    public static void glfwShowWindow(long window) {
        SDLVideo.SDL_ShowWindow(window);
    }

    public static void glfwFocusWindow(long window) {
        SDLVideo.SDL_RaiseWindow(window);
    }

    public static int glfwGetWindowAttrib(long window, int attrib) {
        long flags = SDLVideo.SDL_GetWindowFlags(window);
        boolean on = switch (attrib) {
            case GLFW_FOCUSED -> (flags & SDLVideo.SDL_WINDOW_INPUT_FOCUS) != 0;
            case GLFW_ICONIFIED -> (flags & SDLVideo.SDL_WINDOW_MINIMIZED) != 0;
            case GLFW_VISIBLE -> (flags & SDLVideo.SDL_WINDOW_HIDDEN) == 0;
            case GLFW_DECORATED -> (flags & SDLVideo.SDL_WINDOW_BORDERLESS) == 0;
            default -> false;
        };
        return on ? GLFW_TRUE : GLFW_FALSE;
    }

    public static void glfwSetWindowAttrib(long window, int attrib, int value) {
        if (attrib == GLFW_DECORATED) SDLVideo.SDL_SetWindowBordered(window, value == GLFW_TRUE);
    }

    /^* Windowed only (monitor 0): leaves fullscreen, then sizes and places the window. ^/
    public static void glfwSetWindowMonitor(long window, long monitor, int x, int y, int width, int height, int refreshRate) {
        SDLVideo.SDL_SetWindowFullscreen(window, false);
        SDLVideo.SDL_SetWindowSize(window, width, height);
        SDLVideo.SDL_SetWindowPosition(window, x, y);
    }

    public static void glfwGetWindowPos(long window, int[] x, int[] y) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer px = stack.mallocInt(1);
            IntBuffer py = stack.mallocInt(1);
            SDLVideo.SDL_GetWindowPosition(window, px, py);
            x[0] = px.get(0);
            y[0] = py.get(0);
        }
    }

    public static void glfwGetWindowSize(long window, int[] width, int[] height) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer pw = stack.mallocInt(1);
            IntBuffer ph = stack.mallocInt(1);
            SDLVideo.SDL_GetWindowSize(window, pw, ph);
            width[0] = pw.get(0);
            height[0] = ph.get(0);
        }
    }

    public static long glfwGetPrimaryMonitor() {
        return SDLVideo.SDL_GetPrimaryDisplay();
    }

    /^* The connected displays (ids), or null. ^/
    public static PointerBuffer glfwGetMonitors() {
        IntBuffer ids = SDLVideo.SDL_GetDisplays();
        if (ids == null) return null;
        try {
            PointerBuffer out = PointerBuffer.allocateDirect(ids.limit());
            for (int i = 0; i < ids.limit(); i++) out.put(i, ids.get(i));
            return out;
        } finally {
            SDLStdinc.SDL_free(ids);
        }
    }

    public static void glfwGetMonitorPos(long monitor, int[] x, int[] y) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            SDL_Rect bounds = SDL_Rect.malloc(stack);
            if (SDLVideo.SDL_GetDisplayBounds((int) monitor, bounds)) {
                x[0] = bounds.x();
                y[0] = bounds.y();
            }
        }
    }

    public static GLFWVidMode glfwGetVideoMode(long monitor) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            SDL_Rect bounds = SDL_Rect.malloc(stack);
            return SDLVideo.SDL_GetDisplayBounds((int) monitor, bounds) ? new GLFWVidMode(bounds.w(), bounds.h()) : null;
        }
    }
}
*///?}
