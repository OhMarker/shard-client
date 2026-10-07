package gg.shard.client.mixin;

import gg.shard.client.ShardClient;
import gg.shard.client.modules.visual.ParticleMultiplierModule;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleEngine.class)
abstract class ParticleEngineMixin {
    @Inject(method = "createParticle", at = @At("HEAD"), cancellable = true)
    private void shard$filterParticle(ParticleOptions options, double x, double y, double z,
                                      double dx, double dy, double dz, CallbackInfoReturnable<Particle> cir) {
        if (!ShardClient.isReady()) return;
        if (ShardClient.modules().get(ParticleMultiplierModule.class).shouldSkip(options)) cir.setReturnValue(null);
    }
}
