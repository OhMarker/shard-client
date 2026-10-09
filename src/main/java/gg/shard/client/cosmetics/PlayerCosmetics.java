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

    /** A catalogue entry the client can draw: its slot type (cape, cloak, shield, bandana) and texture URL. */
    public record Item(String id, String type, String textureUrl) {
        /** The equip slot: capes and cloaks share the back slot "cape". */
        public String slot() {
            return "cloak".equals(type) ? "cape" : type;
        }
    }

    /** Types the client renders; bundles (and anything newer) are ignored. */
    static final java.util.Set<String> DRAWN_TYPES = java.util.Set.of("cape", "cloak", "shield", "bandana");

    /**
     * cosmetics-v2.json (or the v1 file, which has the same shape): every cape, cloak, shield and
     * bandana with an allowed texture URL. Dev runs may also allow http://127.0.0.1 and file: URLs.
     */
    public static Map<String, Item> parseCatalogueV2(String json, boolean allowLocal) {
        Map<String, Item> out = new HashMap<>();
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
            if (id == null || !SAFE_ID.matcher(id).matches() || type == null || !DRAWN_TYPES.contains(type) || url == null) continue;
            if (!allowedUrl(url, allowLocal)) continue;
            out.put(id, new Item(id, type, url));
        }
        return out;
    }

    static boolean allowedUrl(String url, boolean allowLocal) {
        String lower = url.toLowerCase(Locale.ROOT);
        if (lower.startsWith("https://")) return true;
        return allowLocal && (lower.startsWith("http://127.0.0.1:") || lower.startsWith("file:/"));
    }

    /**
     * Dev catalogue read from a local file: a texture URL on the meta repository becomes the same
     * path next to that file when it exists there (the art is not pushed yet); others stay as they are.
     */
    public static String localTexture(String url, String metaBase, String catalogueFileUrl, java.util.function.Predicate<String> exists) {
        if (url == null || !url.startsWith(metaBase) || !catalogueFileUrl.startsWith("file:")) return url;
        int slash = catalogueFileUrl.lastIndexOf('/');
        if (slash < 0) return url;
        String local = catalogueFileUrl.substring(0, slash + 1) + url.substring(metaBase.length());
        try {
            return exists.test(local) ? local : url;
        } catch (RuntimeException e) {
            return url;
        }
    }

    /** What one player wears in each slot (null = nothing). */
    public record Equipped(String cape, String shield, String bandana) {
        public static final Equipped NONE = new Equipped(null, null, null);

        public boolean empty() {
            return cape == null && shield == null && bandana == null;
        }

        public String slot(String slot) {
            return switch (slot) {
                case "cape", "cloak" -> cape;
                case "shield" -> shield;
                case "bandana" -> bandana;
                default -> null;
            };
        }
    }

    /**
     * The {@code players} object of {@code GET /v1/equipped}: with {@code v=2} each value is
     * {@code {cape, shield, bandana}}; an older server answers with the cape id string, which is
     * read as a cape only. Unsafe ids are dropped; players wearing nothing are left out.
     */
    public static Map<UUID, Equipped> parseEquipped(JsonObject players) {
        Map<UUID, Equipped> out = new HashMap<>();
        if (players == null) return out;
        for (Map.Entry<String, JsonElement> e : players.entrySet()) {
            UUID uuid = parseUuid(e.getKey());
            if (uuid == null) continue;
            Equipped eq = parseSlots(e.getValue());
            if (!eq.empty()) out.put(uuid, eq);
        }
        return out;
    }

    /** One player's slots: an object with cape/shield/bandana, or a bare cape id string. */
    public static Equipped parseSlots(JsonElement value) {
        if (value == null || value.isJsonNull()) return Equipped.NONE;
        if (value.isJsonPrimitive()) return new Equipped(safe(value), null, null);
        if (!value.isJsonObject()) return Equipped.NONE;
        JsonObject o = value.getAsJsonObject();
        String cape = safe(o.get("cape"));
        if (cape == null) cape = safe(o.get("cloak"));
        return new Equipped(cape, safe(o.get("shield")), safe(o.get("bandana")));
    }

    private static String safe(JsonElement e) {
        if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isString()) return null;
        String id = e.getAsString();
        return SAFE_ID.matcher(id).matches() ? id : null;
    }

    /**
     * Null when a {@code w}x{@code h} picture fits the slot: capes and cloaks use vanilla's 64x32
     * cape layout, shields vanilla's 64x64 shield layout, bandanas are square art; every side a
     * multiple of 64 and at most {@code maxWidth}.
     */
    public static String layoutProblem(String type, int w, int h, int maxWidth) {
        if (w <= 0 || h <= 0 || w % 64 != 0 || w > maxWidth) {
            return type + " textures must be 64 to " + maxWidth + " px wide in steps of 64, got " + w + "x" + h;
        }
        return switch (type) {
            case "cape", "cloak" -> w == h * 2 ? null : "cape textures use the 64x32 layout, got " + w + "x" + h;
            case "shield", "bandana" -> w == h ? null : type + " textures are square, got " + w + "x" + h;
            default -> "unknown cosmetic type " + type;
        };
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
