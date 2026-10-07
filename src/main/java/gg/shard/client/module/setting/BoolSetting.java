package gg.shard.client.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public final class BoolSetting extends Setting<Boolean> {
    public BoolSetting(String name, String description, boolean defaultValue) {
        super(name, description, defaultValue);
    }

    public void toggle() {
        set(!get());
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(get());
    }

    @Override
    public boolean fromJson(JsonElement element) {
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isBoolean()) return false;
        setSilently(element.getAsBoolean());
        return true;
    }

    @Override
    public boolean parse(String text) {
        switch (text.trim().toLowerCase()) {
            case "true", "on", "yes", "1" -> set(true);
            case "false", "off", "no", "0" -> set(false);
            case "toggle" -> toggle();
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public String display() {
        return get() ? "On" : "Off";
    }
}
