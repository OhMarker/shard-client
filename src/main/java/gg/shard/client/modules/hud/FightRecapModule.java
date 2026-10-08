package gg.shard.client.modules.hud;

import gg.shard.client.combat.CombatTracker;
import gg.shard.client.combat.FightLog;
import gg.shard.client.gui.Theme;
import gg.shard.client.hud.HudModule;
import gg.shard.client.module.setting.IntSetting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Fight Recap: after a kill, a death or a fight that went quiet, a short summary for a few seconds. */
public final class FightRecapModule extends HudModule {
    private final IntSetting show = add(new IntSetting("Show for", "How long the recap stays", 10, 3, 30, 1, " s"));

    public FightRecapModule() {
        super("Fight Recap", "After a kill or death: hits, damage, pops, crystals and fight length.", 0.01, 0.50);
    }

    @Override
    protected boolean hasAlignment() {
        return true;
    }

    @Override
    public String icon() {
        return "recap";
    }

    @Override
    public String about() {
        return "When a fight ends (a kill, your death, or 15 seconds of quiet) shows how it went: length, hits dealt and taken, damage taken, totem pops on both sides, "
                + "crystals you placed, your best reach and combo. Built from your own actions and the events every client receives.";
    }

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        FightLog.Recap r = CombatTracker.LOG.lastRecap();
        if (r == null || System.currentTimeMillis() - CombatTracker.LOG.lastRecapAt() > show.get() * 1000L) {
            size(1, 1);
            return;
        }
        String head = switch (r.result()) {
            case KILL -> "Kill" + (r.opponent().isEmpty() ? "" : " - " + r.opponent());
            case DEATH -> "Death" + (r.opponent().isEmpty() ? "" : " - " + r.opponent());
            case ENDED -> "Fight ended";
        };
        long s = r.durationMs() / 1000;
        List<String> lines = new ArrayList<>();
        lines.add(head);
        lines.add(String.format(Locale.ROOT, "%d:%02d  -  %d hits, %d taken", s / 60, s % 60, r.hitsDealt(), r.hitsTaken()));
        lines.add(String.format(Locale.ROOT, "%.0f damage taken  -  %d crystals", r.damageTaken(), r.crystals()));
        lines.add(String.format(Locale.ROOT, "Pops %d you / %d them", r.popsYou(), r.popsThem()));
        lines.add(String.format(Locale.ROOT, "Best reach %.2f  -  combo %d", r.bestReach(), r.bestCombo()));
        int headColor = r.result() == FightLog.Result.KILL ? Theme.success() : r.result() == FightLog.Result.DEATH ? Theme.danger() : style().value();
        List<Integer> colors = new ArrayList<>(List.of(headColor, style().value(), style().value(), style().value(), style().value()));
        lines(g, lines, colors);
    }
}
