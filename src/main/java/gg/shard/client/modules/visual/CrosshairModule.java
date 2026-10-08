package gg.shard.client.modules.visual;

import gg.shard.client.gui.Render2D;
import gg.shard.client.module.Module;
import gg.shard.client.module.ModuleCategory;
import gg.shard.client.module.setting.BoolSetting;
import gg.shard.client.module.setting.ColorSetting;
import gg.shard.client.module.setting.EnumSetting;
import gg.shard.client.module.setting.IntSetting;
import gg.shard.client.util.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameType;

import java.util.List;

/**
 * Replaces the vanilla crosshair with a configurable one. Vanilla's own rules still apply: it
 * only shows in first person, never with the GUI hidden, and the debug axes take precedence.
 * The dynamic gap and the hit marker react to the attacks vanilla already performed; they never
 * cause one.
 */
public final class CrosshairModule extends Module {
    public enum Style { CROSS, DOT, CROSS_DOT, CIRCLE }

    private static final long GAP_MS = 150;

    private final EnumSetting<Style> style = add(new EnumSetting<>("Style", "Shape of the crosshair", Style.CROSS).group("Shape"));
    private final IntSetting size = add(new IntSetting("Size", "Length of each arm", 5, 1, 12, 1, "px").group("Shape"));
    private final IntSetting gap = add(new IntSetting("Gap", "Space around the centre", 2, 0, 6, 1, "px").group("Shape"));
    private final IntSetting thickness = add(new IntSetting("Thickness", "Line width", 1, 1, 3, 1, "px").group("Shape"));
    private final BoolSetting dynamicGap = add(new BoolSetting("Dynamic gap", "Widen the gap for a moment when you attack", true).group("Shape")
            .details("Reacts to attacks you perform; it does not change aim or timing."));
    private final IntSetting gapKick = add(new IntSetting("Gap kick", "How far the gap widens on an attack", 3, 1, 8, 1, "px").group("Shape"));
    private final ColorSetting color = add(new ColorSetting("Colour", "Crosshair colour", 0xFFFFFFFF).group("Colour"));
    private final BoolSetting outline = add(new BoolSetting("Outline", "Dark outline for contrast", true).group("Colour"));
    private final BoolSetting highlightTarget = add(new BoolSetting("Highlight target", "Change colour while aiming at a player or mob", true).group("Colour"));
    private final ColorSetting targetColor = add(new ColorSetting("Target colour", "Colour while aiming at a living target", 0xFFFB7185).group("Colour"));
    private final BoolSetting hitMarker = add(new BoolSetting("Hit marker", "Flash an X around the crosshair when you hit a living target", true).group("Hit marker"));
    private final ColorSetting hitMarkerColor = add(new ColorSetting("Marker colour", "Colour of the hit marker", 0xFFFFFFFF, false).group("Hit marker"));
    private final IntSetting hitMarkerMs = add(new IntSetting("Marker time", "How long the marker stays", 250, 100, 600, 50, " ms").group("Hit marker"));

    private long attackAt = Long.MIN_VALUE / 2;
    private long hitAt = Long.MIN_VALUE / 2;

    public CrosshairModule() {
        super("Crosshair", "Your own crosshair: shape, size, colour, target highlight and hit marker.", ModuleCategory.VISUALS);
        targetColor.visibleWhen(highlightTarget::get);
        gapKick.visibleWhen(dynamicGap::get);
        hitMarkerColor.visibleWhen(hitMarker::get);
        hitMarkerMs.visibleWhen(hitMarker::get);
    }

    @Override
    public String about() {
        return "Draws a crosshair of your choice in place of vanilla's and keeps vanilla's rules (first person only, hidden with the GUI, debug axes win). "
                + "The target highlight reads what vanilla already picked under the cursor; the gap kick and hit marker react to attacks you made. Nothing aims or clicks for you.";
    }

    @Override
    public String icon() {
        return "crosshair";
    }

    @Override
    public List<String> conflictingMods() {
        return List.of("custom-crosshair-mod");
    }

    /** Called after vanilla sent an attack; living targets also start the hit marker. */
    public void onAttack(Entity target) {
        long now = System.currentTimeMillis();
        attackAt = now;
        if (target instanceof LivingEntity) hitAt = now;
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
        long now = System.currentTimeMillis();
        if (dynamicGap.get()) {
            float progress = Math.min(1f, (now - attackAt) / (float) GAP_MS);
            gp += Math.round(gapKick.get() * (1f - Render2D.easeOut(progress)));
        }
        if (outline.get()) draw(g, cx, cy, s + 1, Math.max(0, gp - 1), t + 2, 0xB0000000);
        draw(g, cx, cy, s, gp, t, c);
        if (hitMarker.get()) {
            long age = now - hitAt;
            if (age >= 0 && age < hitMarkerMs.get()) {
                float alpha = 1f - age / (float) hitMarkerMs.get();
                int mc2 = Colors.fade(hitMarkerColor.get(), alpha);
                int r0 = gp + s + 3;
                for (int i = 0; i < 5; i++) {
                    int d = r0 + i;
                    int half = t / 2;
                    if (outline.get()) {
                        int oc = Colors.fade(0xB0000000, alpha);
                        Render2D.fill(g, cx - d - half - 1, cy - d - half - 1, t + 2, t + 2, oc);
                        Render2D.fill(g, cx + d - half - 1, cy - d - half - 1, t + 2, t + 2, oc);
                        Render2D.fill(g, cx - d - half - 1, cy + d - half - 1, t + 2, t + 2, oc);
                        Render2D.fill(g, cx + d - half - 1, cy + d - half - 1, t + 2, t + 2, oc);
                    }
                    Render2D.fill(g, cx - d - half, cy - d - half, t, t, mc2);
                    Render2D.fill(g, cx + d - half, cy - d - half, t, t, mc2);
                    Render2D.fill(g, cx - d - half, cy + d - half, t, t, mc2);
                    Render2D.fill(g, cx + d - half, cy + d - half, t, t, mc2);
                }
            }
        }
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
