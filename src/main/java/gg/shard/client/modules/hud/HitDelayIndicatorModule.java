package gg.shard.client.modules.hud;

import gg.shard.client.gui.Render2D;
import gg.shard.client.gui.Theme;
import gg.shard.client.hud.HudModule;
import gg.shard.client.module.setting.BoolSetting;
import gg.shard.client.module.setting.IntSetting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;

/**
 * Shows the vanilla attack cooldown as a bar near the crosshair. This is the same value the
 * vanilla attack indicator exposes; it just makes it readable mid-fight.
 */
public final class HitDelayIndicatorModule extends HudModule {
    private final IntSetting width = add(new IntSetting("Width", "Bar width in pixels", 40, 16, 120, 2, "px"));
    private final BoolSetting hideWhenReady = add(new BoolSetting("Hide when ready", "Only show while the attack is recharging", true));
    private final BoolSetting center = add(new BoolSetting("Follow crosshair", "Ignore the saved position and sit under the crosshair", true));

    public HitDelayIndicatorModule() {
        super("Hit Delay", "Attack cooldown bar under the crosshair.", 0.47, 0.56);
    }

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        LocalPlayer p = mc().player;
        if (p == null) return;
        float strength = p.getAttackStrengthScale(delta.getGameTimeDeltaPartialTick(true));
        boolean ready = strength >= 1f;
        int w = width.get();
        int h = 3;
        if (center.get()) {
            // Undo the manager's translation so the bar always hugs the crosshair.
            int cx = g.guiWidth() / 2 - pixelX(g.guiWidth());
            int cy = g.guiHeight() / 2 - pixelY(g.guiHeight()) + 12;
            if (!(ready && hideWhenReady.get())) Render2D.bar(g, cx - w / 2, cy, w, h, strength, ready ? Theme.success() : Theme.accent());
            size(w, h);
            return;
        }
        if (!(ready && hideWhenReady.get())) Render2D.bar(g, 0, 0, w, h, strength, ready ? Theme.success() : Theme.accent());
        size(w, h);
    }
}
