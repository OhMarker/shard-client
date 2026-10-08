package gg.shard.client.cosmetics;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/** Parsing for the meta cosmetics catalogue and player uuids. Pure, unit-tested. */
public final class PlayerCosmetics {
    private PlayerCosmetics() {}

    static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    /** Cosmetic id to https texture URL for back items (cape, cloak); other entries are skipped. */
    public static Map<String, String> parseCatalogue(String json) {
        return parseCatalogue(json, false);
    }

    /** As {@link #parseCatalogue(String)}; dev runs may also allow http://127.0.0.1 (smoke test). */
    public static Map<String, String> parseCatalogue(String json, boolean allowLocalHttp) {
        Map<String, String> out = new HashMap<>();
        JsonElement root = JsonParser.parseString(json);
        if (!root.isJsonObject()) return out;
        JsonElement list = root.getAsJsonObject().get("cosmetics");
        if (list == null || !list.isJsonArray()) return out;
        for (JsonElement item : list.getAsJsonArray()) {
            if (!item.isJsonObject()) continue;
            JsonObject o = item.getAsJsonObject();
            String id = string(o, "id");
            String type = string(o, "type");
            String url = string(o, "textureUrl");
            if (id == null || !SAFE_ID.matcher(id).matches()) continue;
            if (!"cape".equals(type) && !"cloak".equals(type)) continue;
            if (url == null) continue;
            String lower = url.toLowerCase(Locale.ROOT);
            if (!lower.startsWith("https://") && !(allowLocalHttp && lower.startsWith("http://127.0.0.1:"))) continue;
            out.put(id, url);
        }
        return out;
    }

    static UUID parseUuid(String raw) {
        String hex = raw.replace("-", "").toLowerCase(Locale.ROOT);
        if (!hex.matches("[0-9a-f]{32}")) return null;
        return new UUID(Long.parseUnsignedLong(hex.substring(0, 16), 16), Long.parseUnsignedLong(hex.substring(16), 16));
    }

    private static String string(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isString() ? e.getAsString() : null;
    }
}
