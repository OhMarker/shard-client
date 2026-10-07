package gg.shard.client.modules.hud;

import gg.shard.client.gui.Render2D;
import gg.shard.client.gui.Theme;
import gg.shard.client.hud.HudModule;
import gg.shard.client.input.ClickTracker;
import gg.shard.client.module.setting.BoolSetting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;

public final class CpsModule extends HudModule {
    private final BoolSetting background = add(new BoolSetting("Show background", "Dark backing behind the text", true));
    private final BoolSetting right = add(new BoolSetting("Right button", "Also show right-click CPS", true));

    public CpsModule() {
        super("CPS", "Clicks per second for the left and right mouse buttons.", 0.01, 0.14);
    }

    @Override
    public boolean defaultEnabled() {
        return true;
    }

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        String text = "CPS " + ClickTracker.left() + (right.get() ? " | " + ClickTracker.right() : "");
        int w = font().width(text) + 6;
        if (background.get()) Render2D.rounded(g, 0, 0, w, 12, 0x66000000);
        Render2D.text(g, font(), text, 3, 2, Theme.text(), true);
        size(w, 12);
    }

    @Override
    public String icon() {
        return "glyph:cps";
    }
}
