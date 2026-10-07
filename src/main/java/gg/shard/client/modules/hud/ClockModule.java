package gg.shard.client.modules.hud;

import gg.shard.client.gui.Render2D;
import gg.shard.client.gui.Theme;
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
    private final BoolSetting background = add(new BoolSetting("Background", "Dark backing", true));

    public ClockModule() {
        super("Clock", "Real-world time.", 0.93, 0.95);
    }

    @Override
    public boolean needsPlayer() {
        return false;
    }

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        String text = LocalTime.now().format(twelveHour.get() ? H12 : H24);
        int w = font().width(text) + 6;
        if (background.get()) Render2D.rounded(g, 0, 0, w, 12, 0x66000000);
        Render2D.text(g, font(), text, 3, 2, Theme.text(), true);
        size(w, 12);
    }
}
