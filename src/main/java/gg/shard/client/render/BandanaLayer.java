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
//?} else if >=1.21.2 {
/*import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
*///?} else {
/*import com.mojang.blaze3d.vertex.VertexConsumer;
import gg.shard.client.ShardClient;
import gg.shard.client.modules.visual.CosmeticsModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.EquipmentSlot;
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
//?} else if >=1.21.2 {
/*public final class BandanaLayer extends RenderLayer<PlayerRenderState, PlayerModel> {
*///?} else {
/*// Before 1.21.2 (no render states) layers get the player itself.
public final class BandanaLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
*///?}
    private static final List<BandanaMesh.Quad> MESH = BandanaMesh.build();

    //? if >=1.21.9 {
    public BandanaLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
        super(parent);
    }

    @Override
    public void submit(PoseStack pose, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
    //?} else if >=1.21.2 {
    /*public BandanaLayer(RenderLayerParent<PlayerRenderState, PlayerModel> parent) {
        super(parent);
    }

    // Before 1.21.9 layers draw straight into the buffers.
    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, PlayerRenderState state, float yRot, float xRot) {
    *///?} else {
    /*public BandanaLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer state,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
    *///?}
        //? if >=1.21.2 {
        Identifier texture = ((BandanaState) state).shard$bandana();
        if (texture == null || state.isInvisible) return;
        //?} else {
        /*// The texture is looked up here (AvatarRendererMixin fills a render state from 1.21.2).
        Identifier texture = null;
        if (ShardClient.isReady()) {
            CosmeticsModule cosmetics = ShardClient.modules().get(CosmeticsModule.class);
            if (cosmetics != null) {
                try {
                    texture = cosmetics.bandanaTexture(state.getUUID(), state == Minecraft.getInstance().player);
                } catch (RuntimeException e) {
                    ShardClient.LOGGER.error("Cosmetics: bandana lookup failed", e);
                }
            }
        }
        if (texture == null || state.isInvisible()) return;
        *///?}
        //? if >=1.21.4 {
        if (!state.headEquipment.isEmpty() || !state.headItem.isEmpty() || state.wornHeadType != null) return;
        //?} else if <1.21.2 {
        /*if (!state.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) return;
        *///?} else {
        /*// Before 1.21.4 headItem is the head slot's stack (helmets, skulls and blocks alike).
        if (!state.headItem.isEmpty()) return;
        *///?}
        int overlay = LivingEntityRenderer.getOverlayCoords(state, 0.0F);
        pose.pushPose();
        //? if >=1.21.2 {
        PlayerModel model = getParentModel();
        model.root().translateAndRotate(pose);
        //?} else {
        /*// Before 1.21.2 the layer runs inside the body transform (as vanilla's CustomHeadLayer).
        PlayerModel<AbstractClientPlayer> model = getParentModel();
        *///?}
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
