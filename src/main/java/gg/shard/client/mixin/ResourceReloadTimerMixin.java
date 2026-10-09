package gg.shard.client.mixin;

import gg.shard.client.ShardClient;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Logs how long each resource reload takes ("Shard: resource reload took N ms"), so start-up and
 * resource pack changes can be measured from any log the owner sends.
 */
@Mixin(ReloadableResourceManager.class)
abstract class ResourceReloadTimerMixin {
    @Inject(method = "createReload", at = @At("RETURN"), require = 0)
    private void shard$timeReload(CallbackInfoReturnable<ReloadInstance> cir) {
        long start = System.nanoTime();
        ReloadInstance reload = cir.getReturnValue();
        if (reload == null) return;
        reload.done().whenComplete((v, e) ->
                ShardClient.LOGGER.info("Shard: resource reload took {} ms", (System.nanoTime() - start) / 1_000_000));
    }
}
