package gg.shard.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import gg.shard.client.render.ItemTints;
import net.minecraft.client.model.Model;
//? if >=1.21.9 {
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
//?} else {
/*import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
*///?}
import net.minecraft.client.renderer.blockentity.BannerRenderer;
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
    //? if <1.21.9 {
    /*// Before 1.21.9 the base and the layers are drawn straight into buffers (renderPatterns).
    //? if >=1.21.2 {
    private static final String RENDER_PATTERNS = "renderPatterns(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/model/geom/ModelPart;Lnet/minecraft/client/resources/model/Material;ZLnet/minecraft/world/item/DyeColor;Lnet/minecraft/world/level/block/entity/BannerPatternLayers;ZZ)V";
    @WrapOperation(method = RENDER_PATTERNS,
            at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/resources/model/Material;buffer(Lnet/minecraft/client/renderer/MultiBufferSource;Ljava/util/function/Function;ZZ)Lcom/mojang/blaze3d/vertex/VertexConsumer;"),
            require = 0)
    private static VertexConsumer shard$translucentBase(Material material, MultiBufferSource buffers, Function<net.minecraft.resources.Identifier, RenderType> factory,
                                                        boolean sheeted, boolean foil, Operation<VertexConsumer> original) {
        return original.call(material, buffers, ItemTints.shieldTranslucent() ? (Function<net.minecraft.resources.Identifier, RenderType>) RenderTypes::entityTranslucent : factory,
                sheeted, foil);
    }
    //?} else {
    /^// Before 1.21.2 renderPatterns has no "sheeted" flag and the base buffer takes none either.
    private static final String RENDER_PATTERNS = "renderPatterns(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/model/geom/ModelPart;Lnet/minecraft/client/resources/model/Material;ZLnet/minecraft/world/item/DyeColor;Lnet/minecraft/world/level/block/entity/BannerPatternLayers;Z)V";
    @WrapOperation(method = RENDER_PATTERNS,
            at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/resources/model/Material;buffer(Lnet/minecraft/client/renderer/MultiBufferSource;Ljava/util/function/Function;Z)Lcom/mojang/blaze3d/vertex/VertexConsumer;"),
            require = 0)
    private static VertexConsumer shard$translucentBase(Material material, MultiBufferSource buffers, Function<net.minecraft.resources.Identifier, RenderType> factory,
                                                        boolean foil, Operation<VertexConsumer> original) {
        return original.call(material, buffers, ItemTints.shieldTranslucent() ? (Function<net.minecraft.resources.Identifier, RenderType>) RenderTypes::entityTranslucent : factory,
                foil);
    }
    ^///?}

    @WrapOperation(method = RENDER_PATTERNS,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/geom/ModelPart;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V"),
            require = 0)
    private static void shard$tintBase(ModelPart part, PoseStack pose, VertexConsumer buffer, int light, int overlay, Operation<Void> original) {
        int tint = ItemTints.shield();
        if (tint == ItemTints.NONE) original.call(part, pose, buffer, light, overlay);
        else part.render(pose, buffer, light, overlay, ItemTints.multiply(-1, tint));
    }

    @WrapOperation(method = "renderPatternLayer", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/model/geom/ModelPart;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"),
            require = 0)
    private static void shard$tintLayer(ModelPart part, PoseStack pose, VertexConsumer buffer, int light, int overlay, int color, Operation<Void> original) {
        original.call(part, pose, buffer, light, overlay, ItemTints.multiply(color, ItemTints.shield()));
    }
    *///?}

    // 26.1: submitPatterns no longer draws the shield base (ShieldSpecialRendererMixin tints it);
    // only the pattern layers are left here.
    //? if >=1.21.9 <26.1 {
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

    // 26.2: the pattern layers are submitted to an OrderedSubmitNodeCollector; 26.3 drops the
    // crumbling argument and takes a UvMapping.
    //? if >=26.3 {
    /*@WrapOperation(method = "submitPatternLayer", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/OrderedSubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/UvMapping;I)V"),
            require = 0)
    private static void shard$tintLayer(net.minecraft.client.renderer.OrderedSubmitNodeCollector collector, Model<?> model, Object state, PoseStack pose, RenderType type, int light, int overlay,
                                        int color, net.minecraft.client.renderer.texture.UvMapping uv, int outline, Operation<Void> original) {
        original.call(collector, model, state, pose, type, light, overlay, ItemTints.multiply(color, ItemTints.shield()), uv, outline);
    }
    *///?} else if >=26.2 {
    /*@WrapOperation(method = "submitPatternLayer", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/OrderedSubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"),
            require = 0)
    private static void shard$tintLayer(net.minecraft.client.renderer.OrderedSubmitNodeCollector collector, Model<?> model, Object state, PoseStack pose, RenderType type, int light, int overlay, int color,
                                        TextureAtlasSprite sprite, int outline, ModelFeatureRenderer.CrumblingOverlay crumbling, Operation<Void> original) {
        original.call(collector, model, state, pose, type, light, overlay, ItemTints.multiply(color, ItemTints.shield()), sprite, outline, crumbling);
    }
    *///?} else if >=1.21.9 {
    @WrapOperation(method = "submitPatternLayer", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"),
            require = 0)
    private static void shard$tintLayer(SubmitNodeCollector collector, Model<?> model, Object state, PoseStack pose, RenderType type, int light, int overlay, int color,
                                        TextureAtlasSprite sprite, int outline, ModelFeatureRenderer.CrumblingOverlay crumbling, Operation<Void> original) {
        original.call(collector, model, state, pose, type, light, overlay, ItemTints.multiply(color, ItemTints.shield()), sprite, outline, crumbling);
    }
    //?}
}
