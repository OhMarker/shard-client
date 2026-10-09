package gg.shard.client.gui.menu;

import gg.shard.client.account.AccountManager;
import gg.shard.client.gui.Fonts;
import gg.shard.client.gui.Icons;
import gg.shard.client.gui.Render2D;
import gg.shard.client.launcher.AccountBridge;
import gg.shard.client.util.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
//? if >=26.1 {
/*import net.minecraft.client.gui.components.PlayerFaceExtractor;
*///?} else {
import net.minecraft.client.gui.components.PlayerFaceRenderer;
//?}
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.component.ResolvableProfile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The current account (head + name) in a pill; with Shard Launcher's bridge a click opens a list of
 * the launcher's accounts and "Add account" (docs/ACCOUNT-SWITCH-API.md). Without a bridge it is
 * just the name. Heads come from vanilla's player skin cache (the default skin until loaded).
 */
final class AccountSwitcher {
    static final int PILL_H = 36;
    private static final int HEAD = 20;
    private static final int ROW_H = 40;
    private static final int MENU_W = 260;

    private final MenuScreen screen;
    private final Map<UUID, ResolvableProfile> profiles = new HashMap<>();
    private boolean open;
    private float openAnim;
    private int pillX;
    private int pillY;
    private int pillW;

    AccountSwitcher(MenuScreen screen) {
        this.screen = screen;
    }

    boolean open() {
        return open;
    }

    void close() {
        open = false;
    }

    void toggle() {
        open = !open;
        if (open) {
            screen.setAnim("acct:open", 0f);
            AccountManager.get().refresh();
        }
    }

    void tick() {
        AccountManager.get().tick();
    }

    int width() {
        String name = AccountManager.get().currentName();
        int chevron = AccountManager.get().available() ? 14 + 8 : 0;
        return 8 + HEAD + 10 + Fonts.widthInt(name, Fonts.Weight.MEDIUM, 13) + 12 + chevron;
    }

    /** The pill, right-aligned at {@code right}. */
    void renderPill(GuiGraphics g, int right, int top) {
        AccountManager am = AccountManager.get();
        pillW = width();
        pillX = right - pillW;
        pillY = top;
        boolean clickable = am.available();
        boolean hover = clickable && screen.hovered("acct:pill", pillX, pillY, pillW, PILL_H);
        if (clickable) screen.hit("acct:pill", pillX, pillY, pillW, PILL_H, b -> toggle());
        float hov = screen.hoverAnim("acct:pill", hover || open);
        Render2D.roundedRect(g, pillX, pillY, pillW, PILL_H, 8, Colors.mix(Colors.withAlpha(MenuScreen.SURFACE, 0xD8), Colors.withAlpha(MenuScreen.ACTIVE, 0xFA), hov));
        Render2D.roundedOutline(g, pillX, pillY, pillW, PILL_H, 8, Colors.mix(MenuScreen.BORDER, MenuScreen.BORDER_HOVER, hov));
        drawHead(g, am.currentUuid(), pillX + 8, pillY + (PILL_H - HEAD) / 2, HEAD);
        int tx = pillX + 8 + HEAD + 10;
        Fonts.draw(g, am.currentName(), Fonts.Weight.MEDIUM, 13, tx, pillY + (PILL_H - Fonts.lineHeight(13)) / 2, MenuScreen.TEXT);
        if (clickable) {
            Icons.draw(g, open ? "chevron-up" : "chevron-down", pillX + pillW - 12 - 14, pillY + (PILL_H - 14) / 2, 14,
                    Colors.mix(MenuScreen.MUTED, MenuScreen.TEXT, hov));
        }
    }

