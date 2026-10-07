package gg.shard.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import gg.shard.client.ShardClient;
import gg.shard.client.modules.combat.CrystalOptimizerModule;
import gg.shard.client.modules.visual.CrystalTweaksModule;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EndCrystalRenderer;
import net.minecraft.client.renderer.entity.state.EndCrystalRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Crystal Size scaling plus the Crystal Optimizer's "placed by me" pulse. */
@Mixin(EndCrystalRenderer.class)
abstract class EndCrystalRendererMixin {
    @Inject(method = "submit", at = @At("HEAD"))
    private void shard$scaleCrystal(EndCrystalRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
                                    CameraRenderState camera, CallbackInfo ci) {
        if (!ShardClient.isReady()) return;
        float s = ShardClient.modules().get(CrystalTweaksModule.class).renderScale()
                * ShardClient.modules().get(CrystalOptimizerModule.class).highlightScale(state.x, state.y, state.z);
        if (s != 1f) poseStack.scale(s, s, s);
    }
}
