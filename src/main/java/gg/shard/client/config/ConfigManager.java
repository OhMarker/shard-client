package gg.shard.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import gg.shard.client.ShardClient;
import gg.shard.client.module.Module;
import gg.shard.client.module.ModuleManager;
import gg.shard.client.module.setting.Setting;
import gg.shard.client.server.ServerBlacklist;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * JSON persistence for modules, HUD layout, server rules and GUI state. Writes are atomic
 * (temp file + move) and debounced: callers mark dirty, the tick loop flushes.
 *
 * <p>Schema history: 1 (0.1.0), 2 (0.2.0: merged modules, "show-background"), 3 (0.3.0: HUD
 * style group, hidden appearance/HUD-defaults modules, profile descriptions). Every module gets
 * the file's version while loading so renamed keys migrate (see {@link Module#migrateSetting}).
 */
public final class ConfigManager {
    public static final int VERSION = Module.CURRENT_CONFIG_VERSION;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    /** A saved profile: file name plus the optional description stored inside it. */
    public record ProfileInfo(String name, String description) {}

    private final Path dir;
    private final Path file;
    private final Path profilesDir;
    private final ModuleManager modules;
    private JsonObject gui = new JsonObject();
    private boolean dirty;
    private long dirtySince;
    private int loadedVersion = VERSION;

    public ConfigManager(Path configDir, ModuleManager modules) {
        this.dir = configDir.resolve("shard");
        this.file = dir.resolve("config.json");
        this.profilesDir = dir.resolve("profiles");
        this.modules = modules;
    }

    public Path directory() {
        return dir;
    }

    /** Opaque GUI state (last category, panel widths) owned by the settings screen. */
    public JsonObject gui() {
        return gui;
    }

    /** Schema version of the file that was last loaded (for the log and the About box). */
    public int loadedVersion() {
        return loadedVersion;
    }

    public void markDirty() {
        if (!dirty) dirtySince = System.currentTimeMillis();
        dirty = true;
    }

    /** Called every tick; writes at most once per second while changes keep coming. */
    public void flushIfDirty() {
        if (dirty && System.currentTimeMillis() - dirtySince > 1000) save();
    }

    public void load() {
        loadFrom(file);
    }

    public synchronized void save() {
        dirty = false;
        try {
            writeTo(file, null);
        } catch (IOException e) {
            ShardClient.LOGGER.error("Could not save config", e);
        }
    }

    // ---- profiles ---------------------------------------------------------------------------

    public List<String> profiles() {
        List<String> out = new ArrayList<>();
        for (ProfileInfo p : profileInfos()) out.add(p.name());
        return out;
    }

    public List<ProfileInfo> profileInfos() {
        List<ProfileInfo> out = new ArrayList<>();
        if (!Files.isDirectory(profilesDir)) return out;
        try (Stream<Path> files = Files.list(profilesDir)) {
            files.filter(p -> p.getFileName().toString().endsWith(".json")).forEach(p -> {
                String name = p.getFileName().toString().replaceFirst("\\.json$", "");
                out.add(new ProfileInfo(name, readDescription(p)));
            });
        } catch (IOException e) {
            ShardClient.LOGGER.warn("Could not list profiles", e);
        }
        out.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
        return out;
    }

