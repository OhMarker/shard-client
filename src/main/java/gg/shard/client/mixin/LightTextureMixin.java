package gg.shard.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import gg.shard.client.ShardClient;
import gg.shard.client.modules.visual.FullbrightModule;
//? if <26.1
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Fullbright: replace the gamma value the light texture reads from the options. In 1.21.11
 * updateLightTexture reads two doubles as floats: darknessEffectScale first, gamma second.
 * 26.1 replaced LightTexture with a render state; LightmapRenderStateExtractor.extract reads
 * gamma first there.
 */
//? if >=26.1 {
/*@Mixin(net.minecraft.client.renderer.LightmapRenderStateExtractor.class)
*///?} else {
@Mixin(LightTexture.class)
//?}
abstract class LightTextureMixin {
    //? if >=26.1 {
    /*@ModifyExpressionValue(method = "extract", at = @At(value = "INVOKE", target = "Ljava/lang/Double;floatValue()F", ordinal = 0), require = 0)
    *///?} else {
    @ModifyExpressionValue(method = "updateLightTexture", at = @At(value = "INVOKE", target = "Ljava/lang/Double;floatValue()F", ordinal = 1), require = 0)
    //?}

    private float shard$gamma(float original) {
        FullbrightModule.hookSeen = true;
        if (!ShardClient.isReady()) return original;
        return ShardClient.modules().get(FullbrightModule.class).gamma(original);
    }
}
