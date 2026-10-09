package gg.shard.client.mixin;

import gg.shard.client.ShardClient;
import gg.shard.client.modules.perf.EntityOptimizerModule;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Entity Optimizer: start a fresh per-frame count before vanilla walks the entities to draw
 * (LevelRenderer up to 26.1, the new LevelExtractor from 26.2).
 */
//? if >=26.2 {
/*@Mixin(net.minecraft.client.renderer.extract.LevelExtractor.class)
*///?} else {
@Mixin(LevelRenderer.class)
//?}
abstract class LevelRendererEntitiesMixin {
    @Inject(method = "extractVisibleEntities", at = @At("HEAD"), require = 0)
    private void shard$beginEntityFrame(Camera camera, Frustum frustum, DeltaTracker delta, LevelRenderState state, CallbackInfo ci) {
        if (ShardClient.isReady()) ShardClient.modules().get(EntityOptimizerModule.class).beginFrame();
    }
}
