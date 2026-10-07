package gg.shard.client.hud;

import gg.shard.client.ShardClient;
import gg.shard.client.module.Module;
import gg.shard.client.module.ModuleManager;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.List;

/** Draws every enabled HUD module through Fabric's HUD element registry. */
public final class HudManager {
    public static final Identifier LAYER = Identifier.fromNamespaceAndPath(ShardClient.MOD_ID, "hud");
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

    private void renderLayer(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.screen instanceof HudEditorScreen) return;
        for (HudModule m : hudModules()) {
            if (!m.isEnabled()) continue;
            if (m.needsPlayer() && mc.player == null) continue;
            renderOne(g, delta, m);
        }
    }

    /** Shared by the live HUD and the editor so both show identical output. */
    public static void renderOne(GuiGraphics g, DeltaTracker delta, HudModule m) {
        int x = m.pixelX(g.guiWidth());
        int y = m.pixelY(g.guiHeight());
        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale((float) m.scale(), (float) m.scale());
        try {
            m.render(g, delta);
        } catch (RuntimeException e) {
            ShardClient.LOGGER.error("HUD module {} failed to render; disabling it", m.name(), e);
            m.setEnabled(false);
        } finally {
            pose.popMatrix();
        }
    }
}
