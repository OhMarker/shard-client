package gg.shard.client.modules.hud;

import gg.shard.client.gui.Render2D;
import gg.shard.client.gui.Theme;
import gg.shard.client.hud.HudModule;
import gg.shard.client.hud.HudStyle;
import gg.shard.client.module.setting.BoolSetting;
import gg.shard.client.module.setting.EnumSetting;
import gg.shard.client.module.setting.IntSetting;
import gg.shard.client.util.Colors;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class ArmorStatusModule extends HudModule {
    public enum Layout { VERTICAL, HORIZONTAL }

    private final EnumSetting<Layout> layout = add(new EnumSetting<>("Layout", "Stack pieces vertically or in a row", Layout.VERTICAL));
    private final BoolSetting hands = add(new BoolSetting("Held items", "Include main hand and offhand", true));
    private final BoolSetting percent = add(new BoolSetting("Percent", "Show durability as a percentage instead of points", false));
    private final IntSetting warnAt = add(new IntSetting("Warn below", "Turn red under this durability percentage", 20, 5, 60, 5, "%"));
    private final BoolSetting bars = add(new BoolSetting("Durability bars", "A thin coloured bar under each piece", true));
    private final BoolSetting flashLow = add(new BoolSetting("Flash when low", "Pulse a piece that is below the warning percentage", true));
    private final BoolSetting hideEmpty = add(new BoolSetting("Hide empty slots", "Skip slots with nothing in them instead of showing a placeholder", true));

    public ArmorStatusModule() {
        super("Armor", "Durability of your armour and held items, with low-durability warnings.", 0.01, 0.40);
    }

    @Override
    protected String legacyKey() {
        return "armor-status";
    }

    @Override
    public String about() {
        return "Your own armour pieces and held items with their remaining durability, from your inventory. Other players' gear is not shown.";
    }

    @Override
    public boolean defaultEnabled() {
        return true;
    }

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        LocalPlayer p = mc().player;
        if (p == null) return;
        List<ItemStack> items = new ArrayList<>();
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            items.add(p.getItemBySlot(slot));
        }
        if (hands.get()) {
            items.add(p.getItemBySlot(EquipmentSlot.MAINHAND));
            items.add(p.getItemBySlot(EquipmentSlot.OFFHAND));
        }
        if (hideEmpty.get()) items.removeIf(ItemStack::isEmpty);
        if (items.isEmpty()) {
            size(1, 1);
            return;
        }
        HudStyle.Resolved st = style();
        int pad = st.padding();
        boolean vertical = layout.get() == Layout.VERTICAL;
        int icon = 16;
        int rowH = Math.max(icon + 2, lineH() + 2);
        int labelW = Math.max(36, textW("100%"));
        int w = vertical ? pad * 2 + icon + 6 + labelW : pad * 2 + items.size() * (icon + 6) - 6;
        int h = vertical ? pad * 2 + items.size() * rowH - 2 : pad * 2 + icon + 2 + lineH();
        box(g, st, w, h);

        int i = 0;
        for (ItemStack stack : items) {
            int x = vertical ? pad : pad + i * (icon + 6);
            int y = vertical ? pad + i * rowH : pad;
            if (!stack.isEmpty()) {
                g.renderItem(stack, x, y);
                if (stack.isDamaged() || stack.getMaxDamage() > 0) {
                    int max = stack.getMaxDamage();
                    int left = max - stack.getDamageValue();
                    double frac = max == 0 ? 1 : (double) left / max;
                    boolean low = frac * 100 < warnAt.get();
                    int color = low ? Theme.danger() : Colors.health(frac);
                    if (low && flashLow.get() && (System.currentTimeMillis() / 300) % 2 == 0) {
                        Render2D.roundedRect(g, x - 1, y - 1, icon + 2, icon + 2, 3, Colors.withAlpha(Theme.danger(), 0x70));
                        g.renderItem(stack, x, y);
                    }
                    if (bars.get() && max > 0) {
                        Render2D.fill(g, x + 1, y + icon, icon - 2, 1, 0x80000000);
                        Render2D.fill(g, x + 1, y + icon, Math.max(1, (int) Math.round((icon - 2) * frac)), 1, color);
                    }
                    String label = percent.get() || max == 0 ? Math.round(frac * 100) + "%" : String.valueOf(left);
                    if (vertical) text(g, st, label, x + icon + 6, y + (icon - lineH()) / 2, color);
                    else text(g, st, label, x + (icon - textW(label)) / 2, y + icon + 2, color);
                }
            } else {
                Render2D.roundedRect(g, x + 2, y + 2, icon - 4, icon - 4, 3, Colors.withAlpha(st.text(), 0x22));
            }
            i++;
        }
        size(w, h);
    }

    @Override
    public String icon() {
        return "armor";
    }
}
