package gg.shard.client.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.Locale;

public final class DoubleSetting extends Setting<Double> {
    private final double min;
    private final double max;
    private final double step;
    private final String suffix;

    public DoubleSetting(String name, String description, double defaultValue, double min, double max, double step) {
        this(name, description, defaultValue, min, max, step, "");
    }

    public DoubleSetting(String name, String description, double defaultValue, double min, double max, double step, String suffix) {
        super(name, description, defaultValue);
        if (min > max) throw new IllegalArgumentException("min > max for " + name);
        this.min = min;
        this.max = max;
        this.step = step <= 0 ? 0.01 : step;
        this.suffix = suffix;
    }

    public double min() {
        return min;
    }

    public double max() {
        return max;
    }

    public double step() {
        return step;
    }

    public String suffix() {
        return suffix;
    }

    public float getFloat() {
        return get().floatValue();
    }

    public double fraction() {
        return max == min ? 0 : (get() - min) / (max - min);
    }

    public void setFraction(double f) {
        double raw = min + Math.max(0, Math.min(1, f)) * (max - min);
        set(Math.round(raw / step) * step);
    }

    @Override
    protected Double clamp(Double candidate) {
        double v = Math.max(min, Math.min(max, candidate));
        // Avoid 0.30000000000000004-style noise in the GUI and config.
        return Math.round(v * 10_000d) / 10_000d;
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(get());
    }

    @Override
    public boolean fromJson(JsonElement element) {
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) return false;
        setSilently(element.getAsDouble());
        return true;
    }

    @Override
    public boolean parse(String text) {
        try {
            set(Double.parseDouble(text.trim()));
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public String display() {
        double v = get();
        String s = v == Math.rint(v) && step >= 1 ? String.valueOf((long) v) : String.format(Locale.ROOT, step >= 0.1 ? "%.1f" : "%.2f", v);
        return s + suffix;
    }
}
