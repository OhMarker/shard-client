package gg.shard.client.modules.hud;

import gg.shard.client.gui.Theme;
import gg.shard.client.hud.HudModule;
import gg.shard.client.hud.HudStyle;
import gg.shard.client.module.setting.BoolSetting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;

public final class PingModule extends HudModule {
    private final BoolSetting colorize = add(new BoolSetting("Colour by latency", "Green under 60 ms, yellow under 150, red above", true)
            .details("Overrides the value colour from the style while on."));
    private final BoolSetting spike = add(new BoolSetting("Spike warning", "Mark the number when ping jumps well above your last 30 seconds", true));
    private final BoolSetting graphOn = add(new BoolSetting("Graph", "Ping over the last 30 seconds under the number", false));
    private final float[] history = new float[30];
    private int historyCount;
    private long lastSample;

    public PingModule() {
        super("Ping", "Your latency to the server, with spike warnings.", 0.01, 0.06);
    }

    @Override
    protected String defaultLabel() {
        return "Ping";
    }

    @Override
    public String about() {
        return "Your round-trip latency as the server reports it in the tab list. Nothing is sent to measure it; the number is the one every player can see.";
    }

    @Override
    public boolean defaultEnabled() {
        return true;
    }

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        int ping = currentPing();
        long now = System.currentTimeMillis();
        if (ping >= 0 && now - lastSample >= 1000) {
            lastSample = now;
            System.arraycopy(history, 0, history, 1, history.length - 1);
            history[0] = ping;
            historyCount = Math.min(history.length, historyCount + 1);
        }
        String text = ping < 0 ? "--" : ping + " ms";
        int color = 0;
        if (colorize.get() && ping >= 0) color = ping < 60 ? Theme.success() : ping < 150 ? Theme.warning() : Theme.danger();
        boolean spiking = spike.get() && isSpike(ping);
        if (spiking) text += "  spike";
        if (!graphOn.get()) {
            line(g, text, spiking ? Theme.danger() : color);
            return;
        }
        HudStyle.Resolved st = style();
        String label = labelText(st);
        int pad = st.padding();
        int lw = textW(label);
        int gw = 60;
        int gh = 14;
        int w = Math.max(pad * 2 + lw + textW(text), pad * 2 + gw);
        int h = pad * 2 + lineH() + 3 + gh;
        box(g, st, w, h);
        text(g, st, label, pad, pad, st.text());
        text(g, st, text, pad + lw, pad, spiking ? Theme.danger() : color == 0 ? st.value() : color);
        float max = 60;
        for (int i = 0; i < historyCount; i++) max = Math.max(max, history[i]);
        // 30 samples stretched to the graph width.
        float[] bars = new float[gw];
        for (int i = 0; i < gw; i++) bars[i] = historyCount == 0 ? 0 : history[Math.min(historyCount - 1, i * history.length / gw)];
        int shown = historyCount == 0 ? 0 : Math.min(gw, historyCount * gw / history.length);
        graph(g, pad, pad + lineH() + 3, gw, gh, bars, shown, max, 150, gg.shard.client.util.Colors.withAlpha(st.value(), 0xB0),
                Theme.warning(), gg.shard.client.util.Colors.withAlpha(0xFF000000, 0x40));
        size(w, h);
    }

    /** Well above the recent average: 1.5x and at least 40 ms more. */
    private boolean isSpike(int ping) {
        if (ping < 0 || historyCount < 5) return false;
        float sum = 0;
        for (int i = 1; i < historyCount; i++) sum += history[i];
        float avg = sum / (historyCount - 1);
        return ping > avg * 1.5f && ping > avg + 40;
    }

    static int currentPing() {
        ClientPacketListener connection = HudModule.mc().getConnection();
        if (connection == null || HudModule.mc().player == null) return -1;
        PlayerInfo info = connection.getPlayerInfo(HudModule.mc().player.getUUID());
        return info == null ? -1 : info.getLatency();
    }

    @Override
    public String icon() {
        return "ping";
    }
}
