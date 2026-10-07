package gg.shard.client.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import gg.shard.client.util.Colors;

/** ARGB colour. Stored as #AARRGGBB; parses #RRGGBB (alpha 255) and #AARRGGBB. */
public final class ColorSetting extends Setting<Integer> {
    private final boolean allowAlpha;

    public ColorSetting(String name, String description, int defaultArgb) {
        this(name, description, defaultArgb, true);
    }

    public ColorSetting(String name, String description, int defaultArgb, boolean allowAlpha) {
        super(name, description, defaultArgb);
        this.allowAlpha = allowAlpha;
    }

    public boolean allowAlpha() {
        return allowAlpha;
    }

    public int alpha() {
        return (get() >>> 24) & 0xFF;
    }

    public int rgb() {
        return get() & 0xFFFFFF;
    }

    public void setAlpha(int alpha) {
        set(((alpha & 0xFF) << 24) | rgb());
    }

    public void setRgb(int rgb) {
        set((alpha() << 24) | (rgb & 0xFFFFFF));
    }

    @Override
    protected Integer clamp(Integer candidate) {
        return allowAlpha ? candidate : (0xFF << 24) | (candidate & 0xFFFFFF);
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(Colors.toHex(get()));
    }

    @Override
    public boolean fromJson(JsonElement element) {
        if (!element.isJsonPrimitive()) return false;
        if (element.getAsJsonPrimitive().isNumber()) {
            setSilently(element.getAsInt());
            return true;
        }
        Integer parsed = Colors.parseHex(element.getAsString());
        if (parsed == null) return false;
        setSilently(parsed);
        return true;
    }

    @Override
    public boolean parse(String text) {
        Integer parsed = Colors.parseHex(text);
        if (parsed == null) return false;
        set(parsed);
        return true;
    }

    @Override
    public String display() {
        return Colors.toHex(get());
    }
}
