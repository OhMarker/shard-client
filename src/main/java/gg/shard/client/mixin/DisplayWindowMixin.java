package gg.shard.client.mixin;

import com.mojang.blaze3d.platform.Window;
import gg.shard.client.ShardClient;
import gg.shard.client.modules.utility.DisplayModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Display: F11 goes borderless instead of exclusive fullscreen when chosen. From 26.3 F11 is
 * Minecraft.toggleFullscreen (it flips the fullscreen option); before, Window.toggleFullScreen.
 */
//? if >=26.3 {
/*@Mixin(net.minecraft.client.Minecraft.class)
*///?} else {
@Mixin(Window.class)
//?}
abstract class DisplayWindowMixin {
    private static boolean shard$inToggle;

    //? if >=26.3 {
    /*@Inject(method = "toggleFullscreen", at = @At("HEAD"), cancellable = true, require = 0)
    *///?} else {
    @Inject(method = "toggleFullScreen", at = @At("HEAD"), cancellable = true, require = 0)
    //?}
    private void shard$borderless(CallbackInfo ci) {
        if (!ShardClient.isReady() || shard$inToggle) return;
        shard$inToggle = true;
        try {
            //? if >=26.3 {
            /*Window window = ((net.minecraft.client.Minecraft) (Object) this).getWindow();
            *///?} else {
            Window window = (Window) (Object) this;
            //?}
            if (ShardClient.modules().get(DisplayModule.class).onToggleFullscreen(window)) ci.cancel();
        } finally {
            shard$inToggle = false;
        }
    }
}
