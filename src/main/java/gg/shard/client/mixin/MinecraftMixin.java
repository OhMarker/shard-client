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
}
