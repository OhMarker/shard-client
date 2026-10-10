package gg.shard.client.gui.menu;

import gg.shard.client.ShardClient;
import gg.shard.client.gui.Fonts;
import gg.shard.client.gui.Icons;
import gg.shard.client.gui.Render2D;
import gg.shard.client.gui.TextInput;
import gg.shard.client.util.Colors;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.DirectJoinServerScreen;
//? if >=1.21.9 {
import net.minecraft.client.gui.screens.ManageServerScreen;
//?}
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.ServerStatusPinger;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.server.LanServer;
import net.minecraft.client.server.LanServerDetection;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Shard's server list. Same data and behaviour as vanilla's JoinMultiplayerScreen (its ServerList,
 * ServerStatusPinger, LAN detection, ConnectScreen and the vanilla add / edit / direct-connect /
 * delete dialogs), drawn in Shard Launcher's look: a panel only as tall as its rows with Servers and
 * LAN tabs, search, one card per server (icon, name or address, MOTD, ping pill, players), hover
 * actions (favourite, move, edit, delete) and Add / Direct connection / Join. Favourites stay at the
 * top. Double-click or Enter joins; Shift+Up/Down reorders, like vanilla; Ctrl+F or typing searches.
 */
public final class ShardMultiplayerScreen extends MenuScreen {
    private static final int EDGE = 24;
    private static final int PANEL_MAX_W = 900;
    private static final int TAB_H = 56;
    private static final int FOOT_H = 64;
    private static final int ROW_H = 80;
    private static final int ROW_GAP = 8;
    private static final int ADD_ROW_H = 52;
    private static final int LIST_PAD = 12;
    private static final int ICON = 56;
    private static final int EMPTY_H = 250;
    private static final int SEARCH_W = 240;
    private static final int ACT = 30;

    private enum Tab { SERVERS, LAN }

