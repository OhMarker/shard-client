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

/**
 * Crystal Visuals (size, base, colours, opacity, aimed outline) plus the Crystal Optimizer's
 * "placed by me" pulse. Recoloured crystals are drawn as two tinted models, the glass frames and
 * the core, so each gets its own colour.
 */
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
        ShardClient.modules().get(CrystalTweaksModule.class).adjustState(crystal, state);
        int outline = ShardClient.modules().get(CrystalOptimizerModule.class).highlightOutline(state.x, state.y, state.z);
        if (outline != 0) state.outlineColor = outline;
    }

    private static final net.minecraft.resources.Identifier SHARD$TEXTURE = net.minecraft.resources.Identifier.withDefaultNamespace("textures/entity/end_crystal/end_crystal.png");

    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = "submit(Lnet/minecraft/client/renderer/entity/state/EndCrystalRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"),
            require = 0)
    private void shard$recolour(SubmitNodeCollector collector, net.minecraft.client.model.Model<?> model, Object state, PoseStack pose,
                                net.minecraft.client.renderer.rendertype.RenderType type, int light, int overlay, int outline,
                                net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay crumbling,
                                com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original) {
        CrystalTweaksModule tweaks = ShardClient.isReady() ? ShardClient.modules().get(CrystalTweaksModule.class) : null;
        if (tweaks == null || !tweaks.recolours() || !(state instanceof EndCrystalRenderState crystal)) {
            original.call(collector, model, state, pose, type, light, overlay, outline, crumbling);
            return;
        }
        var rt = tweaks.translucent() ? net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucent(SHARD$TEXTURE) : type;
        collector.submitModel(tweaks.frameModel(), crystal, pose, rt, light, overlay, tweaks.frameTint(), null, outline, crumbling);
        collector.submitModel(tweaks.coreModel(), crystal, pose, rt, light, overlay, tweaks.coreTint(), null, outline, crumbling);
    }
}
