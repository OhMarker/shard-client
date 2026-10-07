package gg.shard.client.modules.visual;

import gg.shard.client.module.Module;
import gg.shard.client.module.ModuleCategory;
import gg.shard.client.module.setting.BoolSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * Cleaner kills: no falling-over tilt, no red death tint, and optionally no lingering body.
 * Equivalent to Walksy's No Death Animation (which it yields to when installed).
 */
public final class DeathAnimationModule extends Module {
    private final BoolSetting hideTilt = add(new BoolSetting("Hide tilt", "Skip the falling-over death animation", true));
    private final BoolSetting skipTint = add(new BoolSetting("Skip red tint", "Do not tint dying entities red", true));
    private final BoolSetting removeInstantly = add(new BoolSetting("Remove instantly", "Drop dead entities at once instead of after a second", false));
    private final BoolSetting playersOnly = add(new BoolSetting("Players only", "Leave mobs and animals alone", false));

    public DeathAnimationModule() {
        super("No Death Animation", "Dead players and mobs stop tilting and flashing red.", ModuleCategory.VISUALS);
    }

    @Override
    public boolean defaultEnabled() {
        return true;
    }

    @Override
    public String icon() {
        return "glyph:skull";
    }

    @Override
    public List<String> conflictingMods() {
        return List.of("nodeathanimation");
    }

    private boolean applies(LivingEntity entity) {
        if (!isEnabled() || entity == Minecraft.getInstance().player) return false;
        return !playersOnly.get() || entity instanceof Player;
    }

    public boolean hidesTilt(LivingEntity entity) {
        return hideTilt.get() && applies(entity);
    }

    public boolean skipsTint(LivingEntity entity) {
        return skipTint.get() && applies(entity) && entity.isDeadOrDying();
    }

    public boolean removesInstantly(LivingEntity entity) {
        return removeInstantly.get() && applies(entity);
    }
}
