package gg.shard.client.modules.settings;

import gg.shard.client.module.Module;
import gg.shard.client.module.ModuleCategory;
import gg.shard.client.module.setting.BoolSetting;

/**
 * Settings for Shard's title screen and server list (gg.shard.client.gui.menu). Hidden, always-on
 * holder like {@link HudDefaultsModule}; the settings can be shown on Settings → Appearance.
 */
public final class MenuScreensModule extends Module {
    public final BoolSetting shardScreens = add(new BoolSetting("Shard title screen",
            "Shard's title screen and server list instead of vanilla's", true));
    public final BoolSetting realmsButton = add(new BoolSetting("Realms button",
            "Show Minecraft Realms on Shard's title screen", false));

    public MenuScreensModule() {
        super("Menu Screens", "Shard's title screen and server list.", ModuleCategory.UTILITY);
    }

    @Override
    public String about() {
        return "Replaces the title screen and the multiplayer server list with Shard's versions, which also "
                + "switch between your Shard Launcher accounts. Switch it off to get vanilla's screens back.";
    }

    @Override
    public boolean hidden() {
        return true;
    }

    @Override
    public boolean defaultEnabled() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void setEnabled(boolean value) {
        // Always on; the settings decide.
    }
}