    /** The account list under the pill; drawn after everything else. */
    void renderDropdown(GuiGraphics g) {
        AccountManager am = AccountManager.get();
        openAnim = screen.anim("acct:open", 1f, 140f);
        List<AccountBridge.Account> list = am.accounts();
        String error = am.error();
        boolean canSwitch = am.canSwitch() || am.switchingTo() != null;
        int rows = Math.max(1, list.size());
        int h = 8 + 22 + rows * ROW_H + 9 + ROW_H + (error != null ? 30 : 0) + (am.adding() ? 30 : 0) + (!canSwitch ? 30 : 0) + 8;
        int w = Math.max(MENU_W, pillW);
        int x = pillX + pillW - w;
        int y = pillY + PILL_H + 6 + Math.round((1f - Render2D.easeOut(openAnim)) * -4);
        screen.hit("acct:menu", x, y, w, h, b -> {});
        Render2D.roundedRect(g, x + 1, y + 3, w, h, 8, 0x40000000);
        Render2D.panel(g, x, y, w, h, 8, MenuScreen.SURFACE, MenuScreen.BORDER_HOVER);
        int cy = y + 8;
        Fonts.draw(g, "Accounts", Fonts.Weight.MEDIUM, 11, x + 14, cy + (22 - Fonts.lineHeight(11)) / 2, MenuScreen.MUTED);
        cy += 22;
        if (list.isEmpty()) {
            Fonts.draw(g, am.loading() ? "Loading accounts…" : "No accounts in Shard Launcher", Fonts.Weight.REGULAR, 12, x + 14,
                    cy + (ROW_H - Fonts.lineHeight(12)) / 2, MenuScreen.MUTED);
            cy += ROW_H;
        }
        UUID current = am.currentUuid();
        for (AccountBridge.Account a : list) {
            String key = "acct:row:" + a.id();
            boolean active = current.equals(a.profileId());
            boolean switching = a.id().equals(am.switchingTo());
            boolean enabled = !active && am.canSwitch();
            boolean hover = enabled && screen.hovered(key, x + 6, cy, w - 12, ROW_H);
            if (enabled) screen.hit(key, x + 6, cy, w - 12, ROW_H, b -> am.switchTo(a));
            float hov = screen.hoverAnim(key, hover);
            if (hov > 0 || active) Render2D.roundedRect(g, x + 6, cy, w - 12, ROW_H, 6, active ? MenuScreen.ACTIVE : Colors.withAlpha(MenuScreen.ACTIVE, Math.round(255 * hov)));
            drawHead(g, a.profileId(), x + 14, cy + (ROW_H - 24) / 2, 24);
            int nameColor = enabled || active ? MenuScreen.TEXT : MenuScreen.SOFT;
            Fonts.drawClipped(g, a.name(), Fonts.Weight.MEDIUM, 13, x + 14 + 24 + 10, cy + (ROW_H - Fonts.lineHeight(13)) / 2, w - 24 - 24 - 10 - 70, nameColor);
            if (active) {
                Icons.draw(g, "check", x + w - 14 - 16, cy + (ROW_H - 16) / 2, 16, MenuScreen.SUCCESS);
            } else if (switching) {
                Fonts.drawRight(g, "Switching…", Fonts.Weight.REGULAR, 11, x + w - 14, cy + (ROW_H - Fonts.lineHeight(11)) / 2, MenuScreen.MUTED);
            }
            cy += ROW_H;
        }
        g.fill(x + 1, cy + 4, x + w - 1, cy + 5, MenuScreen.BORDER);
        cy += 9;
        boolean adding = am.adding();
        String addKey = "acct:add";
        boolean addHover = !adding && screen.hovered(addKey, x + 6, cy, w - 12, ROW_H);
        if (!adding) screen.hit(addKey, x + 6, cy, w - 12, ROW_H, b -> am.addAccount());
        float addHov = screen.hoverAnim(addKey, addHover);
        if (addHov > 0) Render2D.roundedRect(g, x + 6, cy, w - 12, ROW_H, 6, Colors.withAlpha(MenuScreen.ACTIVE, Math.round(255 * addHov)));
        Render2D.roundedRect(g, x + 14, cy + (ROW_H - 24) / 2, 24, 24, 6, MenuScreen.ACTIVE);
        Icons.draw(g, "user-plus", x + 14 + 4, cy + (ROW_H - 16) / 2, 16, Colors.mix(MenuScreen.SOFT, MenuScreen.TEXT, addHov));
        Fonts.draw(g, "Add account", Fonts.Weight.MEDIUM, 13, x + 14 + 24 + 10, cy + (ROW_H - Fonts.lineHeight(13)) / 2, adding ? MenuScreen.SOFT : MenuScreen.TEXT);
        cy += ROW_H;
        if (adding) cy = note(g, x, cy, w, "Finish signing in in Shard Launcher", MenuScreen.MUTED);
        if (!canSwitch) cy = note(g, x, cy, w, "Leave the world to switch accounts", MenuScreen.MUTED);
        if (error != null) note(g, x, cy, w, error, MenuScreen.DANGER);
    }

    private static int note(GuiGraphics g, int x, int cy, int w, String text, int color) {
        Fonts.drawClipped(g, text, Fonts.Weight.REGULAR, 11, x + 14, cy + (30 - Fonts.lineHeight(11)) / 2, w - 28, color);
        return cy + 30;
    }

    /** The player's face from vanilla's skin cache, or the default skin while it loads. */
    void drawHead(GuiGraphics g, UUID uuid, int x, int y, int size) {
        Minecraft mc = Minecraft.getInstance();
        PlayerSkin skin;
        try {
            if (uuid == null) uuid = mc.getUser().getProfileId();
            //? if >=1.21.9 {
            ResolvableProfile profile = profiles.computeIfAbsent(uuid, ResolvableProfile::createUnresolved);
            skin = mc.playerSkinRenderCache().getOrDefault(profile).playerSkin();
            //?} else {
            /*// Before 1.21.9: resolve the profile (textures) through the skull cache, then the skin manager.
            ResolvableProfile profile = profiles.computeIfAbsent(uuid, id -> new ResolvableProfile(java.util.Optional.empty(), java.util.Optional.of(id),
                    new com.mojang.authlib.properties.PropertyMap()));
            ResolvableProfile resolved = profile.pollResolve();
            if (resolved != null && resolved != profile) profiles.put(uuid, resolved);
            skin = resolved == null ? net.minecraft.client.resources.DefaultPlayerSkin.get(uuid) : mc.getSkinManager().getInsecureSkin(resolved.gameProfile());
            *///?}
        } catch (RuntimeException e) {
            skin = net.minecraft.client.resources.DefaultPlayerSkin.get(uuid);
        }
        Render2D.roundedRect(g, x - 1, y - 1, size + 2, size + 2, 3, MenuScreen.ACTIVE);
        //? if >=26.1 {
        /*PlayerFaceExtractor.extractRenderState(g, skin, x, y, size);
        *///?} else {
        PlayerFaceRenderer.draw(g, skin, x, y, size);
        //?}

    }
}
