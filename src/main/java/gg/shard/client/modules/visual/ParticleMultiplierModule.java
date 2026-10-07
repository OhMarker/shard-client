package gg.shard.client.modules.visual;

import gg.shard.client.module.Module;
import gg.shard.client.module.ModuleCategory;
import gg.shard.client.module.setting.IntSetting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Keeps only a percentage of the particles that dominate crystal fights. Also the biggest
 * single frame-time win in a 100-crystal scenario (see BENCHMARKS.md).
 */
public final class ParticleMultiplierModule extends Module {
    private final IntSetting explosions = add(new IntSetting("Explosions", "Explosion particles to keep", 25, 0, 100, 5, "%"));
    private final IntSetting totems = add(new IntSetting("Totem pops", "Totem particles to keep", 50, 0, 100, 5, "%"));
    private final IntSetting crits = add(new IntSetting("Crits", "Critical hit and enchanted hit sparkles", 100, 0, 100, 5, "%"));
    private final IntSetting damage = add(new IntSetting("Damage hearts", "Damage indicator hearts", 100, 0, 100, 5, "%"));
    private final IntSetting smoke = add(new IntSetting("Smoke", "Smoke from explosions and fire", 50, 0, 100, 5, "%"));

    public ParticleMultiplierModule() {
        super("Particle Multiplier", "Thin out explosion, totem and hit particles so fights stay readable and fast.", ModuleCategory.VISUALS);
    }

    @Override
    public boolean defaultEnabled() {
        return true;
    }

    /** Called from the ParticleEngine mixin for every particle spawn. */
    public boolean shouldSkip(ParticleOptions options) {
        if (!isEnabled()) return false;
        ParticleType<?> type = options.getType();
        int keep;
        if (type == ParticleTypes.EXPLOSION || type == ParticleTypes.EXPLOSION_EMITTER) keep = explosions.get();
        else if (type == ParticleTypes.TOTEM_OF_UNDYING) keep = totems.get();
        else if (type == ParticleTypes.CRIT || type == ParticleTypes.ENCHANTED_HIT) keep = crits.get();
        else if (type == ParticleTypes.DAMAGE_INDICATOR) keep = damage.get();
        else if (type == ParticleTypes.SMOKE || type == ParticleTypes.LARGE_SMOKE) keep = smoke.get();
        else return false;
        if (keep >= 100) return false;
        if (keep <= 0) return true;
        return ThreadLocalRandom.current().nextInt(100) >= keep;
    }
}
