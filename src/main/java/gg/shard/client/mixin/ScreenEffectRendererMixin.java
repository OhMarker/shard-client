package gg.shard.client.mixin;

import gg.shard.client.ShardClient;
import gg.shard.client.modules.visual.LowFireModule;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Low Fire: vanilla's renderFire translates each flame quad by (±0.24, -0.3, 0) and colours it
 * with alpha 0.9; both constants are adjusted here. Both hooks are optional so a vanilla change
 * degrades the feature instead of crashing.
 */
@Mixin(ScreenEffectRenderer.class)
abstract class ScreenEffectRendererMixin {
    @ModifyArg(method = "renderFire", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"), index = 1, require = 0)
    private static float shard$fireHeight(float y) {
        return ShardClient.isReady() ? ShardClient.modules().get(LowFireModule.class).fireY(y) : y;
    }

    @ModifyArg(method = "renderFire", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;setColor(FFFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;"), index = 3, require = 0)
    private static float shard$fireAlpha(float alpha) {
        return ShardClient.isReady() ? ShardClient.modules().get(LowFireModule.class).fireAlpha(alpha) : alpha;
    }
}
