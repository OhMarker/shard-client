package gg.shard.client.mixin;

import gg.shard.client.ShardClient;
import gg.shard.client.modules.visual.NametagsModule;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Nametags: decorate the name tag component after vanilla decided whether to show one. */
@Mixin(EntityRenderer.class)
abstract class EntityRendererMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V",
            at = @At("TAIL"))
    private void shard$nameTag(Entity entity, EntityRenderState state, float partialTick, CallbackInfo ci) {
        if (!ShardClient.isReady() || state.nameTag == null) return;
        state.nameTag = ShardClient.modules().get(NametagsModule.class).decorate(entity, state.nameTag);
    }
}
