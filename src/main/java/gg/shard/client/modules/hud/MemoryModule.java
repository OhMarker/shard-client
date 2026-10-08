package gg.shard.client.modules.hud;

import gg.shard.client.gui.Render2D;
import gg.shard.client.gui.Theme;
import gg.shard.client.hud.HudModule;
import gg.shard.client.hud.HudStyle;
import gg.shard.client.module.setting.BoolSetting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;

public final class MemoryModule extends HudModule {
    private final BoolSetting bar = add(new BoolSetting("Bar", "Show a usage bar under the text", true));

    private long lastSampleMs;
    private long used;
    private long max;

    public MemoryModule() {
        super("Memory", "JVM heap in use versus the maximum.", 0.01, 0.95);
    }

    @Override
    protected String defaultLabel() {
        return "Mem";
    }

    @Override
    public String about() {
        return "How much of the Java heap the game is using, sampled twice a second. Turns yellow above 85%, which is when the launcher's memory setting is worth raising.";
    }

    @Override
    public boolean needsPlayer() {
        return false;
    }

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        long now = System.currentTimeMillis();
        if (now - lastSampleMs > 500) {
            Runtime rt = Runtime.getRuntime();
            used = rt.totalMemory() - rt.freeMemory();
            max = rt.maxMemory();
            lastSampleMs = now;
        }
        int percent = max == 0 ? 0 : (int) (used * 100 / max);
        String value = percent + "%  " + (used >> 20) + "/" + (max >> 20) + " MB";
        HudStyle.Resolved st = style();
        String label = labelText(st);
        int pad = st.padding();
        int lw = textW(label);
        int w = pad * 2 + lw + textW(value);
        int h = pad * 2 + lineH() + (bar.get() ? 6 : 0);
        box(g, st, w, h);
        text(g, st, label, pad, pad, st.text());
        text(g, st, value, pad + lw, pad, percent > 85 ? Theme.warning() : st.value());
        if (bar.get()) Render2D.bar(g, pad, pad + lineH() + 2, w - pad * 2, 3, percent / 100.0, percent > 85 ? Theme.warning() : Theme.accent());
        size(w, h);
    }

    @Override
    public String icon() {
        return "memory";
    }
}
