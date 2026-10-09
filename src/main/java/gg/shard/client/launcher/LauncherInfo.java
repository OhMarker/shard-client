package gg.shard.client.launcher;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import gg.shard.client.ShardClient;
import gg.shard.client.util.Colors;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * launcher-info.json written by Shard Launcher into the game directory before each launch.
 * Everything is optional: running from any other launcher yields {@link #DEFAULTS}.
 * Format: CONTRACT.md in the launcher repository.
 */
public record LauncherInfo(
        boolean present,
        String launcherVersion,
        String accountId,
        String username,
        String minecraftVersion,
        String instanceName,
        String shardBuild,
        int accent,
        String theme,
        Path equippedPath,
        Path sharedConfigPath,
        /** Shard Launcher's account bridge (docs/ACCOUNT-SWITCH-API.md); null hides account switching. */
        AccountBridge.Endpoint accountBridge
) {
    public static final int DEFAULT_ACCENT = 0xFF22D3EE;
    public static final LauncherInfo DEFAULTS =
            new LauncherInfo(false, null, null, null, null, null, null, DEFAULT_ACCENT, "dark", null, null, null);

    public static LauncherInfo load(Path gameDir) {
        Path file = gameDir.resolve("launcher-info.json");
        if (!Files.isRegularFile(file)) return DEFAULTS;
        try {
            String text = Files.readString(file, StandardCharsets.UTF_8);
            JsonElement parsed = JsonParser.parseString(text);
            if (!parsed.isJsonObject()) return DEFAULTS;
            return fromJson(parsed.getAsJsonObject());
        } catch (IOException | RuntimeException e) {
            ShardClient.LOGGER.warn("launcher-info.json could not be read; using defaults", e);
            return DEFAULTS;
        }
    }

    static LauncherInfo fromJson(JsonObject o) {
        Integer accent = Colors.parseHex(str(o, "accent"));
        String equipped = str(o, "equippedPath");
        String shared = str(o, "sharedConfigPath");
        return new LauncherInfo(
                true,
                str(o, "launcherVersion"),
                str(o, "accountId"),
                str(o, "username"),
                str(o, "minecraftVersion"),
                str(o, "instanceName"),
                str(o, "shardBuild"),
                accent == null ? DEFAULT_ACCENT : (0xFF << 24) | (accent & 0xFFFFFF),
                "light".equalsIgnoreCase(str(o, "theme")) ? "light" : "dark",
                equipped == null ? null : Path.of(equipped),
                shared == null ? null : Path.of(shared),
                AccountBridge.Endpoint.parse(o.get("accountBridge"))
        );
    }

    private static String str(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isString() ? e.getAsString() : null;
    }
}
