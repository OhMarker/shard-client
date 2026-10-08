package gg.shard.client.modules.hud;

import gg.shard.client.gui.Theme;
import gg.shard.client.hud.HudModule;
import gg.shard.client.hud.HudStyle;
import gg.shard.client.module.setting.BoolSetting;
import gg.shard.client.module.setting.EnumSetting;
import gg.shard.client.module.setting.StringSetting;
import gg.shard.client.util.ItemIds;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/** Counts the crystal-PvP kit: crystals, obsidian, gapples, totems, anchors, glowstone, pearls, xp, plus any items you type in. */
public final class ItemCounterModule extends HudModule {
    public enum Layout { ROW, COLUMN }

    /** Below this many the count turns yellow: a crystal fight's "time to refill" points. */
    static int lowAt(Item item) {
        if (item == Items.END_CRYSTAL || item == Items.OBSIDIAN || item == Items.EXPERIENCE_BOTTLE) return 16;
        if (item == Items.GLOWSTONE) return 8;
        if (item == Items.RESPAWN_ANCHOR || item == Items.ENCHANTED_GOLDEN_APPLE) return 4;
        if (item == Items.TOTEM_OF_UNDYING || item == Items.ENDER_PEARL) return 3;
        return 0;
    }

    private final EnumSetting<Layout> layout = add(new EnumSetting<>("Layout", "Row or column", Layout.ROW));
    private final BoolSetting lowWarnings = add(new BoolSetting("Low warnings", "Yellow when you are running low: under 16 crystals, obsidian or XP, 8 glowstone, 4 anchors or gapples, 3 totems or pearls", true));
    private final BoolSetting hideEmpty = add(new BoolSetting("Hide empty", "Skip items you have none of", false));
    private final BoolSetting crystals = add(new BoolSetting("Crystals", "End crystals", true).group("Items"));
    private final BoolSetting obsidian = add(new BoolSetting("Obsidian", "Obsidian blocks", true).group("Items"));
    private final BoolSetting totems = add(new BoolSetting("Totems", "Totems of undying", true).group("Items"));
    private final BoolSetting gapples = add(new BoolSetting("Gapples", "Enchanted golden apples", true).group("Items"));
    private final BoolSetting anchors = add(new BoolSetting("Anchors", "Respawn anchors", true).group("Items"));
    private final BoolSetting glowstone = add(new BoolSetting("Glowstone", "Glowstone blocks", true).group("Items"));
    private final BoolSetting pearls = add(new BoolSetting("Pearls", "Ender pearls", true).group("Items"));
    private final BoolSetting xp = add(new BoolSetting("XP bottles", "Bottles o' enchanting", false).group("Items"));
    private final StringSetting custom = add(new StringSetting("Custom items", "Extra item ids, separated by commas", "", 200).group("Items")
            .details("Example: golden_apple, minecraft:crossbow, arrow. Unknown ids are ignored and the field turns the count red."));

    private String parsedFor;
    private final List<Item> customItems = new ArrayList<>();

    public ItemCounterModule() {
        super("Item Counter", "How much of your crystal kit you have left.", 0.30, 0.80);
    }

    @Override
    public String about() {
        return "Counts the chosen items across your own inventory, hotbar and offhand. Add any item by id in the Custom items field; nothing is ever moved or used for you.";
    }

    @Override
    public boolean defaultEnabled() {
        return true;
    }

    private List<Item> customItems() {
        String text = custom.get();
        if (!text.equals(parsedFor)) {
            customItems.clear();
            for (String id : ItemIds.parse(text)) {
                Identifier location = Identifier.tryParse(id);
                if (location == null) continue;
                BuiltInRegistries.ITEM.getOptional(location).ifPresent(customItems::add);
            }
            parsedFor = text;
        }
        return customItems;
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
        for (Item extra : customItems()) if (!items.contains(extra)) items.add(extra);

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
        HudStyle.Resolved st = style();
        int pad = st.padding();
        boolean row = layout.get() == Layout.ROW;
        int icon = 16;
        int cellW = icon + 4 + Math.max(textW("64"), 14);
        int cellH = Math.max(icon, lineH());
        int w = row ? pad * 2 + shown.size() * (cellW + 6) - 6 : pad * 2 + cellW;
        int h = row ? pad * 2 + cellH : pad * 2 + shown.size() * (cellH + 2) - 2;
        box(g, st, w, h);
        for (int i = 0; i < shown.size(); i++) {
            int x = row ? pad + i * (cellW + 6) : pad;
            int y = row ? pad : pad + i * (cellH + 2);
            g.renderItem(new ItemStack(shown.get(i)), x, y + (cellH - icon) / 2);
            String label = String.valueOf(counts.get(i));
            int c = counts.get(i);
            int color = c == 0 ? Theme.danger() : lowWarnings.get() && c < lowAt(shown.get(i)) ? Theme.warning() : st.value();
            text(g, st, label, x + icon + 4, y + (cellH - lineH()) / 2, color);
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

    @Override
    public String icon() {
        return "item-counter";
    }
}
