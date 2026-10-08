package gg.shard.client.modules.hud;

import gg.shard.client.hud.HudModule;
import gg.shard.client.module.setting.BoolSetting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public final class ClockModule extends HudModule {
    private static final DateTimeFormatter H24 = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter H12 = DateTimeFormatter.ofPattern("h:mm a");

    private final BoolSetting twelveHour = add(new BoolSetting("12-hour", "Use 12-hour time with AM/PM", false));

    public ClockModule() {
        super("Clock", "Real-world time.", 0.93, 0.95);
    }

    @Override
    protected String defaultLabel() {
        return "";
    }

    @Override
    public String about() {
        return "Your computer's clock, so you can keep track of time without leaving the game.";
    }

    @Override
    public boolean needsPlayer() {
        return false;
    }

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        line(g, LocalTime.now().format(twelveHour.get() ? H12 : H24), 0);
    }

    @Override
    public String icon() {
        return "item:clock";
    }
}
