package gg.shard.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.platform.FramerateLimitTracker;
import gg.shard.client.ShardClient;
import gg.shard.client.modules.utility.DisplayModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Display: background and menu frame caps on top of vanilla's limit. */
@Mixin(FramerateLimitTracker.class)
abstract class FramerateLimitTrackerMixin {
    @ModifyReturnValue(method = "getFramerateLimit", at = @At("RETURN"), require = 0)
    private int shard$limit(int original) {
        return ShardClient.isReady() ? ShardClient.modules().get(DisplayModule.class).framerateLimit(original) : original;
    }
}
