package gg.shard.client.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import gg.shard.client.gui.ClickGuiScreen;

/** Mod Menu entrypoint: the "Configure" button opens the Shard click GUI. */
public final class ShardModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ClickGuiScreen::new;
    }
}
