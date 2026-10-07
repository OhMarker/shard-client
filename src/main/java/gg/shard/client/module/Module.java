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
 */
public abstract class Module {
    private final String name;
    private final String description;
    private final ModuleCategory category;
    private final List<Setting<?>> settings = new ArrayList<>();
    private boolean enabled;
    private int keybind = Keys.NONE;
    private Consumer<Module> changeListener = m -> {};

    protected Module(String name, String description, ModuleCategory category) {
        this.name = Objects.requireNonNull(name);
        this.description = Objects.requireNonNull(description);
        this.category = Objects.requireNonNull(category);
    }

    public String name() {
        return name;
    }

    /** Stable config/command key, e.g. "armor-status". */
    public String key() {
        return name.toLowerCase().replace(' ', '-');
    }

    public String description() {
        return description;
    }

    public ModuleCategory category() {
        return category;
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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean value) {
        if (enabled == value) return;
        enabled = value;
        try {
            if (value) onEnable();
            else onDisable();
        } finally {
            changeListener.accept(this);
        }
    }

    public void toggle() {
        setEnabled(!enabled);
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
        if (in.has("keybind") && in.get("keybind").isJsonPrimitive()) keybind = in.get("keybind").getAsInt();
        if (in.has("settings") && in.get("settings").isJsonObject()) {
            for (Map.Entry<String, JsonElement> e : in.getAsJsonObject("settings").entrySet()) {
                Setting<?> s = setting(e.getKey());
                if (s != null) s.fromJson(e.getValue());
            }
        }
        loadExtra(in);
        boolean wantEnabled = in.has("enabled") ? in.get("enabled").getAsBoolean() : defaultEnabled();
        setEnabled(wantEnabled);
    }

    /** Subclasses persist extra state (HUD position) here. */
    protected void saveExtra(JsonObject out) {}

    protected void loadExtra(JsonObject in) {}

    @Override
    public String toString() {
        return name;
    }
}
