package gg.shard.client.modules.hud;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import gg.shard.client.hud.HudModule;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;

public final class FpsModule extends HudModule {
    public FpsModule() {
        super("FPS", "Frames per second.", 0.01, 0.02);
    }

    @Override
    protected String defaultLabel() {
        return "FPS";
    }

    @Override
    public String about() {
        return "The client's current frame rate, the same number vanilla's F3 screen shows. Informational only; it changes nothing about rendering.";
    }

    @Override
    public boolean defaultEnabled() {
        return true;
    }

    @Override
    public boolean needsPlayer() {
        return false;
    }

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        line(g, String.valueOf(mc().getFps()), 0);
    }

    @Override
    public String icon() {
        return "fps";
    }

    // 0.2.0: "Color" was the text colour and "Label" a switch for the FPS prefix.

    @Override
    protected boolean migratesKey(String key, int version) {
        return version < 3 && key.equals("label");
    }

    @Override
    protected void migrateSetting(String key, JsonElement value, JsonObject all, int version) {
        switch (key) {
            case "color" -> {
                style.custom.set(true);
                style.text.fromJson(value);
                style.value.fromJson(value);
            }
            case "label" -> {
                if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean()) style.label.set(value.getAsBoolean() ? "FPS" : "");
                else style.label.fromJson(value);
            }
            default -> super.migrateSetting(key, value, all, version);
        }
    }
}
