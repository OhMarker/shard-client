package gg.shard.client.util;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Parses user-typed, comma-separated registry ids ("ender_pearl, minecraft:tnt"). No Minecraft imports. */
public final class ItemIds {
    private ItemIds() {}

    /** Normalised ids in the order typed, without duplicates; invalid tokens are dropped. */
    public static List<String> parse(String text) {
        Set<String> out = new LinkedHashSet<>();
        if (text == null) return new ArrayList<>();
        for (String raw : text.split("[,\\s]+")) {
            String id = normalize(raw);
            if (id != null) out.add(id);
        }
        return new ArrayList<>(out);
    }

    /** "Ender_Pearl" → "minecraft:ender_pearl"; null when the token is not a valid id. */
    public static String normalize(String raw) {
        if (raw == null) return null;
        String t = raw.trim().toLowerCase(Locale.ROOT);
        if (t.isEmpty()) return null;
        if (!t.contains(":")) t = "minecraft:" + t;
        return t.matches("[a-z0-9_.-]+:[a-z0-9_./-]+") ? t : null;
    }
}
