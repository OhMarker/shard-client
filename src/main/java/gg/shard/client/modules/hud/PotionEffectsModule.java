package gg.shard.client.modules.hud;

import gg.shard.client.gui.Render2D;
import gg.shard.client.gui.Theme;
import gg.shard.client.hud.HudModule;
import gg.shard.client.hud.HudStyle;
import gg.shard.client.module.setting.BoolSetting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class PotionEffectsModule extends HudModule {
    private final BoolSetting hideAmbient = add(new BoolSetting("Hide beacon effects", "Skip ambient effects from beacons and conduits", false));
    private final BoolSetting blink = add(new BoolSetting("Blink when ending", "Flash effects with under 5 seconds left", true));

    public PotionEffectsModule() {
        super("Potion Effects", "Active effects with amplifier and remaining time.", 0.86, 0.02);
    }

    @Override
    protected boolean hasAlignment() {
        return true;
    }

    @Override
    public String about() {
        return "Your active potion effects with their level and time left, the same list the inventory screen shows, sorted so the one ending first is on top.";
    }

    @Override
    public boolean defaultEnabled() {
        return true;
    }

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        LocalPlayer p = mc().player;
        if (p == null) return;
        List<MobEffectInstance> effects = new ArrayList<>(p.getActiveEffects());
        if (hideAmbient.get()) effects.removeIf(MobEffectInstance::isAmbient);
        effects.sort(Comparator.comparingInt(MobEffectInstance::getDuration));
        if (effects.isEmpty()) {
            size(1, 1);
            return;
        }
        HudStyle.Resolved st = style();
        int pad = st.padding();
        int widest = 0;
        List<String> lines = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (MobEffectInstance e : effects) {
            String name = e.getEffect().value().getDisplayName().getString();
            String amp = e.getAmplifier() > 0 ? " " + roman(e.getAmplifier() + 1) : "";
            String time = e.isInfiniteDuration() ? "∞" : formatTicks(e.getDuration());
            String line = name + amp + "  " + time;
            lines.add(line);
            boolean ending = blink.get() && !e.isInfiniteDuration() && e.getDuration() < 100;
            boolean hide = ending && (now / 300) % 2 == 0;
            colors.add(hide ? Theme.danger() : st.value());
            widest = Math.max(widest, textW(line));
        }
        int barW = 3;
        int innerW = barW + 6 + widest;
        int w = pad * 2 + innerW;
        int h = pad * 2 + lines.size() * lineH();
        box(g, st, w, h);
        for (int i = 0; i < lines.size(); i++) {
            int lineY = pad + i * lineH();
            int tint = 0xFF000000 | (effects.get(i).getEffect().value().getColor() & 0xFFFFFF);
            int x = alignX(st, pad, innerW, barW + 6 + textW(lines.get(i)));
            Render2D.fill(g, x, lineY + 2, barW, lineH() - 4, tint);
            text(g, st, lines.get(i), x + barW + 6, lineY, colors.get(i));
        }
        size(w, h);
    }

    static String formatTicks(int ticks) {
        int seconds = ticks / 20;
        if (seconds >= 3600) return (seconds / 3600) + "h" + ((seconds % 3600) / 60) + "m";
        return (seconds / 60) + ":" + String.format("%02d", seconds % 60);
    }

    static String roman(int n) {
        return switch (n) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(n);
        };
    }

    @Override
    public String icon() {
        return "item:potion";
    }
}
