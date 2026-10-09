package gg.shard.client.mixin;

import gg.shard.client.ShardClient;
import gg.shard.client.modules.perf.EntityOptimizerModule;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Entity Optimizer: pearl portal particles and the per-tick particle cap (spawn side only). */
@Mixin(ParticleEngine.class)
abstract class ParticleLimitMixin {
    @Inject(method = "createParticle", at = @At("HEAD"), cancellable = true, require = 0)
    private void shard$limitParticle(ParticleOptions options, double x, double y, double z,
                                     double dx, double dy, double dz, CallbackInfoReturnable<Particle> cir) {
        if (ShardClient.isReady() && ShardClient.modules().get(EntityOptimizerModule.class).skipsParticle(options)) cir.setReturnValue(null);
    }
}
