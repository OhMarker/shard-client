package gg.shard.client.mixin;

// Before 1.21.2 elytra have their own layer (EquipmentLayerRenderer draws them from 1.21.2).
//? if <1.21.2 {
/*import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import gg.shard.client.ShardClient;
import gg.shard.client.modules.visual.HitColorModule;
import gg.shard.client.render.ItemTints;
import net.minecraft.client.model.ElytraModel;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/^* Hit Color on elytra before 1.21.2, as EquipmentLayerRendererMixin does for armour. ^/
@Mixin(ElytraLayer.class)
abstract class ElytraLayerMixin {
    @WrapOperation(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ElytraModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V"))
    private void shard$hitTint(ElytraModel<?> model, PoseStack pose, VertexConsumer buffer, int light, int overlay, Operation<Void> original) {
        int tint = ShardClient.isReady() && ItemTints.wearerHurt() ? ShardClient.modules().get(HitColorModule.class).armorTint() : ItemTints.NONE;
        if (tint == ItemTints.NONE) original.call(model, pose, buffer, light, overlay);
        else model.renderToBuffer(pose, buffer, light, overlay, tint);
    }
}
*///?}
