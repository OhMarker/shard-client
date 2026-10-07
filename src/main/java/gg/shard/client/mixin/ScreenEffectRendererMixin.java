package gg.shard.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import gg.shard.client.ShardClient;
import gg.shard.client.modules.perf.TotemAnimationModule;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Optional removal of the full-screen totem pop animation (a measurable frame-time spike). */
@Mixin(ScreenEffectRenderer.class)
abstract class ScreenEffectRendererMixin {
    @Inject(method = "renderItemActivationAnimation", at = @At("HEAD"), cancellable = true, require = 0)
    private void shard$hideTotemAnimation(PoseStack poseStack, float partialTick, SubmitNodeCollector collector, CallbackInfo ci) {
        if (ShardClient.isReady() && ShardClient.modules().get(TotemAnimationModule.class).hideAnimation()) ci.cancel();
    }
}
