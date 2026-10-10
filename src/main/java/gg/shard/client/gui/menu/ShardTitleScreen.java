package gg.shard.client.gui.menu;

import gg.shard.client.ShardClient;
import gg.shard.client.gui.ClickGuiScreen;
import gg.shard.client.gui.Fonts;
import gg.shard.client.gui.Icons;
import gg.shard.client.gui.Render2D;
import gg.shard.client.modules.settings.MenuScreensModule;
import gg.shard.client.util.Colors;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.CreditsAndAttributionScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.SafetyScreen;
import net.minecraft.client.gui.screens.options.AccessibilityOptionsScreen;
import net.minecraft.client.gui.screens.options.LanguageSelectScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.ServerStatusPinger;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

/**
 * Shard's title screen in Shard Launcher's look: the launcher's crystal backdrop, its shard mark
 * glowing over the wordmark, then Multiplayer (the accent button), Singleplayer, (Realms), and a row
 * of Options, Mod Menu (when installed) and Quit; under them "Jump back in" rejoins the last server
 * played from Shard's server list. Language, accessibility and the Shard menu bottom left with the
 * version, the account switcher top right. Every button does what vanilla's does.
 */
public final class ShardTitleScreen extends MenuScreen {
    private static final int COLUMN_W = 380;
    private static final int BIG_H = 50;
    private static final int SMALL_H = 44;
    private static final int GAP = 10;
    private static final int EDGE = 24;
    private static final int LOGO = 120;
    private static final int RECENT_H = 60;
    private static final String COPYRIGHT = "Copyright Mojang AB. Do not distribute!";
    private float fade;
    private ServerStatusPinger pinger;
    private ServerData recent;
    private ServerCards.Entry recentEntry;

