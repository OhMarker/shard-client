package gg.shard.client.modules.hud;

import gg.shard.client.gui.Render2D;
import gg.shard.client.gui.Theme;
import gg.shard.client.hud.HudModule;
import gg.shard.client.module.setting.BoolSetting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;

public final class MemoryModule extends HudModule {
    private final BoolSetting background = add(new BoolSetting("Show background", "Dark backing", true));
    private final BoolSetting bar = add(new BoolSetting("Bar", "Show a usage bar under the text", true));

    private long lastSampleMs;
    private long used;
    private long max;

    public MemoryModule() {
        super("Memory", "JVM heap in use versus the maximum.", 0.01, 0.95);
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
        String text = "Mem " + percent + "%  " + (used >> 20) + "/" + (max >> 20) + " MB";
        int w = font().width(text) + 6;
        int h = bar.get() ? 16 : 12;
        if (background.get()) Render2D.rounded(g, 0, 0, w, h, 0x66000000);
        Render2D.text(g, font(), text, 3, 2, percent > 85 ? Theme.warning() : Theme.text(), true);
        if (bar.get()) Render2D.bar(g, 3, 12, w - 6, 2, percent / 100.0, percent > 85 ? Theme.warning() : Theme.accent());
        size(w, h);
    }

    @Override
    public String icon() {
        return "glyph:memory";
    }
}
