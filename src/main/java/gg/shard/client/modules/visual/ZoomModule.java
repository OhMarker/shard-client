package gg.shard.client.modules.visual;

import com.mojang.blaze3d.platform.InputConstants;
import gg.shard.client.module.Module;
import gg.shard.client.module.ModuleCategory;
import gg.shard.client.module.setting.BoolSetting;
import gg.shard.client.module.setting.DoubleSetting;
import gg.shard.client.module.setting.KeybindSetting;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/** Hold a key to zoom the camera. The FOV change is applied in GameRendererMixin. */
public final class ZoomModule extends Module {
    private final KeybindSetting key = add(new KeybindSetting("Key", "Hold to zoom", GLFW.GLFW_KEY_C));
    private final DoubleSetting factor = add(new DoubleSetting("Zoom", "How far to zoom in", 4.0, 1.5, 12.0, 0.5, "x"));
    private final BoolSetting smooth = add(new BoolSetting("Smooth", "Ease in and out instead of snapping", true));

    private float current = 1f;

    public ZoomModule() {
        super("Zoom", "Hold a key to zoom in.", ModuleCategory.VISUALS);
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
        float target = held() ? (float) (1.0 / factor.get()) : 1f;
        if (smooth.get()) {
            current += (target - current) * 0.25f;
            if (Math.abs(target - current) < 0.002f) current = target;
        } else current = target;
        return fov * current;
    }

    @Override
    public String icon() {
        return "item:spyglass";
    }

    @Override
    public java.util.List<String> conflictingMods() {
        return java.util.List.of("wi_zoom");
    }
}
