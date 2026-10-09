package gg.shard.client.mixin;

//? if >=26.1 {
/*import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import gg.shard.client.ShardClient;
import gg.shard.client.modules.visual.WeatherTimeModule;
import net.minecraft.client.ClientClockManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/^*
 * Weather and Time on 26.1+: the sky, light and moon follow the level's world clocks
 * (Level.getDayTime is gone), so the client's clocks report the pinned time. The server's clocks
 * (and so mobs, crops and everything else) are untouched.
 ^/
@Mixin(ClientClockManager.class)
abstract class ClientClockManagerMixin {
    @ModifyReturnValue(method = "getTotalTicks", at = @At("RETURN"), require = 0)
    private long shard$time(long original) {
        if (!ShardClient.isReady()) return original;
        long fixed = ShardClient.modules().get(WeatherTimeModule.class).fixedTime();
        return fixed >= 0 ? fixed : original;
    }
}
*///?}
