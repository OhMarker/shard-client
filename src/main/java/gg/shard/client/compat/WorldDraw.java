package gg.shard.client.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
//? if >=26.2 {
/*import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
*///?} else {
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
//?}

/**
 * What Shard's world overlays need to draw: the camera-relative pose, the buffers and the camera
 * position. Fabric's WorldRenderContext provides it from 1.21.10; 1.21.9's Fabric API has no
 * world render events, so LevelRendererEventsMixin builds it there. From 26.2 the level pass has
 * no immediate buffers: shapes are submitted to the frame's SubmitNodeCollector instead.
 */
//? if >=26.2 {
/*public record WorldDraw(PoseStack matrices, SubmitNodeCollector submits, Vec3 camera) {
    public static WorldDraw of(net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext ctx) {
        return new WorldDraw(ctx.poseStack(), ctx.submitNodeCollector(), ctx.levelState().cameraRenderState.pos);
    }

    public boolean ready() {
        return submits != null;
    }

    /^* Outlines {@code shape} at (x, y, z) relative to the camera, {@code width} pixels wide. ^/
    public void outline(VoxelShape shape, double x, double y, double z, int color, float width, boolean afterTerrain) {
        matrices.pushPose();
        matrices.translate(x, y, z);
        submits.submitShapeOutline(matrices, shape, RenderTypes.lines(), color, width, afterTerrain);
        matrices.popPose();
    }
*///?} else {
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

    public boolean ready() {
        return consumers != null;
    }

    /** Outlines {@code shape} at (x, y, z) relative to the camera, {@code width} pixels wide. */
    public void outline(VoxelShape shape, double x, double y, double z, int color, float width, boolean afterTerrain) {
        VertexConsumer lines = consumers.getBuffer(Lines.type(width));
        Lines.shape(matrices, lines, shape, x, y, z, color, width);
    }
//?}
}
