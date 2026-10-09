package gg.shard.client.compat;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
//? if <26.2 {
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.world.phys.shapes.VoxelShape;
//?}
//? if <1.21.11 {
/*import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
//? if >=1.21.5 {
import com.mojang.blaze3d.pipeline.RenderPipeline;
//?}
import java.util.HashMap;
import java.util.Map;
*///?}

/**
 * World-space lines with a chosen width in pixels. From 1.21.11 the width travels with each
 * vertex (ShapeRenderer takes it); before, it was one value per draw (the render type's line
 * state), so older versions get one lines render type per width. From 26.2 shapes are submitted
 * (WorldDraw.outline), not drawn into a buffer here.
 */
public final class Lines {
    private Lines() {}

    /** The buffer type for lines {@code width} pixels wide. */
    public static RenderType type(float width) {
        //? if >=1.21.11 {
        return RenderTypes.lines();
        //?} else {
        /*return WIDE.computeIfAbsent(Math.round(width * 4f), q -> new WideLines(q / 4f));
        *///?}
    }

    //? if <26.2 {
    /** Outlines {@code shape} at (x, y, z) relative to the camera; {@code consumer} from {@link #type}. */
    public static void shape(PoseStack pose, VertexConsumer consumer, VoxelShape shape, double x, double y, double z, int color, float width) {
        //? if >=1.21.11 {
        ShapeRenderer.renderShape(pose, consumer, shape, x, y, z, color, width);
        //?} else if >=1.21.2 {
        /*ShapeRenderer.renderShape(pose, consumer, shape, x, y, z, color);
        *///?} else {
        /*// Before 1.21.2 the shape outline is LevelRenderer's private renderShape (float colour).
        PoseStack.Pose last = pose.last();
        shape.forAllEdges((x0, y0, z0, x1, y1, z1) -> {
            float nx = (float) (x1 - x0), ny = (float) (y1 - y0), nz = (float) (z1 - z0);
            float len = net.minecraft.util.Mth.sqrt(nx * nx + ny * ny + nz * nz);
            nx /= len;
            ny /= len;
            nz /= len;
            consumer.addVertex(last, (float) (x0 + x), (float) (y0 + y), (float) (z0 + z)).setColor(color).setNormal(last, nx, ny, nz);
            consumer.addVertex(last, (float) (x1 + x), (float) (y1 + y), (float) (z1 + z)).setColor(color).setNormal(last, nx, ny, nz);
        });
        *///?}
    }
    //?}

    //? if <1.21.11 {
    /*private static final Map<Integer, RenderType> WIDE = new HashMap<>();
    /^* Width for the draw in progress, read by CompositeRenderTypeMixin; NaN = vanilla's. ^/
    private static float drawWidth = Float.NaN;

    /^* Vanilla's line width for this draw unless a {@link WideLines} draw is in progress. ^/
    public static float lineWidth(float vanilla) {
        return Float.isNaN(drawWidth) ? vanilla : drawWidth;
    }

    /^* Vanilla's lines type with its own width: draws through RenderType.lines() with the width swapped in. ^/
    private static final class WideLines extends RenderType {
        private final RenderType base = RenderType.lines();
        private final float width;

        WideLines(float width) {
            //? if >=1.21.5 {
            super("shard_lines_" + width, 1536, false, false, () -> {}, () -> {});
            //?} else {
            /^super("shard_lines_" + width, RenderType.lines().format(), RenderType.lines().mode(), 1536, false, false, () -> {}, () -> {});
            ^///?}
            this.width = width;
        }

        @Override
        public void draw(MeshData mesh) {
            drawWidth = width;
            try {
                base.draw(mesh);
            } finally {
                drawWidth = Float.NaN;
            }
        }

        @Override
        public VertexFormat format() {
            return base.format();
        }

        @Override
        public VertexFormat.Mode mode() {
            return base.mode();
        }

        //? if >=1.21.9 {
        @Override
        public RenderPipeline pipeline() {
            return base.pipeline();
        }
        //?} else if >=1.21.5 <1.21.6 {
        /^@Override
        public RenderPipeline getRenderPipeline() {
            return base.getRenderPipeline();
        }

        @Override
        public com.mojang.blaze3d.pipeline.RenderTarget getRenderTarget() {
            return base.getRenderTarget();
        }
        ^///?}
    }
    *///?}
}
