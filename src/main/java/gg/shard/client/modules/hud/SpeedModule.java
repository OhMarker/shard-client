package gg.shard.client.modules.hud;

import gg.shard.client.hud.HudModule;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;

import java.util.Locale;

/** Speed: how fast you move horizontally, in blocks per second. */
public final class SpeedModule extends HudModule {
    private double lastX = Double.NaN;
    private double lastZ;
    private double speed;

    public SpeedModule() {
        super("Speed", "Your horizontal speed in blocks per second.", 0.01, 0.30);
    }

    @Override
    protected String defaultLabel() {
        return "Speed";
    }

    @Override
    public String icon() {
        return "speed";
    }

    @Override
    public String about() {
        return "Your horizontal speed in blocks per second, from how far you moved each tick. Informational only.";
    }

    @Override
    public void onTick() {
        LocalPlayer p = mc().player;
        if (p == null) return;
        if (!Double.isNaN(lastX)) {
            double d = Math.hypot(p.getX() - lastX, p.getZ() - lastZ) * 20;
            speed = speed * 0.7 + d * 0.3;
        }
        lastX = p.getX();
        lastZ = p.getZ();
    }

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        line(g, String.format(Locale.ROOT, "%.2f b/s", speed), 0);
    }
}
