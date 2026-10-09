package gg.shard.client.mixin;

import gg.shard.client.ShardClient;
import gg.shard.client.modules.visual.SkyModule;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.SkyRenderState;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Sky module: recolour the Overworld sky dome (and optionally drop the sunrise glow) after vanilla extracted it. */
@Mixin(SkyRenderer.class)
abstract class SkyRendererMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"), require = 0)
    //? if >=1.21.11 {
    private void shard$skyColour(ClientLevel level, float partialTick, Camera camera, SkyRenderState state, CallbackInfo ci) {
        if (!ShardClient.isReady() || state.skybox != DimensionType.Skybox.OVERWORLD) return;
    //?} else {
    /*private void shard$skyColour(ClientLevel level, float partialTick, net.minecraft.world.phys.Vec3 cameraPos, SkyRenderState state, CallbackInfo ci) {
        if (!ShardClient.isReady() || state.skyType != net.minecraft.client.renderer.DimensionSpecialEffects.SkyType.OVERWORLD) return;
    *///?}
        SkyModule sky = ShardClient.modules().get(SkyModule.class);
        if (!sky.active(level)) return;
        state.skyColor = sky.sky(state.skyColor, state.sunAngle, level.getRainLevel(partialTick), level.getThunderLevel(partialTick));
        if (!sky.keepsSunriseGlow()) state.sunriseAndSunsetColor = 0;
    }
}
