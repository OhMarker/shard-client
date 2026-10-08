package gg.shard.client.modules.visual;

import gg.shard.client.module.Module;
import gg.shard.client.module.ModuleCategory;
import gg.shard.client.module.setting.DoubleSetting;

/** Shrinks end crystals so the player behind them stays visible. Applied in EndCrystalRendererMixin. */
public final class CrystalTweaksModule extends Module {
    private final DoubleSetting scale = add(new DoubleSetting("Scale", "Crystal model size", 0.65, 0.25, 1.0, 0.05, "x"));

    public CrystalTweaksModule() {
        super("Crystal Size", "Smaller end crystals so you can see through the fight.", ModuleCategory.VISUALS);
    }

    @Override
    public boolean defaultEnabled() {
        return true;
    }

    public float renderScale() {
        return isEnabled() ? scale.getFloat() : 1f;
    }

    @Override
    public String icon() {
        return "item:end_crystal";
    }

    @Override
    public String about() {
        return "Draws end crystals smaller so the player behind them stays visible. Hitboxes, placement and explosions are unchanged; this is purely visual.";
    }
}
