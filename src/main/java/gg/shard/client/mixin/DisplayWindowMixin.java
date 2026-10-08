package gg.shard.client.mixin;

import com.mojang.blaze3d.platform.Window;
import gg.shard.client.ShardClient;
import gg.shard.client.modules.utility.DisplayModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Display: F11 goes borderless instead of exclusive fullscreen when chosen. */
@Mixin(Window.class)
abstract class DisplayWindowMixin {
    private static boolean shard$inToggle;

    @Inject(method = "toggleFullScreen", at = @At("HEAD"), cancellable = true, require = 0)
    private void shard$borderless(CallbackInfo ci) {
        if (!ShardClient.isReady() || shard$inToggle) return;
        shard$inToggle = true;
        try {
            if (ShardClient.modules().get(DisplayModule.class).onToggleFullscreen((Window) (Object) this)) ci.cancel();
        } finally {
            shard$inToggle = false;
        }
    }
}
