package gg.shard.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.VertexConsumer;
import gg.shard.client.ShardClient;
import gg.shard.client.modules.visual.LowFireModule;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.feature.FlameFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Low Fire, burning players and mobs: the flame column is squashed vertically, tinted, and drawn
 * with the translucent sheet when it should be see-through. Optional hooks.
 */
@Mixin(FlameFeatureRenderer.class)
abstract class FlameFeatureRendererMixin {
    @ModifyArg(method = "renderFlame", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack$Pose;scale(FFF)V"), index = 1, require = 0)
    private float shard$flameHeight(float y) {
        return ShardClient.isReady() ? y * ShardClient.modules().get(LowFireModule.class).entityHeightFactor() : y;
    }

    @WrapOperation(method = "renderFlame", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/Sheets;cutoutBlockSheet()Lnet/minecraft/client/renderer/rendertype/RenderType;"), require = 0)
    private RenderType shard$flameSheet(Operation<RenderType> original) {
        if (ShardClient.isReady() && ShardClient.modules().get(LowFireModule.class).entityTranslucent()) return Sheets.translucentItemSheet();
        return original.call();
    }

    @WrapOperation(method = "fireVertex", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;setColor(I)Lcom/mojang/blaze3d/vertex/VertexConsumer;"), require = 0)
    private static VertexConsumer shard$flameColor(VertexConsumer consumer, int color, Operation<VertexConsumer> original) {
        return original.call(consumer, ShardClient.isReady() ? ShardClient.modules().get(LowFireModule.class).entityColor(color) : color);
    }
}
