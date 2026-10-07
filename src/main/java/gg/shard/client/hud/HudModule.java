package gg.shard.client.hud;

import com.google.gson.JsonObject;
import gg.shard.client.module.Module;
import gg.shard.client.module.ModuleCategory;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * A module that draws something on screen. Position is stored as a fraction of the GUI size so
 * layouts survive resolution and GUI-scale changes; the editor drags these around.
 */
public abstract class HudModule extends Module {
    private double fx;
    private double fy;
    private double scale = 1.0;
    private int lastWidth = 10;
    private int lastHeight = 10;

    protected HudModule(String name, String description, double defaultFx, double defaultFy) {
        this(name, description, ModuleCategory.HUD, defaultFx, defaultFy);
    }

    /** For modules that draw on screen but belong to another category in the GUI (Toggle Sprint). */
    protected HudModule(String name, String description, ModuleCategory category, double defaultFx, double defaultFy) {
        super(name, description, category);
        this.fx = defaultFx;
        this.fy = defaultFy;
    }

    protected static Minecraft mc() {
        return Minecraft.getInstance();
    }

    protected static Font font() {
        return Minecraft.getInstance().font;
    }

    /** Draw at (0,0); the manager has already translated and scaled. Report bounds via {@link #size}. */
    public abstract void render(GuiGraphics g, DeltaTracker delta);

    protected void size(int width, int height) {
        this.lastWidth = Math.max(1, width);
        this.lastHeight = Math.max(1, height);
    }

    public int width() {
        return lastWidth;
    }

    public int height() {
        return lastHeight;
    }

    public double scale() {
        return scale;
    }

    public void setScale(double value) {
        this.scale = Math.max(0.5, Math.min(3.0, Math.round(value * 20) / 20.0));
    }

    public double fractionX() {
        return fx;
    }

    public double fractionY() {
        return fy;
    }

    public int pixelX(int guiWidth) {
        return (int) Math.round(fx * guiWidth);
    }

    public int pixelY(int guiHeight) {
        return (int) Math.round(fy * guiHeight);
    }

    public void setPixelPosition(int x, int y, int guiWidth, int guiHeight) {
        int maxX = Math.max(0, guiWidth - scaledWidth());
        int maxY = Math.max(0, guiHeight - scaledHeight());
        int cx = Math.max(0, Math.min(maxX, x));
        int cy = Math.max(0, Math.min(maxY, y));
        this.fx = guiWidth == 0 ? 0 : (double) cx / guiWidth;
        this.fy = guiHeight == 0 ? 0 : (double) cy / guiHeight;
    }

    public int scaledWidth() {
        return (int) Math.ceil(lastWidth * scale);
    }

    public int scaledHeight() {
        return (int) Math.ceil(lastHeight * scale);
    }

    /** Whether to draw when there is no player (main menu). HUD modules normally need a world. */
    public boolean needsPlayer() {
        return true;
    }

    @Override
    protected void saveExtra(JsonObject out) {
        JsonObject hud = new JsonObject();
        hud.addProperty("x", fx);
        hud.addProperty("y", fy);
        hud.addProperty("scale", scale);
        out.add("hud", hud);
    }

    @Override
    protected void loadExtra(JsonObject in) {
        if (!in.has("hud") || !in.get("hud").isJsonObject()) return;
        JsonObject hud = in.getAsJsonObject("hud");
        if (hud.has("x")) fx = Math.max(0, Math.min(1, hud.get("x").getAsDouble()));
        if (hud.has("y")) fy = Math.max(0, Math.min(1, hud.get("y").getAsDouble()));
        if (hud.has("scale")) setScale(hud.get("scale").getAsDouble());
    }

    public void resetPosition(double defaultFx, double defaultFy) {
        this.fx = defaultFx;
        this.fy = defaultFy;
        this.scale = 1.0;
    }
}
