package gg.shard.client.mixin;

// 26.3 replaced ItemInHandRenderer with FirstPersonHandsAndItemsRenderer (FirstPersonHandsMixin).
//? if <26.3 {
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import gg.shard.client.ShardClient;
import gg.shard.client.modules.visual.LowShieldModule;
import gg.shard.client.modules.visual.SmallItemsModule;
import gg.shard.client.render.ItemTints;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
//? if >=1.21.9 {
import net.minecraft.client.renderer.SubmitNodeCollector;
//?} else {
/*import net.minecraft.client.renderer.MultiBufferSource;
*///?}
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Before 1.21.9 hands and items are drawn into a MultiBufferSource instead of submitted; the
// hooks are the same with that argument type.
/**
 * Small Items: just before the held item is drawn it is moved and shrunk in hand space. Shield
 * tint/opacity: the colour is published to {@link ItemTints} for the duration of the shield's
 * submit, where ShieldSpecialRendererMixin and BannerRendererMixin pick it up.
 *
 * <p>Shield: right after vanilla pushes the hand's pose the shield is moved in view space (so
 * "lower" is down the screen), and the item itself is scaled in its own space just before it is
 * drawn (so "width" is the shield's face, around its own centre). Both hooks are optional.
 */
@Mixin(ItemInHandRenderer.class)
abstract class ItemInHandRendererMixin {
    @org.spongepowered.asm.mixin.Unique private boolean shard$armScaled;

    /** Small Items (Small Items mod look): the empty arm is drawn at 60% too, like that mod. */
    @Inject(method = "renderPlayerArm", at = @At("HEAD"), require = 0)
    //? if >=1.21.9 {
    private void shard$smallArm(PoseStack poseStack, SubmitNodeCollector collector, int light, float equipProgress, float swingProgress,
    //?} else {
    /*private void shard$smallArm(PoseStack poseStack, MultiBufferSource collector, int light, float equipProgress, float swingProgress,
    *///?}
                                HumanoidArm arm, CallbackInfo ci) {
        shard$armScaled = false;
        if (!ShardClient.isReady()) return;
        float s = ShardClient.modules().get(SmallItemsModule.class).armScale();
        if (s == 1f) return;
        poseStack.pushPose();
        poseStack.scale(s, s, s);
        shard$armScaled = true;
    }

    @Inject(method = "renderPlayerArm", at = @At("RETURN"), require = 0)
    //? if >=1.21.9 {
    private void shard$smallArmEnd(PoseStack poseStack, SubmitNodeCollector collector, int light, float equipProgress, float swingProgress,
    //?} else {
    /*private void shard$smallArmEnd(PoseStack poseStack, MultiBufferSource collector, int light, float equipProgress, float swingProgress,
    *///?}
                                   HumanoidArm arm, CallbackInfo ci) {
        if (!shard$armScaled) return;
        shard$armScaled = false;
        poseStack.popPose();
    }

    @Inject(method = "renderArmWithItem",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", ordinal = 0, shift = At.Shift.AFTER),
            require = 0)
    private void shard$moveShield(AbstractClientPlayer player, float partialTick, float pitch, InteractionHand hand, float swingProgress,
    //? if >=1.21.9 {
                                  ItemStack stack, float equipProgress, PoseStack poseStack, SubmitNodeCollector collector, int light, CallbackInfo ci) {
    //?} else {
    /*ItemStack stack, float equipProgress, PoseStack poseStack, MultiBufferSource collector, int light, CallbackInfo ci) {
    *///?}
        if (!ShardClient.isReady()) return;
        float[] offset = ShardClient.modules().get(LowShieldModule.class).offsetFor(player, hand, stack);
        if (offset != null) poseStack.translate(offset[0], offset[1], 0f);
    }

    @WrapOperation(method = "renderArmWithItem", at = @At(value = "INVOKE",
    //? if >=1.21.9 {
            target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V"),
            require = 0)
    private void shard$scaleShield(ItemInHandRenderer self, LivingEntity entity, ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                                   SubmitNodeCollector collector, int light, Operation<Void> original,
    //?} else if >=1.21.5 {
    /*target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"),
            require = 0)
    private void shard$scaleShield(ItemInHandRenderer self, LivingEntity entity, ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                                   MultiBufferSource collector, int light, Operation<Void> original,
    *///?} else {
    /*// Before 1.21.5 renderItem also takes whether it is the left hand.
    target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"),
            require = 0)
    private void shard$scaleShield(ItemInHandRenderer self, LivingEntity entity, ItemStack stack, ItemDisplayContext context, boolean leftHand,
                                   PoseStack poseStack, MultiBufferSource collector, int light, Operation<Void> original,
    *///?}
                                   @Local(argsOnly = true) AbstractClientPlayer player, @Local(argsOnly = true) InteractionHand hand) {
        //? if >=1.21.5 {
        Runnable draw = () -> original.call(self, entity, stack, context, poseStack, collector, light);
        //?} else {
        /*// Before 1.21.4 the shield's holder is not known when it is drawn (no ItemModelResolver):
        // ShieldCosmetics gets it here for the first-person shield.
        Runnable draw = () -> {
            //? if <1.21.4 {
            gg.shard.client.render.ShieldCosmetics.push(entity, context);
            //?}
            try {
                original.call(self, entity, stack, context, leftHand, poseStack, collector, light);
            } finally {
                //? if <1.21.4 {
                gg.shard.client.render.ShieldCosmetics.pop();
                //?}
            }
        };
        *///?}
        if (!ShardClient.isReady()) {
            draw.run();
            return;
        }
        LowShieldModule shield = ShardClient.modules().get(LowShieldModule.class);
        float[] scale = shield.scaleFor(player, hand, stack);
        int color = shield.colorFor(player, hand, stack);
        HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        float[] small = ShardClient.modules().get(SmallItemsModule.class).transformFor(hand, arm, stack);
        if (scale == null && small == null && color == ItemTints.NONE) {
            draw.run();
            return;
        }
        poseStack.pushPose();
        if (small != null) {
            poseStack.translate(small[1], small[2], small[3]);
            poseStack.scale(small[0], small[0], small[0]);
        }
        if (scale != null) poseStack.scale(scale[0], scale[1], scale[2]);
        ItemTints.beginShield(color);
        try {
            draw.run();
        } finally {
            ItemTints.endShield();
            poseStack.popPose();
        }
    }
}
//?}
