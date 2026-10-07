package gg.shard.client.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.Locale;

public final class EnumSetting<E extends Enum<E>> extends Setting<E> {
    private final Class<E> type;

    public EnumSetting(String name, String description, E defaultValue) {
        super(name, description, defaultValue);
        this.type = defaultValue.getDeclaringClass();
    }

    public E[] values() {
        return type.getEnumConstants();
    }

    public void cycle(boolean forward) {
        E[] values = values();
        int index = get().ordinal() + (forward ? 1 : -1);
        set(values[Math.floorMod(index, values.length)]);
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(get().name());
    }

    @Override
    public boolean fromJson(JsonElement element) {
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) return false;
        E parsed = find(element.getAsString());
        if (parsed == null) return false;
        setSilently(parsed);
        return true;
    }

    @Override
    public boolean parse(String text) {
        E parsed = find(text);
        if (parsed == null) return false;
        set(parsed);
        return true;
    }

    private E find(String text) {
        String wanted = text.trim().replace(' ', '_').replace('-', '_');
        for (E value : values()) {
            if (value.name().equalsIgnoreCase(wanted)) return value;
        }
        return null;
    }

    @Override
    public String display() {
        return pretty(get());
    }

    public static String pretty(Enum<?> value) {
        String[] parts = value.name().toLowerCase(Locale.ROOT).split("_");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (!out.isEmpty()) out.append(' ');
            out.append(Character.toUpperCase(part.charAt(0))).append(part, 1, part.length());
        }
        return out.toString();
    }
}
