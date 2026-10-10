package gg.shard.client.cosmetics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Ordering and labels for the in-game Cosmetics tab. Pure, unit-tested. */
public final class CosmeticsTab {
    private CosmeticsTab() {}

    /** The equip slots, in the order the tab shows them. */
    public static final List<String> SLOTS = List.of("cape", "shield", "bandana");

    /** "Cape", "Shield", "Bandana". */
    public static String slotLabel(String slot) {
        if (slot == null || slot.isEmpty()) return "";
        return slot.substring(0, 1).toUpperCase(Locale.ROOT) + slot.substring(1);
    }

    /** Rarer first, like the launcher's shop: special, mythic, legendary, epic, rare, uncommon, common, unknown. */
    public static int rarityRank(String rarity) {
        return switch (rarity == null ? "" : rarity) {
            case "special" -> 0;
            case "mythic" -> 1;
            case "legendary" -> 2;
            case "epic" -> 3;
            case "rare" -> 4;
            case "uncommon" -> 5;
            case "common" -> 6;
            default -> 7;
        };
    }

    /**
     * The items to show for a slot filter (null = every slot): owned ones first, then by slot
     * (cape, shield, bandana), rarity (rarest first) and name.
     */
    public static List<PlayerCosmetics.Item> list(Collection<PlayerCosmetics.Item> items, Set<String> owned, String slot) {
        List<PlayerCosmetics.Item> out = new ArrayList<>();
        for (PlayerCosmetics.Item item : items) if (slot == null || slot.equals(item.slot())) out.add(item);
        out.sort(Comparator.<PlayerCosmetics.Item>comparingInt(i -> owned.contains(i.id()) ? 0 : 1)
                .thenComparingInt(i -> SLOTS.indexOf(i.slot()) < 0 ? SLOTS.size() : SLOTS.indexOf(i.slot()))
                .thenComparingInt(i -> rarityRank(i.rarity()))
                .thenComparing(i -> i.name().toLowerCase(Locale.ROOT)));
        return out;
    }
}
