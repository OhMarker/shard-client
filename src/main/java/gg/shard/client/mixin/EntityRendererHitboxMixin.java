package gg.shard.client.mixin;

//? if >=1.21.5 <1.21.11 {
/*import gg.shard.client.ShardClient;
import gg.shard.client.modules.visual.HitboxModule;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/^*
 * Hitboxes module before 1.21.11 (no EntityHitboxDebugRenderer yet): vanilla extracts F3+B boxes
 * only for entities it would box (not invisible, rendered, reduced debug info off). Shard takes
 * that entity for its own styled box (HitboxModule.render) and leaves vanilla's box empty.
 ^/
@Mixin(EntityRenderer.class)
abstract class EntityRendererHitboxMixin {
    @Inject(method = "extractHitboxes(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V",
            at = @At("HEAD"), cancellable = true)
    private void shard$styledHitbox(Entity entity, EntityRenderState state, float partialTick, CallbackInfo ci) {
        if (!ShardClient.isReady()) return;
        HitboxModule hitboxes = ShardClient.modules().get(HitboxModule.class);
        if (!hitboxes.styles()) return;
        hitboxes.queue(entity, partialTick);
        state.hitboxesRenderState = null;
        state.serverHitboxesRenderState = null;
        ci.cancel();
    }
}
*///?} else if <1.21.5 {
/*import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.llamalad7.mixinextras.injector.WrapWithCondition;
import gg.shard.client.ShardClient;
import gg.shard.client.modules.visual.HitboxModule;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/^*
 * Hitboxes module before 1.21.5 (no hitbox render states): EntityRenderDispatcher.render draws
 * the F3+B box itself, only for entities vanilla would box. Shard takes that entity for its own
 * styled box (HitboxModule.render) and skips vanilla's.
 ^/
@Mixin(EntityRenderDispatcher.class)
abstract class EntityRendererHitboxMixin {
    @WrapWithCondition(method = "render(Lnet/minecraft/world/entity/Entity;DDDFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/EntityRenderer;)V",
            at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;renderHitbox(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/world/entity/Entity;FFFF)V"))
    private boolean shard$styledHitbox(PoseStack pose, VertexConsumer consumer, Entity entity, float partialTick, float r, float g, float b) {
        if (!ShardClient.isReady()) return true;
        HitboxModule hitboxes = ShardClient.modules().get(HitboxModule.class);
        if (!hitboxes.styles()) return true;
        hitboxes.queue(entity, partialTick);
        return false;
    }
}
*///?}
