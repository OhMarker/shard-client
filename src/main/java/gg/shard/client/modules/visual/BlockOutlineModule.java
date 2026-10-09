package gg.shard.client.modules.visual;

import gg.shard.client.compat.WorldDraw;
import gg.shard.client.module.Module;
import gg.shard.client.module.ModuleCategory;
import gg.shard.client.module.setting.BoolSetting;
import gg.shard.client.module.setting.ColorSetting;
import gg.shard.client.module.setting.IntSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.BlockOutlineRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Block Outline: your own colour and thickness for the outline of the block you look at, and an
 * optional crystal-spot hint: on obsidian or bedrock with room above it (air, no entities in the
 * way, judged from what the client already has) the outline turns a second colour. Visual only,
 * nothing is placed; the looked-at block is the one vanilla picked, so nothing is scanned.
 */
public final class BlockOutlineModule extends Module {
    private final ColorSetting color = add(new ColorSetting("Colour", "Outline colour (alpha is opacity)", 0xCCFFFFFF));
    private final IntSetting width = add(new IntSetting("Thickness", "Line width", 3, 1, 8, 1, ""));
    private final BoolSetting crystalSpot = add(new BoolSetting("Crystal spot hint", "Use another colour on obsidian or bedrock a crystal could go on", true)
            .details("Checks the block above is air and nothing stands in the space a crystal needs. Visual only; it never places anything."));
    private final ColorSetting spotColor = add(new ColorSetting("Spot colour", "Outline colour on a free crystal spot", 0xFF4ADE80));

    public BlockOutlineModule() {
        super("Block Outline", "Your own outline colour and thickness, with a crystal spot hint.", ModuleCategory.VISUALS);
        spotColor.visibleWhen(crystalSpot::get);
    }

    @Override
    public String icon() {
        return "outline";
    }

    @Override
    public String about() {
        return "Replaces vanilla's thin black block outline with your colour and thickness. With the crystal spot hint, obsidian and bedrock you could place a crystal on "
                + "(air above and nothing in the way, from what the client already knows) get their own colour. It only draws; placing is still you.";
    }

    /** Returns false to cancel vanilla's outline after drawing ours. */
    public boolean render(WorldDraw ctx, BlockOutlineRenderState state) {
        if (!isEnabled() || !ctx.ready()) return true;
        Minecraft mc = Minecraft.getInstance();
        Vec3 cam = ctx.camera();
        BlockPos pos = state.pos();
        int c = color.get();
        if (crystalSpot.get() && mc.level != null && freeCrystalSpot(mc, pos)) c = spotColor.get();
        ctx.outline(state.shape(), pos.getX() - cam.x, pos.getY() - cam.y, pos.getZ() - cam.z, c, width.get(), state.isTranslucent());
        return false;
    }

    private static boolean freeCrystalSpot(Minecraft mc, BlockPos pos) {
        BlockState below = mc.level.getBlockState(pos);
        if (!below.is(Blocks.OBSIDIAN) && !below.is(Blocks.BEDROCK)) return false;
        if (!mc.level.getBlockState(pos.above()).isAir()) return false;
        AABB space = new AABB(pos.getX(), pos.getY() + 1, pos.getZ(), pos.getX() + 1, pos.getY() + 3, pos.getZ() + 1);
        return mc.level.getEntities(null, space).isEmpty();
    }
}
