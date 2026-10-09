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

    /** GUI Scales, inventory: draw a scaled screen under its pose factor with the mouse divided to match. */
    //? if >=26.1 {
    /*@com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = "extractGui", at = @At(value = "INVOKE",
    *///?} else {
    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = "render", at = @At(value = "INVOKE",
    //?}

            target = "Lnet/minecraft/client/gui/screens/Screen;renderWithTooltipAndSubtitles(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"), require = 0)
    private void shard$scaledScreen(net.minecraft.client.gui.screens.Screen screen, net.minecraft.client.gui.GuiGraphics g, int mx, int my, float pt,
                                    com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original) {
        double f = gg.shard.client.gui.ScaledScreen.factorOf(screen);
        if (f == 1.0) {
            original.call(screen, g, mx, my, pt);
            return;
        }
        g.pose().pushMatrix();
        g.pose().scale((float) f, (float) f);
        original.call(screen, g, (int) Math.floor(mx / f), (int) Math.floor(my / f), pt);
        g.pose().popMatrix();
    }
}
