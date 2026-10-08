package gg.shard.client.mixin;

import gg.shard.client.input.ClickTracker;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Click counting for the CPS module; hooks the vanilla attack/use entry points. */
@Mixin(Minecraft.class)
abstract class MinecraftMixin {
    @Inject(method = "startAttack", at = @At("HEAD"), require = 0)
    private void shard$onAttack(CallbackInfoReturnable<Boolean> cir) {
        ClickTracker.recordLeft();
    }

    @Inject(method = "startUseItem", at = @At("HEAD"), require = 0)
    private void shard$onUse(CallbackInfo ci) {
        ClickTracker.recordRight();
    }

    /** Display: custom window title. */
    @com.llamalad7.mixinextras.injector.ModifyReturnValue(method = "createTitle", at = @At("RETURN"), require = 0)
    private String shard$title(String original) {
        if (!gg.shard.client.ShardClient.isReady()) return original;
        String custom = gg.shard.client.ShardClient.modules().get(gg.shard.client.modules.utility.DisplayModule.class).titleOverride();
        return custom != null ? custom : original;
    }
}
