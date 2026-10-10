package gg.shard.client.gui.menu;

import gg.shard.client.ShardClient;
import gg.shard.client.mixin.JoinMultiplayerScreenAccessor;
import gg.shard.client.modules.settings.MenuScreensModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;

/** Decides when Shard's title screen and server list stand in for vanilla's (MinecraftScreenSwapMixin). */
public final class MenuScreens {
    private MenuScreens() {}

    public static boolean enabled() {
        return ShardClient.isReady() && ShardClient.modules().get(MenuScreensModule.class).shardScreens.get();
    }

    /** Shard's version of {@code screen} when it is exactly vanilla's title screen or server list. */
    public static Screen replace(Screen screen) {
        if (screen == null) {
            // setScreen(null) with no world makes vanilla build its own TitleScreen inside the
            // method, past this hook (cancelling Create World from Singleplayer does this).
            if (enabled() && Minecraft.getInstance().level == null) return new ShardTitleScreen();
            return null;
        }
        if (!enabled()) return screen;
        try {
            if (screen.getClass() == TitleScreen.class) return new ShardTitleScreen();
            if (screen.getClass() == JoinMultiplayerScreen.class) {
                return new ShardMultiplayerScreen(((JoinMultiplayerScreenAccessor) screen).shard$lastScreen());
            }
        } catch (RuntimeException e) {
            ShardClient.LOGGER.error("Shard menu screens: falling back to vanilla", e);
        }
        return screen;
    }
}
