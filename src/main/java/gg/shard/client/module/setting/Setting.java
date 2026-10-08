package gg.shard.client.module.setting;

import com.google.gson.JsonElement;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A typed, persisted, GUI-driven module option. Adding a setting field to a module is all it
 * takes to get a row in the settings screen, a line in the config file and a chat command.
 */
public abstract class Setting<T> {
    private final String name;
    private final String description;
    private final T defaultValue;
    private T value;
    private String group = "";
    private String details = "";
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

    /** Optional heading the settings screen shows above this setting and its neighbours. */
    public String group() {
        return group;
    }

    /** Returns this setting typed as the caller's subclass so {@code add(new X(...).group(..))} infers X. */
    @SuppressWarnings("unchecked")
    public <S extends Setting<T>> S group(String label) {
        this.group = label == null ? "" : label;
        return (S) this;
    }

    /** Optional second line the settings panel shows on hover: the detail behind the one-line description. */
    public String details() {
        return details;
    }

    @SuppressWarnings("unchecked")
    public <S extends Setting<T>> S details(String text) {
        this.details = text == null ? "" : text;
        return (S) this;
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
