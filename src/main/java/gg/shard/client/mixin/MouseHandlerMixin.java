package gg.shard.client.mixin;

import gg.shard.client.ShardClient;
import gg.shard.client.modules.visual.ZoomModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Zoom "Scroll to adjust": while zoomed in-game, the wheel changes the zoom instead of the hotbar slot. */
@Mixin(MouseHandler.class)
abstract class MouseHandlerMixin {
    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true, require = 0)
    private void shard$zoomScroll(long window, double xOffset, double yOffset, CallbackInfo ci) {
        if (!ShardClient.isReady() || Minecraft.getInstance().screen != null) return;
        if (ShardClient.modules().get(ZoomModule.class).onScroll(yOffset)) ci.cancel();
    }
}
