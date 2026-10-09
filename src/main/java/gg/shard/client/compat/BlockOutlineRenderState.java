package gg.shard.client.compat;

// Before 1.21.9 there is no block outline render state; Fabric's BLOCK_OUTLINE event gives the
// block, and this stand-in carries what BlockOutlineModule reads (the stonecutter replacement
// points the vanilla class here on older versions).
//? if <1.21.9 {
/*import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public record BlockOutlineRenderState(BlockPos pos, VoxelShape shape, boolean isTranslucent) {
    /^* The outline vanilla is about to draw (its shape for the camera entity, as vanilla's renderHitOutline). ^/
    public static BlockOutlineRenderState of(WorldRenderContext ctx, WorldRenderContext.BlockOutlineContext outline) {
        BlockPos pos = outline.blockPos();
        VoxelShape shape = outline.blockState().getShape(ctx.world(), pos, CollisionContext.of(outline.entity()));
        //? if >=1.21.4 {
        return new BlockOutlineRenderState(pos, shape, ctx.translucentBlockOutline());
        //?} else {
        /^// Fabric fires BLOCK_OUTLINE only in the pass vanilla draws this block's outline in.
        boolean translucent = net.minecraft.client.renderer.ItemBlockRenderTypes.getChunkRenderType(outline.blockState()).sortOnUpload();
        return new BlockOutlineRenderState(pos, shape, translucent);
        ^///?}
    }
}
*///?}
