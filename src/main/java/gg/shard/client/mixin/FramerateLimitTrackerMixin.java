package gg.shard.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
//? if >=1.21.2 {
import com.mojang.blaze3d.platform.FramerateLimitTracker;
//?}
import gg.shard.client.ShardClient;
import gg.shard.client.modules.utility.DisplayModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Display: background and menu frame caps on top of vanilla's limit. */
//? if >=1.21.2 {
@Mixin(FramerateLimitTracker.class)
//?} else {
/*// Before 1.21.2 the limit is Minecraft's private getFramerateLimit (no inactivity limits).
@Mixin(net.minecraft.client.Minecraft.class)
*///?}
abstract class FramerateLimitTrackerMixin {
    @ModifyReturnValue(method = "getFramerateLimit", at = @At("RETURN"), require = 0)
    private int shard$limit(int original) {
        return ShardClient.isReady() ? ShardClient.modules().get(DisplayModule.class).framerateLimit(original) : original;
    }
}
