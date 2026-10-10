package gg.shard.client.gui.menu;

import com.mojang.blaze3d.platform.NativeImage;
import gg.shard.client.ShardClient;
import gg.shard.client.gui.Fonts;
import gg.shard.client.gui.Icons;
import gg.shard.client.gui.Render2D;
import gg.shard.client.modules.settings.MenuScreensModule;
import gg.shard.client.util.Colors;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.FaviconTexture;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerStatusPinger;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
//? if >=1.21.11
import net.minecraft.server.network.EventLoopGroupHolder;
import net.minecraft.util.Util;

import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * What the server list and the title screen's Jump back in card share: pinging a saved server (as
 * vanilla's OnlineServerEntry), its icon texture, the ping pill and player count, the name shown
 * for it, joining (which remembers the server), and favourites.
 */
final class ServerCards {
    private ServerCards() {}

    private static final ExecutorService PING_POOL = Executors.newFixedThreadPool(5, r -> {
        Thread t = new Thread(r, "Shard server ping");
        t.setDaemon(true);
        return t;
    });

    /** A saved server and its icon texture. */
    static final class Entry {
        final ServerData data;
        final FaviconTexture icon;
        byte[] lastIconBytes;

        Entry(ServerData data, FaviconTexture icon) {
            this.data = data;
            this.icon = icon;
        }
    }

    static Entry entry(ServerData data) {
        return new Entry(data, FaviconTexture.forServer(Minecraft.getInstance().getTextureManager(), data.ip));
    }

    static void pingIfNeeded(ServerStatusPinger pinger, ServerData data, Runnable saved) {
        if (data.state() != ServerData.State.INITIAL) return;
        data.setState(ServerData.State.PINGING);
        data.motd = CommonComponents.EMPTY;
        data.status = CommonComponents.EMPTY;
        Minecraft minecraft = Minecraft.getInstance();
        PING_POOL.submit(() -> {
            try {
                Runnable onSaved = () -> minecraft.execute(saved);
                Runnable pinged = () -> data.setState(data.protocol == SharedConstants.getCurrentVersion().protocolVersion()
                        ? ServerData.State.SUCCESSFUL : ServerData.State.INCOMPATIBLE);
                //? if >=1.21.11 {
                pinger.pingServer(data, onSaved, pinged, EventLoopGroupHolder.remote(minecraft.options.useNativeTransport()));
                //?} else {
                /*pinger.pingServer(data, onSaved, pinged);
                *///?}
            } catch (UnknownHostException e) {
                data.setState(ServerData.State.UNREACHABLE);
                data.motd = Component.translatable("multiplayer.status.cannot_resolve").withColor(0xFFFF5555);
            } catch (Exception e) {
                data.setState(ServerData.State.UNREACHABLE);
                data.motd = Component.translatable("multiplayer.status.cannot_connect").withColor(0xFFFF5555);
            }
        });
    }

    /** Uploads the server's icon when it changed; returns true when a broken icon was dropped (save the list). */
    static boolean updateIcon(Entry e) {
        byte[] bytes = e.data.getIconBytes();
        if (Arrays.equals(bytes, e.lastIconBytes)) return false;
        e.lastIconBytes = bytes;
        if (bytes == null) {
            e.icon.clear();
            return false;
        }
        try {
            e.icon.upload(NativeImage.read(bytes));
            return false;
        } catch (Throwable t) {
            ShardClient.LOGGER.error("Invalid icon for server {} ({})", e.data.name, e.data.ip, t);
            e.data.setIconBytes(null);
            e.lastIconBytes = null;
            e.icon.clear();
            return true;
        }
    }

    static void drawIcon(GuiGraphics g, Entry e, int x, int y, int size) {
        Render2D.roundedRect(g, x - 1, y - 1, size + 2, size + 2, 9, 0xFF141923);
        g.blit(RenderPipelines.GUI_TEXTURED, e.icon.textureLocation(), x, y, 0f, 0f, size, size, 64, 64, 64, 64);
    }

    /** The saved name, or the address when the name is still vanilla's default "Minecraft Server". */
    static String displayName(ServerData data) {
        String name = data.name == null ? "" : data.name.trim();
        if (name.isEmpty() || name.equals(I18n.get("selectServer.defaultName"))) return data.ip;
        return name;
    }

    static boolean showsAddressSeparately(ServerData data) {
        return !displayName(data).equals(data.ip);
    }

    // ---- status ---------------------------------------------------------------------------------

    static int pingColor(ServerData data) {
        return switch (data.state()) {
            case PINGING, INITIAL -> MenuScreen.MUTED;
            case UNREACHABLE, INCOMPATIBLE -> MenuScreen.DANGER;
            default -> data.ping < 150 ? MenuScreen.SUCCESS : data.ping < 300 ? 0xFFA3E635 : data.ping < 600 ? MenuScreen.WARNING : MenuScreen.DANGER;
        };
    }

