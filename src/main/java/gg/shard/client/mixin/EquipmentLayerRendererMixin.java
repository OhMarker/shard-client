package gg.shard.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import gg.shard.client.ShardClient;
import gg.shard.client.modules.visual.HitColorModule;
import gg.shard.client.render.ItemTints;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
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
}
