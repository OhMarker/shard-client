package gg.shard.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import gg.shard.client.ShardClient;
import gg.shard.client.modules.utility.SoundsModule;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Sounds: scales the final volume of the sounds the Sounds module covers. */
@Mixin(SoundEngine.class)
abstract class SoundEngineMixin {
    @ModifyReturnValue(method = "calculateVolume(Lnet/minecraft/client/resources/sounds/SoundInstance;)F", at = @At("RETURN"), require = 0)
    private float shard$volume(float original, SoundInstance sound) {
        return ShardClient.isReady() ? original * ShardClient.modules().get(SoundsModule.class).factor(sound) : original;
    }
}
