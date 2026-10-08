package gg.shard.client.modules.hud;

import gg.shard.client.gui.Fonts;
import gg.shard.client.hud.HudModule;
import gg.shard.client.hud.HudStyle;
import gg.shard.client.module.setting.IntSetting;
import gg.shard.client.util.Colors;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;

/** Compass: a heading strip like a shooter's, centred on where you look. */
public final class CompassModule extends HudModule {
    private static final String[] NAMES = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
    private final IntSetting width = add(new IntSetting("Width", "Width of the strip", 140, 80, 260, 10, ""));

    public CompassModule() {
        super("Compass", "A heading strip at the top of the screen.", 0.43, 0.01);
    }

    @Override
    public String icon() {
        return "compass";
    }

    @Override
    public String about() {
        return "A compass strip with the eight directions and tick marks, centred on the way you face. Uses your own rotation only.";
    }

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        LocalPlayer p = mc().player;
        if (p == null) return;
        HudStyle.Resolved st = style();
        int w = width.get();
        int h = 18;
        box(g, st, w, h);
        float yaw = (p.getYRot(delta.getGameTimeDeltaPartialTick(true)) % 360 + 360) % 360; // 0 = south
        float pxPerDeg = w / 120f; // 120 degrees visible
        g.enableScissor(0, 0, w, h);
        for (int deg = 0; deg < 360; deg += 15) {
            float d = ((deg - yaw + 540) % 360) - 180;
            if (Math.abs(d) > 62) continue;
            int x = Math.round(w / 2f + d * pxPerDeg);
            if (deg % 45 == 0) {
                String n = NAMES[deg / 45];
                int c = n.length() == 1 ? st.value() : Colors.withAlpha(st.text(), 0xC0);
                Fonts.drawCentered(g, n, WEIGHT, TEXT_SIZE, x, 2 + lineOffset(), c);
            } else {
                g.fill(x, 11, x + 1, 15, Colors.withAlpha(st.text(), 0x70));
            }
        }
        g.disableScissor();
        g.fill(w / 2, h - 3, w / 2 + 1, h, gg.shard.client.gui.Theme.accent());
        size(w, h);
    }
}
