package gg.shard.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import gg.shard.client.ShardClient;
import gg.shard.client.modules.visual.CleanScreenModule;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Clean Screen: skip the camera overlays the player turned off. Optional hooks. */
@Mixin(Gui.class)
abstract class GuiOverlaysMixin {
    @WrapOperation(method = "renderCameraOverlays", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Gui;renderTextureOverlay(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/resources/Identifier;F)V"), require = 0)
    private void shard$textureOverlay(Gui gui, GuiGraphics g, Identifier texture, float alpha, Operation<Void> original) {
        if (ShardClient.isReady()) {
            CleanScreenModule clean = ShardClient.modules().get(CleanScreenModule.class);
            boolean snow = texture.getPath().contains("powder_snow");
            if (snow ? clean.hidePowderSnow() : clean.hidePumpkin()) return;
        }
        original.call(gui, g, texture, alpha);
    }

    @Inject(method = "renderVignette", at = @At("HEAD"), cancellable = true, require = 0)
    private void shard$vignette(GuiGraphics g, Entity entity, CallbackInfo ci) {
        if (ShardClient.isReady() && ShardClient.modules().get(CleanScreenModule.class).hideVignette()) ci.cancel();
    }

    @Inject(method = "renderPortalOverlay", at = @At("HEAD"), cancellable = true, require = 0)
    private void shard$portal(GuiGraphics g, float alpha, CallbackInfo ci) {
        if (ShardClient.isReady() && ShardClient.modules().get(CleanScreenModule.class).hidePortal()) ci.cancel();
    }
}
