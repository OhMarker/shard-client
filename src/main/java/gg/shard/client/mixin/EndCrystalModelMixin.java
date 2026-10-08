package gg.shard.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import gg.shard.client.ShardClient;
import gg.shard.client.modules.visual.CrystalTweaksModule;
import net.minecraft.client.model.object.crystal.EndCrystalModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Crystal Visuals: spin speed (vanilla turns 3 degrees per tick) and an optional stop to the bounce. */
@Mixin(EndCrystalModel.class)
abstract class EndCrystalModelMixin {
    @ModifyVariable(method = "setupAnim", at = @At(value = "STORE", ordinal = 0), ordinal = 0, require = 0)
    private float shard$spin(float degrees) {
        return ShardClient.isReady() ? degrees * ShardClient.modules().get(CrystalTweaksModule.class).spinFactor() : degrees;
    }

    @WrapOperation(method = "setupAnim", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EndCrystalRenderer;getY(F)F"), require = 0)
    private float shard$bounce(float age, Operation<Float> original) {
        if (ShardClient.isReady() && ShardClient.modules().get(CrystalTweaksModule.class).noBounce()) return original.call(0f);
        return original.call(age);
    }
}
