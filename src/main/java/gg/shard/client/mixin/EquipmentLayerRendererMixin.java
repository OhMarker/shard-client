package gg.shard.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import gg.shard.client.ShardClient;
import gg.shard.client.modules.visual.HitColorModule;
import gg.shard.client.render.ItemTints;
import net.minecraft.client.model.Model;
//? if >=1.21.9 {
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
//?} else {
/*import com.mojang.blaze3d.vertex.VertexConsumer;
*///?}
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Hit Color on armour (and elytra, which goes through the same renderer): armour pipelines are
 * built with NO_OVERLAY, so the hurt overlay never reaches them; instead the layer colour is
 * multiplied by the hit tint while the wearer's render state has the red overlay. Glint passes
 * are left alone.
 */
@Mixin(EquipmentLayerRenderer.class)
abstract class EquipmentLayerRendererMixin {
    //? if >=26.3 {
    /*// 26.3: no crumbling argument, a UvMapping, and the glint is part of the armour type itself
    // (only the trim glint is still a separate pass).
    @WrapOperation(method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/OrderedSubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/UvMapping;I)V"),
            require = 0)
    private void shard$hitTintArmor(OrderedSubmitNodeCollector collector, Model<?> model, Object state, PoseStack pose, RenderType type, int light, int overlay,
                                    int color, net.minecraft.client.renderer.texture.UvMapping uv, int outline, Operation<Void> original) {
        if (ShardClient.isReady() && state instanceof LivingEntityRenderState living && living.hasRedOverlay && type != RenderTypes.trimmedArmorGlint()) {
            int tint = ShardClient.modules().get(HitColorModule.class).armorTint();
            if (tint != ItemTints.NONE) color = ItemTints.multiply(color, tint);
        }
        original.call(collector, model, state, pose, type, light, overlay, color, uv, outline);
    }
    *///?} else if >=1.21.9 {
    @WrapOperation(method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/OrderedSubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"),
            require = 0)
    private void shard$hitTintArmor(OrderedSubmitNodeCollector collector, Model<?> model, Object state, PoseStack pose, RenderType type, int light, int overlay,
                                    int color, TextureAtlasSprite sprite, int outline, ModelFeatureRenderer.CrumblingOverlay crumbling, Operation<Void> original) {
        if (ShardClient.isReady() && state instanceof LivingEntityRenderState living && living.hasRedOverlay && type != RenderTypes.armorEntityGlint()) {
            int tint = ShardClient.modules().get(HitColorModule.class).armorTint();
            if (tint != ItemTints.NONE) color = ItemTints.multiply(color, tint);
        }
        original.call(collector, model, state, pose, type, light, overlay, color, sprite, outline, crumbling);
    }
    //?} else {
    /*// Before 1.21.9 the layers are drawn straight into buffers (the glint rides on the armour's foil
    // buffer, so only the colour changes); the wearer's hurt state comes from ItemTints.
    //? if >=1.21.4 {
    private static final String RENDER_LAYERS = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/resources/Identifier;)V";
    //?} else {
    /^// 1.21.2/1.21.3: equipment models are named by id (EquipmentModel), not by asset key.
    private static final String RENDER_LAYERS = "renderLayers(Lnet/minecraft/world/item/equipment/EquipmentModel$LayerType;Lnet/minecraft/resources/Identifier;Lnet/minecraft/client/model/Model;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/resources/Identifier;)V";
    ^///?}

    @WrapOperation(method = RENDER_LAYERS,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/Model;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"),
            require = 0)
    private void shard$hitTintArmor(Model model, PoseStack pose, VertexConsumer buffer, int light, int overlay, int color, Operation<Void> original) {
        original.call(model, pose, buffer, light, overlay, shard$tint(color));
    }

    /^* Armour trims (drawn without a colour). ^/
    @WrapOperation(method = RENDER_LAYERS,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/Model;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V"),
            require = 0)
    private void shard$hitTintTrim(Model model, PoseStack pose, VertexConsumer buffer, int light, int overlay, Operation<Void> original) {
        int color = shard$tint(-1);
        if (color == -1) original.call(model, pose, buffer, light, overlay);
        else model.renderToBuffer(pose, buffer, light, overlay, color);
    }

    private static int shard$tint(int color) {
        if (!ShardClient.isReady() || !ItemTints.wearerHurt()) return color;
        int tint = ShardClient.modules().get(HitColorModule.class).armorTint();
        return tint == ItemTints.NONE ? color : ItemTints.multiply(color, tint);
    }
    *///?}
}
