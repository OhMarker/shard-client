package gg.shard.client.modules.hud;

import gg.shard.client.gui.Render2D;
import gg.shard.client.gui.Theme;
import gg.shard.client.hud.HudModule;
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
    private final BoolSetting background = add(new BoolSetting("Background", "Dark backing behind the items", true));

    public ArmorStatusModule() {
        super("Armor Status", "Durability of your armor and held items, with a low-durability warning.", 0.01, 0.40);
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
        boolean vertical = layout.get() == Layout.VERTICAL;
        int rowH = 18;
        int labelW = 34;
        int w = vertical ? 18 + labelW : items.size() * 18 + 4;
        int h = vertical ? items.size() * rowH + 2 : 18 + 10;
        if (background.get()) Render2D.rounded(g, 0, 0, w, h, 0x66000000);

        int i = 0;
        for (ItemStack stack : items) {
            int x = vertical ? 2 : 2 + i * 18;
            int y = vertical ? 1 + i * rowH : 1;
            if (!stack.isEmpty()) {
                g.renderItem(stack, x, y);
                g.renderItemDecorations(font(), stack, x, y);
                if (stack.isDamaged() || stack.getMaxDamage() > 0) {
                    int max = stack.getMaxDamage();
                    int left = max - stack.getDamageValue();
                    double frac = max == 0 ? 1 : (double) left / max;
                    int color = frac * 100 < warnAt.get() ? Theme.danger() : Colors.health(frac);
                    String label = percent.get() || max == 0 ? Math.round(frac * 100) + "%" : String.valueOf(left);
                    if (vertical) Render2D.text(g, font(), label, x + 19, y + 4, color, true);
                    else Render2D.text(g, font(), label, x - 1, y + 18, color, true);
                }
            } else {
                Render2D.rounded(g, x + 2, y + 2, 12, 12, 0x22FFFFFF);
            }
            i++;
        }
        size(w, h);
    }
}
