package gg.shard.client.modules.utility;

import gg.shard.client.module.Module;
import gg.shard.client.module.ModuleCategory;
import gg.shard.client.module.setting.IntSetting;
import net.minecraft.client.resources.sounds.SoundInstance;

/**
 * Sounds: separate volumes for explosions (crystals and anchors), hurt sounds and hit sounds, so
 * totem pops and footsteps are not drowned out mid-fight. Totem pop volume lives in Totem Pops.
 */
public final class SoundsModule extends Module {
    private final IntSetting explosions = add(new IntSetting("Explosions", "Crystal and anchor explosions", 60, 0, 150, 5, "%"));
    private final IntSetting hurt = add(new IntSetting("Hurt sounds", "Players getting hurt", 100, 0, 150, 5, "%"));
    private final IntSetting hits = add(new IntSetting("Hit sounds", "Sweep, crit, strong and weak attack sounds", 100, 0, 150, 5, "%"));
    private final IntSetting glass = add(new IntSetting("Crystal break", "The glass sound when a crystal is dropped by Crystal Optimizer", 100, 0, 150, 5, "%"));

    public SoundsModule() {
        super("Sounds", "Separate volumes for explosions, hurt and hit sounds.", ModuleCategory.UTILITY);
    }

    @Override
    public String icon() {
        return "sound";
    }

    @Override
    public String about() {
        return "Turns down (or up) the sounds that fill a crystal fight: explosions, players getting hurt and attack sounds, each on its own slider, "
                + "so you still hear totem pops and footsteps. The totem pop's own volume is in Totem Pops. Only what you hear changes.";
    }

    /** Multiplier for a sound about to play. */
    public float factor(SoundInstance sound) {
        if (!isEnabled() || sound == null || sound.getIdentifier() == null) return 1f;
        String path = sound.getIdentifier().getPath();
        if (path.startsWith("entity.generic.explode")) return explosions.get() / 100f;
        if (path.startsWith("entity.player.hurt")) return hurt.get() / 100f;
        if (path.startsWith("entity.player.attack")) return hits.get() / 100f;
        if (path.startsWith("block.glass.break")) return glass.get() / 100f;
        return 1f;
    }
}
