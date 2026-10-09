package gg.shard.client.gui.menu;

import com.mojang.blaze3d.platform.NativeImage;
import gg.shard.client.ShardClient;
import gg.shard.client.gui.Fonts;
import gg.shard.client.gui.Icons;
import gg.shard.client.gui.Render2D;
import gg.shard.client.util.Colors;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DirectJoinServerScreen;
import net.minecraft.client.gui.screens.FaviconTexture;
//? if >=1.21.9 {
import net.minecraft.client.gui.screens.ManageServerScreen;
//?}
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.ServerStatusPinger;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.server.LanServer;
import net.minecraft.client.server.LanServerDetection;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
//? if >=1.21.11
import net.minecraft.server.network.EventLoopGroupHolder;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Shard's server list. Same data and behaviour as vanilla's JoinMultiplayerScreen (its ServerList,
 * ServerStatusPinger, LAN detection, ConnectScreen and the vanilla add / edit / direct-connect /
 * delete dialogs), drawn in the mod menu's style: a centred panel with Servers and LAN tabs, one
 * card per server (icon, name, MOTD, players, ping), and Join / Direct connect / Add / Edit / Delete
 * / Refresh. Double-click or Enter joins; Shift+Up/Down reorders, like vanilla.
 */
public final class ShardMultiplayerScreen extends MenuScreen {
    private static final int EDGE = 24;
    private static final int PANEL_MAX_W = 900;
    private static final int TAB_H = 48;
    private static final int FOOT_H = 64;
    private static final int ROW_H = 72;
    private static final int ROW_GAP = 6;
    private static final int LIST_PAD = 12;
    private static final int ICON = 48;
    private static final ExecutorService PING_POOL = Executors.newFixedThreadPool(5, r -> {
        Thread t = new Thread(r, "Shard server ping");
        t.setDaemon(true);
        return t;
    });

    private enum Tab { SERVERS, LAN }

    /** A saved server and its icon texture. */
    private static final class Entry {
        final ServerData data;
        final FaviconTexture icon;
        byte[] lastIconBytes;

        Entry(ServerData data, FaviconTexture icon) {
            this.data = data;
            this.icon = icon;
        }
    }

    private final Screen parent;
    private ServerStatusPinger pinger;
    private ServerList servers;
    private LanServerDetection.LanServerList lanList;
    private LanServerDetection.LanServerDetector lanDetector;
    private final List<LanServer> lanServers = new ArrayList<>();
    private final Map<ServerData, Entry> entries = new IdentityHashMap<>();
    private Tab tab = Tab.SERVERS;
    private int selected = -1;
    private int selectedLan = -1;
    private ServerData editing;
    private int scroll;
    private float scrollShown;
    private int listX;
    private int listY;
    private int listW;
    private int listH;

    public ShardMultiplayerScreen(Screen parent) {
        super(Component.translatable("multiplayer.title"), PANEL_MAX_W + 2 * EDGE, 600);
        this.parent = parent;
    }

    @Override
    protected boolean blurBackground() {
        return true;
    }

    @Override
    protected int dimColor() {
        return 0x40000000;
    }

    // ---- lifecycle (mirrors JoinMultiplayerScreen) ----------------------------------------------

    @Override
    protected void init() {
        if (servers != null) return; // already set up (resize)
        pinger = new ServerStatusPinger();
        servers = new ServerList(minecraft);
        servers.load();
        lanList = new LanServerDetection.LanServerList();
        try {
            lanDetector = new LanServerDetection.LanServerDetector(lanList);
            lanDetector.start();
        } catch (Exception e) {
            ShardClient.LOGGER.warn("Unable to start LAN server detection: {}", e.getMessage());
        }
        if (selected >= servers.size()) selected = servers.size() - 1;
    }

    @Override
    protected void repositionElements() {
        // Laid out every frame.
    }

    @Override
    public void tick() {
        super.tick();
        if (lanList != null) {
            List<LanServer> dirty = lanList.takeDirtyServers();
            if (dirty != null) {
                lanServers.clear();
                lanServers.addAll(dirty);
                if (selectedLan >= lanServers.size()) selectedLan = -1;
            }
        }
        if (pinger != null) pinger.tick();
    }

