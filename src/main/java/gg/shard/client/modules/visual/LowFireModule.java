package gg.shard.client.modules.visual;

import gg.shard.client.module.Module;
import gg.shard.client.module.ModuleCategory;
import gg.shard.client.module.setting.DoubleSetting;
import gg.shard.client.module.setting.IntSetting;

/** Pushes the first-person fire overlay down and fades it so you can still see the fight. */
public final class LowFireModule extends Module {
    private final DoubleSetting height = add(new DoubleSetting("Lower by", "How far down to move the flames", 0.5, 0.0, 1.0, 0.05));
    private final IntSetting opacity = add(new IntSetting("Opacity", "Flame opacity", 80, 20, 100, 5, "%"));

    public LowFireModule() {
        super("Low Fire", "Smaller, lower, see-through flames while you burn.", ModuleCategory.VISUALS);
    }

    @Override
    public boolean defaultEnabled() {
        return true;
    }

    @Override
    public String icon() {
        return "item:fire_charge";
    }

    /** Vanilla translates the flame quad by -0.3; this pushes it further down. */
    public float fireY(float original) {
        return isEnabled() ? original - (float) (height.get() * 0.6) : original;
    }

    public float fireAlpha(float original) {
        return isEnabled() ? original * opacity.get() / 100f : original;
    }
}
