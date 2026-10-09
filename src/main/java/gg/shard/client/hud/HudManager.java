package gg.shard.client.hud;

import gg.shard.client.ShardClient;
import gg.shard.client.dev.SmokeTest;
import gg.shard.client.gui.Render2D;
import gg.shard.client.gui.Scale;
import gg.shard.client.module.Module;
import gg.shard.client.module.ModuleManager;
import gg.shard.client.modules.visual.CrosshairModule;
import gg.shard.client.modules.visual.TotemPopModule;
//? if >=1.21.6 {
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
//?} else {
/*import net.fabricmc.fabric.api.client.rendering.v1.HudLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.IdentifiedLayer;
*///?}
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
    /** Every frame's time while the HUD layer draws (FPS 1% low and graph). */
    public static final gg.shard.client.util.FrameStats FRAMES = new gg.shard.client.util.FrameStats(1000);
    /** Benchmark only: total nanoseconds per HUD element while profiling. */
    public static final java.util.Map<String, long[]> PROFILE = new java.util.concurrent.ConcurrentHashMap<>();
    public static boolean profiling;
    /** CPU time of the whole Shard HUD layer per frame, in ms (benchmark). */
    public static final gg.shard.client.util.FrameStats LAYER_MS = new gg.shard.client.util.FrameStats(4000);
    private static long lastFrameNs;
    private final ModuleManager modules;

    public HudManager(ModuleManager modules) {
        this.modules = modules;
    }

    public void start() {
        //? if >=1.21.6 {
        HudElementRegistry.addLast(LAYER, this::renderLayer);
        //?} else {
        /*// Before 1.21.6 Fabric wraps Gui's LayeredDraw; the last root layer draws after the subtitles.
        HudLayerRegistrationCallback.EVENT.register(drawer -> drawer.addLayer(IdentifiedLayer.of(LAYER, this::renderLayer)));
        *///?}
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
        long now = System.nanoTime();
        if (lastFrameNs != 0) FRAMES.add((now - lastFrameNs) / 1_000_000f);
        lastFrameNs = now;
        try {
            renderLayerTimed(g, delta);
        } finally {
            LAYER_MS.add(Math.max(0.0001f, (System.nanoTime() - now) / 1_000_000f));
        }
    }

    private void renderLayerTimed(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.screen instanceof HudEditorScreen) return;
        updateScale(mc);
        for (HudModule m : hudModules()) {
            if (!m.isEnabled()) continue;
            if (m.needsPlayer() && mc.player == null) continue;
            if (!profiling) {
                renderOne(g, delta, m);
                continue;
            }
            long t = System.nanoTime();
            renderOne(g, delta, m);
            long[] acc = PROFILE.computeIfAbsent(m.key(), k -> new long[2]);
            acc[0] += System.nanoTime() - t;
            acc[1]++;
        }
        if (mc.player != null) {
            modules.get(CrosshairModule.class).render(g);
            modules.get(TotemPopModule.class).renderFlash(g);
            modules.get(gg.shard.client.modules.visual.LowHealthModule.class).render(g);
            modules.get(gg.shard.client.modules.combat.CrystalOptimizerModule.class).renderReadout(g);
        }
    }

    /** Shared by the live HUD and the editor so both show identical output. */
    public static void renderOne(GuiGraphics g, DeltaTracker delta, HudModule m) {
        // Whole physical pixels, so text stays crisp after a 1-pixel nudge at any GUI scale.
        int guiScale = Minecraft.getInstance().getWindow().getGuiScale();
        float x = Math.round(m.posX(g.guiWidth()) * guiScale) / (float) guiScale;
        float y = Math.round(m.posY(g.guiHeight()) * guiScale) / (float) guiScale;
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
        // Old fractional positions become anchored once the element's real size is known.
        m.resolve(g.guiWidth(), g.guiHeight());
    }
}
