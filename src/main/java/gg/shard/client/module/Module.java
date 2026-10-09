package gg.shard.client.module;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import gg.shard.client.module.setting.Setting;
import gg.shard.client.util.Keys;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * A toggleable feature. Pure Java on purpose: modules declare settings and react to
 * enable/disable/tick; Minecraft-facing work happens in subclasses and mixins that consult
 * the module's state.
 *
 * <p>A module can be switched on by the user yet still be inactive: when another installed mod
 * already provides the feature ({@link #blockedBy()}) or when the current server is on the
 * module's blacklist ({@link #isSuppressed()}). {@link #isEnabled()} is the effective state that
 * mixins and the HUD consult; {@link #isToggledOn()} is the user's intent shown by the switch.
 */
public abstract class Module {
    private final String name;
    private final String description;
    private final ModuleCategory category;
    private final List<Setting<?>> settings = new ArrayList<>();
    private boolean enabled;
    private int keybind = Keys.NONE;
    private String blockedBy;
    private boolean suppressed;
    private Consumer<Module> changeListener = m -> {};

    protected Module(String name, String description, ModuleCategory category) {
        this.name = Objects.requireNonNull(name);
        this.description = Objects.requireNonNull(description);
        this.category = Objects.requireNonNull(category);
    }

    public String name() {
        return name;
    }

    /** Stable config/command key, e.g. "armor-status". Renamed modules keep their old key via {@link #legacyKey}. */
    public String key() {
        String legacy = legacyKey();
        return legacy != null ? legacy : name.toLowerCase().replace(' ', '-');
    }

    /**
     * The key from before a rename (0.4.0 shortened several names), so configs, keybinds, server
     * rules, favorites and HUD presets keep pointing at the module. Null for modules never renamed.
     */
    protected String legacyKey() {
        return null;
    }

    public String description() {
        return description;
    }

    public ModuleCategory category() {
        return category;
    }

    /**
     * Icon shown on the module card: {@code item:<minecraft item id>} for a vanilla item or
     * {@code glyph:<name>} for one of Shard's built-in 16x16 glyphs. Null uses the category's
     * glyph.
     */
    public String icon() {
        return null;
    }

    /**
     * Fabric mod ids that implement the same behaviour. When one is loaded this module stays
     * inactive so nothing is applied twice; the settings screen explains why.
     */
    public List<String> conflictingMods() {
        return List.of();
    }

    public List<Setting<?>> settings() {
        return Collections.unmodifiableList(settings);
    }

    public Setting<?> setting(String key) {
        for (Setting<?> s : settings) if (s.key().equalsIgnoreCase(key) || s.name().equalsIgnoreCase(key)) return s;
        return null;
    }

    protected <S extends Setting<?>> S add(S setting) {
        settings.add(setting);
        setting.onChange(v -> changeListener.accept(this));
        return setting;
    }

    /** Effective state: switched on, not shadowed by another mod, not blacklisted here. */
    public boolean isEnabled() {
        return enabled && blockedBy == null && !suppressed;
    }

    /** What the user's switch says, regardless of mods or server rules. */
    public boolean isToggledOn() {
        return enabled;
    }

    public void setEnabled(boolean value) {
        if (!value && alwaysOn()) return;
        if (enabled == value) return;
        boolean wasActive = isEnabled();
        enabled = value;
        try {
            transition(wasActive, isEnabled());
        } finally {
            changeListener.accept(this);
        }
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    /** Name of the installed mod that shadows this module, or null. */
    public String blockedBy() {
        return blockedBy;
    }

    public void setBlockedBy(String modName) {
        if (Objects.equals(blockedBy, modName)) return;
        boolean wasActive = isEnabled();
        blockedBy = modName;
        transition(wasActive, isEnabled());
    }

    /** True while the current server's blacklist forces this module off. */
    public boolean isSuppressed() {
        return suppressed;
    }

    public void setSuppressed(boolean value) {
        if (suppressed == value) return;
        boolean wasActive = isEnabled();
        suppressed = value;
        transition(wasActive, isEnabled());
    }

    private void transition(boolean wasActive, boolean active) {
        if (active && !wasActive) onEnable();
        else if (!active && wasActive) onDisable();
    }

    public int keybind() {
        return keybind;
    }

    public void setKeybind(int key) {
        if (keybind == key) return;
        keybind = key;
        changeListener.accept(this);
    }

    /** Enabled by default when no config exists yet. */
    public boolean defaultEnabled() {
        return false;
    }

    /** Hidden from the GUI (always on, no user-facing options). */
    public boolean hidden() {
        return false;
    }

    /** Never switched off (no toggle, no keybind); its settings live on their own page. */
    public boolean alwaysOn() {
        return false;
    }

    protected void onEnable() {}

    protected void onDisable() {}

    /** Called every client tick while enabled and a world is loaded. */
    public void onTick() {}

    void setChangeListener(Consumer<Module> listener) {
        this.changeListener = Objects.requireNonNull(listener);
    }

    // ---- persistence -------------------------------------------------------------------

    public JsonObject save() {
        JsonObject out = new JsonObject();
        out.addProperty("enabled", enabled);
        out.addProperty("keybind", keybind);
        JsonObject values = new JsonObject();
        for (Setting<?> s : settings) {
            if (!s.isDefault()) values.add(s.key(), s.toJson());
        }
        out.add("settings", values);
        saveExtra(out);
        return out;
    }

    public void load(JsonObject in) {
        load(in, CURRENT_CONFIG_VERSION);
    }

    /** Config schema version written by this build; older files go through {@link #migrateSetting}. */
    public static final int CURRENT_CONFIG_VERSION = 3;

    /**
     * Loads saved state written by config schema {@code version}. Keys the current build does
     * not know are offered to {@link #migrateSetting} so renamed settings keep their values.
     */
    public void load(JsonObject in, int version) {
        if (in.has("keybind") && in.get("keybind").isJsonPrimitive()) keybind = in.get("keybind").getAsInt();
        if (in.has("settings") && in.get("settings").isJsonObject()) {
            JsonObject values = in.getAsJsonObject("settings");
            for (Map.Entry<String, JsonElement> e : values.entrySet()) {
                Setting<?> s = setting(e.getKey());
                if (s != null && !(version < CURRENT_CONFIG_VERSION && migratesKey(e.getKey(), version))) {
                    s.fromJson(e.getValue());
                    continue;
                }
                migrateSetting(e.getKey(), e.getValue(), values, version);
            }
        }
        loadExtra(in);
        boolean wantEnabled = in.has("enabled") ? in.get("enabled").getAsBoolean() : defaultEnabled();
        setEnabled(wantEnabled);
    }

    /**
     * True when {@code key} from a config of {@code version} must go through
     * {@link #migrateSetting} even though a setting with that key still exists (its meaning
     * changed). The default handles nothing.
     */
    protected boolean migratesKey(String key, int version) {
        return false;
    }

    /**
     * A saved key this build has no setting for (or one {@link #migratesKey} flagged). Subclasses
     * map renamed keys here; {@code all} is the whole saved settings object in case a new value
     * depends on several old ones. The default handles 0.1.0's "background" → "show-background".
     */
    protected void migrateSetting(String key, JsonElement value, JsonObject all, int version) {
        if (key.equals("background")) {
            Setting<?> s = setting("show-background");
            if (s != null) s.fromJson(value);
        }
    }

    /**
     * Longer explanation shown in the settings panel header: exactly what the module does and
     * does not do. Defaults to the one-line description.
     */
    public String about() {
        return description;
    }

    /** Subclasses persist extra state (HUD position) here. */
    protected void saveExtra(JsonObject out) {}

    protected void loadExtra(JsonObject in) {}

    /** Resets the extra state (HUD position and scale) for "Reset all". */
    public void resetExtra() {}

    @Override
    public String toString() {
        return name;
    }
}
