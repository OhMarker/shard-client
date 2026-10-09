package gg.shard.client.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import gg.shard.client.compat.KeyCodes;
import gg.shard.client.util.Keys;

/** A key code (the game's; saved as GLFW's, see KeyCodes), or -1 for unbound. */
public final class KeybindSetting extends Setting<Integer> {
    public KeybindSetting(String name, String description, int defaultKey) {
        super(name, description, defaultKey);
    }

    public boolean isBound() {
        return get() >= 0;
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(KeyCodes.save(get()));
    }

    @Override
    public boolean fromJson(JsonElement element) {
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) return false;
        setSilently(KeyCodes.load(element.getAsInt()));
        return true;
    }

    @Override
    public boolean parse(String text) {
        int key = Keys.fromName(text);
        if (key == Integer.MIN_VALUE) return false;
        set(key);
        return true;
    }

    @Override
    public String display() {
        return Keys.name(get());
    }
}
