package gg.shard.client.modules.visual;

import com.mojang.blaze3d.platform.InputConstants;
import gg.shard.client.module.Module;
import gg.shard.client.module.ModuleCategory;
import gg.shard.client.module.setting.BoolSetting;
import gg.shard.client.module.setting.DoubleSetting;
import gg.shard.client.module.setting.KeybindSetting;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * Hold a key to zoom the camera. The FOV change is applied in GameRendererMixin; scrolling while
 * zoomed is taken in MouseHandlerMixin so it adjusts the zoom instead of switching hotbar slots.
 */
public final class ZoomModule extends Module {
    private static final double MIN_ZOOM = 1.5;
    private static final double MAX_ZOOM = 50.0;

    private final KeybindSetting key = add(new KeybindSetting("Key", "Hold to zoom", GLFW.GLFW_KEY_C));
    private final DoubleSetting factor = add(new DoubleSetting("Zoom", "How far to zoom in", 4.0, 1.5, 12.0, 0.5, "x"));
    private final BoolSetting smooth = add(new BoolSetting("Smooth", "Ease in and out instead of snapping", true));
    private final BoolSetting scrollAdjust = add(new BoolSetting("Scroll to adjust", "While zoomed, the mouse wheel changes the zoom instead of the hotbar slot", true)
            .details("The adjusted zoom resets to the Zoom value each time you press the key."));
    private final BoolSetting cinematic = add(new BoolSetting("Cinematic camera", "Use vanilla's smooth camera while zoomed", false)
            .details("The same option as vanilla's Smooth Camera key; your own setting comes back when you let go."));

    private float current = 1f;
    private double held = -1;
    private boolean wasHeld;
    private Boolean savedSmoothCamera;

    public ZoomModule() {
        super("Zoom", "Hold a key to zoom in, scroll to adjust.", ModuleCategory.VISUALS);
    }

    @Override
    public String about() {
        return "Narrows the field of view while you hold the key, like a spyglass you do not have to equip. Scroll to zoom further, and optionally "
                + "use vanilla's cinematic camera while zoomed. It changes only what your camera shows.";
    }

    @Override
    public boolean defaultEnabled() {
        return true;
    }

    private boolean held() {
        Minecraft mc = Minecraft.getInstance();
        return isEnabled() && mc.screen == null && key.isBound() && InputConstants.isKeyDown(mc.getWindow(), key.get());
    }

    /** Multiplies the vanilla FOV; 1.0 means no zoom. Called every frame from the mixin. */
    public float applyFov(float fov) {
        boolean down = held();
        if (down && !wasHeld) held = factor.get();
        if (down != wasHeld) toggleCinematic(down);
        wasHeld = down;
        float target = down ? (float) (1.0 / held) : 1f;
        if (smooth.get()) {
            current += (target - current) * 0.25f;
            if (Math.abs(target - current) < 0.002f) current = target;
        } else current = target;
        return fov * current;
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
        held = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, held * (amount > 0 ? 1.15 : 1 / 1.15)));
        return true;
    }

    @Override
    protected void onDisable() {
        if (wasHeld) toggleCinematic(false);
        wasHeld = false;
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
