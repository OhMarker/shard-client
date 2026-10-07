package gg.shard.client.modules.hud;

import gg.shard.client.gui.Render2D;
import gg.shard.client.gui.Theme;
import gg.shard.client.hud.HudModule;
import gg.shard.client.module.setting.BoolSetting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class PotionEffectsModule extends HudModule {
    private final BoolSetting background = add(new BoolSetting("Background", "Dark backing behind the list", true));
    private final BoolSetting hideAmbient = add(new BoolSetting("Hide beacon effects", "Skip ambient effects from beacons and conduits", false));
    private final BoolSetting blink = add(new BoolSetting("Blink when ending", "Flash effects with under 5 seconds left", true));

    public PotionEffectsModule() {
        super("Potion Effects", "Active effects with amplifier and remaining time.", 0.86, 0.02);
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
        int w = 0;
        List<String> lines = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        for (MobEffectInstance e : effects) {
            String name = e.getEffect().value().getDisplayName().getString();
            String amp = e.getAmplifier() > 0 ? " " + roman(e.getAmplifier() + 1) : "";
            String time = e.isInfiniteDuration() ? "∞" : formatTicks(e.getDuration());
            String line = name + amp + "  " + time;
            lines.add(line);
            colors.add(0xFF000000 | (e.getEffect().value().getColor() & 0xFFFFFF));
            w = Math.max(w, font().width(line));
        }
        int h = lines.size() * 11 + 4;
        w += 12;
        if (background.get()) Render2D.rounded(g, 0, 0, w, h, 0x66000000);
        long now = System.currentTimeMillis();
        for (int i = 0; i < lines.size(); i++) {
            MobEffectInstance e = effects.get(i);
            boolean ending = blink.get() && !e.isInfiniteDuration() && e.getDuration() < 100;
            boolean hide = ending && (now / 300) % 2 == 0;
            Render2D.fill(g, 3, 4 + i * 11, 3, 7, colors.get(i));
            Render2D.text(g, font(), lines.get(i), 9, 3 + i * 11, hide ? Theme.danger() : Theme.text(), true);
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
}
