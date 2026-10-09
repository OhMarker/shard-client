package gg.shard.client.mixin;

//? if >=1.21.9 <1.21.10 {
/*import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import gg.shard.client.ShardClient;
import gg.shard.client.compat.WorldDraw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.state.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/^*
 * 1.21.9's Fabric API has no world render events, so Shard hooks the same two spots Fabric's
 * AFTER_ENTITIES and BEFORE_BLOCK_OUTLINE use from 1.21.10: after the entity outline batch of
 * the main pass (method_62214 is that lambda), and in renderBlockOutline once the outline is
 * known to be drawn this pass.
 ^/
@Mixin(LevelRenderer.class)
abstract class LevelRendererEventsMixin {
    @WrapOperation(method = "method_62214", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/OutlineBufferSource;endOutlineBatch()V"))
    private void shard$afterEntities(OutlineBufferSource outlines, Operation<Void> original,
                                     @Local PoseStack pose, @Local(argsOnly = true) LevelRenderState state) {
        original.call(outlines);
        ShardClient.afterEntities(new WorldDraw(pose, Minecraft.getInstance().renderBuffers().bufferSource(), state.cameraRenderState.pos));
    }

    @Inject(method = "renderBlockOutline", at = @At(value = "FIELD",
            target = "Lnet/minecraft/client/renderer/state/CameraRenderState;pos:Lnet/minecraft/world/phys/Vec3;"), cancellable = true)
    private void shard$beforeBlockOutline(MultiBufferSource.BufferSource consumers, PoseStack pose, boolean translucent,
                                          LevelRenderState state, CallbackInfo ci) {
        if (!ShardClient.beforeBlockOutline(new WorldDraw(pose, consumers, state.cameraRenderState.pos), state.blockOutlineRenderState)) {
            consumers.endLastBatch();
            ci.cancel();
        }
    }
}
*///?}
