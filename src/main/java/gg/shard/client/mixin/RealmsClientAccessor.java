package gg.shard.client.mixin;

import com.mojang.realmsclient.client.RealmsClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** RealmsClient caches one instance holding the session id; the account switcher clears it. */
@Mixin(RealmsClient.class)
public interface RealmsClientAccessor {
    @Accessor("realmsClientInstance")
    static void shard$setInstance(RealmsClient client) {
        throw new AssertionError();
    }
}
