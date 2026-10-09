package gg.shard.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import gg.shard.client.render.ItemTints;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.object.equipment.ShieldModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.ShieldSpecialRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Shield tint and opacity for the first-person shield only (the context is set by
 * ItemInHandRendererMixin while that shield is submitted). The shield model normally draws with
 * entity_solid, which ignores alpha; while it should be see-through it uses entity_translucent
 * on the same atlas instead. The tint goes in the per-model colour vanilla passes as -1.
 */
@Mixin(ShieldSpecialRenderer.class)
abstract class ShieldSpecialRendererMixin {
    @WrapOperation(method = "submit(Lnet/minecraft/core/component/DataComponentMap;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;IIZI)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/object/equipment/ShieldModel;renderType(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;"),
            require = 0)
    private RenderType shard$translucentShield(ShieldModel model, Identifier atlas, Operation<RenderType> original) {
        return ItemTints.shieldTranslucent() ? RenderTypes.entityTranslucent(atlas) : original.call(model, atlas);
    }

    @WrapOperation(method = "submit(Lnet/minecraft/core/component/DataComponentMap;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;IIZI)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModelPart(Lnet/minecraft/client/model/geom/ModelPart;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ZZILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;I)V"),
            require = 0)
    private void shard$tintShield(SubmitNodeCollector collector, ModelPart part, PoseStack pose, RenderType type, int light, int overlay,
                                  TextureAtlasSprite sprite, boolean sheeted, boolean foil, int color, ModelFeatureRenderer.CrumblingOverlay crumbling,
                                  int outline, Operation<Void> original) {
        original.call(collector, part, pose, type, light, overlay, sprite, sheeted, foil, ItemTints.multiply(color, ItemTints.shield()), crumbling, outline);
    }
}
