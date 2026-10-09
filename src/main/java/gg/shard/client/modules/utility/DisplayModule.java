package gg.shard.client.modules.utility;

import com.mojang.blaze3d.platform.Window;
import gg.shard.client.ShardClient;
import gg.shard.client.module.Module;
import gg.shard.client.module.ModuleCategory;
import gg.shard.client.module.setting.BoolSetting;
import gg.shard.client.module.setting.EnumSetting;
import gg.shard.client.module.setting.IntSetting;
import gg.shard.client.module.setting.Labeled;
import gg.shard.client.module.setting.StringSetting;
import net.minecraft.client.Minecraft;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;

/**
 * Display: borderless fullscreen (a window without a frame covering the monitor, so alt-tab is
 * instant with no black screen or resolution switch) on F11 or at start-up, a frame cap while
 * the game is in the background and in menus, a custom window title, and quick switches for
 * vanilla's raw mouse input and VSync.
 */
public final class DisplayModule extends Module {
    public enum Fullscreen implements Labeled {
        BORDERLESS("Borderless"), EXCLUSIVE("Exclusive (vanilla)");

        private final String label;

        Fullscreen(String label) {
            this.label = label;
        }

        @Override
        public String label() {
            return label;
        }
    }

    public enum Screen implements Labeled {
        CURRENT("Where the window is"), FIRST("Monitor 1"), SECOND("Monitor 2"), THIRD("Monitor 3");

        private final String label;

        Screen(String label) {
            this.label = label;
        }

        @Override
        public String label() {
            return label;
        }
    }

    private final EnumSetting<Fullscreen> fullscreen = add(new EnumSetting<>("F11 fullscreen", "What F11 switches to", Fullscreen.BORDERLESS).group("Fullscreen")
            .details("Borderless is a frameless window the size of the monitor: alt-tab is instant and the resolution never changes. Exclusive is vanilla's fullscreen."));
    private final EnumSetting<Screen> monitor = add(new EnumSetting<>("Monitor", "Which monitor borderless fullscreen uses", Screen.CURRENT).group("Fullscreen"));
    private final BoolSetting startBorderless = add(new BoolSetting("Start borderless", "Go borderless fullscreen as soon as the game opens", false).group("Fullscreen"));
    private final IntSetting background = add(new IntSetting("Background FPS", "Frame cap while you are alt-tabbed (0 = no cap)", 30, 0, 240, 5, " fps").group("Frame rate")
            .details("Saves power and heat while the game is in the background. Vanilla's own limits for a minimised or AFK game still apply."));
    private final IntSetting menu = add(new IntSetting("Menu FPS", "Frame cap on the title screen and other menus outside a world (vanilla: 60)", 60, 30, 240, 10, " fps").group("Frame rate"));
    private final StringSetting title = add(new StringSetting("Window title", "Shown on the window and the taskbar; empty keeps vanilla's", "Shard Client", 64).group("Window"));
    private final BoolSetting rawInput = add(new BoolSetting("Raw mouse input", "Vanilla's raw input option: mouse movement without Windows acceleration", true).group("Window"));
    private final BoolSetting vsync = add(new BoolSetting("VSync", "Vanilla's VSync option: no tearing, but adds input delay", false).group("Window"));

    private boolean borderless;
    private int savedX;
    private int savedY;
    private int savedW;
    private int savedH;
    private boolean started;

    public DisplayModule() {
        super("Display", "Borderless fullscreen, background and menu frame caps, window title, raw input and VSync.", ModuleCategory.UTILITY);
        title.onChange(v -> refreshTitle());
        rawInput.onChange(v -> applyVanilla());
        vsync.onChange(v -> applyVanilla());
    }

    @Override
    public boolean defaultEnabled() {
        return true;
    }

    /** Window options live on Settings → Window, always in effect. */
    @Override
    public boolean hidden() {
        return true;
    }

    @Override
    public boolean alwaysOn() {
        return true;
    }

    @Override
    public String icon() {
        return "window";
    }

    @Override
    public String about() {
        return "Borderless fullscreen on F11 (or at start-up): a frameless window covering the monitor, so alt-tab is instant and the resolution never changes. "
                + "Caps the frame rate while the game is in the background or in menus, sets the window title, and mirrors vanilla's raw mouse input and VSync.";
    }

    @Override
    protected void onEnable() {
        refreshTitle();
        applyVanilla();
    }

    @Override
    protected void onDisable() {
        if (borderless) setBorderless(false);
        refreshTitle();
    }

    @Override
    public void onTick() {
        if (!started) {
            started = true;
            if (startBorderless.get() && !borderless) setBorderless(true);
        }
        // Keep the pointer visible in menus while borderless (some drivers hide it after a mode change).
        Minecraft mc = Minecraft.getInstance();
        if (borderless && mc.screen != null && GLFW.glfwGetInputMode(mc.getWindow().handle(), GLFW.GLFW_CURSOR) != GLFW.GLFW_CURSOR_NORMAL) {
            GLFW.glfwSetInputMode(mc.getWindow().handle(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
        }
    }

    private void refreshTitle() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getWindow() != null) mc.updateTitle();
    }

