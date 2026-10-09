package gg.shard.client.mixin;

// Before 1.21.2 explosion packets finalize an Explosion on the client (ClientPacketListenerMixin
// filters the sound and particle in handleExplosion from 1.21.2).
//? if <1.21.2 {
/*import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import gg.shard.client.ShardClient;
import gg.shard.client.compat.ServerExplosions;
import gg.shard.client.modules.combat.AnchorOptimizerModule;
import gg.shard.client.modules.perf.ExplosionOptimizerModule;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/^* Anchor Optimizer and Explosion Optimizer on server explosions before 1.21.2 (see ServerExplosions). ^/
@Mixin(Explosion.class)
abstract class ExplosionMixin {
    @WrapOperation(method = "finalizeExplosion", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;playLocalSound(DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFZ)V"))
    private void shard$explosionSound(Level level, double x, double y, double z, SoundEvent sound, SoundSource source,
                                      float volume, float pitch, boolean delay, Operation<Void> original) {
        if (ServerExplosions.active() && ShardClient.isReady()) {
            if (ServerExplosions.anchorPredicted() && ShardClient.modules().get(AnchorOptimizerModule.class).mutesServerSound()) return;
            if (!ShardClient.modules().get(ExplosionOptimizerModule.class).allowSound(level.getGameTime())) return;
        }
        original.call(level, x, y, z, sound, source, volume, pitch, delay);
    }

    @WrapOperation(method = "finalizeExplosion", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"))
    private void shard$explosionParticle(Level level, ParticleOptions options, double x, double y, double z,
                                         double dx, double dy, double dz, Operation<Void> original) {
        if (ServerExplosions.active() && ShardClient.isReady()) {
            if (ServerExplosions.anchorPredicted() && ShardClient.modules().get(AnchorOptimizerModule.class).skipsServerParticles()) return;
            if (options.getType() == ParticleTypes.EXPLOSION_EMITTER && ShardClient.modules().get(ExplosionOptimizerModule.class).skipsEmitter()) {
                original.call(level, ParticleTypes.EXPLOSION, x, y, z, 0.0, 0.0, 0.0);
                return;
            }
        }
        original.call(level, options, x, y, z, dx, dy, dz);
    }
}
*///?}
