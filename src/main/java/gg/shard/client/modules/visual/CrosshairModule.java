package gg.shard.client.modules.visual;

import gg.shard.client.gui.Render2D;
import gg.shard.client.module.Module;
import gg.shard.client.module.ModuleCategory;
import gg.shard.client.module.setting.BoolSetting;
import gg.shard.client.module.setting.ColorSetting;
import gg.shard.client.module.setting.EnumSetting;
import gg.shard.client.module.setting.IntSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameType;

import java.util.List;

/**
 * Replaces the vanilla crosshair with a configurable one. Vanilla's own rules still apply: it
 * only shows in first person, never with the GUI hidden, and the debug axes take precedence.
 */
public final class CrosshairModule extends Module {
    public enum Style { CROSS, DOT, CROSS_DOT, CIRCLE }

    private final EnumSetting<Style> style = add(new EnumSetting<>("Style", "Shape of the crosshair", Style.CROSS).group("Shape"));
    private final IntSetting size = add(new IntSetting("Size", "Length of each arm", 5, 1, 12, 1, "px").group("Shape"));
    private final IntSetting gap = add(new IntSetting("Gap", "Space around the centre", 2, 0, 6, 1, "px").group("Shape"));
    private final IntSetting thickness = add(new IntSetting("Thickness", "Line width", 1, 1, 3, 1, "px").group("Shape"));
    private final ColorSetting color = add(new ColorSetting("Colour", "Crosshair colour", 0xFFFFFFFF).group("Colour"));
    private final BoolSetting outline = add(new BoolSetting("Outline", "Dark outline for contrast", true).group("Colour"));
    private final BoolSetting highlightTarget = add(new BoolSetting("Highlight target", "Change colour while aiming at a player or mob", true).group("Colour"));
    private final ColorSetting targetColor = add(new ColorSetting("Target colour", "Colour while aiming at a living target", 0xFFFB7185).group("Colour"));

    public CrosshairModule() {
        super("Crosshair", "Your own crosshair: shape, size, colour and a target highlight.", ModuleCategory.VISUALS);
        targetColor.visibleWhen(highlightTarget::get);
    }

    @Override
    public String icon() {
        return "glyph:crosshair";
    }

    @Override
    public List<String> conflictingMods() {
        return List.of("custom-crosshair-mod");
    }

    /** True when vanilla's crosshair should be cancelled and ours drawn instead. */
    public boolean replacesVanilla() {
        if (!isEnabled()) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null) return false;
        if (mc.debugEntries != null && mc.debugEntries.isCurrentlyEnabled(DebugScreenEntries.THREE_DIMENSIONAL_CROSSHAIR)) return false;
        return true;
    }

    /** Draws the crosshair in the HUD layer; respects vanilla's first-person and spectator rules. */
    public void render(GuiGraphics g) {
        if (!replacesVanilla()) return;
        Minecraft mc = Minecraft.getInstance();
        if (!mc.options.getCameraType().isFirstPerson()) return;
        if (mc.gameMode != null && mc.gameMode.getPlayerMode() == GameType.SPECTATOR && mc.crosshairPickEntity == null) return;
        int cx = g.guiWidth() / 2;
        int cy = g.guiHeight() / 2;
        int c = color.get();
        if (highlightTarget.get() && mc.crosshairPickEntity instanceof LivingEntity) c = targetColor.get();
        int t = thickness.get();
        int s = size.get();
        int gp = gap.get();
        if (outline.get()) draw(g, cx, cy, s + 1, Math.max(0, gp - 1), t + 2, 0xB0000000);
        draw(g, cx, cy, s, gp, t, c);
    }

    private void draw(GuiGraphics g, int cx, int cy, int s, int gp, int t, int c) {
        int half = t / 2;
        switch (style.get()) {
            case CROSS -> arms(g, cx, cy, s, gp, t, half, c);
            case DOT -> Render2D.fill(g, cx - half, cy - half, t, t, c);
            case CROSS_DOT -> {
                arms(g, cx, cy, s, gp, t, half, c);
                Render2D.fill(g, cx - half, cy - half, t, t, c);
            }
            case CIRCLE -> {
                int r = gp + s / 2 + 1;
                Render2D.roundedOutline(g, cx - r, cy - r, r * 2 + 1, r * 2 + 1, r, c);
                Render2D.fill(g, cx - half, cy - half, t, t, c);
            }
        }
    }

    private static void arms(GuiGraphics g, int cx, int cy, int s, int gp, int t, int half, int c) {
        Render2D.fill(g, cx - half, cy - gp - s, t, s, c);
        Render2D.fill(g, cx - half, cy + gp + 1, t, s, c);
        Render2D.fill(g, cx - gp - s, cy - half, s, t, c);
        Render2D.fill(g, cx + gp + 1, cy - half, s, t, c);
    }
}
