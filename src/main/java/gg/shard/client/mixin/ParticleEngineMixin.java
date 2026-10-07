package gg.shard.client.mixin;

import gg.shard.client.ShardClient;
import gg.shard.client.modules.perf.ExplosionOptimizerModule;
import gg.shard.client.modules.visual.TotemPopModule;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.ThreadLocalRandom;

/** One hook for every particle spawn: the explosion optimizer and totem particle thinning. */
@Mixin(ParticleEngine.class)
abstract class ParticleEngineMixin {
    @Inject(method = "createParticle", at = @At("HEAD"), cancellable = true)
    private void shard$filterParticle(ParticleOptions options, double x, double y, double z,
                                      double dx, double dy, double dz, CallbackInfoReturnable<Particle> cir) {
        if (!ShardClient.isReady()) return;
        if (options.getType() == ParticleTypes.TOTEM_OF_UNDYING) {
            int keep = ShardClient.modules().get(TotemPopModule.class).particlePercent();
            if (keep <= 0 || (keep < 100 && ThreadLocalRandom.current().nextInt(100) >= keep)) cir.setReturnValue(null);
            return;
        }
        if (ShardClient.modules().get(ExplosionOptimizerModule.class).shouldSkip(options)) cir.setReturnValue(null);
    }
}
