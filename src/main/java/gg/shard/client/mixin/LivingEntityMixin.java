package gg.shard.client.mixin;

import gg.shard.client.event.ShardEvents;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
abstract class LivingEntityMixin {
    @Inject(method = "handleEntityEvent", at = @At("HEAD"))
    private void shard$onEntityEvent(byte id, CallbackInfo ci) {
        if (id == EntityEvent.PROTECTED_FROM_DEATH) {
            ShardEvents.fireTotemPop((LivingEntity) (Object) this);
        }
    }
}
