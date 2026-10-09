package gg.shard.client.modules.visual;

import com.mojang.blaze3d.platform.InputConstants;
import gg.shard.client.module.Module;
import gg.shard.client.module.ModuleCategory;
import gg.shard.client.module.setting.BoolSetting;
import gg.shard.client.module.setting.DoubleSetting;
import gg.shard.client.module.setting.IntSetting;
import gg.shard.client.module.setting.KeybindSetting;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * Hold a key to zoom the camera. The FOV change is applied in GameRendererMixin every frame and
 * eased by frame time (exponential smoothing in log-zoom space, so zooming 2x to 4x feels the same
 * as 4x to 8x at any frame rate); scrolling while zoomed is taken in MouseHandlerMixin so it
 * adjusts the zoom target instead of switching hotbar slots, and the same easing animates it.
 * "Smooth sensitivity" scales the mouse by the current zoom, also in MouseHandlerMixin.
 */
public final class ZoomModule extends Module {
    private static final double MIN_ZOOM = 1.5;
    private static final double MAX_ZOOM = 50.0;

    private final KeybindSetting key = add(new KeybindSetting("Key", "Hold to zoom", GLFW.GLFW_KEY_C));
    private final DoubleSetting factor = add(new DoubleSetting("Zoom", "How far to zoom in", 4.0, 1.5, 20.0, 0.5, "x"));
    private final BoolSetting smooth = add(new BoolSetting("Smooth", "Ease in and out instead of snapping", true));
    private final IntSetting easeMs = add(new IntSetting("Smoothness", "How long the zoom takes to settle; higher is softer", 90, 20, 400, 10, " ms")
            .details("Measured in time, not frames, so it feels the same at 60 and 360 fps."));
    private final BoolSetting scrollAdjust = add(new BoolSetting("Scroll to adjust", "While zoomed, the mouse wheel changes the zoom instead of the hotbar slot", true)
            .details("The adjusted zoom resets to the Zoom value each time you press the key."));
    private final IntSetting scrollStep = add(new IntSetting("Scroll step", "How much one wheel notch zooms", 15, 5, 50, 5, "%"));
    private final BoolSetting smoothSensitivity = add(new BoolSetting("Smooth sensitivity", "Slow the mouse down as you zoom in, so aiming feels the same", true)
            .details("Scales your mouse movement by the current zoom, like a spyglass. Your sensitivity setting is not changed."));
    private final BoolSetting cinematic = add(new BoolSetting("Cinematic camera", "Use vanilla's smooth camera while zoomed", false)
            .details("The same option as vanilla's Smooth Camera key; your own setting comes back when you let go."));

    /** Natural log of the current FOV multiplier (0 = no zoom), eased toward the target. */
    private double currentLog;
    private long lastNs;
    private double held = -1;
    private boolean wasHeld;
    private Boolean savedSmoothCamera;
    /** Dev smoke test only: behave as if the key were held (the unfocused dev window gets no key input). */
    private boolean forcedHeld;

    public ZoomModule() {
        super("Zoom", "Hold a key to zoom in, scroll to adjust.", ModuleCategory.VISUALS);
        easeMs.visibleWhen(smooth::get);
        scrollStep.visibleWhen(scrollAdjust::get);
    }

    @Override
    public String about() {
        return "Narrows the field of view while you hold the key, like a spyglass you do not have to equip. The zoom glides by frame time, scrolling "
                + "zooms further with the same glide, and Smooth sensitivity slows the mouse as you zoom so aiming feels the same. Optionally use "
                + "vanilla's cinematic camera while zoomed. It changes only what your camera shows.";
    }

    @Override
    public boolean defaultEnabled() {
        return true;
    }

    private boolean held() {
        Minecraft mc = Minecraft.getInstance();
        return isEnabled() && mc.screen == null && (forcedHeld || key.isBound() && InputConstants.isKeyDown(mc.getWindow(), key.get()));
    }

    /** Dev smoke test only: hold or release the zoom without a real key press. */
    public void forceHeldForSmoke(boolean held) {
        forcedHeld = held;
    }

    /** Multiplies the vanilla FOV; 1.0 means no zoom. Called every frame from the mixin. */
    public float applyFov(float fov) {
        boolean down = held();
        if (down && !wasHeld) held = factor.get();
        if (down != wasHeld) toggleCinematic(down);
        wasHeld = down;
        double targetLog = down ? -Math.log(held) : 0;
        long now = System.nanoTime();
        double dt = lastNs == 0 ? 0 : Math.min(0.1, (now - lastNs) / 1e9);
        lastNs = now;
        currentLog = smooth.get() ? ease(currentLog, targetLog, dt, easeMs.get() / 1000.0) : targetLog;
        return (float) (fov * Math.exp(currentLog));
    }

    /**
     * Frame-rate independent exponential approach: after {@code settle} seconds about 95% of the
     * distance is covered (three time constants). Snaps when within 0.1% of the target.
     */
    static double ease(double current, double target, double dt, double settle) {
        if (settle <= 0) return target;
        double next = current + (target - current) * (1 - Math.exp(-3.0 * dt / settle));
        return Math.abs(target - next) < 0.001 ? target : next;
    }

    /** Multiplier for mouse turning: the current FOV ratio while zoomed, else 1. */
    public double sensitivityScale() {
        if (!isEnabled() || !smoothSensitivity.get() || currentLog >= 0) return 1.0;
        return Math.exp(currentLog);
    }

    private void toggleCinematic(boolean on) {
        Minecraft mc = Minecraft.getInstance();
        if (on && cinematic.get()) {
            savedSmoothCamera = mc.options.smoothCamera;
            mc.options.smoothCamera = true;
        } else if (!on && savedSmoothCamera != null) {
            mc.options.smoothCamera = savedSmoothCamera;
            savedSmoothCamera = null;
        }
    }

    /** True when the scroll was used to change the zoom (the mixin then cancels vanilla's handling). */
    public boolean onScroll(double amount) {
        if (!scrollAdjust.get() || !wasHeld || amount == 0) return false;
        double step = 1 + scrollStep.get() / 100.0;
        held = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, held * (amount > 0 ? step : 1 / step)));
        return true;
    }

    @Override
    protected void onDisable() {
        if (wasHeld) toggleCinematic(false);
        wasHeld = false;
        currentLog = 0;
        lastNs = 0;
    }

    @Override
    public String icon() {
        return "zoom";
    }

    @Override
    public java.util.List<String> conflictingMods() {
        return java.util.List.of("wi_zoom");
    }
}