    public ShardTitleScreen() {
        super(Component.translatable("narrator.screen.title"), COLUMN_W + 2 * 240, 680);
        manageBlur = false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    protected int dimColor() {
        return 0x59000000;
    }

    @Override
    protected void init() {
        super.init();
        if (pinger != null) return;
        pinger = new ServerStatusPinger();
        String last = ServerCards.settings().lastServer.get();
        if (!last.isEmpty() && ServerCards.settings().jumpBackIn.get()) {
            ServerList list = new ServerList(minecraft);
            list.load();
            // get() also finds servers joined once through Direct Connection (kept hidden).
            recent = list.get(last);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (pinger != null) pinger.tick();
    }

    @Override
    public void removed() {
        if (pinger != null) pinger.removeAll();
        pinger = null;
        if (recentEntry != null) recentEntry.icon.close();
        recentEntry = null;
        recent = null;
        super.removed();
    }

    @Override
    protected void renderPage(GuiGraphics g, float partialTick) {
        fade = anim("fade", 1f, 400f);
        boolean realms = ShardClient.modules().get(MenuScreensModule.class).realmsButton.get();
        boolean modMenu = FabricLoader.getInstance().isModLoaded("modmenu");
        boolean showRecent = recent != null && ServerCards.settings().jumpBackIn.get();
        int rowsBig = realms ? 3 : 2;
        int blockH = LOGO + 12 + 40 + 20 + 36 + rowsBig * (BIG_H + GAP) + SMALL_H + (showRecent ? 16 + RECENT_H : 0);
        int cx = designW / 2;
        int top = Math.max(EDGE + AccountSwitcher.PILL_H + 12, (designH - 40 - blockH) / 2);
        top += Math.round((1f - Render2D.easeOut(fade)) * 6);

        // The launcher's mark, glowing, floating a little.
        float bob = gg.shard.client.gui.Theme.reduceMotion() ? 0f : (float) Math.sin(Util.getMillis() / 900.0) * 2f;
        int ly = top + Math.round(bob);
        ShardBackdrop.glow(g, cx, ly + LOGO / 2, 170, 0.24f * fade);
        ShardBackdrop.logo(g, cx - LOGO / 2, ly, LOGO, fade);
        int wy = top + LOGO + 12;
        int text = Colors.withAlpha(TEXT, Math.round(255 * fade));
        Fonts.drawCentered(g, "Shard", Fonts.Weight.SEMIBOLD, 32, cx, wy, text);
        String tag = "Minecraft " + SharedConstants.getCurrentVersion().name();
        Fonts.drawCentered(g, tag, Fonts.Weight.MEDIUM, 13, cx, wy + Fonts.lineHeight(32) + 2, Colors.withAlpha(SOFT, Math.round(255 * fade)));
        int y = wy + 40 + 20 + 36;
        int x = cx - COLUMN_W / 2;

        Component blocked = multiplayerDisabledReason();
        button(g, "multiplayer", x, y, COLUMN_W, BIG_H, "server", I18n.get("menu.multiplayer"), 15, Style.PRIMARY, blocked == null,
                b -> openMultiplayer());
        if (blocked != null && hovered("multiplayer", x, y, COLUMN_W, BIG_H)) pendingTooltip = new Object[]{blocked.getString(), cx, y};
        y += BIG_H + GAP;
        button(g, "singleplayer", x, y, COLUMN_W, BIG_H, "user", I18n.get("menu.singleplayer"), 15, Style.NORMAL, true,
                b -> minecraft.setScreen(new SelectWorldScreen(this)));
        y += BIG_H + GAP;
        if (realms) {
            button(g, "realms", x, y, COLUMN_W, BIG_H, "cloud", I18n.get("menu.online"), 15, Style.NORMAL, blocked == null,
                    b -> minecraft.setScreen(new com.mojang.realmsclient.RealmsMainScreen(this)));
            y += BIG_H + GAP;
        }

        int count = modMenu ? 3 : 2;
        int w = (COLUMN_W - (count - 1) * GAP) / count;
        int bx = x;
        button(g, "options", bx, y, w, SMALL_H, "settings", I18n.get("menu.options").replace("...", "").replace("…", ""), 14, Style.NORMAL, true,
                // 26.1 and 26.2 take an in-world flag (gone again in 26.3).
                //? if >=26.1 <26.3 {
                /*b -> minecraft.setScreen(new OptionsScreen(this, minecraft.options, false)));
                *///?} else {
                b -> minecraft.setScreen(new OptionsScreen(this, minecraft.options)));
                //?}

        bx += w + GAP;
        if (modMenu) {
            button(g, "mods", bx, y, w, SMALL_H, "puzzle", "Mods", 14, Style.NORMAL, true, b -> openModMenu());
            bx += w + GAP;
        }
        button(g, "quit", bx, y, x + COLUMN_W - bx, SMALL_H, "power", I18n.get("menu.quit"), 14, Style.QUIT, true,
                b -> minecraft.stop());
        y += SMALL_H;

        if (showRecent) renderRecent(g, x, y + 16, COLUMN_W, blocked == null);

        // Account switcher, top right.
        accounts.renderPill(g, designW - EDGE, EDGE);

        // Bottom left: shortcuts and the Shard version.
        int by = designH - EDGE - 38;
        iconButton(g, "language", "language", EDGE, by, 38, 18, I18n.get("options.language"),
                b -> minecraft.setScreen(new LanguageSelectScreen(this, minecraft.options, minecraft.getLanguageManager())));
        iconButton(g, "accessibility", "accessibility", EDGE + 46, by, 38, 18, I18n.get("options.accessibility"),
                b -> minecraft.setScreen(new AccessibilityOptionsScreen(this, minecraft.options)));
        iconButton(g, "shard-menu", "logo", EDGE + 92, by, 38, 18, "Shard menu", b -> minecraft.setScreen(new ClickGuiScreen(this)));
        String version = "Shard " + shardVersion();
        int vw = Fonts.widthInt(version, Fonts.Weight.MEDIUM, 12) + 22;
        int vx = EDGE + 92 + 38 + 12;
        int vy = by + (38 - 26) / 2;
        Render2D.roundedRect(g, vx, vy, vw, 26, 13, 0x0AFFFFFF);
        Render2D.roundedOutline(g, vx, vy, vw, 26, 13, BORDER);
        Fonts.draw(g, version, Fonts.Weight.MEDIUM, 12, vx + 11, vy + (26 - Fonts.lineHeight(12)) / 2, SOFT);

        // Bottom right: Mojang's notice opens the credits, as on vanilla's title screen.
        int cw = Fonts.widthInt(COPYRIGHT, Fonts.Weight.REGULAR, 11);
        int crx = designW - EDGE - cw;
        int cry = by + (38 - Fonts.lineHeight(11)) / 2;
        boolean crHover = hovered("copyright", crx - 4, cry - 4, cw + 8, Fonts.lineHeight(11) + 8);
        hit("copyright", crx - 4, cry - 4, cw + 8, Fonts.lineHeight(11) + 8, b -> minecraft.setScreen(new CreditsAndAttributionScreen(this)));
        Fonts.draw(g, COPYRIGHT, Fonts.Weight.REGULAR, 11, crx, cry, crHover ? TEXT : SUBTLE);
        if (crHover) g.fill(crx, cry + Fonts.lineHeight(11) - 1, crx + cw, cry + Fonts.lineHeight(11), TEXT);
    }

    /** The last server played from Shard's server list: icon, name, ping; a click joins it. */
    private void renderRecent(GuiGraphics g, int x, int y, int w, boolean allowed) {
        if (recentEntry == null) recentEntry = ServerCards.entry(recent);
        ServerCards.updateIcon(recentEntry);
        ServerCards.pingIfNeeded(pinger, recent, () -> {});
        String key = "recent";
        boolean hover = allowed && hovered(key, x, y, w, RECENT_H);
        if (allowed) hit(key, x, y, w, RECENT_H, b -> joinRecent());
        float hov = hoverAnim(key, hover);
        Render2D.roundedRect(g, x, y, w, RECENT_H, 12, Colors.mix(Colors.withAlpha(SURFACE, 0xD8), Colors.withAlpha(HOVER, 0xF0), hov));
        Render2D.roundedOutline(g, x, y, w, RECENT_H, 12, Colors.mix(BORDER, BORDER_HOVER, hov));
        int icon = 40;
        ServerCards.drawIcon(g, recentEntry, x + 10, y + (RECENT_H - icon) / 2, icon);
        int go = 34;
        int goX = x + w - 12 - go;
        int goY = y + (RECENT_H - go) / 2;
        Render2D.roundedRect(g, goX, goY, go, go, 9, Colors.withAlpha(accent(), Math.round(0x1A + 0x30 * hov)));
        Icons.draw(g, "play", goX + (go - 16) / 2, goY + (go - 16) / 2, 16, accent());
        int pillW = ServerCards.pingPill(g, recent, goX - 10, y + (RECENT_H - 22) / 2);
        int tx = x + 10 + icon + 12;
        int textW = goX - 10 - pillW - 12 - tx;
        int lh = Fonts.lineHeight(10) + 2 + Fonts.lineHeight(14);
        int ty = y + (RECENT_H - lh) / 2;
        Fonts.draw(g, "JUMP BACK IN", Fonts.Weight.SEMIBOLD, 10, tx, ty, MUTED);
        Fonts.drawClipped(g, ServerCards.displayName(recent), Fonts.Weight.SEMIBOLD, 14, tx, ty + Fonts.lineHeight(10) + 2, textW, TEXT);
    }

    /** Joins the last server; without the multiplayer safety notice accepted, opens the list instead. */
    private void joinRecent() {
        if (recent == null) return;
        if (!minecraft.options.skipMultiplayerWarning) {
            openMultiplayer();
            return;
        }
        ServerCards.join(this, recent);
    }

    private void openMultiplayer() {
        Screen next = minecraft.options.skipMultiplayerWarning ? new JoinMultiplayerScreen(this) : new SafetyScreen(this);
        minecraft.setScreen(next);
    }

    private void openModMenu() {
        try {
            Class<?> mods = Class.forName("com.terraformersmc.modmenu.gui.ModsScreen");
            minecraft.setScreen((Screen) mods.getConstructor(Screen.class).newInstance(this));
        } catch (ReflectiveOperationException | LinkageError e) {
            ShardClient.LOGGER.warn("Title screen: could not open Mod Menu", e);
        }
    }

    /** Same checks as vanilla's title screen: multiplayer switched off, or the account banned. */
    private Component multiplayerDisabledReason() {
        if (!minecraft.allowsMultiplayer()) {
            if (minecraft.isNameBanned()) return Component.translatable("title.multiplayer.disabled.banned.name");
            var ban = minecraft.multiplayerBan();
            if (ban != null) {
                return ban.expires() != null ? Component.translatable("title.multiplayer.disabled.banned.temporary")
                        : Component.translatable("title.multiplayer.disabled.banned.permanent");
            }
            return Component.translatable("title.multiplayer.disabled");
        }
        return null;
    }
}
