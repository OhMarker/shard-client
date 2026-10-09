package gg.shard.client.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;

/**
 * What Shard's world overlays need to draw: the camera-relative pose, the buffers and the camera
 * position. Fabric's WorldRenderContext provides it from 1.21.10; 1.21.9's Fabric API has no
 * world render events, so LevelRendererEventsMixin builds it there.
 */
public record WorldDraw(PoseStack matrices, MultiBufferSource consumers, Vec3 camera) {
    //? if >=26.1 {
    /*public static WorldDraw of(net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext ctx) {
        return new WorldDraw(ctx.poseStack(), ctx.bufferSource(), ctx.levelState().cameraRenderState.pos);
    }
    *///?} else if >=1.21.10 {
    public static WorldDraw of(net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext ctx) {
        return new WorldDraw(ctx.matrices(), ctx.consumers(), ctx.worldState().cameraRenderState.pos);
    }
    //?}

}
