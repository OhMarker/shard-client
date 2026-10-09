package gg.shard.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import gg.shard.client.ShardClient;
import gg.shard.client.modules.visual.SkyModule;
import gg.shard.client.modules.visual.SkyPalette;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
//? if >=1.21.6
import net.minecraft.client.renderer.fog.environment.AtmosphericFogEnvironment;
//? if >=1.21.11 <26.1
import net.minecraft.client.renderer.PanoramicScreenshotParameters;
//? if >=1.21.11 {
import net.minecraft.world.attribute.EnvironmentAttributes;
//?} else if >=1.21.6 {

/*import net.minecraft.client.renderer.fog.environment.AirBasedFogEnvironment;
*///?} else {
/*import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import net.minecraft.client.renderer.FogRenderer;
import org.joml.Vector4f;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
*///?}
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Sky module: the Overworld's fog / horizon colour. Rebuilds vanilla's mix (horizon colour, the
 * sunrise tint when facing the sun, then towards the sky colour by render distance) with the
 * module's colours, and blends the result with vanilla's by Strength.
 */
//? if >=1.21.11 {
@Mixin(AtmosphericFogEnvironment.class)
//?} else if >=1.21.6 {
/*// Before 1.21.11 getBaseColor lives in the shared superclass (also the nether / boss fog).
@Mixin(AirBasedFogEnvironment.class)
*///?} else {
/*// Before 1.21.6 there are no fog environments: FogRenderer.computeFogColor works the air colour
// out inline (slots 7-9, finished where the branch resets biomeChangedTime) and then darkens it.
@Mixin(FogRenderer.class)
*///?}
abstract class AtmosphericFogEnvironmentMixin {
    // 26.3 returns the colour as a float vector.
    //? if >=26.3 {
    /*@ModifyReturnValue(method = "getBaseColor", at = @At("RETURN"), require = 0)
    private Vector3fc shard$fogColour(Vector3fc original, ClientLevel level, Camera camera, int renderDistance, float partialTick) {
        if (!((Object) this instanceof AtmosphericFogEnvironment)) return original;
        int vanilla = net.minecraft.util.ARGB.colorFromVector3f(original);
        int colour = shard$fog(vanilla, level, camera, renderDistance, partialTick);
        return colour == vanilla ? original : net.minecraft.util.ARGB.vector3fFromRGB24(colour);
    }
    *///?} else if >=1.21.6 {
    @ModifyReturnValue(method = "getBaseColor", at = @At("RETURN"), require = 0)
    private int shard$fogColour(int original, ClientLevel level, Camera camera, int renderDistance, float partialTick) {
        if (!((Object) this instanceof AtmosphericFogEnvironment)) return original;
        return shard$fog(original, level, camera, renderDistance, partialTick);
    }
    //?} else {
    /*@Inject(method = "computeFogColor", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/FogRenderer;biomeChangedTime:J",
            opcode = Opcodes.PUTSTATIC, ordinal = 4), require = 0)
    private static void shard$fogColour(Camera camera, float partialTick, ClientLevel level, int renderDistance, float darken,
                                        CallbackInfoReturnable<Vector4f> cir, @Local(index = 7) LocalFloatRef r,
                                        @Local(index = 8) LocalFloatRef g, @Local(index = 9) LocalFloatRef b) {
        int original = net.minecraft.util.ARGB.colorFromFloat(1f, r.get(), g.get(), b.get());
        int colour = shard$fog(original, level, camera, renderDistance, partialTick);
        if (colour == original) return;
        r.set(net.minecraft.util.ARGB.redFloat(colour));
        g.set(net.minecraft.util.ARGB.greenFloat(colour));
        b.set(net.minecraft.util.ARGB.blueFloat(colour));
    }
    *///?}

    @org.spongepowered.asm.mixin.Unique
    private static int shard$fog(int original, ClientLevel level, Camera camera, int renderDistance, float partialTick) {
        if (!ShardClient.isReady()) return original;
        SkyModule sky = ShardClient.modules().get(SkyModule.class);
        if (!sky.active(level) || !sky.tintsFog()) return original;
        //? if >=1.21.11 {
        float sunAngle = camera.attributeProbe().getValue(EnvironmentAttributes.SUN_ANGLE, partialTick) * (float) (Math.PI / 180.0);
        //?} else {
        /*float sunAngle = level.getSunAngle(partialTick);
        *///?}
        float rain = level.getRainLevel(partialTick);
        float thunder = level.getThunderLevel(partialTick);
        int fog = sky.horizon(sunAngle, rain, thunder);
        if (renderDistance >= 4 && sky.keepsSunriseGlow()) {
            float side = Math.sin(sunAngle) > 0 ? -1f : 1f;
            //? if >=26.1 {
            /*Vector3fc forward = camera.isPanoramicMode() ? camera.panoramicForwards() : camera.forwardVector();
            *///?} else if >=1.21.11 {
            PanoramicScreenshotParameters pano = Minecraft.getInstance().gameRenderer.getPanoramicScreenshotParameters();
            Vector3fc forward = pano != null ? pano.forwardVector() : camera.forwardVector();
            //?} else {

            /*Vector3fc forward = camera.getLookVector();
            *///?}
            float facing = forward.dot(side, 0f, 0f);
            if (facing > 0) {
                //? if >=26.3 {
                /*int glow = net.minecraft.util.ARGB.colorFromVector4f(camera.attributeProbe().getValue(EnvironmentAttributes.SUNRISE_SUNSET_COLOR, partialTick));
                *///?} else if >=1.21.11 {
                int glow = camera.attributeProbe().getValue(EnvironmentAttributes.SUNRISE_SUNSET_COLOR, partialTick);
                //?} else {
                /*float time = level.getTimeOfDay(partialTick);
                int glow = level.effects().isSunriseOrSunset(time) ? level.effects().getSunriseOrSunsetColor(time) : 0;
                *///?}
                float a = ((glow >>> 24) & 0xFF) / 255f;
                if (a > 0) fog = SkyPalette.lerp(facing * a, fog, glow);
            }
        }
        int skyColour = sky.ownSky(sunAngle, rain, thunder);
        //? if >=1.21.11 {
        float far = Math.min(camera.attributeProbe().getValue(EnvironmentAttributes.SKY_FOG_END_DISTANCE, partialTick) / 16f, renderDistance);
        //?} else {
        /*float far = renderDistance;
        *///?}
        float o = 0.25f + 0.75f * Math.max(0f, Math.min(1f, far / 32f));
        o = 1f - (float) Math.pow(o, 0.25);
        return sky.blendFog(original, SkyPalette.lerp(o, fog, skyColour));
    }
}
