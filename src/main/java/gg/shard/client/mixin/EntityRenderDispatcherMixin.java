package gg.shard.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import gg.shard.client.ShardClient;
import gg.shard.client.modules.perf.EntityOptimizerModule;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Entity Optimizer: after vanilla's culling says an entity is drawn this frame, the module may skip drawing it. */
@Mixin(EntityRenderDispatcher.class)
abstract class EntityRenderDispatcherMixin {
    @ModifyReturnValue(method = "shouldRender", at = @At("RETURN"), require = 0)
    //? if >=26.3 {
    /*private boolean shard$entityOptimizer(boolean original, Entity entity, Frustum frustum, double camX, double camY, double camZ, float partialTick) {
    *///?} else {
    private boolean shard$entityOptimizer(boolean original, Entity entity, Frustum frustum, double camX, double camY, double camZ) {
    //?}
        if (!original || !ShardClient.isReady()) return original;
        return !ShardClient.modules().get(EntityOptimizerModule.class).hides(entity);
    }
}