    /** Ping bars and milliseconds in a tinted pill, right-aligned at {@code right}; returns its width. */
    static int pingPill(GuiGraphics g, ServerData data, int right, int y) {
        String text;
        int level;
        switch (data.state()) {
            case PINGING, INITIAL -> {
                text = "";
                long t = Util.getMillis() / 120 % 8;
                level = (int) (t < 5 ? t + 1 : 8 - t);
            }
            case UNREACHABLE -> {
                text = "Offline";
                level = -1;
            }
            case INCOMPATIBLE -> {
                text = "Incompatible";
                level = -1;
            }
            default -> {
                text = data.ping + " ms";
                level = data.ping < 150 ? 5 : data.ping < 300 ? 4 : data.ping < 600 ? 3 : data.ping < 1000 ? 2 : 1;
            }
        }
        int color = pingColor(data);
        int barsW = 5 * 3 + 4 * 2;
        int tw = text.isEmpty() ? 0 : Fonts.widthInt(text, Fonts.Weight.SEMIBOLD, 12) + 7;
        int w = 9 + barsW + tw + 9;
        int h = 22;
        int x = right - w;
        Render2D.roundedRect(g, x, y, w, h, h / 2, Colors.withAlpha(color, 0x1C));
        int bx = x + 9;
        for (int i = 0; i < 5; i++) {
            int bh = 3 + i * 2;
            boolean lit = level > 0 && i < level;
            int c = level < 0 ? Colors.withAlpha(color, i == 0 ? 0xFF : 0x40)
                    : lit ? (data.state() == ServerData.State.SUCCESSFUL ? color : MenuScreen.SOFT) : Colors.withAlpha(color, 0x30);
            Render2D.fill(g, bx + i * 5, y + 16 - bh, 3, bh, c);
        }
        if (!text.isEmpty()) Fonts.draw(g, text, Fonts.Weight.SEMIBOLD, 12, bx + barsW + 7, y + (h - Fonts.lineHeight(12)) / 2, color);
        return w;
    }

    /** "1,657 / 50,000" with a person icon, right-aligned; returns its width (0 when unknown). */
    static int players(GuiGraphics g, ServerData data, int right, int y) {
        if (data.players == null || data.state() != ServerData.State.SUCCESSFUL) return 0;
        String online = String.format(Locale.ROOT, "%,d", data.players.online());
        String max = String.format(Locale.ROOT, " / %,d", data.players.max());
        int ow = Fonts.widthInt(online, Fonts.Weight.MEDIUM, 12);
        int mw = Fonts.widthInt(max, Fonts.Weight.REGULAR, 12);
        int x = right - ow - mw;
        Fonts.draw(g, online, Fonts.Weight.MEDIUM, 12, x, y, MenuScreen.SOFT);
        Fonts.draw(g, max, Fonts.Weight.REGULAR, 12, x + ow, y, MenuScreen.MUTED);
        Icons.draw(g, "user", x - 6 - 13, y + (Fonts.lineHeight(12) - 13) / 2, 13, MenuScreen.MUTED);
        return ow + mw + 19;
    }

    /** "64 / 500 playing" once the server has answered, else null. */
    static String playingLine(ServerData data) {
        if (data.players == null || data.state() != ServerData.State.SUCCESSFUL) return null;
        return String.format(Locale.ROOT, "%,d / %,d playing", data.players.online(), data.players.max());
    }

    /** How full the server is, as a thin bar {@code w} wide right-aligned at {@code right}; false when unknown. */
    static boolean capacityBar(GuiGraphics g, ServerData data, int right, int y, int w) {
        if (data.players == null || data.state() != ServerData.State.SUCCESSFUL || data.players.max() <= 0) return false;
        float full = Math.min(1f, Math.max(0f, data.players.online() / (float) data.players.max()));
        int x = right - w;
        Render2D.roundedRect(g, x, y, w, 3, 1, 0x1AFFFFFF);
        int fw = full > 0f ? Math.max(3, Math.round(w * full)) : 0;
        if (fw > 0) Render2D.roundedRect(g, x, y, fw, 3, 1, full >= 0.9f ? MenuScreen.WARNING : MenuScreen.SOFT);
        return true;
    }

    // ---- joining and memory ---------------------------------------------------------------------

    static void join(Screen from, ServerData data) {
        MenuScreensModule settings = settings();
        if (!data.ip.equals(settings.lastServer.get())) {
            settings.lastServer.set(data.ip);
            ShardClient.config().save();
        }
        ConnectScreen.startConnecting(from, Minecraft.getInstance(), ServerAddress.parseString(data.ip), data, false, null);
    }

    static Set<String> favourites() {
        Set<String> out = new LinkedHashSet<>();
        for (String s : settings().favouriteServers.get().split("\n")) {
            String t = s.trim().toLowerCase(Locale.ROOT);
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    static boolean isFavourite(Set<String> favourites, ServerData data) {
        return favourites.contains(data.ip.trim().toLowerCase(Locale.ROOT));
    }

    static void setFavourites(Set<String> favourites) {
        settings().favouriteServers.set(String.join("\n", favourites));
        ShardClient.config().save();
    }

    static MenuScreensModule settings() {
        return ShardClient.modules().get(MenuScreensModule.class);
    }
}