    private void applyVanilla() {
        if (!isEnabled()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.options == null) return;
        //? if >=26.3 {
        /*// SDL3 reads the mouse raw in relative mode; the Windows pointer acceleration is a hint.
        org.lwjgl.sdl.SDLHints.SDL_SetHint(org.lwjgl.sdl.SDLHints.SDL_HINT_MOUSE_RELATIVE_SYSTEM_SCALE, rawInput.get() ? "0" : "1");
        *///?} else {
        if (mc.options.rawMouseInput().get() != rawInput.get()) mc.options.rawMouseInput().set(rawInput.get());
        //?}
        if (mc.options.enableVsync().get() != vsync.get()) mc.options.enableVsync().set(vsync.get());
    }

    /** The window title, or null for vanilla's. */
    public String titleOverride() {
        return isEnabled() && !title.get().isBlank() ? title.get() : null;
    }

    /** Frame cap given vanilla's; background and menu caps only ever lower or set the menu one. */
    public int framerateLimit(int vanilla) {
        if (!isEnabled()) return vanilla;
        Minecraft mc = Minecraft.getInstance();
        int limit = vanilla;
        if (mc.level == null && mc.screen != null) limit = menu.get();
        if (background.get() > 0 && !mc.isWindowActive()) limit = Math.min(limit, background.get());
        return limit;
    }

    /** F11: returns true when Shard handled it (borderless), false to let vanilla toggle exclusive fullscreen. */
    public boolean onToggleFullscreen(Window window) {
        if (!isEnabled() || fullscreen.get() != Fullscreen.BORDERLESS) {
            if (borderless) setBorderless(false);
            return false;
        }
        setBorderless(!borderless);
        return true;
    }

    public boolean isBorderless() {
        return borderless;
    }

    /** Switches borderless fullscreen on or off (also used by the smoke test). */
    public void setBorderless(boolean on) {
        Minecraft mc = Minecraft.getInstance();
        Window window = mc.getWindow();
        long handle = window.handle();
        if (on == borderless) return;
        if (on) {
            //? if >=26.3 {
            /*// Leave vanilla's fullscreen first (the option drives the window; applied right away).
            if (mc.options.fullscreen().get()) {
                mc.options.fullscreen().set(false);
                window.updateFullscreenIfChanged();
            }
            *///?} else {
            if (window.isFullscreen()) window.toggleFullScreen(); // leave exclusive first (this hook lets it through)
            //?}
            savedX = window.getX();
            savedY = window.getY();
            savedW = window.getScreenWidth();
            savedH = window.getScreenHeight();
            long target = pickMonitor(handle);
            if (target == 0L) return;
            int[] mx = new int[1];
            int[] my = new int[1];
            GLFW.glfwGetMonitorPos(target, mx, my);
            GLFWVidMode mode = GLFW.glfwGetVideoMode(target);
            if (mode == null) return;
            GLFW.glfwSetWindowAttrib(handle, GLFW.GLFW_DECORATED, GLFW.GLFW_FALSE);
            // One pixel taller than the monitor: a borderless window that exactly covers the screen is
            // treated by Windows as fullscreen, which can hide the mouse cursor in menus.
            GLFW.glfwSetWindowMonitor(handle, 0L, mx[0], my[0], mode.width(), mode.height() + 1, GLFW.GLFW_DONT_CARE);
            borderless = true;
            showCursorInMenus(mc, handle);
            ShardClient.LOGGER.info("Borderless fullscreen on {}x{} at {},{}", mode.width(), mode.height(), mx[0], my[0]);
        } else {
            GLFW.glfwSetWindowAttrib(handle, GLFW.GLFW_DECORATED, GLFW.GLFW_TRUE);
            int w = savedW > 0 ? savedW : 1280;
            int h = savedH > 0 ? savedH : 720;
            GLFW.glfwSetWindowMonitor(handle, 0L, savedX, savedY, w, h, GLFW.GLFW_DONT_CARE);
            borderless = false;
            showCursorInMenus(mc, handle);
        }
    }

    /** Changing the window can leave the cursor hidden; in a menu it must be the normal pointer. */
    private static void showCursorInMenus(Minecraft mc, long handle) {
        if (mc.screen != null || mc.player == null) {
            GLFW.glfwSetInputMode(handle, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
        }
    }

    /** The chosen monitor, or the one containing the window's centre. */
    private long pickMonitor(long handle) {
        PointerBuffer monitors = GLFW.glfwGetMonitors();
        if (monitors == null || monitors.limit() == 0) return GLFW.glfwGetPrimaryMonitor();
        int wanted = switch (monitor.get()) {
            case FIRST -> 0;
            case SECOND -> 1;
            case THIRD -> 2;
            case CURRENT -> -1;
        };
        if (wanted >= 0) return monitors.get(Math.min(wanted, monitors.limit() - 1));
        int[] wx = new int[1];
        int[] wy = new int[1];
        int[] ww = new int[1];
        int[] wh = new int[1];
        GLFW.glfwGetWindowPos(handle, wx, wy);
        GLFW.glfwGetWindowSize(handle, ww, wh);
        int cx = wx[0] + ww[0] / 2;
        int cy = wy[0] + wh[0] / 2;
        for (int i = 0; i < monitors.limit(); i++) {
            long m = monitors.get(i);
            int[] mx = new int[1];
            int[] my = new int[1];
            GLFW.glfwGetMonitorPos(m, mx, my);
            GLFWVidMode mode = GLFW.glfwGetVideoMode(m);
            if (mode != null && cx >= mx[0] && cx < mx[0] + mode.width() && cy >= my[0] && cy < my[0] + mode.height()) return m;
        }
        return GLFW.glfwGetPrimaryMonitor();
    }
}
