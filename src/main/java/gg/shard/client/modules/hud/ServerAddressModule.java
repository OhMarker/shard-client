package gg.shard.client.modules.hud;

import gg.shard.client.gui.Render2D;
import gg.shard.client.gui.Theme;
import gg.shard.client.hud.HudModule;
import gg.shard.client.module.setting.BoolSetting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ServerData;

public final class ServerAddressModule extends HudModule {
    private final BoolSetting background = add(new BoolSetting("Background", "Dark backing behind the text", true));

    public ServerAddressModule() {
        super("Server Address", "The server you are connected to.", 0.01, 0.18);
    }

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        ServerData server = mc().getCurrentServer();
        String text = server == null ? (mc().hasSingleplayerServer() ? "Singleplayer" : "Not connected") : server.ip;
        int w = font().width(text) + 6;
        if (background.get()) Render2D.rounded(g, 0, 0, w, 12, 0x66000000);
        Render2D.text(g, font(), text, 3, 2, Theme.muted(), true);
        size(w, 12);
    }
}
