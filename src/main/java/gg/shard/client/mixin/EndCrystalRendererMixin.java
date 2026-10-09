package gg.shard.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import gg.shard.client.ShardClient;
import gg.shard.client.modules.combat.CrystalOptimizerModule;
import gg.shard.client.modules.visual.CrystalTweaksModule;
//? if >=1.21.9 {
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.CameraRenderState;
//?} else if >=1.21.2 {
/*import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EndCrystalModel;
import net.minecraft.client.renderer.MultiBufferSource;
*///?} else {
/*import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
*///?}
import net.minecraft.client.renderer.entity.EndCrystalRenderer;
//? if >=1.21.2 {
import net.minecraft.client.renderer.entity.state.EndCrystalRenderState;
//?}
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
    //? if >=1.21.2 {
    //? if >=1.21.9 {
    @Inject(method = "submit", at = @At("HEAD"))
    private void shard$scaleCrystal(EndCrystalRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
                                    CameraRenderState camera, CallbackInfo ci) {
    //?} else {
    /*// Before 1.21.9 the crystal is drawn straight into a buffer (render, not submit).
    @Inject(method = "render(Lnet/minecraft/client/renderer/entity/state/EndCrystalRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"))
    private void shard$scaleCrystal(EndCrystalRenderState state, PoseStack poseStack, MultiBufferSource buffers, int light, CallbackInfo ci) {
    *///?}
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
        // Before 1.21.9 outlines follow the entity, not its render state (CrystalTweaksModule.outline).
        //? if >=1.21.9 {
        int outline = ShardClient.modules().get(CrystalOptimizerModule.class).highlightOutline(state.x, state.y, state.z);
        if (outline != 0) state.outlineColor = outline;
        //?}
    }

    private static final net.minecraft.resources.Identifier SHARD$TEXTURE = net.minecraft.resources.Identifier.withDefaultNamespace("textures/entity/end_crystal/end_crystal.png");

    //? if >=1.21.9 {
    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = "submit(Lnet/minecraft/client/renderer/entity/state/EndCrystalRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
    //?} else {
    /*@com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = "render(Lnet/minecraft/client/renderer/entity/state/EndCrystalRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
    *///?}
    //? if >=26.3 {
            /*// 26.3: no crumbling argument (crumbling is its own submit).
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/resources/Identifier;III)V"),
            require = 0)
    private void shard$recolour(SubmitNodeCollector collector, net.minecraft.client.model.Model<?> model, Object state, PoseStack pose,
                                net.minecraft.resources.Identifier texture, int light, int overlay, int outline,
                                com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original) {
        CrystalTweaksModule tweaks = ShardClient.isReady() ? ShardClient.modules().get(CrystalTweaksModule.class) : null;
        if (tweaks == null || !tweaks.recolours() || !(state instanceof EndCrystalRenderState crystal)) {
            original.call(collector, model, state, pose, texture, light, overlay, outline);
            return;
        }
        var rt = tweaks.translucent() ? net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucent(SHARD$TEXTURE) : model.renderType(texture);
        collector.submitModel(tweaks.frameModel(), crystal, pose, rt, light, overlay, tweaks.frameTint(), null, outline);
        collector.submitModel(tweaks.coreModel(), crystal, pose, rt, light, overlay, tweaks.coreTint(), null, outline);
    }
    *///?} else if >=26.1 {
            /*// 26.1 submits the crystal with its texture (the model picks the render type).
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/resources/Identifier;IIILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"),
            require = 0)
    private void shard$recolour(SubmitNodeCollector collector, net.minecraft.client.model.Model<?> model, Object state, PoseStack pose,
                                net.minecraft.resources.Identifier texture, int light, int overlay, int outline,
                                net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay crumbling,
                                com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original) {
        CrystalTweaksModule tweaks = ShardClient.isReady() ? ShardClient.modules().get(CrystalTweaksModule.class) : null;
        if (tweaks == null || !tweaks.recolours() || !(state instanceof EndCrystalRenderState crystal)) {
            original.call(collector, model, state, pose, texture, light, overlay, outline, crumbling);
            return;
        }
        net.minecraft.client.renderer.rendertype.RenderType type = model.renderType(texture);
    *///?} else if >=1.21.9 {
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
    //?} else {
    /*// Before 1.21.9 the model is drawn into the buffer vanilla took for the crystal's render type;
    // recoloured crystals draw the two tinted models instead (translucent from their own buffer).
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EndCrystalModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V"),
            require = 0)
    private void shard$recolour(EndCrystalModel model, PoseStack pose, VertexConsumer buffer, int light, int overlay,
                                com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original,
                                @Local(argsOnly = true) EndCrystalRenderState crystal, @Local(argsOnly = true) MultiBufferSource buffers) {
        CrystalTweaksModule tweaks = ShardClient.isReady() ? ShardClient.modules().get(CrystalTweaksModule.class) : null;
        if (tweaks == null || !tweaks.recolours()) {
            original.call(model, pose, buffer, light, overlay);
            return;
        }
        VertexConsumer vc = tweaks.translucent() ? buffers.getBuffer(net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucent(SHARD$TEXTURE)) : buffer;
        EndCrystalModel frame = tweaks.frameModel();
        frame.setupAnim(crystal);
        frame.renderToBuffer(pose, vc, light, overlay, tweaks.frameTint());
        EndCrystalModel core = tweaks.coreModel();
        core.setupAnim(crystal);
        core.renderToBuffer(pose, vc, light, overlay, tweaks.coreTint());
    }
    *///?}

    //? if >=1.21.9 <26.3 {
        var rt = tweaks.translucent() ? net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucent(SHARD$TEXTURE) : type;
        collector.submitModel(tweaks.frameModel(), crystal, pose, rt, light, overlay, tweaks.frameTint(), null, outline, crumbling);
        collector.submitModel(tweaks.coreModel(), crystal, pose, rt, light, overlay, tweaks.coreTint(), null, outline, crumbling);
    }
    //?}
    //?} else {
    /*// Before 1.21.2 there are no render states and no EndCrystalModel: the renderer draws its own
    // base, two glass frames and cube from the entity, so the spin, bounce and base (EndCrystalModelMixin
    // and adjustState later) are handled here, and recoloured crystals tint each part as it is drawn.
    @Shadow @Final private ModelPart cube;

    @Inject(method = "render(Lnet/minecraft/world/entity/boss/enderdragon/EndCrystal;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"))
    private void shard$scaleCrystal(EndCrystal crystal, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light, CallbackInfo ci) {
        if (!ShardClient.isReady()) return;
        float s = ShardClient.modules().get(CrystalTweaksModule.class).renderScale()
                * ShardClient.modules().get(CrystalOptimizerModule.class).highlightScale(crystal.getX(), crystal.getY(), crystal.getZ());
        if (s != 1f) poseStack.scale(s, s, s);
    }

    // Spin: vanilla turns (time + partialTick) * 3 degrees.
    @ModifyConstant(method = "render(Lnet/minecraft/world/entity/boss/enderdragon/EndCrystal;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            constant = @Constant(floatValue = 3.0F))
    private float shard$spin(float degrees) {
        return ShardClient.isReady() ? degrees * ShardClient.modules().get(CrystalTweaksModule.class).spinFactor() : degrees;
    }

    @WrapOperation(method = "render(Lnet/minecraft/world/entity/boss/enderdragon/EndCrystal;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EndCrystalRenderer;getY(Lnet/minecraft/world/entity/boss/enderdragon/EndCrystal;F)F"))
    private float shard$bounce(EndCrystal crystal, float partialTick, Operation<Float> original) {
        if (ShardClient.isReady() && ShardClient.modules().get(CrystalTweaksModule.class).noBounce()) {
            float h = 0.5F; // getY at age 0, as the later model's getY(0)
            return (h * h + h) * 0.4F - 1.4F;
        }
        return original.call(crystal, partialTick);
    }

    @WrapOperation(method = "render(Lnet/minecraft/world/entity/boss/enderdragon/EndCrystal;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/boss/enderdragon/EndCrystal;showsBottom()Z"))
    private boolean shard$base(EndCrystal crystal, Operation<Boolean> original) {
        return original.call(crystal) && !(ShardClient.isReady() && ShardClient.modules().get(CrystalTweaksModule.class).hidesBase());
    }

    @WrapOperation(method = "render(Lnet/minecraft/world/entity/boss/enderdragon/EndCrystal;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/geom/ModelPart;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V"))
    private void shard$recolour(ModelPart part, PoseStack pose, VertexConsumer buffer, int light, int overlay, Operation<Void> original,
                                @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) MultiBufferSource buffers) {
        CrystalTweaksModule tweaks = ShardClient.isReady() ? ShardClient.modules().get(CrystalTweaksModule.class) : null;
        if (tweaks == null || !tweaks.recolours()) {
            original.call(part, pose, buffer, light, overlay);
            return;
        }
        VertexConsumer vc = tweaks.translucent() ? buffers.getBuffer(net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucent(SHARD$TEXTURE)) : buffer;
        part.render(pose, vc, light, overlay, part == cube ? tweaks.coreTint() : tweaks.frameTint());
    }

    private static final net.minecraft.resources.Identifier SHARD$TEXTURE = net.minecraft.resources.Identifier.withDefaultNamespace("textures/entity/end_crystal/end_crystal.png");
    *///?}
}