    private final Screen parent;
    private ServerStatusPinger pinger;
    private ServerList servers;
    private LanServerDetection.LanServerList lanList;
    private LanServerDetection.LanServerDetector lanDetector;
    private final List<LanServer> lanServers = new ArrayList<>();
    private final Map<ServerData, ServerCards.Entry> entries = new IdentityHashMap<>();
    private final TextInput search = new TextInput(64).placeholder("Search servers").padLeft(30);
    private boolean searchFocused;
    /** Indices into {@link #servers} that match the search, rebuilt each frame (no allocation). */
    private int[] visible = new int[0];
    private int visibleCount;
    private Set<String> favourites;
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
        favourites = ServerCards.favourites();
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
        for (ServerCards.Entry e : entries.values()) e.icon.close();
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
        ServerCards.join(this, data);
    }

    private void joinSelected() {
        if (tab == Tab.SERVERS) {
            ServerData data = selectedServer();
            if (data != null) join(data);
        } else if (selectedLan >= 0 && selectedLan < lanServers.size()) {
            LanServer lan = lanServers.get(selectedLan);
            ConnectLan.join(this, new ServerData(lan.getMotd(), lan.getAddress(), ServerData.Type.LAN));
        }
    }

    /** LAN worlds are not remembered for Jump back in. */
    private static final class ConnectLan {
        static void join(Screen from, ServerData data) {
            net.minecraft.client.gui.screens.ConnectScreen.startConnecting(from, net.minecraft.client.Minecraft.getInstance(),
                    net.minecraft.client.multiplayer.resolver.ServerAddress.parseString(data.ip), data, false, null);
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
        String oldIp = data.ip;
        minecraft.setScreen(serverEditor("manageServer.edit.title", ok -> {
            if (ok) {
                data.name = editing.name;
                data.ip = editing.ip;
                data.copyFrom(editing);
                servers.save();
                // A favourite keeps its star when its address changes.
                String old = oldIp.trim().toLowerCase(Locale.ROOT);
                if (favourites.remove(old)) {
                    favourites.add(data.ip.trim().toLowerCase(Locale.ROOT));
                    ServerCards.setFavourites(favourites);
                }
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

    /** Stars or unstars a server; a new favourite moves up to sit after the other favourites. */
    private void toggleFavourite(int index) {
        ServerData data = servers.get(index);
        String ip = data.ip.trim().toLowerCase(Locale.ROOT);
        if (favourites.remove(ip)) {
            ServerCards.setFavourites(favourites);
            return;
        }
        favourites.add(ip);
        ServerCards.setFavourites(favourites);
        int target = 0;
        while (target < index && ServerCards.isFavourite(favourites, servers.get(target))) target++;
        for (int i = index; i > target; i--) servers.swap(i, i - 1);
        servers.save();
        if (selected == index) selected = target;
        else if (selected >= target && selected < index) selected++;
    }

    // ---- input ---------------------------------------------------------------------------------

    @Override
    protected boolean designClicked(double x, double y, int button, boolean doubleClick) {
        searchFocused = false;
        return super.designClicked(x, y, button, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (accounts.open() && event.key() == GLFW.GLFW_KEY_ESCAPE) {
            accounts.close();
            return true;
        }
        int key = event.key();
        int mods = event.modifiers();
        boolean shift = (mods & GLFW.GLFW_MOD_SHIFT) != 0;
        boolean ctrl = (mods & (GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_SUPER)) != 0;
        if (ctrl && key == GLFW.GLFW_KEY_F && tab == Tab.SERVERS) {
            searchFocused = true;
            search.cursorToEnd();
            return true;
        }
        if (searchFocused) {
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                if (search.isEmpty()) searchFocused = false;
                else search.setValue("");
                return true;
            }
            boolean listKey = key == GLFW.GLFW_KEY_UP || key == GLFW.GLFW_KEY_DOWN || key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER;
            if (!listKey) {
                search.keyPressed(key, mods);
                scroll = 0;
                return true;
            }
        }
        int count = tab == Tab.SERVERS ? visibleCount : lanServers.size();
        switch (key) {
            case GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_DOWN -> {
                int d = key == GLFW.GLFW_KEY_UP ? -1 : 1;
                if (tab == Tab.SERVERS && shift && search.isEmpty()) move(d);
                else if (count > 0) {
                    if (tab == Tab.SERVERS) {
                        int pos = positionOf(selected);
                        int next = Math.max(0, Math.min(count - 1, pos < 0 ? 0 : pos + d));
                        selected = visible[next];
                        revealSelection(next);
                    } else {
                        int next = Math.max(0, Math.min(count - 1, selectedLan < 0 ? 0 : selectedLan + d));
                        selectedLan = next;
                        revealSelection(next);
                    }
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

    @Override
    public boolean charTyped(CharacterEvent event) {
        String ch = event.codepointAsString();
        if (tab != Tab.SERVERS || ch.isEmpty()) return super.charTyped(event);
        // Type anywhere to search, as in the mod menu.
        if (!searchFocused && !Character.isLetterOrDigit(ch.codePointAt(0))) return super.charTyped(event);
        if (!searchFocused) {
            searchFocused = true;
            search.cursorToEnd();
        }
        search.charTyped(ch);
        scroll = 0;
        return true;
    }

    private int positionOf(int index) {
        for (int i = 0; i < visibleCount; i++) if (visible[i] == index) return i;
        return -1;
    }

    private void revealSelection(int position) {
        int top = position * (ROW_H + ROW_GAP);
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

    // ---- drawing ------------------------------------------------------------------------------

    private void filter() {
        int n = servers.size();
        if (visible.length < n) visible = new int[Math.max(n, 16)];
        String q = search.value().trim().toLowerCase(Locale.ROOT);
        visibleCount = 0;
        for (int i = 0; i < n; i++) {
            ServerData d = servers.get(i);
            if (q.isEmpty() || d.name.toLowerCase(Locale.ROOT).contains(q) || d.ip.toLowerCase(Locale.ROOT).contains(q)) visible[visibleCount++] = i;
        }
    }

    private int contentHeight() {
        if (tab == Tab.LAN) return lanServers.isEmpty() ? EMPTY_H : lanServers.size() * (ROW_H + ROW_GAP) - ROW_GAP + 2 * LIST_PAD;
        if (servers.size() == 0 || visibleCount == 0) return EMPTY_H;
        int rows = visibleCount * (ROW_H + ROW_GAP) - ROW_GAP;
        if (search.isEmpty()) rows += ROW_GAP + ADD_ROW_H;
        return rows + 2 * LIST_PAD;
    }

    @Override
    protected void renderPage(GuiGraphics g, float partialTick) {
        if (servers == null) return;
        filter();
        float fade = anim("fade", 1f, 220f);
        int slide = Math.round((1f - Render2D.easeOut(fade)) * 6);
        int panelW = Math.min(PANEL_MAX_W, designW - 2 * EDGE);
        int panelX = (designW - panelW) / 2;
        int headY = EDGE;
        int panelY = headY + AccountSwitcher.PILL_H + 16 + slide;
        int maxH = designH - EDGE - panelY;
        int panelH = Math.min(maxH, TAB_H + contentHeight() + FOOT_H);

        // Header: back, title, account.
        iconButton(g, "back", "chevron-left", panelX, headY, 36, 18, I18n.get("gui.back"), b -> onClose());
        Fonts.draw(g, I18n.get("multiplayer.title"), Fonts.Weight.SEMIBOLD, 20, panelX + 36 + 14, headY + (36 - Fonts.lineHeight(20)) / 2, TEXT);
        accounts.renderPill(g, panelX + panelW, headY);

        Render2D.roundedRect(g, panelX - 2, panelY + 8, panelW + 4, panelH, 18, 0x50000000);
        Render2D.roundedRect(g, panelX, panelY, panelW, panelH, 16, Colors.withAlpha(0xFF0B0F18, 0xD6));
        Render2D.roundedOutline(g, panelX, panelY, panelW, panelH, 16, BORDER);
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
        int tx = x + 22;
        for (Tab t : Tab.values()) {
            String label = t == Tab.SERVERS ? "Servers" : "LAN";
            int count = t == Tab.SERVERS ? servers.size() : lanServers.size();
            int lw = Fonts.widthInt(label, Fonts.Weight.MEDIUM, 14);
            String badge = Integer.toString(count);
            int bw = Math.max(20, Fonts.widthInt(badge, Fonts.Weight.MEDIUM, 11) + 12);
            int full = lw + 8 + bw;
            String key = "tab:" + t.name();
            boolean on = tab == t;
            boolean hover = hovered(key, tx - 8, y, full + 16, TAB_H);
            hit(key, tx - 8, y, full + 16, TAB_H, b -> {
                tab = t;
                scroll = 0;
                scrollShown = 0;
            });
            float hov = hoverAnim(key, hover);
            float onAnim = anim(key + ":on", on ? 1f : 0f, 160f);
            int color = on ? TEXT : Colors.mix(MUTED, SOFT, hov);
            Fonts.draw(g, label, Fonts.Weight.MEDIUM, 14, tx, y + (TAB_H - Fonts.lineHeight(14)) / 2, color);
            int by = y + (TAB_H - 18) / 2;
            int a = accent();
            Render2D.roundedRect(g, tx + lw + 8, by, bw, 18, 9, on ? Colors.withAlpha(a, 0x1F) : 0x10FFFFFF);
            Fonts.drawCentered(g, badge, Fonts.Weight.MEDIUM, 11, tx + lw + 8 + bw / 2, by + (18 - Fonts.lineHeight(11)) / 2, on ? a : SOFT);
            if (onAnim > 0.01f) {
                int uw = Math.round(full * Render2D.easeOut(onAnim));
                Render2D.roundedRect(g, tx + (full - uw) / 2, y + TAB_H - 2, uw, 2, 1, a);
            }
            tx += full + 28;
        }
        int rx = x + w - 12 - 34;
        int ry = y + (TAB_H - 34) / 2;
        iconButton(g, "refresh", "refresh", rx, ry, 34, 16, I18n.get("selectServer.refresh"), b -> refresh());
        if (tab == Tab.SERVERS) {
            int sw = Math.min(SEARCH_W, rx - 10 - tx);
            if (sw >= 120) renderSearch(g, rx - 10 - sw, ry, sw, 34);
        }
    }

    private void renderSearch(GuiGraphics g, int x, int y, int w, int h) {
        hit("search", x, y, w, h, b -> {
            searchFocused = true;
            search.clickAt(x, w, mouseX);
        });
        search.render(g, x, y, w, h, searchFocused);
        if (searchFocused) Render2D.roundedOutline(g, x, y, w, h, gg.shard.client.gui.Theme.radiusSmall(), Colors.withAlpha(accent(), 0x90));
        Icons.draw(g, "search", x + 10, y + (h - 14) / 2, 14, MUTED);
        if (search.isEmpty() && !searchFocused) {
            String kb = "Ctrl F";
            int kw = Fonts.widthInt(kb, Fonts.Weight.MEDIUM, 10) + 10;
            int kx = x + w - 8 - kw;
            Render2D.roundedOutline(g, kx, y + (h - 18) / 2, kw, 18, 4, BORDER_HOVER);
            Fonts.drawCentered(g, kb, Fonts.Weight.MEDIUM, 10, kx + kw / 2, y + (h - Fonts.lineHeight(10)) / 2, SUBTLE);
        }
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
        if (servers.size() == 0) {
            empty(g, "server", "No servers yet", "Add a server to save it here, or connect to an address once without saving it.", true);
            return;
        }
        if (visibleCount == 0) {
            empty(g, "search", "No servers match \"" + search.value().trim() + "\"", "Search looks at server names and addresses.", false);
            return;
        }
        boolean addRow = search.isEmpty();
        int contentH = visibleCount * (ROW_H + ROW_GAP) - ROW_GAP + (addRow ? ROW_GAP + ADD_ROW_H : 0);
        int off = clampScroll(contentH);
        boolean scrolls = contentH > listH - 2 * LIST_PAD;
        clip(g, listX, listY, listW, listH);
        int rowX = listX + LIST_PAD;
        int rowW = listW - 2 * LIST_PAD - (scrolls ? 8 : 0);
        for (int p = 0; p < visibleCount; p++) {
            int i = visible[p];
            ServerData data = servers.get(i);
            ServerCards.pingIfNeeded(pinger, data, this::saveIfOpen);
            int ry = listY + LIST_PAD + p * (ROW_H + ROW_GAP) - off;
            if (ry + ROW_H < listY || ry > listY + listH) continue;
            renderServerRow(g, i, data, rowX, ry, rowW);
        }
        if (addRow) {
            int ay = listY + LIST_PAD + visibleCount * (ROW_H + ROW_GAP) - off;
            boolean hover = hovered("add-row", rowX, ay, rowW, ADD_ROW_H);
            hit("add-row", rowX, ay, rowW, ADD_ROW_H, b -> addServer());
            float hov = hoverAnim("add-row", hover);
            if (hov > 0) Render2D.roundedRect(g, rowX, ay, rowW, ADD_ROW_H, 12, Colors.withAlpha(0xFFFFFFFF, Math.round(0x08 * hov)));
            Render2D.roundedOutline(g, rowX, ay, rowW, ADD_ROW_H, 12, Colors.mix(BORDER_HOVER, Colors.withAlpha(accent(), 0x80), hov));
            String label = "Add a server";
            int lw = Fonts.widthInt(label, Fonts.Weight.MEDIUM, 13) + 22;
            int lx = rowX + (rowW - lw) / 2;
            int c = Colors.mix(MUTED, TEXT, hov);
            Icons.draw(g, "plus", lx, ay + (ADD_ROW_H - 15) / 2, 15, c);
            Fonts.draw(g, label, Fonts.Weight.MEDIUM, 13, lx + 22, ay + (ADD_ROW_H - Fonts.lineHeight(13)) / 2, c);
        }
        unclip(g);
        scrollbar(g, contentH, off);
    }

    private void saveIfOpen() {
        if (servers != null) servers.save();
    }

    private void renderServerRow(GuiGraphics g, int index, ServerData data, int x, int y, int w) {
        ServerCards.Entry e = entries.computeIfAbsent(data, ServerCards::entry);
        if (ServerCards.updateIcon(e)) saveIfOpen();
        String key = "server:" + index;
        boolean sel = index == selected;
        boolean hover = hovered(key, x, y, w, ROW_H);
        hit(key, x, y, w, ROW_H, b -> {
            selected = index;
            if ((b & DOUBLE) != 0) join(data);
        });
        float hov = hoverAnim(key, hover);
        float selA = anim(key + ":sel", sel ? 1f : 0f, 140f);
        int a = accent();
        if (selA > 0.01f) glow(g, x, y, w, ROW_H, 12, 0.35f * selA);
        int base = Colors.mix(0x09FFFFFF, 0x10FFFFFF, hov);
        Render2D.roundedRect(g, x, y, w, ROW_H, 12, base);
        if (selA > 0.01f) Render2D.roundedRect(g, x, y, w, ROW_H, 12, Colors.withAlpha(a, Math.round(0x1A * selA)));
        int border = Colors.mix(Colors.mix(0x00000000, BORDER_HOVER, hov), Colors.withAlpha(a, 0x80), selA);
        Render2D.roundedOutline(g, x, y, w, ROW_H, 12, border);
        if (selA > 0.01f) Render2D.roundedRect(g, x, y + 18, 3, ROW_H - 36, 1, Colors.withAlpha(a, Math.round(255 * selA)));

        int ix = x + 12;
        int iy = y + (ROW_H - ICON) / 2;
        ServerCards.drawIcon(g, e, ix, iy, ICON);

        int right = x + w - 16;
        int pillW = ServerCards.pingPill(g, data, right, y + 15);
        int playersW = ServerCards.players(g, data, right, y + 47);
        int statusW = Math.max(pillW, playersW);

        // Hover actions: favourite, move up/down, edit, delete.
        boolean fav = ServerCards.isFavourite(favourites, data);
        int actionsRight = right - statusW - 14;
        int actionsW = 0;
        if (hover || sel) {
            actionsW = renderActions(g, index, actionsRight, y + (ROW_H - ACT) / 2, fav);
        }

        int tx = ix + ICON + 16;
        int textRight = actionsRight - (actionsW > 0 ? actionsW + 12 : 0);
        int textW = textRight - tx;
        int ny = y + 14;
        int nx = tx;
        if (fav) {
            Icons.draw(g, "favorite-on", nx, ny + (Fonts.lineHeight(15) - 14) / 2, 14, WARNING);
            nx += 20;
        }
        String name = ServerCards.displayName(data);
        String shownName = Fonts.clip(name, Fonts.Weight.SEMIBOLD, 15, textRight - nx);
        Fonts.draw(g, shownName, Fonts.Weight.SEMIBOLD, 15, nx, ny, TEXT);
        if (ServerCards.showsAddressSeparately(data)) {
            int ax = nx + Fonts.widthInt(shownName, Fonts.Weight.SEMIBOLD, 15) + 10;
            if (textRight - ax > 40) Fonts.drawClipped(g, data.ip, Fonts.Weight.REGULAR, 12, ax, ny + Fonts.baseline(15) - Fonts.baseline(12), textRight - ax, MUTED);
        }
        int motdY = ny + Fonts.lineHeight(15) + 5;
        if (data.state() == ServerData.State.INCOMPATIBLE) {
            drawComponent(g, Component.translatable("multiplayer.status.incompatible").append(" · ").append(data.version), 13, tx, motdY, textW, 2, DANGER);
        } else if (data.motd != null && !data.motd.getString().isEmpty()) {
            drawComponent(g, data.motd, 13, tx, motdY, textW, 2, SOFT);
        } else if (data.state() == ServerData.State.PINGING) {
            Fonts.draw(g, I18n.get("multiplayer.status.pinging"), Fonts.Weight.REGULAR, 13, tx, motdY, SUBTLE);
        }

        // The vanilla player list tooltip, in Shard's style.
        if (hover && playersW > 0 && data.playerList != null && !data.playerList.isEmpty()
                && Render2D.hovered(mouseX, mouseY, right - playersW, y + 44, playersW, 22)) {
            StringBuilder names = new StringBuilder();
            for (int i = 0; i < Math.min(12, data.playerList.size()); i++) {
                if (i > 0) names.append(", ");
                names.append(data.playerList.get(i).getString());
            }
            pendingTooltip = new Object[]{Fonts.clip(names.toString(), Fonts.Weight.MEDIUM, 11, 420), right - playersW / 2, y + 44};
        }
    }

    /** The row's action buttons, right-aligned at {@code right}; returns their total width. */
    private int renderActions(GuiGraphics g, int index, int right, int y, boolean fav) {
        int n = 5;
        int gap = 4;
        int total = n * ACT + (n - 1) * gap;
        int x = right - total;
        action(g, "fav:" + index, x, y, fav ? "favorite-on" : "favorites", fav ? "Remove from favourites" : "Add to favourites",
                fav ? WARNING : 0, true, () -> toggleFavourite(index));
        x += ACT + gap;
        boolean searching = !search.isEmpty();
        action(g, "up:" + index, x, y, "chevron-up", "Move up", 0, !searching && index > 0, () -> {
            selected = index;
            move(-1);
        });
        x += ACT + gap;
        action(g, "down:" + index, x, y, "chevron-down", "Move down", 0, !searching && index < servers.size() - 1, () -> {
            selected = index;
            move(1);
        });
        x += ACT + gap;
        action(g, "edit:" + index, x, y, "pencil", I18n.get("selectServer.edit"), 0, true, () -> {
            selected = index;
            editServer();
        });
        x += ACT + gap;
        action(g, "del:" + index, x, y, "trash", I18n.get("selectServer.delete"), DANGER, true, () -> {
            selected = index;
            deleteServer();
        });
        return total;
    }

    private void action(GuiGraphics g, String key, int x, int y, String icon, String tooltip, int tint, boolean enabled, Runnable run) {
        if (enabled) hit(key, x, y, ACT, ACT, b -> run.run());
        boolean hover = enabled && hovered(key, x, y, ACT, ACT);
        float hov = hoverAnim(key, hover);
        Render2D.roundedRect(g, x, y, ACT, ACT, 8, Colors.mix(0x40000000, 0x70000000, hov));
        Render2D.roundedOutline(g, x, y, ACT, ACT, 8, Colors.mix(BORDER, tint == DANGER ? Colors.withAlpha(DANGER, 0x70) : BORDER_HOVER, hov));
        int color = !enabled ? SUBTLE : tint != 0 ? (tint == DANGER ? Colors.mix(SOFT, DANGER, hov) : tint) : Colors.mix(SOFT, TEXT, hov);
        Icons.draw(g, icon, x + (ACT - 15) / 2, y + (ACT - 15) / 2, 15, color);
        if (hover) pendingTooltip = new Object[]{tooltip, x + ACT / 2, y};
    }

    private void renderLan(GuiGraphics g) {
        int n = lanServers.size();
        if (n == 0) {
            String dots = ".".repeat((int) (Util.getMillis() / 400 % 4));
            empty(g, "wifi", I18n.get("lanServer.scanning").replace("...", "").replace("…", "") + dots,
                    "Worlds opened to LAN on this network show up here.", false);
            return;
        }
        int contentH = n * (ROW_H + ROW_GAP) - ROW_GAP;
        int off = clampScroll(contentH);
        clip(g, listX, listY, listW, listH);
        int rowX = listX + LIST_PAD;
        int rowW = listW - 2 * LIST_PAD;
        int a = accent();
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
            Render2D.roundedRect(g, rowX, y, rowW, ROW_H, 12, sel ? Colors.withAlpha(a, 0x1A) : Colors.mix(0x09FFFFFF, 0x10FFFFFF, hov));
            Render2D.roundedOutline(g, rowX, y, rowW, ROW_H, 12, sel ? Colors.withAlpha(a, 0x80) : Colors.mix(0x00000000, BORDER_HOVER, hov));
            if (sel) Render2D.roundedRect(g, rowX, y + 18, 3, ROW_H - 36, 1, a);
            int ix = rowX + 12;
            int iy = y + (ROW_H - ICON) / 2;
            Render2D.roundedRect(g, ix, iy, ICON, ICON, 10, ACTIVE);
            Icons.draw(g, "wifi", ix + 16, iy + 16, 24, SOFT);
            int tx = ix + ICON + 16;
            Fonts.drawClipped(g, lan.getMotd(), Fonts.Weight.SEMIBOLD, 15, tx, y + 18, rowW - (tx - rowX) - 80, TEXT);
            Fonts.draw(g, lan.getAddress(), Fonts.Weight.REGULAR, 13, tx, y + 18 + Fonts.lineHeight(15) + 4, MUTED);
            String pill = I18n.get("lanServer.title");
            int pw = Fonts.widthInt(pill, Fonts.Weight.MEDIUM, 11) + 16;
            Render2D.roundedRect(g, rowX + rowW - 16 - pw, y + (ROW_H - 22) / 2, pw, 22, 11, Colors.withAlpha(a, 0x1C));
            Fonts.drawCentered(g, pill, Fonts.Weight.MEDIUM, 11, rowX + rowW - 16 - pw / 2, y + (ROW_H - Fonts.lineHeight(11)) / 2, a);
        }
        unclip(g);
        scrollbar(g, contentH, off);
    }

    /** Empty state: a glowing icon tile, a title and a line; with {@code actions}, Add and Direct buttons. */
    private void empty(GuiGraphics g, String icon, String title, String hint, boolean actions) {
        int cx = listX + listW / 2;
        int blockH = 84 + 16 + Fonts.lineHeight(18) + 8 + Fonts.lineHeight(13) + (actions ? 20 + 40 : 0);
        int top = listY + Math.max(16, (listH - blockH) / 2);
        ShardBackdrop.glow(g, cx, top + 30, 80, 0.18f);
        Render2D.roundedRect(g, cx - 42, top, 84, 84, 22, 0x0DFFFFFF);
        Render2D.roundedOutline(g, cx - 42, top, 84, 84, 22, BORDER_HOVER);
        Icons.draw(g, icon, cx - 18, top + 24, 36, accent());
        int y = top + 84 + 16;
        Fonts.drawCentered(g, title, Fonts.Weight.SEMIBOLD, 18, cx, y, TEXT);
        y += Fonts.lineHeight(18) + 8;
        Fonts.drawCentered(g, Fonts.clip(hint, Fonts.Weight.REGULAR, 13, listW - 40), Fonts.Weight.REGULAR, 13, cx, y, SOFT);
        if (!actions) return;
        y += Fonts.lineHeight(13) + 20;
        String add = I18n.get("selectServer.add");
        String direct = I18n.get("selectServer.direct");
        int aw = Fonts.widthInt(add, Fonts.Weight.MEDIUM, 14) + 54;
        int dw = Fonts.widthInt(direct, Fonts.Weight.MEDIUM, 14) + 54;
        int bx = cx - (aw + 10 + dw) / 2;
        button(g, "empty-add", bx, y, aw, 40, "plus", add, 14, Style.PRIMARY, true, b -> addServer());
        button(g, "empty-direct", bx + aw + 10, y, dw, 40, "link", direct, 14, Style.NORMAL, true, b -> directConnect());
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
        int bh = 40;
        int by = y + (FOOT_H - bh) / 2;
        int bx = x + 12;
        boolean servList = tab == Tab.SERVERS;
        boolean hasSel = servList ? selectedServer() != null : selectedLan >= 0 && selectedLan < lanServers.size();

        int addW = Fonts.widthInt(I18n.get("selectServer.add"), Fonts.Weight.MEDIUM, 14) + 54;
        button(g, "add", bx, by, addW, bh, "plus", I18n.get("selectServer.add"), 14, Style.NORMAL, true, b -> addServer());
        bx += addW + 10;
        int directW = Fonts.widthInt(I18n.get("selectServer.direct"), Fonts.Weight.MEDIUM, 14) + 54;
        button(g, "direct", bx, by, directW, bh, "link", I18n.get("selectServer.direct"), 14, Style.NORMAL, true, b -> directConnect());
        bx += directW;

        // "Join NA PvP": the button says where it goes.
        String join = I18n.get("selectServer.select");
        if (hasSel && servList) join = "Join " + ServerCards.displayName(selectedServer());
        else if (hasSel) join = "Join " + lanServers.get(selectedLan).getMotd();
        int joinW = Math.min(260, Math.max(170, Fonts.widthInt(join, Fonts.Weight.SEMIBOLD, 14) + 56));
        int rx = x + w - 12 - joinW;
        button(g, "join", rx, by, joinW, bh, "play", join, 14, Style.PRIMARY, hasSel, b -> joinSelected());

        String hint = "Double-click a server to join";
        int hw = Fonts.widthInt(hint, Fonts.Weight.REGULAR, 12);
        if (rx - 16 - (bx + 16) >= hw) Fonts.draw(g, hint, Fonts.Weight.REGULAR, 12, bx + 16, by + (bh - Fonts.lineHeight(12)) / 2, SUBTLE);
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

    /** Types into the search field (smoke test). */
    public void searchFor(String text) {
        search.setValue(text);
        searchFocused = !text.isEmpty();
    }
}
