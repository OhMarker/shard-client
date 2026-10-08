package gg.shard.client.modules.hud;

import gg.shard.client.gui.Theme;
import gg.shard.client.hud.HudModule;
import gg.shard.client.module.setting.BoolSetting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;

public final class PingModule extends HudModule {
    private final BoolSetting colorize = add(new BoolSetting("Colour by latency", "Green under 60 ms, yellow under 150, red above", true)
            .details("Overrides the value colour from the style while on."));

    public PingModule() {
        super("Ping", "Your latency to the server from the tab list.", 0.01, 0.06);
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
        String text = ping < 0 ? "--" : ping + " ms";
        int color = 0;
        if (colorize.get() && ping >= 0) color = ping < 60 ? Theme.success() : ping < 150 ? Theme.warning() : Theme.danger();
        line(g, text, color);
    }

    static int currentPing() {
        ClientPacketListener connection = HudModule.mc().getConnection();
        if (connection == null || HudModule.mc().player == null) return -1;
        PlayerInfo info = connection.getPlayerInfo(HudModule.mc().player.getUUID());
        return info == null ? -1 : info.getLatency();
    }

    @Override
    public String icon() {
        return "glyph:ping";
    }
}
