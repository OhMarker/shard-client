package gg.shard.client.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public final class IntSetting extends Setting<Integer> {
    private final int min;
    private final int max;
    private final int step;
    private final String suffix;

    public IntSetting(String name, String description, int defaultValue, int min, int max) {
        this(name, description, defaultValue, min, max, 1, "");
    }

    public IntSetting(String name, String description, int defaultValue, int min, int max, int step, String suffix) {
        super(name, description, defaultValue);
        if (min > max) throw new IllegalArgumentException("min > max for " + name);
        this.min = min;
        this.max = max;
        this.step = Math.max(1, step);
        this.suffix = suffix;
    }

    public int min() {
        return min;
    }

    public int max() {
        return max;
    }

    public int step() {
        return step;
    }

    public String suffix() {
        return suffix;
    }

    /** 0..1 position for sliders. */
    public double fraction() {
        return max == min ? 0 : (double) (get() - min) / (max - min);
    }

    public void setFraction(double f) {
        double raw = min + Math.max(0, Math.min(1, f)) * (max - min);
        set((int) Math.round(raw / step) * step);
    }

    @Override
    protected Integer clamp(Integer candidate) {
        return Math.max(min, Math.min(max, candidate));
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(get());
    }

    @Override
    public boolean fromJson(JsonElement element) {
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) return false;
        setSilently(element.getAsInt());
        return true;
    }

    @Override
    public boolean parse(String text) {
        try {
            set(Integer.parseInt(text.trim()));
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public String display() {
        return get() + suffix;
    }
}
