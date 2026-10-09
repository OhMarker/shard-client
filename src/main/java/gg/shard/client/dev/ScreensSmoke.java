package gg.shard.client.dev;

import gg.shard.client.ShardClient;
import gg.shard.client.account.AccountManager;
import gg.shard.client.gui.menu.MenuScreen;
import gg.shard.client.gui.menu.ShardMultiplayerScreen;
import gg.shard.client.gui.menu.ShardTitleScreen;
import gg.shard.client.launcher.AccountBridge;
import gg.shard.client.modules.settings.MenuScreensModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.network.chat.Component;

/**
 * {@code -PsmokeOnly=screens}: Shard's title screen, account switcher and server list, plus the
 * vanilla fallback. Runs outside a world (leaves one first when -PquickPlay joined it). Needs
 * {@code -PfakeBridge} for the account steps and a server for the ping (the smoke server's address
 * comes from {@code shard.smoke.server}). Ends by joining that server from Shard's list.
 */
final class ScreensSmoke {
    private ScreensSmoke() {}

    private static int outTicks;
    private static int t = -1;
    private static String server;
    private static boolean joined;
    private static int inWorldTicks;

    static void tick(Minecraft mc) {
        if (mc.screen instanceof AccessibilityOnboardingScreen) {
            mc.options.onboardAccessibility = false;
            mc.options.save();
            mc.setScreen(new TitleScreen());
            outTicks = 0;
            return;
        }
        if (t < 0) {
            mc.options.pauseOnLostFocus = false;
            if (mc.getOverlay() != null) return;
            if (mc.level != null) {
                ShardClient.LOGGER.info("Smoke screens: leaving the world first");
                mc.disconnectFromWorld(Component.literal("Smoke: screens pass"));
                mc.setScreen(new TitleScreen());
                outTicks = 0;
                return;
            }
            if (++outTicks < 60) return;
            t = 0;
            server = System.getProperty("shard.smoke.server", "localhost:25599");
        }
        if (joined) {
            if (mc.level != null && mc.player != null) {
                if (++inWorldTicks == 60) {
                    SmokeTest.SUMMARY.addProperty("screensJoinedFromList", true);
                    SmokeTest.SUMMARY.addProperty("screensCanSwitchInWorld", AccountManager.get().canSwitch());
                    SmokeTest.finish(mc);
                }
            } else if (++t > 2000) {
                SmokeTest.SUMMARY.addProperty("screensJoinedFromList", false);
                SmokeTest.finish(mc);
            }
            return;
        }
        step(mc, t++);
    }

    /** A control to click as soon as a frame has drawn it (the dev window can render slowly). */
    private static String pendingClick;

    private static void click(String key) {
        pendingClick = key;
    }

