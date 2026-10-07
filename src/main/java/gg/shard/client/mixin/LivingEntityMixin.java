package gg.shard.client.mixin;

import gg.shard.client.ShardClient;
import gg.shard.client.modules.visual.DeathAnimationModule;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * No Death Animation's "remove instantly": on the client, drop a dying entity on its first
 * death tick instead of after vanilla's 20-tick animation. Never touches the local player.
 */
@Mixin(LivingEntity.class)
abstract class LivingEntityMixin {
    @Inject(method = "tickDeath", at = @At("HEAD"), cancellable = true)
    private void shard$instantDespawn(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!self.level().isClientSide() || !ShardClient.isReady() || self.deathTime != 0) return;
        if (ShardClient.modules().get(DeathAnimationModule.class).removesInstantly(self)) {
            self.remove(Entity.RemovalReason.KILLED);
            ci.cancel();
        }
    }
}
