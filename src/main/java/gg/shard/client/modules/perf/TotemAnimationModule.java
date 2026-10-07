package gg.shard.client.modules.perf;

import gg.shard.client.module.Module;
import gg.shard.client.module.ModuleCategory;

/**
 * Hides the full-screen totem pop animation. The animation renders a large item model over the
 * whole screen for 30 ticks and is a measurable frame-time spike exactly when you least want one.
 * The pop sound, particles and the HUD counter still fire.
 */
public final class TotemAnimationModule extends Module {
    public TotemAnimationModule() {
        super("No Totem Animation", "Skip the full-screen totem pop animation.", ModuleCategory.PERFORMANCE);
    }

    public boolean hideAnimation() {
        return isEnabled();
    }
}
