package gg.shard.client.modules.hud;

import gg.shard.client.hud.HudModule;
import gg.shard.client.input.ClickTracker;
import gg.shard.client.module.setting.BoolSetting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;

public final class CpsModule extends HudModule {
    private final BoolSetting right = add(new BoolSetting("Right button", "Also show right-click CPS after a divider", true));

    public CpsModule() {
        super("CPS", "Clicks per second for the left and right mouse buttons.", 0.01, 0.14);
    }

    @Override
    protected String defaultLabel() {
        return "CPS";
    }

    @Override
    public String about() {
        return "Counts your own clicks over the last second, left and right. It only counts; it never clicks for you.";
    }

    @Override
    public boolean defaultEnabled() {
        return true;
    }

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        line(g, ClickTracker.left() + (right.get() ? " | " + ClickTracker.right() : ""), 0);
    }

    @Override
    public String icon() {
        return "glyph:cps";
    }
}
