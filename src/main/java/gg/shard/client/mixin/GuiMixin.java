package gg.shard.client.mixin;

import gg.shard.client.ShardClient;
import gg.shard.client.modules.visual.CrosshairModule;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Crosshair module: hide vanilla's crosshair while Shard draws its own in the HUD layer. */
@Mixin(Gui.class)
abstract class GuiMixin {
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true, require = 0)
    private void shard$hideCrosshair(GuiGraphics graphics, DeltaTracker delta, CallbackInfo ci) {
        if (ShardClient.isReady() && ShardClient.modules().get(CrosshairModule.class).replacesVanilla()) ci.cancel();
    }
}
