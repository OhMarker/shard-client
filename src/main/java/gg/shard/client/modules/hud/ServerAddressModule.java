package gg.shard.client.modules.hud;

import gg.shard.client.hud.HudModule;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ServerData;

public final class ServerAddressModule extends HudModule {
    public ServerAddressModule() {
        super("Server", "The server you are connected to.", 0.01, 0.18);
    }

    @Override
    protected String legacyKey() {
        return "server-address";
    }

    @Override
    protected String defaultLabel() {
        return "";
    }

    @Override
    public String about() {
        return "The address you joined, exactly as it appears in the server list. Handy for screenshots and clips.";
    }

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        ServerData server = mc().getCurrentServer();
        String text = server == null ? (mc().hasSingleplayerServer() ? "Singleplayer" : "Not connected") : server.ip;
        line(g, text, 0);
    }

    @Override
    public String icon() {
        return "server";
    }
}
