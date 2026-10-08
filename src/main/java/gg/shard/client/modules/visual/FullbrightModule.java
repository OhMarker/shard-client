package gg.shard.client.modules.visual;

import gg.shard.client.ShardClient;
import gg.shard.client.module.Module;
import gg.shard.client.module.ModuleCategory;
import gg.shard.client.module.setting.DoubleSetting;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * Overrides the gamma value the light texture is built from, so caves and night fights are fully
 * lit. If the LightTexture hook could not be applied (a mapping change, another mod replacing
 * the class), the module falls back to maxing out the vanilla brightness slider.
 */
public final class FullbrightModule extends Module {
    private final DoubleSetting brightness = add(new DoubleSetting("Brightness", "Gamma used for the light texture (vanilla's slider tops out at 1)", 10.0, 1.0, 15.0, 0.5));

    /** Set by the LightTexture mixin the first time it runs; proves the hook is live. */
    public static volatile boolean hookSeen;

    private Double savedGamma;
    private int ticksEnabled;

    public FullbrightModule() {
        super("Fullbright", "See everything, everywhere, at full brightness.", ModuleCategory.VISUALS);
    }

    @Override
    public String icon() {
        return "item:glowstone_dust";
    }

    @Override
    public List<String> conflictingMods() {
        return List.of("sodium-fullbright");
    }

    /** Called from the LightTexture hook with vanilla's gamma. */
    public float gamma(float original) {
        return isEnabled() ? Math.max(original, brightness.getFloat()) : original;
    }

    @Override
    protected void onEnable() {
        ticksEnabled = 0;
    }

    @Override
    public void onTick() {
        if (hookSeen || savedGamma != null) return;
        // Give the renderer a moment to prove the hook works before falling back.
        if (++ticksEnabled < 20) return;
        Minecraft mc = Minecraft.getInstance();
        savedGamma = mc.options.gamma().get();
        mc.options.gamma().set(1.0);
        ShardClient.LOGGER.warn("Fullbright: light-texture hook not active; using the vanilla brightness slider instead");
    }

    @Override
    protected void onDisable() {
        if (savedGamma != null) {
            Minecraft.getInstance().options.gamma().set(savedGamma);
            savedGamma = null;
        }
    }

    @Override
    public String about() {
        return "Lights everything at full brightness by raising the gamma the light texture is built from. It shows what your client already has loaded; nothing hidden is revealed.";
    }
}
