package gg.shard.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import gg.shard.client.ShardClient;
import gg.shard.client.modules.visual.CrystalTweaksModule;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EndCrystalRenderer;
import net.minecraft.client.renderer.entity.state.EndCrystalRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EndCrystalRenderer.class)
abstract class EndCrystalRendererMixin {
    @Inject(method = "submit", at = @At("HEAD"))
    private void shard$scaleCrystal(EndCrystalRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
                                    CameraRenderState camera, CallbackInfo ci) {
        if (!ShardClient.isReady()) return;
        float s = ShardClient.modules().get(CrystalTweaksModule.class).renderScale();
        if (s != 1f) poseStack.scale(s, s, s);
    }
}
