package gg.shard.client.mixin;

import gg.shard.client.ShardClient;
import gg.shard.client.modules.visual.DeathAnimationModule;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
//? if >=1.21.2 {
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
//?} else {
/*import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.texture.OverlayTexture;
*///?}
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** No Death Animation: strip the death tilt and red tint from the extracted render state. */
@Mixin(LivingEntityRenderer.class)
abstract class LivingEntityRendererMixin {
    //? if >=1.21.2 {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
            at = @At("TAIL"))
    private void shard$deathState(LivingEntity entity, LivingEntityRenderState state, float partialTick, CallbackInfo ci) {
        //? if <1.21.4 {
        /*((gg.shard.client.render.ShieldCosmetics.Holder) state).shard$setHolder(entity);
        *///?}
        if (!ShardClient.isReady()) return;
        DeathAnimationModule module = ShardClient.modules().get(DeathAnimationModule.class);
        if (state.deathTime > 0f && module.hidesTilt(entity)) state.deathTime = 0f;
        if (state.hasRedOverlay && module.skipsTint(entity)) state.hasRedOverlay = false;
        if (state.hasRedOverlay && !ShardClient.modules().get(gg.shard.client.modules.visual.HitColorModule.class).showsOverlay(entity)) state.hasRedOverlay = false;
    }

    //? if <1.21.9 {
    /*// Before 1.21.9 the armour layers draw during render without the wearer's state; Hit Color on
    // armour reads whether the wearer shows the hurt overlay from ItemTints.
    @Inject(method = "render(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"))
    private void shard$wearer(LivingEntityRenderState state, com.mojang.blaze3d.vertex.PoseStack pose, net.minecraft.client.renderer.MultiBufferSource buffers,
                              int light, CallbackInfo ci) {
        gg.shard.client.render.ItemTints.setWearerHurt(state.hasRedOverlay);
    }

    @Inject(method = "render(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("RETURN"))
    private void shard$wearerEnd(LivingEntityRenderState state, com.mojang.blaze3d.vertex.PoseStack pose, net.minecraft.client.renderer.MultiBufferSource buffers,
                                 int light, CallbackInfo ci) {
        gg.shard.client.render.ItemTints.setWearerHurt(false);
    }
    *///?}
    //?} else {
    /*// Before 1.21.2 (no render states) the death tilt and the red overlay come straight from the
    // entity while it renders: setupRotations reads deathTime, render packs the overlay.
    @ModifyExpressionValue(method = "setupRotations", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/LivingEntity;deathTime:I"))
    private int shard$deathTilt(int deathTime, @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) LivingEntity entity) {
        return deathTime > 0 && ShardClient.isReady() && ShardClient.modules().get(DeathAnimationModule.class).hidesTilt(entity) ? 0 : deathTime;
    }

    @WrapOperation(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;getOverlayCoords(Lnet/minecraft/world/entity/LivingEntity;F)I"))
    private int shard$deathTint(LivingEntity entity, float white, Operation<Integer> original) {
        return shard$redOverlay(entity) ? original.call(entity, white) : OverlayTexture.pack(OverlayTexture.u(white), OverlayTexture.v(false));
    }

    // Hit Color on armour reads whether the wearer shows the hurt overlay from ItemTints.
    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"))
    private void shard$wearer(LivingEntity entity, float yaw, float partialTick, com.mojang.blaze3d.vertex.PoseStack pose,
                              net.minecraft.client.renderer.MultiBufferSource buffers, int light, CallbackInfo ci) {
        gg.shard.client.render.ItemTints.setWearerHurt(shard$redOverlay(entity));
    }

    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("RETURN"))
    private void shard$wearerEnd(LivingEntity entity, float yaw, float partialTick, com.mojang.blaze3d.vertex.PoseStack pose,
                                 net.minecraft.client.renderer.MultiBufferSource buffers, int light, CallbackInfo ci) {
        gg.shard.client.render.ItemTints.setWearerHurt(false);
    }

    /^* Whether vanilla's red overlay shows (hurt or dying), after No Death Animation and Hit Color. ^/
    private static boolean shard$redOverlay(LivingEntity entity) {
        if (!(entity.hurtTime > 0 || entity.deathTime > 0)) return false;
        if (!ShardClient.isReady()) return true;
        if (ShardClient.modules().get(DeathAnimationModule.class).skipsTint(entity)) return false;
        return ShardClient.modules().get(gg.shard.client.modules.visual.HitColorModule.class).showsOverlay(entity);
    }
    *///?}
}
