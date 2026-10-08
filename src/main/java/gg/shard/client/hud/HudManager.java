package gg.shard.client.hud;

import gg.shard.client.ShardClient;
import gg.shard.client.dev.SmokeTest;
import gg.shard.client.gui.Render2D;
import gg.shard.client.gui.Scale;
import gg.shard.client.module.Module;
import gg.shard.client.module.ModuleManager;
import gg.shard.client.modules.visual.CrosshairModule;
import gg.shard.client.modules.visual.TotemPopModule;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws every enabled HUD module (plus the crosshair and totem flash) through Fabric's HUD
 * registry. Elements are drawn in HUD units, normalised against the GUI scale exactly like the
 * settings page ({@link Scale#hudScale}), so the HUD looks the same at every GUI scale; the
 * per-element scale from the editor sits on top. Positions stay stored as fractions.
 */
public final class HudManager {
    public static final Identifier LAYER = Identifier.fromNamespaceAndPath(ShardClient.MOD_ID, "hud");
    private static double hudScale = 1.0;
    private final ModuleManager modules;

    public HudManager(ModuleManager modules) {
        this.modules = modules;
    }

    public void start() {
        HudElementRegistry.addLast(LAYER, this::renderLayer);
    }

    public List<HudModule> hudModules() {
        List<HudModule> out = new ArrayList<>();
        for (Module m : modules.all()) if (m instanceof HudModule h) out.add(h);
        return out;
    }

    /** GUI units per HUD unit for the current GUI scale and the global HUD scale setting. */
    public static double hudScale() {
        return hudScale;
    }

    /** Recomputes the HUD scale; the HUD layer and the editor call this once per frame. */
    public static void updateScale(Minecraft mc) {
        int guiScale = mc.getWindow().getGuiScale();
        int percent = ShardClient.modules() == null ? 100 : ShardClient.hudDefaults().hudScale.get();
        hudScale = Scale.hudScale(guiScale, percent);
        Render2D.setPixelsPerUnit(Scale.pixelsPerUnit(hudScale, guiScale));
    }

    private void renderLayer(GuiGraphics g, DeltaTracker delta) {
        SmokeTest.onFrame();
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.screen instanceof HudEditorScreen) return;
        updateScale(mc);
        for (HudModule m : hudModules()) {
            if (!m.isEnabled()) continue;
            if (m.needsPlayer() && mc.player == null) continue;
            renderOne(g, delta, m);
        }
        if (mc.player != null) {
            modules.get(CrosshairModule.class).render(g);
            modules.get(TotemPopModule.class).renderFlash(g);
        }
    }

    /** Shared by the live HUD and the editor so both show identical output. */
    public static void renderOne(GuiGraphics g, DeltaTracker delta, HudModule m) {
        int x = m.pixelX(g.guiWidth());
        int y = m.pixelY(g.guiHeight());
        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        float s = (float) (hudScale * m.scale());
        pose.scale(s, s);
        // Text and icons pick their raster density from this, so a scaled-up element stays sharp.
        double before = Render2D.pixelsPerUnit();
        Render2D.setPixelsPerUnit(s * Minecraft.getInstance().getWindow().getGuiScale());
        try {
            m.render(g, delta);
        } catch (RuntimeException e) {
            ShardClient.LOGGER.error("HUD module {} failed to render; disabling it", m.name(), e);
            m.setEnabled(false);
        } finally {
            Render2D.setPixelsPerUnit(before);
            pose.popMatrix();
        }
    }
}
