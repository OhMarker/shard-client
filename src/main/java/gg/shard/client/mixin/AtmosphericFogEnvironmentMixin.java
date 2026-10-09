package gg.shard.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import gg.shard.client.ShardClient;
import gg.shard.client.modules.visual.SkyModule;
import gg.shard.client.modules.visual.SkyPalette;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.environment.AtmosphericFogEnvironment;
//? if >=1.21.11 <26.1
import net.minecraft.client.renderer.PanoramicScreenshotParameters;
//? if >=1.21.11 {
import net.minecraft.world.attribute.EnvironmentAttributes;
//?} else {

/*import net.minecraft.client.renderer.fog.environment.AirBasedFogEnvironment;
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
//?} else {
/*// Before 1.21.11 getBaseColor lives in the shared superclass (also the nether / boss fog).
@Mixin(AirBasedFogEnvironment.class)
*///?}
abstract class AtmosphericFogEnvironmentMixin {
    // 26.3 returns the colour as a float vector.
    //? if >=26.3 {
    /*@ModifyReturnValue(method = "getBaseColor", at = @At("RETURN"), require = 0)
    private Vector3fc shard$fogColour(Vector3fc original, ClientLevel level, Camera camera, int renderDistance, float partialTick) {
        int vanilla = net.minecraft.util.ARGB.colorFromVector3f(original);
        int colour = shard$fog(vanilla, level, camera, renderDistance, partialTick);
        return colour == vanilla ? original : net.minecraft.util.ARGB.vector3fFromRGB24(colour);
    }
    *///?} else {
    @ModifyReturnValue(method = "getBaseColor", at = @At("RETURN"), require = 0)
    private int shard$fogColour(int original, ClientLevel level, Camera camera, int renderDistance, float partialTick) {
        return shard$fog(original, level, camera, renderDistance, partialTick);
    }
    //?}

    @org.spongepowered.asm.mixin.Unique
    private int shard$fog(int original, ClientLevel level, Camera camera, int renderDistance, float partialTick) {
        if (!ShardClient.isReady() || !((Object) this instanceof AtmosphericFogEnvironment)) return original;
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
