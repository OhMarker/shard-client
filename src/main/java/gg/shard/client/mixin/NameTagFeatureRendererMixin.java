package gg.shard.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import gg.shard.client.ShardClient;
import gg.shard.client.modules.visual.NametagsModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Nametags scale and background opacity. 1.21.11 queues every name tag through
 * {@code NameTagFeatureRenderer.Storage.add}, which scales the pose by 0.025 and reads the
 * background from {@code Options.getBackgroundOpacity(0.25f)}; both are adjusted here.
 */
@Mixin(targets = "net.minecraft.client.renderer.feature.NameTagFeatureRenderer$Storage")
abstract class NameTagFeatureRendererMixin {
    @WrapOperation(method = "add", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V"), require = 0)
    private void shard$scale(PoseStack pose, float x, float y, float z, Operation<Void> original) {
        if (ShardClient.isReady()) {
            float k = ShardClient.modules().get(NametagsModule.class).scale(1f);
            x *= k;
            y *= k;
            z *= k;
        }
        original.call(pose, x, y, z);
    }

    @ModifyExpressionValue(method = "add", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Options;getBackgroundOpacity(F)F"), require = 0)
    private float shard$background(float original) {
        return ShardClient.isReady() ? ShardClient.modules().get(NametagsModule.class).backgroundOpacity(original) : original;
    }
}
