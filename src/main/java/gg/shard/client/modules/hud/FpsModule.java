package gg.shard.client.modules.hud;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import gg.shard.client.gui.Theme;
import gg.shard.client.hud.HudManager;
import gg.shard.client.hud.HudModule;
import gg.shard.client.hud.HudStyle;
import gg.shard.client.module.setting.BoolSetting;
import gg.shard.client.module.setting.IntSetting;
import gg.shard.client.util.Colors;
import gg.shard.client.util.FrameStats;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;

public final class FpsModule extends HudModule {
    private final BoolSetting low = add(new BoolSetting("1% low", "Also show the frame rate of your slowest frames, which is what a stutter feels like", true)
            .details("Measured over the last 1000 frames (a few seconds). A big gap between the two numbers means hitches, even when the average looks fine."));
    private final BoolSetting graphOn = add(new BoolSetting("Frame-time graph", "A small graph of recent frame times under the number", false));
    private final IntSetting graphW = add(new IntSetting("Graph width", "Width of the graph", 64, 32, 160, 8, ""));
    private final float[] samples = new float[160];
    private int shownFps;
    private int shownLow;
    private long lastUpdate;

    public FpsModule() {
        super("FPS", "Frames per second, with your 1% low.", 0.01, 0.02);
        graphW.visibleWhen(graphOn::get);
    }

    @Override
    protected String defaultLabel() {
        return "FPS";
    }

    @Override
    public String about() {
        return "The client's frame rate and, optionally, its 1% low: the frame rate of the slowest 1% of recent frames. "
                + "Averages hide stutters; the 1% low shows them. Informational only; it changes nothing about rendering.";
    }

    @Override
    public boolean defaultEnabled() {
        return true;
    }

    @Override
    public boolean needsPlayer() {
        return false;
    }

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        // Numbers update four times a second so they can be read.
        long now = System.currentTimeMillis();
        if (now - lastUpdate > 250) {
            lastUpdate = now;
            shownFps = mc().getFps();
            shownLow = HudManager.FRAMES.lowFps(1);
        }
        HudStyle.Resolved st = style();
        String label = labelText(st);
        String value = String.valueOf(shownFps);
        String lowText = low.get() && shownLow > 0 ? "  1% " + shownLow : "";
        int pad = st.padding();
        int lw = textW(label);
        int vw = textW(value);
        int w = pad * 2 + lw + vw + textW(lowText);
        int gw = graphOn.get() ? graphW.get() : 0;
        int gh = 14;
        w = Math.max(w, gw + pad * 2);
        int h = pad * 2 + lineH() + (graphOn.get() ? gh + 3 : 0);
        box(g, st, w, h);
        text(g, st, label, pad, pad, st.text());
        text(g, st, value, pad + lw, pad, st.value());
        if (!lowText.isEmpty()) {
            boolean stutter = shownLow < shownFps * 0.5;
            text(g, st, lowText, pad + lw + vw, pad, stutter ? Theme.warning() : Colors.withAlpha(st.value(), 0xA0));
        }
        if (graphOn.get()) {
            FrameStats f = HudManager.FRAMES;
            int n = Math.min(gw, f.count());
            float worst = 0;
            for (int i = 0; i < n; i++) {
                samples[i] = f.recent(i);
                worst = Math.max(worst, samples[i]);
            }
            float avgMs = shownFps > 0 ? 1000f / shownFps : 16f;
            // A normal frame sits at a third of the height; hitches (twice the average) turn yellow.
            graph(g, pad, pad + lineH() + 3, gw, gh, samples, n, Math.max(worst, avgMs * 3), avgMs * 2,
                    Colors.withAlpha(st.value(), 0xB0), Theme.warning(), Colors.withAlpha(0xFF000000, 0x40));
        }
        size(w, h);
    }

    @Override
    public String icon() {
        return "fps";
    }

    // 0.2.0: "Color" was the text colour and "Label" a switch for the FPS prefix.

    @Override
    protected boolean migratesKey(String key, int version) {
        return version < 3 && key.equals("label");
    }

    @Override
    protected void migrateSetting(String key, JsonElement value, JsonObject all, int version) {
        switch (key) {
            case "color" -> {
                style.custom.set(true);
                style.text.fromJson(value);
                style.value.fromJson(value);
            }
            case "label" -> {
                if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean()) style.label.set(value.getAsBoolean() ? "FPS" : "");
                else style.label.fromJson(value);
            }
            default -> super.migrateSetting(key, value, all, version);
        }
    }
}
