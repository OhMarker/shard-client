package gg.shard.client.module.setting;

import com.google.gson.JsonElement;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A typed, persisted, GUI-driven module option. Adding a setting field to a module is all it
 * takes to get a widget in the click GUI, a line in the config file and a chat command.
 */
public abstract class Setting<T> {
    private final String name;
    private final String description;
    private final T defaultValue;
    private T value;
    private Consumer<T> onChange = v -> {};
    private Supplier<Boolean> visible = () -> true;

    protected Setting(String name, String description, T defaultValue) {
        this.name = Objects.requireNonNull(name);
        this.description = Objects.requireNonNull(description);
        this.defaultValue = Objects.requireNonNull(defaultValue);
        this.value = defaultValue;
    }

    public String name() {
        return name;
    }

    /** Stable key used in config files and commands. */
    public String key() {
        return name.toLowerCase().replace(' ', '-');
    }

    public String description() {
        return description;
    }

    public T get() {
        return value;
    }

    public T defaultValue() {
        return defaultValue;
    }

    public void set(T newValue) {
        T clamped = clamp(Objects.requireNonNull(newValue));
        if (Objects.equals(clamped, value)) return;
        value = clamped;
        onChange.accept(clamped);
    }

    /** Sets without firing listeners; used while loading config. */
    protected void setSilently(T newValue) {
        value = clamp(Objects.requireNonNull(newValue));
    }

    public void reset() {
        set(defaultValue);
    }

    public boolean isDefault() {
        return Objects.equals(value, defaultValue);
    }

    /** Subclasses constrain values here (ranges, enum membership). */
    protected T clamp(T candidate) {
        return candidate;
    }

    public Setting<T> onChange(Consumer<T> listener) {
        this.onChange = this.onChange.andThen(Objects.requireNonNull(listener));
        return this;
    }

    public Setting<T> visibleWhen(Supplier<Boolean> condition) {
        this.visible = Objects.requireNonNull(condition);
        return this;
    }

    public boolean isVisible() {
        return visible.get();
    }

    public abstract JsonElement toJson();

    /** Returns false when the element could not be understood (value is left untouched). */
    public abstract boolean fromJson(JsonElement element);

    /** Parses a chat-command argument; returns false on bad input. */
    public abstract boolean parse(String text);

    /** Short display form for the GUI and chat. */
    public abstract String display();
}
