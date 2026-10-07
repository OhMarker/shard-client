package gg.shard.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.vertex.PoseStack;
import gg.shard.client.ShardClient;
import gg.shard.client.modules.visual.NoHurtCamModule;
import gg.shard.client.modules.visual.ZoomModule;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
abstract class GameRendererMixin {
    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    private void shard$noHurtCam(PoseStack poseStack, float partialTick, CallbackInfo ci) {
        if (ShardClient.isReady() && ShardClient.modules().get(NoHurtCamModule.class).isEnabled()) ci.cancel();
    }

    @ModifyReturnValue(method = "getFov", at = @At("RETURN"))
    private float shard$zoomFov(float original, Camera camera, float partialTick, boolean useFovSetting) {
        if (!ShardClient.isReady() || !useFovSetting) return original;
        return ShardClient.modules().get(ZoomModule.class).applyFov(original);
    }
}
