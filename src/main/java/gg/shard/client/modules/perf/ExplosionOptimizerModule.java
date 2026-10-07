package gg.shard.client.modules.perf;

import gg.shard.client.module.Module;
import gg.shard.client.module.ModuleCategory;
import gg.shard.client.module.setting.BoolSetting;
import gg.shard.client.module.setting.IntSetting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Keeps crystal fights fast and readable: thins the particles that dominate a detonation, skips
 * the huge explosion emitter, and caps how many explosion sounds play in one tick. Knockback and
 * block damage are never touched. The 100-crystal scenario in BENCHMARKS.md measures this.
 */
public final class ExplosionOptimizerModule extends Module {
    private final BoolSetting skipEmitter = add(new BoolSetting("Skip explosion emitter", "Replace the huge spreading explosion with a single burst", true).group("Particles"));
    private final IntSetting explosions = add(new IntSetting("Explosions", "Explosion particles to keep", 25, 0, 100, 5, "%").group("Particles"));
    private final IntSetting smoke = add(new IntSetting("Smoke", "Smoke from explosions and fire", 50, 0, 100, 5, "%").group("Particles"));
    private final IntSetting crits = add(new IntSetting("Crits", "Critical and enchanted hit sparkles", 100, 0, 100, 5, "%").group("Particles"));
    private final IntSetting damage = add(new IntSetting("Damage hearts", "Damage indicator hearts", 100, 0, 100, 5, "%").group("Particles"));
    private final IntSetting maxSounds = add(new IntSetting("Sounds per tick", "Explosion sounds allowed in one tick", 3, 1, 10, 1, "").group("Sounds"));

    private long soundTick = Long.MIN_VALUE;
    private int soundsThisTick;

    public ExplosionOptimizerModule() {
        super("Explosion Optimizer", "Fewer explosion particles and capped explosion sounds so big fights stay smooth.", ModuleCategory.PERFORMANCE);
    }

    @Override
    public boolean defaultEnabled() {
        return true;
    }

    @Override
    public String icon() {
        return "item:tnt";
    }

    public boolean skipsEmitter() {
        return isEnabled() && skipEmitter.get();
    }

    /** Called from the ParticleEngine mixin for every particle spawn. */
    public boolean shouldSkip(ParticleOptions options) {
        if (!isEnabled()) return false;
        ParticleType<?> type = options.getType();
        int keep;
        if (type == ParticleTypes.EXPLOSION_EMITTER) return skipEmitter.get();
        if (type == ParticleTypes.EXPLOSION) keep = explosions.get();
        else if (type == ParticleTypes.CRIT || type == ParticleTypes.ENCHANTED_HIT) keep = crits.get();
        else if (type == ParticleTypes.DAMAGE_INDICATOR) keep = damage.get();
        else if (type == ParticleTypes.SMOKE || type == ParticleTypes.LARGE_SMOKE) keep = smoke.get();
        else return false;
        if (keep >= 100) return false;
        if (keep <= 0) return true;
        return ThreadLocalRandom.current().nextInt(100) >= keep;
    }

    /** Whether one more explosion sound may play during game tick {@code gameTime}. */
    public boolean allowSound(long gameTime) {
        if (!isEnabled()) return true;
        if (gameTime != soundTick) {
            soundTick = gameTime;
            soundsThisTick = 0;
        }
        soundsThisTick++;
        return soundsThisTick <= maxSounds.get();
    }
}
