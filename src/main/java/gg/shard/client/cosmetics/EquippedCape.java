package gg.shard.client.cosmetics;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The cape (or cloak) the player equipped in Shard Launcher. The launcher writes
 * {@code equipped.json} and caches every equipped back texture as
 * {@code <cosmetics>/textures/<id>.png} next to it (CONTRACT.md, sections 4 and 5).
 */
public record EquippedCape(String id, Path texture) {

    /** Reads equipped.json; null when nothing is equipped on the back or the file is unusable. */
    public static EquippedCape read(Path equippedJson) {
        if (equippedJson == null || !Files.isRegularFile(equippedJson)) return null;
        try {
            JsonElement root = JsonParser.parseString(Files.readString(equippedJson, StandardCharsets.UTF_8));
            String id = backItemId(root);
            return id == null ? null : new EquippedCape(id, texturePath(equippedJson, id));
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    /**
     * Every slot in equipped.json ({@code cape}/{@code cloak}, and {@code shield}/{@code bandana}
     * once the launcher writes them); {@link PlayerCosmetics.Equipped#NONE} when unusable.
     */
    public static PlayerCosmetics.Equipped readSlots(Path equippedJson) {
        if (equippedJson == null || !Files.isRegularFile(equippedJson)) return PlayerCosmetics.Equipped.NONE;
        try {
            JsonElement root = JsonParser.parseString(Files.readString(equippedJson, StandardCharsets.UTF_8));
            if (!root.isJsonObject()) return PlayerCosmetics.Equipped.NONE;
            return PlayerCosmetics.parseSlots(root.getAsJsonObject().get("equipped"));
        } catch (IOException | RuntimeException e) {
            return PlayerCosmetics.Equipped.NONE;
        }
    }

    /** Where the launcher caches the texture of {@code id} next to equipped.json. */
    public static Path textureFor(Path equippedJson, String id) {
        return texturePath(equippedJson, id);
    }

    /** {@code equipped.cape}, else {@code equipped.cloak} (the launcher never sets both). */
    static String backItemId(JsonElement root) {
        if (root == null || !root.isJsonObject()) return null;
        JsonElement equipped = root.getAsJsonObject().get("equipped");
        if (equipped == null || !equipped.isJsonObject()) return null;
        JsonObject slots = equipped.getAsJsonObject();
        for (String slot : new String[]{"cape", "cloak"}) {
            JsonElement value = slots.get(slot);
            if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                String id = value.getAsString();
                if (!id.isBlank()) return id;
            }
        }
        return null;
    }

    /** Where the launcher caches the texture: same file-name rule as its CosmeticAssets.cachePath. */
    static Path texturePath(Path equippedJson, String id) {
        Path dir = equippedJson.toAbsolutePath().getParent();
        return dir.resolve("textures").resolve(safeId(id) + ".png");
    }

    static String safeId(String id) {
        return id.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    /** A texture identifier path segment: lower case, only [a-z0-9_.-]. */
    public String resourceName() {
        return safeId(id).toLowerCase(java.util.Locale.ROOT);
    }
}
