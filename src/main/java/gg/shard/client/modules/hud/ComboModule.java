package gg.shard.client.modules.hud;

import gg.shard.client.combat.CombatTracker;
import gg.shard.client.hud.HudModule;
import gg.shard.client.module.setting.BoolSetting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;

/** Combo: your hits in a row since you were last hit. */
public final class ComboModule extends HudModule {
    private final BoolSetting hideZero = add(new BoolSetting("Hide at zero", "Only show while you have a combo going", true));

    public ComboModule() {
        super("Combo", "Hits in a row; resets when you get hit.", 0.44, 0.58);
    }

    @Override
    protected String defaultLabel() {
        return "Combo";
    }

    @Override
    public String icon() {
        return "combo";
    }

    @Override
    public String about() {
        return "Counts the hits you land in a row and resets when you take damage. Counted from the attacks you make and your own health.";
    }

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        int c = CombatTracker.LOG.combo();
        if (c == 0 && hideZero.get()) {
            size(1, 1);
            return;
        }
        line(g, String.valueOf(c), 0);
    }
}