    private static String readDescription(Path p) {
        try {
            JsonElement parsed = JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8));
            if (parsed.isJsonObject() && parsed.getAsJsonObject().has("description")) {
                JsonElement d = parsed.getAsJsonObject().get("description");
                if (d.isJsonPrimitive()) return d.getAsString();
            }
        } catch (IOException | RuntimeException ignored) {
            // A broken profile still lists; loading it reports the problem.
        }
        return "";
    }

    public boolean saveProfile(String name) {
        return saveProfile(name, "");
    }

    public boolean saveProfile(String name, String description) {
        String safe = safeName(name);
        if (safe == null) return false;
        try {
            writeTo(profilesDir.resolve(safe + ".json"), description == null ? "" : description.trim());
            return true;
        } catch (IOException e) {
            ShardClient.LOGGER.error("Could not save profile {}", safe, e);
            return false;
        }
    }

    public boolean loadProfile(String name) {
        String safe = safeName(name);
        if (safe == null) return false;
        Path p = profilesDir.resolve(safe + ".json");
        if (!Files.isRegularFile(p)) return false;
        loadFrom(p);
        markDirty();
        return true;
    }

    public boolean deleteProfile(String name) {
        String safe = safeName(name);
        if (safe == null) return false;
        try {
            return Files.deleteIfExists(profilesDir.resolve(safe + ".json"));
        } catch (IOException e) {
            ShardClient.LOGGER.warn("Could not delete profile {}", safe, e);
            return false;
        }
    }

    /** Letters, digits, '-' and '_' only; anything else is rejected rather than silently renamed. */
    public static String safeName(String name) {
        String s = name == null ? "" : name.trim();
        return s.matches("[A-Za-z0-9_-]{1,32}") ? s : null;
    }

    // ---- export / import / reset ------------------------------------------------------------

    /** The whole config as pretty JSON (what Settings → Export copies to the clipboard). */
    public String exportJson() {
        return GSON.toJson(snapshotRoot(null));
    }

    /**
     * Replaces the current config with {@code json} (what Settings → Import pastes). Returns an
     * error message, or null on success; the current config is untouched on failure.
     */
    public String importJson(String json) {
        JsonObject root;
        try {
            JsonElement parsed = JsonParser.parseString(json == null ? "" : json.trim());
            if (!parsed.isJsonObject()) return "That is not a Shard config (expected a JSON object)";
            root = parsed.getAsJsonObject();
        } catch (RuntimeException e) {
            return "That is not valid JSON";
        }
        if (!root.has("modules") || !root.get("modules").isJsonObject()) return "That JSON has no \"modules\" section";
        applyRoot(root);
        markDirty();
        return null;
    }

    /** Every module back to its defaults, keybinds cleared, HUD positions reset, server rules kept. */
    public void resetAll() {
        for (Module m : modules.all()) {
            for (Setting<?> s : m.settings()) s.reset();
            m.setKeybind(gg.shard.client.util.Keys.NONE);
            m.setEnabled(m.defaultEnabled());
            m.resetExtra();
        }
        gui = new JsonObject();
        markDirty();
    }

    // ---- file i/o ---------------------------------------------------------------------------

    private void loadFrom(Path path) {
        if (!Files.isRegularFile(path)) {
            for (Module m : modules.all()) m.setEnabled(m.defaultEnabled());
            return;
        }
        JsonObject root;
        try {
            String text = Files.readString(path, StandardCharsets.UTF_8);
            JsonElement parsed = JsonParser.parseString(text);
            if (!parsed.isJsonObject()) throw new IOException("root is not an object");
            root = parsed.getAsJsonObject();
        } catch (Exception e) {
            ShardClient.LOGGER.error("Config {} is unreadable; quarantining and using defaults", path, e);
            quarantine(path);
            for (Module m : modules.all()) m.setEnabled(m.defaultEnabled());
            return;
        }
        applyRoot(root);
    }

    private void applyRoot(JsonObject root) {
        int version = root.has("version") && root.get("version").isJsonPrimitive() ? root.get("version").getAsInt() : 0;
        loadedVersion = version;
        if (version > VERSION) {
            ShardClient.LOGGER.warn("Config version {} is newer than this client ({}); loading what we understand", version, VERSION);
        } else if (version < VERSION) {
            ShardClient.LOGGER.info("Migrating config from schema {} to {}", version, VERSION);
        }
        JsonObject moduleStates = root.has("modules") && root.get("modules").isJsonObject() ? root.getAsJsonObject("modules") : new JsonObject();
        for (Module m : modules.all()) {
            JsonElement state = moduleStates.get(m.key());
            if (state != null && state.isJsonObject()) m.load(state.getAsJsonObject(), version);
            else {
                for (Setting<?> s : m.settings()) s.reset();
                m.setEnabled(m.defaultEnabled());
            }
        }
        gui = root.has("gui") && root.get("gui").isJsonObject() ? root.getAsJsonObject("gui") : new JsonObject();
        modules.setBlacklist(ServerBlacklist.fromJson(root.get("servers")));
        if (version < VERSION) markDirty();
    }

    private JsonObject snapshotRoot(String description) {
        JsonObject root = new JsonObject();
        root.addProperty("version", VERSION);
        if (description != null) root.addProperty("description", description);
        JsonObject moduleStates = new JsonObject();
        for (Module m : modules.all()) moduleStates.add(m.key(), m.save());
        root.add("modules", moduleStates);
        root.add("servers", modules.blacklist().toJson());
        root.add("gui", gui);
        return root;
    }

    private void writeTo(Path path, String description) throws IOException {
        JsonObject root = snapshotRoot(description);
        Files.createDirectories(path.getParent());
        Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
        Files.writeString(tmp, GSON.toJson(root), StandardCharsets.UTF_8);
        try {
            Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void quarantine(Path path) {
        try {
            Files.move(path, path.resolveSibling(path.getFileName() + ".corrupt-" + System.currentTimeMillis()));
        } catch (IOException ignored) {
            // best effort
        }
    }

    /** Exposed for tests. */
    public static JsonObject snapshot(ModuleManager modules) {
        JsonObject out = new JsonObject();
        for (Module m : modules.all()) out.add(m.key(), m.save());
        return out;
    }
}
