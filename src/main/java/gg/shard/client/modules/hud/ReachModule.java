package gg.shard.client.modules.hud;

import gg.shard.client.combat.CombatTracker;
import gg.shard.client.hud.HudModule;
import gg.shard.client.module.setting.IntSetting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Locale;

/** Reach Display: how far your last hit was, eye to hitbox. Display only. */
public final class ReachModule extends HudModule {
    private final IntSetting hideAfter = add(new IntSetting("Hide after", "Seconds after a hit before it hides (0 keeps it)", 3, 0, 30, 1, " s"));

    public ReachModule() {
        super("Reach", "How far your last hit was.", 0.44, 0.61);
    }

    @Override
    protected String defaultLabel() {
        return "Reach";
    }

    @Override
    public String icon() {
        return "reach";
    }

    @Override
    public String about() {
        return "The distance of your last hit, measured like vanilla from your eyes to the target's hitbox using positions the client already has. "
                + "It only displays the number; reach itself is vanilla.";
    }

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        double r = CombatTracker.LOG.lastReach();
        long age = System.currentTimeMillis() - CombatTracker.LOG.lastReachAt();
        if (r < 0 || (hideAfter.get() > 0 && age > hideAfter.get() * 1000L)) {
            size(1, 1);
            return;
        }
        line(g, String.format(Locale.ROOT, "%.2f m", r), 0);
    }
}
