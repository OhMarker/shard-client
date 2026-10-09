package gg.shard.client.mixin;

import gg.shard.client.ShardClient;
import gg.shard.client.modules.visual.SkyModule;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
//? if >=1.21.9 {
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.SkyRenderState;
//?} else {
/*import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.phys.Vec3;
*///?}
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Sky module: recolour the Overworld sky dome (and optionally drop the sunrise glow) after vanilla extracted it. */
//? if >=1.21.9 {
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
        //? if >=26.3 {
        /*// 26.3 keeps the colours as float vectors.
        int colour = sky.sky(net.minecraft.util.ARGB.colorFromVector3f(state.skyColor), state.sunAngle, level.getRainLevel(partialTick), level.getThunderLevel(partialTick));
        state.skyColor = net.minecraft.util.ARGB.vector3fFromRGB24(colour);
        if (!sky.keepsSunriseGlow()) state.sunriseAndSunsetColor = new org.joml.Vector4f();
        *///?} else {
        state.skyColor = sky.sky(state.skyColor, state.sunAngle, level.getRainLevel(partialTick), level.getThunderLevel(partialTick));
        if (!sky.keepsSunriseGlow()) state.sunriseAndSunsetColor = 0;
        //?}
    }
}
//?} else {
/*// Before 1.21.9 there is no sky render state: the sky pass (a lambda in LevelRenderer.addSkyPass,
// the only caller of these two in LevelRenderer) reads the colours itself.
@Mixin(LevelRenderer.class)
abstract class SkyRendererMixin {
    @WrapOperation(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getSkyColor(Lnet/minecraft/world/phys/Vec3;F)I"),
            require = 0)
    private int shard$skyColour(ClientLevel level, Vec3 pos, float partialTick, Operation<Integer> original) {
        int vanilla = original.call(level, pos, partialTick);
        if (!ShardClient.isReady()) return vanilla;
        SkyModule sky = ShardClient.modules().get(SkyModule.class);
        if (!sky.active(level)) return vanilla;
        return sky.sky(vanilla, level.getSunAngle(partialTick), level.getRainLevel(partialTick), level.getThunderLevel(partialTick));
    }

    @WrapOperation(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/DimensionSpecialEffects;getSunriseOrSunsetColor(F)I"),
            require = 0)
    private int shard$sunriseGlow(DimensionSpecialEffects effects, float timeOfDay, Operation<Integer> original) {
        int vanilla = original.call(effects, timeOfDay);
        if (!ShardClient.isReady()) return vanilla;
        SkyModule sky = ShardClient.modules().get(SkyModule.class);
        return sky.active(net.minecraft.client.Minecraft.getInstance().level) && !sky.keepsSunriseGlow() ? 0 : vanilla;
    }
}
*///?}
