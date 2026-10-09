package gg.shard.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import gg.shard.client.render.ItemTints;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Function;

/**
 * Shield tint and opacity on shields with banner patterns: the base is drawn by
 * BannerRenderer.submitPatterns with entity_solid (swapped for entity_translucent while the
 * first-person shield should be see-through), and the pattern layers (already translucent) get the
 * same multiplier. Outside a first-person shield {@link ItemTints#shield()} is white and opaque,
 * so banners and other shields are untouched.
 */
@Mixin(BannerRenderer.class)
abstract class BannerRendererMixin {
    // 26.1: submitPatterns no longer draws the shield base (ShieldSpecialRendererMixin tints it);
    // only the pattern layers are left here.
    //? if <26.1 {
    @WrapOperation(method = "submitPatterns", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/resources/model/Material;renderType(Ljava/util/function/Function;)Lnet/minecraft/client/renderer/rendertype/RenderType;"),
            require = 0)
    private static RenderType shard$translucentBase(Material material, Function<net.minecraft.resources.Identifier, RenderType> factory, Operation<RenderType> original) {
        return original.call(material, ItemTints.shieldTranslucent() ? (Function<net.minecraft.resources.Identifier, RenderType>) RenderTypes::entityTranslucent : factory);
    }

    @WrapOperation(method = "submitPatterns", at = @At(value = "INVOKE", ordinal = 0,
            target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"),
            require = 0)
    private static void shard$tintBase(SubmitNodeCollector collector, Model<?> model, Object state, PoseStack pose, RenderType type, int light, int overlay, int color,
                                       TextureAtlasSprite sprite, int outline, ModelFeatureRenderer.CrumblingOverlay crumbling, Operation<Void> original) {
        original.call(collector, model, state, pose, type, light, overlay, ItemTints.multiply(color, ItemTints.shield()), sprite, outline, crumbling);
    }
    //?}

    @WrapOperation(method = "submitPatternLayer", at = @At(value = "INVOKE",

            target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"),
            require = 0)
    private static void shard$tintLayer(SubmitNodeCollector collector, Model<?> model, Object state, PoseStack pose, RenderType type, int light, int overlay, int color,
                                        TextureAtlasSprite sprite, int outline, ModelFeatureRenderer.CrumblingOverlay crumbling, Operation<Void> original) {
        original.call(collector, model, state, pose, type, light, overlay, ItemTints.multiply(color, ItemTints.shield()), sprite, outline, crumbling);
    }
}
