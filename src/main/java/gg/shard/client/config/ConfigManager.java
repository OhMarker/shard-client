package gg.shard.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import gg.shard.client.ShardClient;
import gg.shard.client.module.Module;
import gg.shard.client.module.ModuleManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * JSON persistence for modules, HUD layout and GUI panel positions. Writes are atomic
 * (temp file + move) and debounced: callers mark dirty, the tick loop flushes.
 */
public final class ConfigManager {
    public static final int VERSION = 1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final Path dir;
    private final Path file;
    private final Path profilesDir;
    private final ModuleManager modules;
    private JsonObject gui = new JsonObject();
    private boolean dirty;
    private long dirtySince;

    public ConfigManager(Path configDir, ModuleManager modules) {
        this.dir = configDir.resolve("shard");
        this.file = dir.resolve("config.json");
        this.profilesDir = dir.resolve("profiles");
        this.modules = modules;
    }

    public Path directory() {
        return dir;
    }

    /** Opaque GUI state (panel positions) owned by the click GUI. */
    public JsonObject gui() {
        return gui;
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
            writeTo(file);
        } catch (IOException e) {
            ShardClient.LOGGER.error("Could not save config", e);
        }
    }

    public List<String> profiles() {
        List<String> out = new ArrayList<>();
        if (!Files.isDirectory(profilesDir)) return out;
        try (Stream<Path> files = Files.list(profilesDir)) {
            files.filter(p -> p.getFileName().toString().endsWith(".json"))
                    .forEach(p -> out.add(p.getFileName().toString().replaceFirst("\\.json$", "")));
        } catch (IOException e) {
            ShardClient.LOGGER.warn("Could not list profiles", e);
        }
        out.sort(String::compareToIgnoreCase);
        return out;
    }

    public boolean saveProfile(String name) {
        String safe = safeName(name);
        if (safe == null) return false;
        try {
            writeTo(profilesDir.resolve(safe + ".json"));
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

    /** Letters, digits, '-' and '_' only; anything else is rejected rather than silently renamed. */
    private static String safeName(String name) {
        String s = name.trim();
        return s.matches("[A-Za-z0-9_-]{1,32}") ? s : null;
    }

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
        int version = root.has("version") ? root.get("version").getAsInt() : 0;
        if (version > VERSION) {
            ShardClient.LOGGER.warn("Config version {} is newer than this client ({}); loading what we understand", version, VERSION);
        }
        JsonObject moduleStates = root.has("modules") && root.get("modules").isJsonObject() ? root.getAsJsonObject("modules") : new JsonObject();
        for (Module m : modules.all()) {
            JsonElement state = moduleStates.get(m.key());
            if (state != null && state.isJsonObject()) m.load(state.getAsJsonObject());
            else m.setEnabled(m.defaultEnabled());
        }
        gui = root.has("gui") && root.get("gui").isJsonObject() ? root.getAsJsonObject("gui") : new JsonObject();
    }

    private void writeTo(Path path) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("version", VERSION);
        JsonObject moduleStates = new JsonObject();
        for (Module m : modules.all()) moduleStates.add(m.key(), m.save());
        root.add("modules", moduleStates);
        root.add("gui", gui);
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

    @SuppressWarnings("unused")
    private static void debugDump(Map<String, JsonElement> values) {
        values.forEach((k, v) -> ShardClient.LOGGER.debug("{} = {}", k, v));
    }
}
