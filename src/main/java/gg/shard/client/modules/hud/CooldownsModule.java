package gg.shard.client.modules.hud;

import gg.shard.client.gui.Render2D;
import gg.shard.client.hud.HudModule;
import gg.shard.client.hud.HudStyle;
import gg.shard.client.module.setting.BoolSetting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/** Cooldowns: pearls, wind charges, chorus fruit and your shield, from vanilla's item cooldowns. */
public final class CooldownsModule extends HudModule {
    private static final ItemStack[] ITEMS = {new ItemStack(Items.ENDER_PEARL), new ItemStack(Items.WIND_CHARGE), new ItemStack(Items.CHORUS_FRUIT),
            new ItemStack(Items.SHIELD), new ItemStack(Items.GOLDEN_APPLE), new ItemStack(Items.ENCHANTED_GOLDEN_APPLE)};
    private final BoolSetting hideIdle = add(new BoolSetting("Hide when ready", "Only show items that are cooling down", true));

    public CooldownsModule() {
        super("Cooldowns", "Ender pearl, wind charge, chorus fruit and shield cooldowns.", 0.53, 0.80);
    }

    @Override
    public String icon() {
        return "timer";
    }

    @Override
    public String about() {
        return "The item cooldowns vanilla already tracks (the grey sweep in your hotbar), shown as a compact strip so you see them without looking down: "
                + "ender pearls, wind charges, chorus fruit, your shield after an axe hit, and apples on servers that add a cooldown.";
    }

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        LocalPlayer p = mc().player;
        if (p == null) return;
        float pt = delta.getGameTimeDeltaPartialTick(true);
        List<ItemStack> shown = new ArrayList<>();
        List<Float> left = new ArrayList<>();
        for (ItemStack s : ITEMS) {
            float f = p.getCooldowns().getCooldownPercent(s, pt);
            if (f <= 0 && hideIdle.get()) continue;
            if (f <= 0 && p.getInventory().countItem(s.getItem()) == 0) continue;
            shown.add(s);
            left.add(f);
        }
        if (shown.isEmpty()) {
            size(1, 1);
            return;
        }
        HudStyle.Resolved st = style();
        int pad = st.padding();
        int cell = 18;
        int w = pad * 2 + shown.size() * (cell + 4) - 4;
        int h = pad * 2 + cell + 3;
        box(g, st, w, h);
        for (int i = 0; i < shown.size(); i++) {
            int x = pad + i * (cell + 4);
            g.renderItem(shown.get(i), x + 1, pad);
            float f = left.get(i);
            Render2D.fill(g, x + 1, pad + cell, 16, 2, 0x55000000);
            if (f > 0) Render2D.fill(g, x + 1, pad + cell, Math.max(1, Math.round(16 * f)), 2, 0xFFF59E0B);
            else Render2D.fill(g, x + 1, pad + cell, 16, 2, 0xFF4ADE80);
        }
        size(w, h);
    }
}
