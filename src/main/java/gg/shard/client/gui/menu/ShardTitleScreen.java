package gg.shard.client.gui.menu;

import gg.shard.client.ShardClient;
import gg.shard.client.gui.ClickGuiScreen;
import gg.shard.client.gui.Fonts;
import gg.shard.client.gui.Icons;
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
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

/**
 * Shard's title screen: the vanilla panorama, dimmed, under a centred column in the mod menu's
 * style: Singleplayer, Multiplayer, (Realms), then Options, Mod Menu (when installed) and Quit; the
 * language, accessibility and Shard menu shortcuts bottom left; the account switcher top right.
 * Every button does what vanilla's does.
 */
public final class ShardTitleScreen extends MenuScreen {
    private static final int COLUMN_W = 360;
    private static final int BIG_H = 44;
    private static final int SMALL_H = 38;
    private static final int GAP = 8;
    private static final int EDGE = 24;
    private static final String COPYRIGHT = "Copyright Mojang AB. Do not distribute!";
    private float fade;

    public ShardTitleScreen() {
        super(Component.translatable("narrator.screen.title"), COLUMN_W + 2 * 240, 640);
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
    protected void renderPage(GuiGraphics g, float partialTick) {
        fade = anim("fade", 1f, 400f);
        boolean realms = ShardClient.modules().get(MenuScreensModule.class).realmsButton.get();
        boolean modMenu = FabricLoader.getInstance().isModLoaded("modmenu");
        int rowsBig = realms ? 3 : 2;
        int blockH = 64 + 14 + 30 + 40 + rowsBig * (BIG_H + GAP) + SMALL_H;
        int cx = designW / 2;
        int top = Math.max(EDGE + AccountSwitcher.PILL_H + 12, (designH - blockH) / 2 - 10);

        // Mark and wordmark.
        int text = Colors.withAlpha(TEXT, Math.round(255 * fade));
        Icons.draw(g, "logo", cx - 32, top, 64, text);
        int wy = top + 64 + 14;
        Fonts.drawCentered(g, "Shard", Fonts.Weight.SEMIBOLD, 24, cx, wy, text);
        int y = wy + 30 + 40;
        int x = cx - COLUMN_W / 2;

        Component blocked = multiplayerDisabledReason();
        button(g, "singleplayer", x, y, COLUMN_W, BIG_H, "user", I18n.get("menu.singleplayer"), 14, Style.NORMAL, true,
                b -> minecraft.setScreen(new SelectWorldScreen(this)));
        y += BIG_H + GAP;
        button(g, "multiplayer", x, y, COLUMN_W, BIG_H, "server", I18n.get("menu.multiplayer"), 14, Style.NORMAL, blocked == null,
                b -> openMultiplayer());
        if (blocked != null && hovered("multiplayer", x, y, COLUMN_W, BIG_H)) pendingTooltip = new Object[]{blocked.getString(), cx, y};
        y += BIG_H + GAP;
        if (realms) {
            button(g, "realms", x, y, COLUMN_W, BIG_H, "cloud", I18n.get("menu.online"), 14, Style.NORMAL, blocked == null,
                    b -> minecraft.setScreen(new com.mojang.realmsclient.RealmsMainScreen(this)));
            y += BIG_H + GAP;
        }

        int count = modMenu ? 3 : 2;
        int w = (COLUMN_W - (count - 1) * GAP) / count;
        int bx = x;
        button(g, "options", bx, y, w, SMALL_H, "settings", I18n.get("menu.options").replace("...", "").replace("…", ""), 13, Style.NORMAL, true,
                // 26.1 and 26.2 take an in-world flag (gone again in 26.3).
                //? if >=26.1 <26.3 {
                /*b -> minecraft.setScreen(new OptionsScreen(this, minecraft.options, false)));
                *///?} else {
                b -> minecraft.setScreen(new OptionsScreen(this, minecraft.options)));
                //?}

        bx += w + GAP;
        if (modMenu) {
            button(g, "mods", bx, y, w, SMALL_H, "puzzle", "Mods", 13, Style.NORMAL, true, b -> openModMenu());
            bx += w + GAP;
        }
        button(g, "quit", bx, y, x + COLUMN_W - bx, SMALL_H, "power", I18n.get("menu.quit"), 13, Style.NORMAL, true,
                b -> minecraft.stop());

        // Account switcher, top right.
        accounts.renderPill(g, designW - EDGE, EDGE);

        // Bottom left: shortcuts and version.
        int by = designH - EDGE - 36;
        iconButton(g, "language", "language", EDGE, by, 36, 18, I18n.get("options.language"),
                b -> minecraft.setScreen(new LanguageSelectScreen(this, minecraft.options, minecraft.getLanguageManager())));
        iconButton(g, "accessibility", "accessibility", EDGE + 44, by, 36, 18, I18n.get("options.accessibility"),
                b -> minecraft.setScreen(new AccessibilityOptionsScreen(this, minecraft.options)));
        iconButton(g, "shard-menu", "logo", EDGE + 88, by, 36, 18, "Shard menu", b -> minecraft.setScreen(new ClickGuiScreen(this)));
        String version = "Minecraft " + SharedConstants.getCurrentVersion().name() + "  ·  Shard " + shardVersion();
        Fonts.draw(g, version, Fonts.Weight.REGULAR, 12, EDGE + 136, by + (36 - Fonts.lineHeight(12)) / 2, MUTED);

        // Bottom right: Mojang's notice opens the credits, as on vanilla's title screen.
        int cw = Fonts.widthInt(COPYRIGHT, Fonts.Weight.REGULAR, 12);
        int crx = designW - EDGE - cw;
        int cry = by + (36 - Fonts.lineHeight(12)) / 2;
        boolean crHover = hovered("copyright", crx - 4, cry - 4, cw + 8, Fonts.lineHeight(12) + 8);
        hit("copyright", crx - 4, cry - 4, cw + 8, Fonts.lineHeight(12) + 8, b -> minecraft.setScreen(new CreditsAndAttributionScreen(this)));
        Fonts.draw(g, COPYRIGHT, Fonts.Weight.REGULAR, 12, crx, cry, crHover ? TEXT : MUTED);
        if (crHover) g.fill(crx, cry + Fonts.lineHeight(12) - 1, crx + cw, cry + Fonts.lineHeight(12), TEXT);
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
