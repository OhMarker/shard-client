package gg.shard.client.mixin;

import gg.shard.client.ShardClient;
import gg.shard.client.modules.visual.DeathAnimationModule;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** No Death Animation: strip the death tilt and red tint from the extracted render state. */
@Mixin(LivingEntityRenderer.class)
abstract class LivingEntityRendererMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
            at = @At("TAIL"))
    private void shard$deathState(LivingEntity entity, LivingEntityRenderState state, float partialTick, CallbackInfo ci) {
        if (!ShardClient.isReady()) return;
        DeathAnimationModule module = ShardClient.modules().get(DeathAnimationModule.class);
        if (state.deathTime > 0f && module.hidesTilt(entity)) state.deathTime = 0f;
        if (state.hasRedOverlay && module.skipsTint(entity)) state.hasRedOverlay = false;
        if (state.hasRedOverlay && !ShardClient.modules().get(gg.shard.client.modules.visual.HitColorModule.class).showsOverlay(entity)) state.hasRedOverlay = false;
    }
}
