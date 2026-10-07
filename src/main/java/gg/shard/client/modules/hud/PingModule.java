package gg.shard.client.modules.hud;

import gg.shard.client.gui.Render2D;
import gg.shard.client.gui.Theme;
import gg.shard.client.hud.HudModule;
import gg.shard.client.module.setting.BoolSetting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;

public final class PingModule extends HudModule {
    private final BoolSetting background = add(new BoolSetting("Show background", "Dark backing behind the text", true));
    private final BoolSetting colorize = add(new BoolSetting("Colour by latency", "Green under 60 ms, yellow under 150, red above", true));

    public PingModule() {
        super("Ping", "Your latency to the server from the tab list.", 0.01, 0.06);
    }

    @Override
    public boolean defaultEnabled() {
        return true;
    }

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        int ping = currentPing();
        String text = ping < 0 ? "Ping --" : "Ping " + ping + " ms";
        int color = Theme.text();
        if (colorize.get() && ping >= 0) color = ping < 60 ? Theme.success() : ping < 150 ? Theme.warning() : Theme.danger();
        int w = font().width(text) + 6;
        if (background.get()) Render2D.rounded(g, 0, 0, w, 12, 0x66000000);
        Render2D.text(g, font(), text, 3, 2, color, true);
        size(w, 12);
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
