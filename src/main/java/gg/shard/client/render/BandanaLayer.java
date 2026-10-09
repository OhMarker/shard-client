package gg.shard.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import gg.shard.client.cosmetics.BandanaMesh;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
//? if >=1.21.9 {
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
//?} else {
/*import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
*///?}
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * The bandana cosmetic: a thin shell over the top, sides and back of the head with a knot and two
 * tails (BandanaMesh), following the head's pose. Hidden under any helmet or head item, and for
 * invisible players. Feature layer on every player renderer (registered in ShardClient).
 */
//? if >=1.21.9 {
public final class BandanaLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
//?} else {
/*public final class BandanaLayer extends RenderLayer<PlayerRenderState, PlayerModel> {
*///?}
    private static final List<BandanaMesh.Quad> MESH = BandanaMesh.build();

    //? if >=1.21.9 {
    public BandanaLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
        super(parent);
    }

    @Override
    public void submit(PoseStack pose, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
    //?} else {
    /*public BandanaLayer(RenderLayerParent<PlayerRenderState, PlayerModel> parent) {
        super(parent);
    }

    // Before 1.21.9 layers draw straight into the buffers.
    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, PlayerRenderState state, float yRot, float xRot) {
    *///?}
        Identifier texture = ((BandanaState) state).shard$bandana();
        if (texture == null || state.isInvisible) return;
        if (!state.headEquipment.isEmpty() || !state.headItem.isEmpty() || state.wornHeadType != null) return;
        int overlay = LivingEntityRenderer.getOverlayCoords(state, 0.0F);
        pose.pushPose();
        PlayerModel model = getParentModel();
        model.root().translateAndRotate(pose);
        model.getHead().translateAndRotate(pose);
        //? if >=1.21.9 {
        collector.submitCustomGeometry(pose, RenderTypes.entityCutoutNoCull(texture), (p, vc) -> {
            for (BandanaMesh.Quad q : MESH) {
                for (int i = 0; i < 4; i++) {
                    vc.addVertex(p, q.x(i) / 16f, q.y(i) / 16f, q.z(i) / 16f)
                            .setColor(-1)
                            .setUv(q.u(i), q.w(i))
                            .setOverlay(overlay)
                            .setLight(light)
                            .setNormal(p, q.nx(), q.ny(), q.nz());
                }
            }
        });
        //?} else {
        /*VertexConsumer vc = buffers.getBuffer(RenderTypes.entityCutoutNoCull(texture));
        PoseStack.Pose p = pose.last();
        for (BandanaMesh.Quad q : MESH) {
            for (int i = 0; i < 4; i++) {
                vc.addVertex(p, q.x(i) / 16f, q.y(i) / 16f, q.z(i) / 16f)
                        .setColor(-1)
                        .setUv(q.u(i), q.w(i))
                        .setOverlay(overlay)
                        .setLight(light)
                        .setNormal(p, q.nx(), q.ny(), q.nz());
            }
        }
        *///?}
        pose.popPose();
    }
}