    @Override
    public void removed() {
        if (lanDetector != null) {
            lanDetector.interrupt();
            lanDetector = null;
        }
        if (pinger != null) pinger.removeAll();
        for (Entry e : entries.values()) e.icon.close();
        entries.clear();
        servers = null; // reloaded (and pinged again) when this screen is shown again, as vanilla does
        super.removed();
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    /** Vanilla's Refresh: a fresh screen, so every server is pinged again. */
    private void refresh() {
        minecraft.setScreen(new ShardMultiplayerScreen(parent));
    }

    // ---- actions -----------------------------------------------------------------------------

    public void join(ServerData data) {
        ConnectScreen.startConnecting(this, minecraft, ServerAddress.parseString(data.ip), data, false, null);
    }

    private void joinSelected() {
        if (tab == Tab.SERVERS) {
            ServerData data = selectedServer();
            if (data != null) join(data);
        } else if (selectedLan >= 0 && selectedLan < lanServers.size()) {
            LanServer lan = lanServers.get(selectedLan);
            join(new ServerData(lan.getMotd(), lan.getAddress(), ServerData.Type.LAN));
        }
    }

    private ServerData selectedServer() {
        return servers != null && selected >= 0 && selected < servers.size() ? servers.get(selected) : null;
    }

    private void addServer() {
        editing = new ServerData(I18n.get("selectServer.defaultName"), "", ServerData.Type.OTHER);
        minecraft.setScreen(serverEditor("manageServer.add.title", ok -> {
            if (ok) {
                ServerData unhidden = servers.unhide(editing.ip);
                if (unhidden != null) {
                    unhidden.copyNameIconFrom(editing);
                } else {
                    servers.add(editing, false);
                }
                servers.save();
                selected = servers.size() - 1;
                tab = Tab.SERVERS;
            }
            minecraft.setScreen(this);
        }));
    }

    /** Vanilla's add/edit server screen for {@link #editing}. */
    private Screen serverEditor(String titleKey, it.unimi.dsi.fastutil.booleans.BooleanConsumer done) {
        //? if >=1.21.9 {
        return new ManageServerScreen(this, Component.translatable(titleKey), done, editing);
        //?} else {
        /*return new net.minecraft.client.gui.screens.EditServerScreen(this, done, editing);
        *///?}
    }

    private void editServer() {
        ServerData data = selectedServer();
        if (data == null) return;
        editing = new ServerData(data.name, data.ip, ServerData.Type.OTHER);
        editing.copyFrom(data);
        minecraft.setScreen(serverEditor("manageServer.edit.title", ok -> {
            if (ok) {
                data.name = editing.name;
                data.ip = editing.ip;
                data.copyFrom(editing);
                servers.save();
            }
            minecraft.setScreen(this);
        }));
    }

    private void deleteServer() {
        ServerData data = selectedServer();
        if (data == null) return;
        minecraft.setScreen(new ConfirmScreen(ok -> {
            if (ok) {
                servers.remove(data);
                servers.save();
                selected = -1;
            }
            minecraft.setScreen(this);
        }, Component.translatable("selectServer.deleteQuestion"), Component.translatable("selectServer.deleteWarning", data.name),
                Component.translatable("selectServer.deleteButton"), CommonComponents.GUI_CANCEL));
    }

    private void directConnect() {
        editing = new ServerData(I18n.get("selectServer.defaultName"), "", ServerData.Type.OTHER);
        minecraft.setScreen(new DirectJoinServerScreen(this, ok -> {
            if (ok) {
                ServerData existing = servers.get(editing.ip);
                if (existing == null) {
                    servers.add(editing, true);
                    servers.save();
                    join(editing);
                } else {
                    join(existing);
                }
            } else {
                minecraft.setScreen(this);
            }
        }, editing));
    }

    private void move(int delta) {
        int target = selected + delta;
        if (selected < 0 || target < 0 || target >= servers.size()) return;
        servers.swap(selected, target);
        servers.save();
        selected = target;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (accounts.open() && event.key() == GLFW.GLFW_KEY_ESCAPE) {
            accounts.close();
            return true;
        }
        boolean shift = (event.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0;
        int count = tab == Tab.SERVERS ? (servers == null ? 0 : servers.size()) : lanServers.size();
        switch (event.key()) {
            case GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_DOWN -> {
                int d = event.key() == GLFW.GLFW_KEY_UP ? -1 : 1;
                if (tab == Tab.SERVERS && shift) move(d);
                else if (count > 0) {
                    int cur = tab == Tab.SERVERS ? selected : selectedLan;
                    int next = Math.max(0, Math.min(count - 1, cur < 0 ? 0 : cur + d));
                    if (tab == Tab.SERVERS) selected = next;
                    else selectedLan = next;
                    revealSelection(next);
                }
                return true;
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                joinSelected();
                return true;
            }
            case GLFW.GLFW_KEY_DELETE -> {
                if (tab == Tab.SERVERS && selectedServer() != null) deleteServer();
                return true;
            }
            case GLFW.GLFW_KEY_F5 -> {
                refresh();
                return true;
            }
            default -> {
                return super.keyPressed(event);
            }
        }
    }

    private void revealSelection(int index) {
        int top = index * (ROW_H + ROW_GAP);
        if (top < scroll) scroll = top;
        else if (top + ROW_H > scroll + listH - 2 * LIST_PAD) scroll = top + ROW_H - (listH - 2 * LIST_PAD);
    }

    @Override
    protected boolean designScrolled(double x, double y, double sx, double sy) {
        if (accounts.open()) return true;
        if (Render2D.hovered(x, y, listX, listY, listW, listH)) {
            scroll -= (int) Math.round(sy * 40);
            return true;
        }
        return false;
    }

    // ---- pinging and icons (as vanilla's OnlineServerEntry) ------------------------------------

    private Entry entry(ServerData data) {
        return entries.computeIfAbsent(data, d -> new Entry(d, FaviconTexture.forServer(minecraft.getTextureManager(), d.ip)));
    }

    private void pingIfNeeded(ServerData data) {
        if (data.state() != ServerData.State.INITIAL) return;
        data.setState(ServerData.State.PINGING);
        data.motd = CommonComponents.EMPTY;
        data.status = CommonComponents.EMPTY;
        ServerStatusPinger p = pinger;
        PING_POOL.submit(() -> {
            try {
                Runnable saved = () -> minecraft.execute(() -> {
                    if (servers != null) servers.save();
                });
                Runnable pinged = () -> data.setState(data.protocol == SharedConstants.getCurrentVersion().protocolVersion()
                        ? ServerData.State.SUCCESSFUL : ServerData.State.INCOMPATIBLE);
                //? if >=1.21.11 {
                p.pingServer(data, saved, pinged, EventLoopGroupHolder.remote(minecraft.options.useNativeTransport()));
                //?} else {
                /*p.pingServer(data, saved, pinged);
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

    private void updateIcon(Entry e) {
        byte[] bytes = e.data.getIconBytes();
        if (Arrays.equals(bytes, e.lastIconBytes)) return;
        e.lastIconBytes = bytes;
        if (bytes == null) {
            e.icon.clear();
            return;
        }
        try {
            e.icon.upload(NativeImage.read(bytes));
        } catch (Throwable t) {
            ShardClient.LOGGER.error("Invalid icon for server {} ({})", e.data.name, e.data.ip, t);
            e.data.setIconBytes(null);
            e.lastIconBytes = null;
            e.icon.clear();
            if (servers != null) servers.save();
        }
    }

    // ---- drawing ------------------------------------------------------------------------------

    @Override
    protected void renderPage(GuiGraphics g, float partialTick) {
        if (servers == null) return;
        int panelW = Math.min(PANEL_MAX_W, designW - 2 * EDGE);
        int panelX = (designW - panelW) / 2;
        int headY = EDGE;
        int panelY = headY + AccountSwitcher.PILL_H + 16;
        int panelH = designH - EDGE - panelY;

        // Header: back, title, account.
        iconButton(g, "back", "chevron-left", panelX, headY, 36, 18, I18n.get("gui.back"), b -> onClose());
        Fonts.draw(g, I18n.get("multiplayer.title"), Fonts.Weight.SEMIBOLD, 20, panelX + 36 + 14, headY + (36 - Fonts.lineHeight(20)) / 2, TEXT);
        accounts.renderPill(g, panelX + panelW, headY);

        Render2D.roundedRect(g, panelX + 1, panelY + 4, panelW, panelH, 10, 0x30000000);
        Render2D.panel(g, panelX, panelY, panelW, panelH, 10, Colors.withAlpha(BG, 0xF2), BORDER);
        renderTabs(g, panelX, panelY, panelW);

        listX = panelX + 1;
        listY = panelY + TAB_H;
        listW = panelW - 2;
        listH = panelH - TAB_H - FOOT_H;
        if (tab == Tab.SERVERS) renderServers(g);
        else renderLan(g);
        renderFooter(g, panelX, panelY + panelH - FOOT_H, panelW);
    }

    private void renderTabs(GuiGraphics g, int x, int y, int w) {
        g.fill(x + 1, y + TAB_H - 1, x + w - 1, y + TAB_H, BORDER);
        int tx = x + 20;
        for (Tab t : Tab.values()) {
            String label = t == Tab.SERVERS ? "Servers" : "LAN";
            int count = t == Tab.SERVERS ? servers.size() : lanServers.size();
            int lw = Fonts.widthInt(label, Fonts.Weight.MEDIUM, 13);
            String badge = Integer.toString(count);
            int bw = Fonts.widthInt(badge, Fonts.Weight.MEDIUM, 10) + 10;
            int full = lw + 6 + bw;
            String key = "tab:" + t.name();
            boolean on = tab == t;
            boolean hover = hovered(key, tx - 8, y, full + 16, TAB_H);
            hit(key, tx - 8, y, full + 16, TAB_H, b -> {
                tab = t;
                scroll = 0;
                scrollShown = 0;
            });
            float hov = hoverAnim(key, hover);
            int color = on ? TEXT : Colors.mix(MUTED, SOFT, hov);
            Fonts.draw(g, label, Fonts.Weight.MEDIUM, 13, tx, y + (TAB_H - Fonts.lineHeight(13)) / 2, color);
            int by = y + (TAB_H - 16) / 2;
            Render2D.roundedRect(g, tx + lw + 6, by, bw, 16, 8, on ? ACTIVE : Colors.withAlpha(ACTIVE, 0xA0));
            Fonts.drawCentered(g, badge, Fonts.Weight.MEDIUM, 10, tx + lw + 6 + bw / 2, by + (16 - Fonts.lineHeight(10)) / 2, on ? SOFT : MUTED);
            if (on) g.fill(tx, y + TAB_H - 1, tx + full, y + TAB_H, TEXT);
            tx += full + 28;
        }
        iconButton(g, "refresh", "refresh", x + w - 16 - 32, y + (TAB_H - 32) / 2, 32, 16, I18n.get("selectServer.refresh"), b -> refresh());
    }

    private int clampScroll(int contentH) {
        int max = Math.max(0, contentH - (listH - 2 * LIST_PAD));
        scroll = Math.max(0, Math.min(max, scroll));
        if (gg.shard.client.gui.Theme.reduceMotion()) scrollShown = scroll;
        else {
            scrollShown += (scroll - scrollShown) * (1f - (float) Math.exp(-dt * 18f));
            if (Math.abs(scroll - scrollShown) < 0.5f) scrollShown = scroll;
        }
        return Math.round(scrollShown);
    }

    private void renderServers(GuiGraphics g) {
        int n = servers.size();
        if (n == 0) {
            empty(g, "server", "No servers yet", "Add a server or use Direct connect to join one.");
            return;
        }
        int contentH = n * (ROW_H + ROW_GAP) - ROW_GAP;
        int off = clampScroll(contentH);
        clip(g, listX, listY, listW, listH);
        int rowX = listX + LIST_PAD;
        int rowW = listW - 2 * LIST_PAD - (contentH > listH - 2 * LIST_PAD ? 8 : 0);
        for (int i = 0; i < n; i++) {
            ServerData data = servers.get(i);
            pingIfNeeded(data);
            int ry = listY + LIST_PAD + i * (ROW_H + ROW_GAP) - off;
            if (ry + ROW_H < listY || ry > listY + listH) continue;
            renderServerRow(g, i, data, rowX, ry, rowW);
        }
        unclip(g);
        scrollbar(g, contentH, off);
    }

    private void renderServerRow(GuiGraphics g, int index, ServerData data, int x, int y, int w) {
        Entry e = entry(data);
        updateIcon(e);
        String key = "server:" + index;
        boolean sel = index == selected;
        boolean hover = hovered(key, x, y, w, ROW_H);
        hit(key, x, y, w, ROW_H, b -> {
            selected = index;
            if ((b & DOUBLE) != 0) join(data);
        });
        float hov = hoverAnim(key, hover);
        int fill = sel ? ACTIVE : Colors.mix(SURFACE, 0xFF202023, hov);
        Render2D.roundedRect(g, x, y, w, ROW_H, 8, fill);
        Render2D.roundedOutline(g, x, y, w, ROW_H, 8, sel ? BORDER_HOVER : Colors.mix(BORDER, BORDER_HOVER, hov * 0.6f));

        int ix = x + 12;
        int iy = y + (ROW_H - ICON) / 2;
        Render2D.roundedRect(g, ix - 1, iy - 1, ICON + 2, ICON + 2, 6, BG);
        g.blit(RenderPipelines.GUI_TEXTURED, e.icon.textureLocation(), ix, iy, 0f, 0f, ICON, ICON, 64, 64, 64, 64);

        int right = x + w - 16;
        int tx = ix + ICON + 14;
        int statusW = renderStatus(g, data, right, y);
        int textW = right - statusW - 16 - tx;
        Fonts.drawClipped(g, data.name, Fonts.Weight.MEDIUM, 14, tx, y + 12, textW, TEXT);
        int motdY = y + 12 + Fonts.lineHeight(14) + 2;
        if (data.state() == ServerData.State.INCOMPATIBLE) {
            drawComponent(g, Component.translatable("multiplayer.status.incompatible").append(" · ").append(data.version), 12, tx, motdY, textW, 2, DANGER);
        } else if (data.motd != null && !data.motd.getString().isEmpty()) {
            drawComponent(g, data.motd, 12, tx, motdY, textW, 2, MUTED);
        } else if (data.state() == ServerData.State.PINGING) {
            Fonts.draw(g, I18n.get("multiplayer.status.pinging"), Fonts.Weight.REGULAR, 12, tx, motdY, SUBTLE);
        }

        // The vanilla player list tooltip, in Shard's style.
        if (hover && data.playerList != null && !data.playerList.isEmpty()
                && Render2D.hovered(mouseX, mouseY, right - statusW, y + 36, statusW, 22)) {
            StringBuilder names = new StringBuilder();
            for (int i = 0; i < Math.min(12, data.playerList.size()); i++) {
                if (i > 0) names.append(", ");
                names.append(data.playerList.get(i).getString());
            }
            pendingTooltip = new Object[]{Fonts.clip(names.toString(), Fonts.Weight.MEDIUM, 11, 420), right - statusW / 2, y + 36};
        }
    }

    /** Ping bars and milliseconds on the first line, players on the second; returns the width used. */
    private int renderStatus(GuiGraphics g, ServerData data, int right, int y) {
        int lineY = y + 14;
        int barsW = 5 * 3 + 4 * 2;
        int barsX = right - barsW;
        String pingText;
        int pingColor;
        int level;
        switch (data.state()) {
            case PINGING, INITIAL -> {
                pingText = "";
                pingColor = MUTED;
                long t = Util.getMillis() / 120 % 8;
                level = (int) (t < 5 ? t + 1 : 8 - t);
            }
            case UNREACHABLE -> {
                pingText = "Offline";
                pingColor = DANGER;
                level = -1;
            }
            case INCOMPATIBLE -> {
                pingText = "Incompatible";
                pingColor = DANGER;
                level = -1;
            }
            default -> {
                pingText = data.ping + " ms";
                pingColor = data.ping < 150 ? SUCCESS : data.ping < 300 ? 0xFFA3E635 : data.ping < 600 ? WARNING : DANGER;
                level = data.ping < 150 ? 5 : data.ping < 300 ? 4 : data.ping < 600 ? 3 : data.ping < 1000 ? 2 : 1;
            }
        }
        for (int i = 0; i < 5; i++) {
            int bh = 4 + i * 2;
            int bx = barsX + i * 5;
            int by = lineY + 12 - bh;
            boolean lit = level > 0 ? i < level : false;
            int c = level < 0 ? Colors.withAlpha(DANGER, i == 0 ? 0xFF : 0x40) : lit ? (data.state() == ServerData.State.SUCCESSFUL ? pingColor : SOFT) : ACTIVE;
            Render2D.fill(g, bx, by, 3, bh, c);
        }
        int width = barsW;
        if (!pingText.isEmpty()) {
            int pw = Fonts.widthInt(pingText, Fonts.Weight.MEDIUM, 12);
            Fonts.draw(g, pingText, Fonts.Weight.MEDIUM, 12, barsX - 8 - pw, lineY + 6 - Fonts.lineHeight(12) / 2, pingColor);
            width += 8 + pw;
        }
        if (data.players != null && data.state() == ServerData.State.SUCCESSFUL) {
            String players = data.players.online() + " / " + data.players.max();
            int pw = Fonts.widthInt(players, Fonts.Weight.REGULAR, 12);
            int py = y + 38;
            Fonts.draw(g, players, Fonts.Weight.REGULAR, 12, right - pw, py, SOFT);
            Icons.draw(g, "user", right - pw - 6 - 13, py + (Fonts.lineHeight(12) - 13) / 2, 13, MUTED);
            width = Math.max(width, pw + 19);
        }
        return width;
    }

    private void renderLan(GuiGraphics g) {
        int n = lanServers.size();
        if (n == 0) {
            String dots = ".".repeat((int) (Util.getMillis() / 400 % 4));
            empty(g, "wifi", I18n.get("lanServer.scanning").replace("...", "").replace("…", "") + dots,
                    "Worlds opened to LAN on this network show up here.");
            return;
        }
        int contentH = n * (ROW_H + ROW_GAP) - ROW_GAP;
        int off = clampScroll(contentH);
        clip(g, listX, listY, listW, listH);
        int rowX = listX + LIST_PAD;
        int rowW = listW - 2 * LIST_PAD;
        for (int i = 0; i < n; i++) {
            LanServer lan = lanServers.get(i);
            int y = listY + LIST_PAD + i * (ROW_H + ROW_GAP) - off;
            int index = i;
            String key = "lan:" + i;
            boolean sel = i == selectedLan;
            boolean hover = hovered(key, rowX, y, rowW, ROW_H);
            hit(key, rowX, y, rowW, ROW_H, b -> {
                selectedLan = index;
                if ((b & DOUBLE) != 0) joinSelected();
            });
            float hov = hoverAnim(key, hover);
            Render2D.roundedRect(g, rowX, y, rowW, ROW_H, 8, sel ? ACTIVE : Colors.mix(SURFACE, HOVER, hov));
            Render2D.roundedOutline(g, rowX, y, rowW, ROW_H, 8, sel ? BORDER_HOVER : BORDER);
            int ix = rowX + 12;
            int iy = y + (ROW_H - ICON) / 2;
            Render2D.roundedRect(g, ix, iy, ICON, ICON, 6, ACTIVE);
            Icons.draw(g, "wifi", ix + 14, iy + 14, 20, SOFT);
            int tx = ix + ICON + 14;
            Fonts.drawClipped(g, lan.getMotd(), Fonts.Weight.MEDIUM, 14, tx, y + 14, rowW - (tx - rowX) - 80, TEXT);
            Fonts.draw(g, lan.getAddress(), Fonts.Weight.REGULAR, 12, tx, y + 14 + Fonts.lineHeight(14) + 2, MUTED);
            String pill = I18n.get("lanServer.title");
            int pw = Fonts.widthInt(pill, Fonts.Weight.MEDIUM, 10) + 14;
            Render2D.roundedRect(g, rowX + rowW - 16 - pw, y + (ROW_H - 20) / 2, pw, 20, 10, HOVER);
            Fonts.drawCentered(g, pill, Fonts.Weight.MEDIUM, 10, rowX + rowW - 16 - pw / 2, y + (ROW_H - Fonts.lineHeight(10)) / 2, SOFT);
        }
        unclip(g);
        scrollbar(g, contentH, off);
    }

    private void empty(GuiGraphics g, String icon, String title, String hint) {
        int cx = listX + listW / 2;
        int cy = listY + listH / 2 - 40;
        Render2D.roundedRect(g, cx - 24, cy, 48, 48, 10, SURFACE);
        Icons.draw(g, icon, cx - 12, cy + 12, 24, MUTED);
        Fonts.drawCentered(g, title, Fonts.Weight.MEDIUM, 14, cx, cy + 62, TEXT);
        Fonts.drawCentered(g, hint, Fonts.Weight.REGULAR, 12, cx, cy + 62 + Fonts.lineHeight(14) + 2, MUTED);
    }

    private void scrollbar(GuiGraphics g, int contentH, int off) {
        int view = listH - 2 * LIST_PAD;
        if (contentH <= view) return;
        int trackX = listX + listW - LIST_PAD + 2;
        int thumbH = Math.max(24, view * view / contentH);
        int thumbY = listY + LIST_PAD + (int) ((long) (view - thumbH) * off / Math.max(1, contentH - view));
        Render2D.roundedRect(g, trackX, thumbY, 4, thumbH, 2, BORDER_HOVER);
    }

    private void renderFooter(GuiGraphics g, int x, int y, int w) {
        g.fill(x + 1, y, x + w - 1, y + 1, BORDER);
        int bh = 36;
        int by = y + (FOOT_H - bh) / 2;
        int bx = x + 16;
        boolean servList = tab == Tab.SERVERS;
        boolean hasSel = servList ? selectedServer() != null : selectedLan >= 0 && selectedLan < lanServers.size();

        int addW = Fonts.widthInt(I18n.get("selectServer.add"), Fonts.Weight.MEDIUM, 13) + 50;
        button(g, "add", bx, by, addW, bh, "plus", I18n.get("selectServer.add"), 13, Style.NORMAL, true, b -> addServer());
        bx += addW + 8;
        int directW = Fonts.widthInt(I18n.get("selectServer.direct"), Fonts.Weight.MEDIUM, 13) + 50;
        button(g, "direct", bx, by, directW, bh, "link", I18n.get("selectServer.direct"), 13, Style.NORMAL, true, b -> directConnect());

        int rx = x + w - 16;
        int joinW = Math.max(120, Fonts.widthInt(I18n.get("selectServer.select"), Fonts.Weight.MEDIUM, 13) + 50);
        rx -= joinW;
        button(g, "join", rx, by, joinW, bh, "play", I18n.get("selectServer.select"), 13, Style.PRIMARY, hasSel, b -> joinSelected());
        boolean canEdit = servList && selectedServer() != null;
        rx -= 8 + bh;
        iconButtonEnabled(g, "delete", "trash", rx, by, bh, I18n.get("selectServer.delete"), canEdit, true, this::deleteServer);
        rx -= 8 + bh;
        iconButtonEnabled(g, "edit", "pencil", rx, by, bh, I18n.get("selectServer.edit"), canEdit, false, this::editServer);
    }

    /** Icon button that can be disabled (Edit, Delete); Delete turns red on hover. */
    private void iconButtonEnabled(GuiGraphics g, String key, String icon, int x, int y, int size, String tooltip, boolean enabled, boolean danger, Runnable action) {
        if (enabled) hit(key, x, y, size, size, b -> action.run());
        boolean hover = enabled && hovered(key, x, y, size, size);
        float hov = hoverAnim(key, hover);
        int fill = danger ? Colors.mix(Colors.withAlpha(SURFACE, 0xEB), Colors.withAlpha(DANGER, 0x28), hov) : Colors.mix(Colors.withAlpha(SURFACE, 0xEB), HOVER, hov);
        Render2D.roundedRect(g, x, y, size, size, 6, enabled ? fill : Colors.withAlpha(SURFACE, 0x70));
        Render2D.roundedOutline(g, x, y, size, size, 6, enabled ? Colors.mix(BORDER, danger ? Colors.withAlpha(DANGER, 0x60) : BORDER_HOVER, hov) : BORDER);
        int color = !enabled ? SUBTLE : danger ? Colors.mix(SOFT, DANGER, hov) : Colors.mix(SOFT, TEXT, hov);
        Icons.draw(g, icon, x + (size - 16) / 2, y + (size - 16) / 2, 16, color);
        if (hover) pendingTooltip = new Object[]{tooltip, x + size / 2, y};
    }

    // ---- smoke test hooks ------------------------------------------------------------------------

    public void selectServer(int index) {
        tab = Tab.SERVERS;
        selected = index;
    }

    public void showLan() {
        tab = Tab.LAN;
    }

    /** Server count and how many have answered a ping (smoke test). */
    public String pingSummary() {
        if (servers == null) return "not loaded";
        int ok = 0;
        for (int i = 0; i < servers.size(); i++) if (servers.get(i).state() == ServerData.State.SUCCESSFUL) ok++;
        return ok + "/" + servers.size() + " answered";
    }

    public ServerList serverList() {
        return servers;
    }
}
