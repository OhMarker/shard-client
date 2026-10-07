package gg.shard.client.modules.hud;

import gg.shard.client.gui.Render2D;
import gg.shard.client.gui.Theme;
import gg.shard.client.hud.HudModule;
import gg.shard.client.module.setting.BoolSetting;
import gg.shard.client.module.setting.EnumSetting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/** Counts the crystal-PvP kit: crystals, obsidian, gapples, totems, anchors, glowstone, pearls, xp. */
public final class ItemCounterModule extends HudModule {
    public enum Layout { ROW, COLUMN }

    private final EnumSetting<Layout> layout = add(new EnumSetting<>("Layout", "Row or column", Layout.ROW));
    private final BoolSetting hideEmpty = add(new BoolSetting("Hide empty", "Skip items you have none of", false));
    private final BoolSetting background = add(new BoolSetting("Background", "Dark backing", true));
    private final BoolSetting crystals = add(new BoolSetting("Crystals", "", true));
    private final BoolSetting obsidian = add(new BoolSetting("Obsidian", "", true));
    private final BoolSetting totems = add(new BoolSetting("Totems", "", true));
    private final BoolSetting gapples = add(new BoolSetting("Gapples", "Enchanted golden apples", true));
    private final BoolSetting anchors = add(new BoolSetting("Anchors", "Respawn anchors", true));
    private final BoolSetting glowstone = add(new BoolSetting("Glowstone", "", true));
    private final BoolSetting pearls = add(new BoolSetting("Pearls", "", true));
    private final BoolSetting xp = add(new BoolSetting("XP bottles", "", false));

    public ItemCounterModule() {
        super("Item Counter", "How much of your crystal kit you have left.", 0.30, 0.80);
    }

    @Override
    public boolean defaultEnabled() {
        return true;
    }

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        LocalPlayer p = mc().player;
        if (p == null) return;
        Inventory inv = p.getInventory();
        List<Item> items = new ArrayList<>();
        if (crystals.get()) items.add(Items.END_CRYSTAL);
        if (obsidian.get()) items.add(Items.OBSIDIAN);
        if (totems.get()) items.add(Items.TOTEM_OF_UNDYING);
        if (gapples.get()) items.add(Items.ENCHANTED_GOLDEN_APPLE);
        if (anchors.get()) items.add(Items.RESPAWN_ANCHOR);
        if (glowstone.get()) items.add(Items.GLOWSTONE);
        if (pearls.get()) items.add(Items.ENDER_PEARL);
        if (xp.get()) items.add(Items.EXPERIENCE_BOTTLE);

        List<Item> shown = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();
        for (Item item : items) {
            int c = count(inv, item);
            if (c == 0 && hideEmpty.get()) continue;
            shown.add(item);
            counts.add(c);
        }
        if (shown.isEmpty()) {
            size(1, 1);
            return;
        }
        boolean row = layout.get() == Layout.ROW;
        int cell = 30;
        int w = row ? shown.size() * cell + 4 : cell + 16;
        int h = row ? 20 : shown.size() * 18 + 4;
        if (background.get()) Render2D.rounded(g, 0, 0, w, h, 0x66000000);
        for (int i = 0; i < shown.size(); i++) {
            int x = row ? 2 + i * cell : 2;
            int y = row ? 2 : 2 + i * 18;
            g.renderItem(new ItemStack(shown.get(i)), x, y);
            String label = String.valueOf(counts.get(i));
            int color = counts.get(i) == 0 ? Theme.danger() : Theme.text();
            Render2D.text(g, font(), label, x + 17, y + 5, color, true);
        }
        size(w, h);
    }

    static int count(Inventory inv, Item item) {
        int n = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(item)) n += s.getCount();
        }
        return n;
    }
}
