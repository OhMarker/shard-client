package gg.shard.client.modules.hud;

import gg.shard.client.gui.Render2D;
import gg.shard.client.gui.Theme;
import gg.shard.client.hud.HudModule;
import gg.shard.client.module.setting.BoolSetting;
import gg.shard.client.module.setting.ColorSetting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;

public final class FpsModule extends HudModule {
    private final BoolSetting background = add(new BoolSetting("Show background", "Dark backing behind the text", true));
    private final BoolSetting label = add(new BoolSetting("Label", "Show the FPS prefix", true));
    private final ColorSetting color = add(new ColorSetting("Color", "Text colour", 0xFFE8ECF4));

    public FpsModule() {
        super("FPS", "Frames per second.", 0.01, 0.02);
    }

    @Override
    public boolean defaultEnabled() {
        return true;
    }

    @Override
    public boolean needsPlayer() {
        return false;
    }

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        String text = (label.get() ? "FPS " : "") + mc().getFps();
        int w = font().width(text) + 6;
        int h = 12;
        if (background.get()) Render2D.rounded(g, 0, 0, w, h, 0x66000000);
        Render2D.text(g, font(), text, 3, 2, color.get(), true);
        size(w, h);
    }

    static int accentOrText(boolean accent) {
        return accent ? Theme.accent() : Theme.text();
    }

    @Override
    public String icon() {
        return "glyph:fps";
    }
}
