package gg.shard.client.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public final class StringSetting extends Setting<String> {
    private final int maxLength;

    public StringSetting(String name, String description, String defaultValue) {
        this(name, description, defaultValue, 64);
    }

    public StringSetting(String name, String description, String defaultValue, int maxLength) {
        super(name, description, defaultValue);
        this.maxLength = maxLength;
    }

    public int maxLength() {
        return maxLength;
    }

    @Override
    protected String clamp(String candidate) {
        return candidate.length() > maxLength ? candidate.substring(0, maxLength) : candidate;
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(get());
    }

    @Override
    public boolean fromJson(JsonElement element) {
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) return false;
        setSilently(element.getAsString());
        return true;
    }

    @Override
    public boolean parse(String text) {
        set(text);
        return true;
    }

    @Override
    public String display() {
        return get().isEmpty() ? "(empty)" : get();
    }
}
