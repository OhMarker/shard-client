package gg.shard.client.modules.hud;

import gg.shard.client.gui.Theme;
import gg.shard.client.hud.HudModule;
import gg.shard.client.util.TpsEstimator;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Locale;

/** TPS: the server's tick rate, estimated from the time packets it sends every second. */
public final class TpsModule extends HudModule {
    public static final TpsEstimator ESTIMATOR = new TpsEstimator();

    public TpsModule() {
        super("TPS", "Server tick rate, so you can tell server lag from your own.", 0.01, 0.26);
    }

    @Override
    protected String defaultLabel() {
        return "TPS";
    }

    @Override
    public String icon() {
        return "performance";
    }

    @Override
    public String about() {
        return "Estimates the server's ticks per second from the time update every server sends once a second. Under 20 means the server is lagging, "
                + "which explains rubber-banding that is not your connection. Nothing extra is sent.";
    }

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        if (!ESTIMATOR.known()) {
            line(g, "--", 0);
            return;
        }
        double t = ESTIMATOR.tps();
        int c = t >= 19.5 ? Theme.success() : t >= 15 ? Theme.warning() : Theme.danger();
        line(g, String.format(Locale.ROOT, "%.1f", t), c);
    }
}
