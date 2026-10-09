package gg.shard.client.mixin;

import gg.shard.client.ShardClient;
import gg.shard.client.modules.visual.HitboxModule;
import net.minecraft.client.renderer.debug.EntityHitboxDebugRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hitboxes module: vanilla's F3+B renderer has already picked the entity (not invisible, in view,
 * not your own first-person body); Shard draws that box in the module's style instead.
 */
@Mixin(EntityHitboxDebugRenderer.class)
abstract class EntityHitboxDebugRendererMixin {
    @Inject(method = "showHitboxes", at = @At("HEAD"), cancellable = true, require = 0)
    private void shard$styledHitbox(Entity entity, float partialTick, boolean serverSide, CallbackInfo ci) {
        if (!ShardClient.isReady()) return;
        HitboxModule hitboxes = ShardClient.modules().get(HitboxModule.class);
        if (!hitboxes.styles()) return;
        if (!serverSide) hitboxes.draw(entity, partialTick);
        ci.cancel();
    }
}
