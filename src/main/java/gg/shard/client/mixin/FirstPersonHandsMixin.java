package gg.shard.client.mixin;

// 26.3: the first-person hands and items moved from ItemInHandRenderer (ItemInHandRendererMixin)
// to FirstPersonHandsAndItemsRenderer, which draws from render states extracted beforehand.
//? if >=26.3 {
/*import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import gg.shard.client.ShardClient;
import gg.shard.client.modules.visual.LowShieldModule;
import gg.shard.client.modules.visual.SmallItemsModule;
import gg.shard.client.render.ItemTints;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/^*
 * Small Items, Low Shield and the first-person shield tint, as ItemInHandRendererMixin does up to
 * 26.2: the empty arm is scaled, the shield moved right after the hand's pose is pushed, and the
 * held item moved, scaled and tinted around its submit. The modules take the camera's player.
 ^/
@Mixin(FirstPersonHandsAndItemsRenderer.class)
abstract class FirstPersonHandsMixin {
    @Unique private boolean shard$armScaled;

    @Unique
    private static AbstractClientPlayer shard$player() {
        Minecraft mc = Minecraft.getInstance();
        return mc.getCameraEntity() instanceof AbstractClientPlayer p ? p : mc.player;
    }

    @Inject(method = "renderPlayerArm", at = @At("HEAD"), require = 0)
    private void shard$smallArm(PoseStack poseStack, SubmitNodeCollector collector, int light, float inverseArmHeight, float attack,
                                HumanoidArm arm, PlayerRenderState playerState, CallbackInfo ci) {
        shard$armScaled = false;
        if (!ShardClient.isReady()) return;
        float s = ShardClient.modules().get(SmallItemsModule.class).armScale();
        if (s == 1f) return;
        poseStack.pushPose();
        poseStack.scale(s, s, s);
        shard$armScaled = true;
    }

    @Inject(method = "renderPlayerArm", at = @At("RETURN"), require = 0)
    private void shard$smallArmEnd(PoseStack poseStack, SubmitNodeCollector collector, int light, float inverseArmHeight, float attack,
                                   HumanoidArm arm, PlayerRenderState playerState, CallbackInfo ci) {
        if (!shard$armScaled) return;
        shard$armScaled = false;
        poseStack.popPose();
    }

    @Inject(method = "submitArmWithItem",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", ordinal = 0, shift = At.Shift.AFTER),
            require = 0)
    private void shard$moveShield(PlayerRenderState playerState, FirstPersonHandsAndItemsRenderState state, float partialTicks, float xRot,
                                  InteractionHand hand, float attack, ItemStack stack, float inverseArmHeight, PoseStack poseStack,
                                  SubmitNodeCollector collector, int light, CallbackInfo ci) {
        AbstractClientPlayer player = shard$player();
        if (!ShardClient.isReady() || player == null) return;
        float[] offset = ShardClient.modules().get(LowShieldModule.class).offsetFor(player, hand, stack);
        if (offset != null) poseStack.translate(offset[0], offset[1], 0f);
    }

    @WrapOperation(method = "submitArmWithItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"),
            require = 0)
    private void shard$scaleShield(ItemStackRenderState item, PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay, int outline,
                                   Operation<Void> original, @Local(argsOnly = true) InteractionHand hand, @Local(argsOnly = true) ItemStack stack) {
        AbstractClientPlayer player = shard$player();
        if (!ShardClient.isReady() || player == null) {
            original.call(item, poseStack, collector, light, overlay, outline);
            return;
        }
        LowShieldModule shield = ShardClient.modules().get(LowShieldModule.class);
        float[] scale = shield.scaleFor(player, hand, stack);
        int color = shield.colorFor(player, hand, stack);
        HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        float[] small = ShardClient.modules().get(SmallItemsModule.class).transformFor(hand, arm, stack);
        if (scale == null && small == null && color == ItemTints.NONE) {
            original.call(item, poseStack, collector, light, overlay, outline);
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
            original.call(item, poseStack, collector, light, overlay, outline);
        } finally {
            ItemTints.endShield();
            poseStack.popPose();
        }
    }
}
*///?}
