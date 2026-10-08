package gg.shard.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import gg.shard.client.ShardClient;
import gg.shard.client.modules.visual.LowShieldModule;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Shield: right after vanilla pushes the hand's pose the shield is moved in view space (so
 * "lower" is down the screen), and the item itself is scaled in its own space just before it is
 * drawn (so "width" is the shield's face, around its own centre). Both hooks are optional.
 */
@Mixin(ItemInHandRenderer.class)
abstract class ItemInHandRendererMixin {
    @Inject(method = "renderArmWithItem",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", ordinal = 0, shift = At.Shift.AFTER),
            require = 0)
    private void shard$moveShield(AbstractClientPlayer player, float partialTick, float pitch, InteractionHand hand, float swingProgress,
                                  ItemStack stack, float equipProgress, PoseStack poseStack, SubmitNodeCollector collector, int light, CallbackInfo ci) {
        if (!ShardClient.isReady()) return;
        float[] offset = ShardClient.modules().get(LowShieldModule.class).offsetFor(player, hand, stack);
        if (offset != null) poseStack.translate(offset[0], offset[1], 0f);
    }

    @WrapOperation(method = "renderArmWithItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V"),
            require = 0)
    private void shard$scaleShield(ItemInHandRenderer self, LivingEntity entity, ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                                   SubmitNodeCollector collector, int light, Operation<Void> original,
                                   @Local(argsOnly = true) AbstractClientPlayer player, @Local(argsOnly = true) InteractionHand hand) {
        float[] scale = ShardClient.isReady() ? ShardClient.modules().get(LowShieldModule.class).scaleFor(player, hand, stack) : null;
        if (scale == null) {
            original.call(self, entity, stack, context, poseStack, collector, light);
            return;
        }
        poseStack.pushPose();
        poseStack.scale(scale[0], scale[1], scale[2]);
        original.call(self, entity, stack, context, poseStack, collector, light);
        poseStack.popPose();
    }
}