    private static void step(Minecraft mc, int t) {
        MenuScreensModule settings = ShardClient.modules().get(MenuScreensModule.class);
        if (pendingClick != null && mc.screen instanceof MenuScreen m && m.clickHit(pendingClick)) {
            ShardClient.LOGGER.info("Smoke screens: clicked {}", pendingClick);
            pendingClick = null;
        }
        switch (t) {
            case 0 -> {
                // The unfocused dev window counts as idle; keep full frame rate so hovers animate between ticks.
                mc.options.inactivityFpsLimit().set(net.minecraft.client.InactivityFpsLimit.MINIMIZED);
                settings.shardScreens.set(true);
                ServerList list = new ServerList(mc);
                list.load();
                if (list.get(server) == null) list.add(new ServerData("Shard smoke server", server, ServerData.Type.OTHER), false);
                if (list.get("localhost:1") == null) list.add(new ServerData("Offline example", "localhost:1", ServerData.Type.OTHER), false);
                list.save();
                mc.setScreen(new TitleScreen());
                SmokeTest.SUMMARY.addProperty("screensTitleIsShard", mc.screen instanceof ShardTitleScreen);
                SmokeTest.SUMMARY.addProperty("screensBridge", AccountManager.get().available());
            }
            case 2, 58, 116, 148, 236, 324 -> SmokeTest.parkCursor(mc);
            case 40 -> SmokeTest.shot(mc, "screens-title.png", null);
            case 42 -> pointAt(mc, "multiplayer");
            case 56 -> {
                ShardClient.LOGGER.info("Smoke screens: multiplayer hover {}", menu(mc).animValue("multiplayer:hov"));
                SmokeTest.shot(mc, "screens-title-hover.png", null);
            }
            case 60 -> menu(mc).toggleAccounts();
            case 74 -> SmokeTest.shot(mc, "screens-title-accounts.png", null);
            case 76 -> pointAt(mc, "acct:row:acc-2");
            case 90 -> SmokeTest.shot(mc, "screens-title-accounts-hover.png", null);
            case 92 -> click("acct:row:acc-2");
            case 114 -> {
                SmokeTest.SUMMARY.addProperty("screensSwitchedTo", mc.getUser().getName());
                SmokeTest.SUMMARY.addProperty("screensSwitchedUuid", mc.getUser().getProfileId().toString());
                if (menu(mc).accountsOpen()) menu(mc).toggleAccounts();
            }
            case 120 -> SmokeTest.shot(mc, "screens-title-switched.png", null);
            case 122 -> menu(mc).toggleAccounts();
            case 126 -> click("acct:row:acc-3"); // "Expired": the launcher answers 409
            case 144 -> {
                SmokeTest.SUMMARY.addProperty("screensExpiredError", AccountManager.get().error());
                SmokeTest.shot(mc, "screens-title-error.png", null);
            }
            case 146 -> click("acct:add");
            case 152 -> SmokeTest.shot(mc, "screens-title-adding.png", null);
            case 200 -> {
                SmokeTest.SUMMARY.addProperty("screensAccountsAfterAdd", AccountManager.get().accounts().size());
                SmokeTest.shot(mc, "screens-title-added.png", null);
            }
            case 202 -> {
                // Back to the dev account, which the smoke server has as operator.
                for (AccountBridge.Account a : AccountManager.get().accounts()) if (a.id().equals("acc-1")) AccountManager.get().switchTo(a);
            }
            case 216 -> {
                if (menu(mc) != null && menu(mc).accountsOpen()) menu(mc).toggleAccounts();
                setScale(mc, 3);
            }
            case 230 -> SmokeTest.shot(mc, "screens-title-guiscale3.png", null);
            case 232 -> setScale(mc, 0);
            case 234 -> {
                SmokeTest.SUMMARY.addProperty("screensBackTo", mc.getUser().getName());
                mc.setScreen(new JoinMultiplayerScreen(mc.screen));
                SmokeTest.SUMMARY.addProperty("screensMultiplayerIsShard", mc.screen instanceof ShardMultiplayerScreen);
            }
            case 290 -> {
                if (mc.screen instanceof ShardMultiplayerScreen s) SmokeTest.SUMMARY.addProperty("screensPings", s.pingSummary());
                SmokeTest.shot(mc, "screens-multiplayer.png", null);
            }
            case 292 -> {
                if (mc.screen instanceof ShardMultiplayerScreen s) s.selectServer(0);
                pointAt(mc, "server:1");
            }
            case 306 -> SmokeTest.shot(mc, "screens-multiplayer-selected.png", null);
            case 308 -> menu(mc).toggleAccounts();
            case 320 -> SmokeTest.shot(mc, "screens-multiplayer-accounts.png", null);
            case 322 -> {
                menu(mc).toggleAccounts();
                if (mc.screen instanceof ShardMultiplayerScreen s) s.showLan();
            }
            case 336 -> SmokeTest.shot(mc, "screens-multiplayer-lan.png", null);
            case 338 -> {
                settings.shardScreens.set(false);
                mc.setScreen(new TitleScreen());
                SmokeTest.SUMMARY.addProperty("screensFallbackIsVanilla", mc.screen != null && mc.screen.getClass() == TitleScreen.class);
            }
            case 360 -> SmokeTest.shot(mc, "screens-vanilla-title.png", null);
            case 362 -> mc.setScreen(new JoinMultiplayerScreen(mc.screen));
            case 400 -> SmokeTest.shot(mc, "screens-vanilla-multiplayer.png", null);
            case 402 -> {
                settings.shardScreens.set(true);
                mc.setScreen(new TitleScreen());
            }
            case 404 -> mc.setScreen(new JoinMultiplayerScreen(mc.screen));
            case 456 -> {
                if (mc.screen instanceof ShardMultiplayerScreen s) s.selectServer(0);
            }
            case 460 -> {
                if (mc.screen instanceof ShardMultiplayerScreen s) {
                    ShardClient.LOGGER.info("Smoke screens: joining {} from Shard's list", s.serverList().get(0).ip);
                    click("join");
                }
            }
            case 470 -> joined = pendingClick == null;
            case 520 -> {
                SmokeTest.SUMMARY.addProperty("screensJoinedFromList", false);
                SmokeTest.finish(mc);
            }
            default -> {
            }
        }
    }

    private static MenuScreen menu(Minecraft mc) {
        if (mc.screen instanceof MenuScreen m) return m;
        ShardClient.LOGGER.error("Smoke screens: no Shard menu screen open ({})", mc.screen == null ? "none" : mc.screen.getClass().getSimpleName());
        return null;
    }

    /** Puts the pointer on the control registered as {@code key}. */
    private static void pointAt(Minecraft mc, String key) {
        MenuScreen m = menu(mc);
        int[] c = m == null ? null : m.hitCentre(key);
        if (c == null) {
            ShardClient.LOGGER.warn("Smoke screens: no control {}", key);
            return;
        }
        double px = c[0] * m.pixelsPerUnitNow();
        double py = c[1] * m.pixelsPerUnitNow();
        org.lwjgl.glfw.GLFW.glfwSetCursorPos(mc.getWindow().handle(), px, py);
        ((gg.shard.client.mixin.MouseHandlerInvoker) mc.mouseHandler).shard$onMove(mc.getWindow().handle(), px, py);
    }

    private static void setScale(Minecraft mc, int scale) {
        mc.options.guiScale().set(scale);
        mc.resizeDisplay();
    }
}
