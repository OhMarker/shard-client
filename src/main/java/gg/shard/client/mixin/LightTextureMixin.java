package gg.shard.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import gg.shard.client.ShardClient;
import gg.shard.client.modules.visual.FullbrightModule;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Fullbright: replace the gamma value the light texture reads from the options. In 1.21.11
 * updateLightTexture reads two doubles as floats: darknessEffectScale first, gamma second.
 */
@Mixin(LightTexture.class)
abstract class LightTextureMixin {
    @ModifyExpressionValue(method = "updateLightTexture", at = @At(value = "INVOKE", target = "Ljava/lang/Double;floatValue()F", ordinal = 1), require = 0)
    private float shard$gamma(float original) {
        FullbrightModule.hookSeen = true;
        if (!ShardClient.isReady()) return original;
        return ShardClient.modules().get(FullbrightModule.class).gamma(original);
    }
}
