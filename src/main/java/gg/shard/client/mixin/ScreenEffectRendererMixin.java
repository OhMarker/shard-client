package gg.shard.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.VertexConsumer;
import gg.shard.client.ShardClient;
import gg.shard.client.modules.visual.LowFireModule;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Low Fire, your screen: vanilla's renderFire translates each flame quad by (±0.24, -0.3, 0) and
 * colours it white with alpha 0.9; the height, colour and opacity are adjusted here. Both hooks
 * are optional so a vanilla change degrades the feature instead of crashing.
 */
@Mixin(ScreenEffectRenderer.class)
abstract class ScreenEffectRendererMixin {
    @ModifyArg(method = "renderFire", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"), index = 1, require = 0)
    private static float shard$fireHeight(float y) {
        return ShardClient.isReady() ? ShardClient.modules().get(LowFireModule.class).fireY(y) : y;
    }

    @WrapOperation(method = "renderFire", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;setColor(FFFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;"), require = 0)
    private static VertexConsumer shard$fireColor(VertexConsumer consumer, float r, float g, float b, float a, Operation<VertexConsumer> original) {
        if (!ShardClient.isReady()) return original.call(consumer, r, g, b, a);
        float[] c = ShardClient.modules().get(LowFireModule.class).screenColor(r, g, b, a);
        return original.call(consumer, c[0], c[1], c[2], c[3]);
    }
}
