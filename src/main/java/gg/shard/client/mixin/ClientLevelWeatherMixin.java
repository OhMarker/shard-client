package gg.shard.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import gg.shard.client.ShardClient;
import gg.shard.client.modules.visual.WeatherTimeModule;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Weather and Time: the client's own level reports clear weather and the pinned time. */
@Mixin(Level.class)
abstract class ClientLevelWeatherMixin {
    @ModifyReturnValue(method = "getRainLevel", at = @At("RETURN"), require = 0)
    private float shard$rain(float original) {
        if (!((Object) this instanceof ClientLevel) || !ShardClient.isReady()) return original;
        return ShardClient.modules().get(WeatherTimeModule.class).clearWeather() ? 0f : original;
    }

    @ModifyReturnValue(method = "getThunderLevel", at = @At("RETURN"), require = 0)
    private float shard$thunder(float original) {
        if (!((Object) this instanceof ClientLevel) || !ShardClient.isReady()) return original;
        return ShardClient.modules().get(WeatherTimeModule.class).clearWeather() ? 0f : original;
    }

    @ModifyReturnValue(method = "getDayTime", at = @At("RETURN"), require = 0)
    private long shard$time(long original) {
        if (!((Object) this instanceof ClientLevel) || !ShardClient.isReady()) return original;
        long fixed = ShardClient.modules().get(WeatherTimeModule.class).fixedTime();
        return fixed >= 0 ? fixed : original;
    }
}
