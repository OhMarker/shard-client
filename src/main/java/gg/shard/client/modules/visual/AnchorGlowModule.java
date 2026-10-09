package gg.shard.client.modules.visual;

import gg.shard.client.compat.WorldDraw;
import gg.shard.client.gui.Fonts;
import gg.shard.client.gui.PanelPreview;
import gg.shard.client.gui.Render2D;
import gg.shard.client.gui.Theme;
import gg.shard.client.module.Module;
import gg.shard.client.module.ModuleCategory;
import gg.shard.client.module.setting.BoolSetting;
import gg.shard.client.module.setting.ColorSetting;
import gg.shard.client.module.setting.IntSetting;
import gg.shard.client.util.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;

import java.util.ArrayList;
import java.util.List;

/**
 * Anchor Glow: an outline on respawn anchors near you, coloured by how many charges they hold,
 * pulsing when full. The outline is drawn depth-tested like vanilla's block outline, so an
 * anchor behind a wall stays hidden; the charge is the same block state vanilla draws as the
 * glowstone on the anchor's top. Nothing is scanned that the client does not already have.
 */
public final class AnchorGlowModule extends Module implements PanelPreview {
    private final IntSetting range = add(new IntSetting("Range", "How far away anchors are outlined", 10, 4, 16, 1, " blocks"));
    private final IntSetting width = add(new IntSetting("Line width", "Thickness of the outline", 3, 1, 6, 1, ""));
    private final BoolSetting pulse = add(new BoolSetting("Pulse when full", "Fully charged anchors pulse so they stand out", true));
    private final BoolSetting empty = add(new BoolSetting("Outline empty anchors", "Also outline anchors with no charge", false).group("Colours"));
    private final ColorSetting c0 = add(new ColorSetting("Empty", "Colour with no charge", 0x99FFFFFF).group("Colours"));
    private final ColorSetting c1 = add(new ColorSetting("1 charge", "Colour with one charge", 0xFFFACC15).group("Colours"));
    private final ColorSetting c2 = add(new ColorSetting("2 charges", "Colour with two charges", 0xFFFB923C).group("Colours"));
    private final ColorSetting c3 = add(new ColorSetting("3 charges", "Colour with three charges", 0xFFF87171).group("Colours"));
    private final ColorSetting c4 = add(new ColorSetting("4 charges", "Colour when full", 0xFFC084FC).group("Colours"));

    private final List<long[]> anchors = new ArrayList<>();
    private int scanTicks;

    public AnchorGlowModule() {
        super("Anchor Glow", "Outline nearby respawn anchors in a colour for each charge level.", ModuleCategory.VISUALS);
        c0.visibleWhen(empty::get);
    }

    @Override
    public String icon() {
        return "glow";
    }

    @Override
    public String about() {
        return "Outlines the respawn anchors around you in a colour for each charge, so you can tell at a glance which ones are loaded. Full anchors can pulse. "
                + "The outline is depth-tested like vanilla's block outline: an anchor behind a wall stays hidden. The charge is the same block state vanilla shows "
                + "as glowstone on the anchor's top.";
    }

    @Override
    public void onTick() {
        if (++scanTicks < 4) return;
        scanTicks = 0;
        anchors.clear();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        BlockPos center = mc.player.blockPosition();
        int r = range.get();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dy = -r; dy <= r; dy++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    m.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    BlockState st = mc.level.getBlockState(m);
                    if (!st.is(Blocks.RESPAWN_ANCHOR)) continue;
                    int charge = st.getValue(RespawnAnchorBlock.CHARGE);
                    if (charge == 0 && !empty.get()) continue;
                    anchors.add(new long[]{m.asLong(), charge});
                }
            }
        }
    }

    @Override
    protected void onDisable() {
        anchors.clear();
    }

    private int colorFor(int charge) {
        return switch (charge) {
            case 0 -> c0.get();
            case 1 -> c1.get();
            case 2 -> c2.get();
            case 3 -> c3.get();
            default -> c4.get();
        };
    }

    /** Draws the outlines; called from Fabric's world render event after entities. */
    public void render(WorldDraw ctx) {
        if (!isEnabled() || anchors.isEmpty() || !ctx.ready()) return;
        Vec3 cam = ctx.camera();
        float pulseT = (float) (0.75 + 0.25 * Math.sin(System.currentTimeMillis() / 180.0));
        for (long[] a : anchors) {
            BlockPos pos = BlockPos.of(a[0]);
            int color = colorFor((int) a[1]);
            if (a[1] >= 4 && pulse.get()) color = Colors.fade(color, pulseT);
            ctx.outline(Shapes.block(), pos.getX() - cam.x, pos.getY() - cam.y, pos.getZ() - cam.z, color, width.get(), false);
        }
    }

    /** Five anchor tiles with their colours. */
    @Override
    public int renderPreview(GuiGraphics g, int x, int y, int w) {
        int h = 56;
        Render2D.roundedRect(g, x, y, w, h, Theme.radius(), 0xFF0B0F18);
        Render2D.roundedOutline(g, x, y, w, h, Theme.radius(), Theme.line());
        int n = 5;
        int tile = 24;
        int gap = (w - 24 - n * tile) / (n - 1);
        for (int i = 0; i < n; i++) {
            int tx = x + 12 + i * (tile + gap);
            int ty = y + 8;
            g.fill(tx, ty, tx + tile, ty + tile, 0xFF2A1F3D);
            for (int k = 0; k < i; k++) g.fill(tx + 3 + k * 5, ty + 3, tx + 6 + k * 5, ty + 6, 0xFFFFD86B);
            boolean show = i > 0 || empty.get();
            if (show) {
                int c = colorFor(i);
                for (int t = 0; t < Math.min(3, width.get()); t++) Render2D.outline(g, tx - 1 - t, ty - 1 - t, tile + 2 + 2 * t, tile + 2 + 2 * t, c);
            }
            Fonts.drawCentered(g, String.valueOf(i), Fonts.Weight.MEDIUM, 10, tx + tile / 2, ty + tile + 6, Theme.subtle());
        }
        return h;
    }
}
