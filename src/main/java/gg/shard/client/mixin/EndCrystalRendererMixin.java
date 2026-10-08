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

    /** Crystal Optimizer "Highlight colour": a glow outline on crystals you just placed (your own, so nothing hidden). */
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/boss/enderdragon/EndCrystal;Lnet/minecraft/client/renderer/entity/state/EndCrystalRenderState;F)V",
            at = @At("TAIL"), require = 0)
    private void shard$highlight(net.minecraft.world.entity.boss.enderdragon.EndCrystal crystal, EndCrystalRenderState state, float partialTick, CallbackInfo ci) {
        if (!ShardClient.isReady()) return;
        int outline = ShardClient.modules().get(CrystalOptimizerModule.class).highlightOutline(state.x, state.y, state.z);
        if (outline != 0) state.outlineColor = outline;
    }
}
