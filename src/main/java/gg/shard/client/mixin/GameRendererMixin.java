package gg.shard.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
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
    // bobHurt's parameters differ between versions (26.1: camera state and pose); none are needed.
    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    private void shard$noHurtCam(CallbackInfo ci) {
        if (ShardClient.isReady() && ShardClient.modules().get(NoHurtCamModule.class).isEnabled()) ci.cancel();
    }

    // 26.1 computes the world FOV in Camera.calculateFov (CameraMixin).
    //? if <26.1 {
    @ModifyReturnValue(method = "getFov", at = @At("RETURN"))
    private float shard$zoomFov(float original, Camera camera, float partialTick, boolean useFovSetting) {
        if (!ShardClient.isReady() || !useFovSetting) return original;
        return ShardClient.modules().get(ZoomModule.class).applyFov(original);
    }
    //?}

    //? if <1.21.6 {
    /*/^* Totem Pops, animation size (before 1.21.6 GameRenderer draws the floating item, scaled (o, -o, o)). ^/
    @org.spongepowered.asm.mixin.injection.ModifyArgs(method = "renderItemActivationAnimation", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V"), require = 0)
    private void shard$totemSize(org.spongepowered.asm.mixin.injection.invoke.arg.Args args) {
        if (!ShardClient.isReady()) return;
        float f = ShardClient.modules().get(gg.shard.client.modules.visual.TotemPopModule.class).animationScale();
        if (f == 1f) return;
        for (int i = 0; i < 3; i++) args.set(i, args.<Float>get(i) * f);
    }
    *///?}
}
