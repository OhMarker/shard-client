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
import gg.shard.client.render.ShieldCosmetics;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The shield cosmetic: when the holder wears one (ShieldCosmetics), the extracted components carry
 * its texture and the shield is drawn with it instead of vanilla's base and banner patterns, with
 * the Shield module's tint and opacity on top.
 *
 * <p>Shield tint and opacity for the first-person shield only (the context is set by
 * ItemInHandRendererMixin while that shield is submitted). The shield model normally draws with
 * entity_solid, which ignores alpha; while it should be see-through it uses entity_translucent
 * on the same atlas instead. The tint goes in the per-model colour vanilla passes as -1.
 */
@Mixin(ShieldSpecialRenderer.class)
abstract class ShieldSpecialRendererMixin {
    @Shadow @Final private ShieldModel model;

    @Inject(method = "extractArgument(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/core/component/DataComponentMap;",
            at = @At("RETURN"), cancellable = true)
    private void shard$cosmetic(ItemStack stack, CallbackInfoReturnable<DataComponentMap> cir) {
        Identifier texture = ShieldCosmetics.currentTexture();
        if (texture != null) {
            DataComponentMap base = cir.getReturnValue();
            cir.setReturnValue(new ShieldCosmetics.Skinned(base == null ? DataComponentMap.EMPTY : base, texture));
        }
    }

    @Inject(method = "submit(Lnet/minecraft/core/component/DataComponentMap;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;IIZI)V",
            at = @At("HEAD"), cancellable = true)
    private void shard$submitCosmetic(DataComponentMap components, ItemDisplayContext context, PoseStack pose, SubmitNodeCollector collector,
                                      int light, int overlay, boolean foil, int outline, CallbackInfo ci) {
        if (!(components instanceof ShieldCosmetics.Skinned skinned)) return;
        ci.cancel();
        // The cosmetic is drawn in vanilla's 64x64 shield layout on the whole texture (no atlas
        // sprite); the Shield module's first-person tint and opacity still apply.
        int color = ItemTints.multiply(-1, ItemTints.shield());
        RenderType type = ItemTints.shieldTranslucent() ? RenderTypes.entityTranslucent(skinned.texture()) : RenderTypes.entitySolid(skinned.texture());
        pose.pushPose();
        pose.scale(1.0F, -1.0F, -1.0F);
        collector.submitModelPart(model.handle(), pose, type, light, overlay, null, false, false, color, null, outline);
        collector.submitModelPart(model.plate(), pose, type, light, overlay, null, false, foil, color, null, outline);
        pose.popPose();
    }
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
