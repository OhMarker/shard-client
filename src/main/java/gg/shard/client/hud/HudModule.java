package gg.shard.client.hud;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import gg.shard.client.ShardClient;
import gg.shard.client.gui.Fonts;
import gg.shard.client.gui.Render2D;
import gg.shard.client.gui.Theme;
import gg.shard.client.module.Module;
import gg.shard.client.module.ModuleCategory;
import gg.shard.client.modules.settings.HudDefaultsModule;
import gg.shard.client.util.Colors;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

/**
 * A module that draws something on screen. Position is stored as a fraction of the GUI size so
 * layouts survive resolution and GUI-scale changes; the editor drags these around. Elements are
 * drawn in HUD units (see {@link HudManager#hudScale()}): like the settings page they look the
 * same at every GUI scale, times the global HUD scale and the element's own scale.
 *
 * <p>Every HUD module declares the shared {@link HudStyle} group and draws through
 * {@link #box}, {@link #text} and {@link #line}, so colours, background, padding, shadow,
 * alignment and labels behave identically everywhere.
 */
public abstract class HudModule extends Module implements gg.shard.client.gui.PanelPreview {
    /** Text size in HUD units; Inter 10 has about the cap height of vanilla's 8px font (7 units). */
    public static final int TEXT_SIZE = 10;
    /** HUD line pitch, tighter than the page so a one-line element with padding 2 is 14 units tall, close to 0.2.0. */
    public static final int LINE = 10;
    public static final Fonts.Weight WEIGHT = Fonts.Weight.MEDIUM;

    private final double defaultFx;
    private final double defaultFy;
    /**
     * Position: anchor per axis ({@link HudGeometry#START}, CENTER, END) plus an offset in HUD
     * units from that anchor, so an element keeps its distance from its corner when the window or
     * GUI scale changes. Defaults and 0.3.0 configs are fractions of the GUI size; they are
     * converted to an anchor the first time the element is drawn ({@link #resolve}).
     */
    private int anchorX = HudGeometry.START;
    private int anchorY = HudGeometry.START;
    private double offX;
    private double offY;
    private boolean fractional = true;
    private double fx;
    private double fy;
    private double scale = 1.0;
    int lastWidth = 10;
    int lastHeight = 10;
    protected final HudStyle style;

    protected HudModule(String name, String description, double defaultFx, double defaultFy) {
        this(name, description, ModuleCategory.HUD, defaultFx, defaultFy);
    }

    /** For modules that draw on screen but belong to another category in the GUI (Toggle Sprint). */
    protected HudModule(String name, String description, ModuleCategory category, double defaultFx, double defaultFy) {
        super(name, description, category);
        this.defaultFx = defaultFx;
        this.defaultFy = defaultFy;
        this.fx = defaultFx;
        this.fy = defaultFy;
        this.style = HudStyle.forModule(this::add, defaultLabel(), hasAlignment());
    }

    @Override
    public void resetExtra() {
        resetPosition(defaultFx, defaultFy);
    }

    /** Label text shown before the value, or null when the element has no label. */
    protected String defaultLabel() {
        return null;
    }

    /** False hides the alignment row for elements where it has no effect. */
    protected boolean hasAlignment() {
        return false;
    }

    protected static Minecraft mc() {
        return Minecraft.getInstance();
    }

    protected static Font font() {
        return Minecraft.getInstance().font;
    }

    /** Draw at (0,0); the manager has already translated and scaled. Report bounds via {@link #size}. */
    public abstract void render(GuiGraphics g, DeltaTracker delta);

    // ---- style helpers ---------------------------------------------------------------------

    /** This element's style after inheriting the global defaults from Settings → HUD. */
    public HudStyle.Resolved style() {
        HudDefaultsModule defaults = ShardClient.modules() == null ? null : ShardClient.hudDefaults();
        HudStyle.Resolved r = style.resolve(defaults == null ? null : defaults.style());
        return previewPreset != null ? r.withPreset(previewPreset) : r;
    }

    // ---- style preview (settings panel) ----------------------------------------------------------

    /** Set while the panel preview draws one variant. */
    private static HudStyle.Preset previewPreset;
    private final java.util.Map<HudStyle.Preset, int[]> previewSizes = new java.util.EnumMap<>(HudStyle.Preset.class);
    private final int[][] previewCells = new int[HudStyle.Preset.values().length][4];
    private final int[] matchButton = new int[4];
    private int previewMouseX;
    private int previewMouseY;

    @Override
    public void previewMouse(int x, int y) {
        previewMouseX = x;
        previewMouseY = y;
    }

    /** This element drawn in each style, at a size that fits; click one to use it. */
    @Override
    public int renderPreview(GuiGraphics g, int x, int y, int width) {
        Minecraft mc = mc();
        HudStyle.Preset[] presets = HudStyle.Preset.values();
        int cellH = 46;
        int h = cellH + 36;
        gg.shard.client.gui.Render2D.roundedRect(g, x, y, width, h, gg.shard.client.gui.Theme.radius(), 0xFF2A3446);
        gg.shard.client.gui.Render2D.roundedOutline(g, x, y, width, h, gg.shard.client.gui.Theme.radius(), gg.shard.client.gui.Theme.line());
        if (needsPlayer() && mc.player == null) {
            gg.shard.client.gui.Fonts.drawCentered(g, "Join a world to preview", gg.shard.client.gui.Fonts.Weight.MEDIUM, 11, x + width / 2, y + h / 2 - 7, gg.shard.client.gui.Theme.subtle());
            return h;
        }
        int gap = 4;
        int cellW = (width - 8 - gap * (presets.length - 1)) / presets.length;
        HudStyle.Preset current = style().preset();
        int savedW = lastWidth;
        int savedH = lastHeight;
        for (int i = 0; i < presets.length; i++) {
            HudStyle.Preset p = presets[i];
            int cx = x + 4 + i * (cellW + gap);
            int cy = y + 4;
            previewCells[i] = new int[]{cx, cy, cellW, cellH};
            boolean on = p == current;
            boolean hover = previewMouseX >= cx && previewMouseX < cx + cellW && previewMouseY >= cy && previewMouseY < cy + cellH;
            gg.shard.client.gui.Render2D.roundedRect(g, cx, cy, cellW, cellH, 6, on ? 0x40000000 : hover ? 0x26000000 : 0x14000000);
            if (on) gg.shard.client.gui.Render2D.roundedOutline(g, cx, cy, cellW, cellH, 6, gg.shard.client.gui.Theme.accentAlpha(0xC0));
            int[] size = previewSizes.getOrDefault(p, new int[]{lastWidth, lastHeight});
            float s = Math.min(1f, Math.min((cellW - 8) / (float) Math.max(1, size[0]), (cellH - 18) / (float) Math.max(1, size[1])));
            var pose = g.pose();
            pose.pushMatrix();
            pose.translate(cx + (cellW - size[0] * s) / 2f, cy + 4 + (cellH - 18 - size[1] * s) / 2f);
            pose.scale(s, s);
            previewPreset = p;
            try {
                render(g, mc.getDeltaTracker());
            } catch (RuntimeException ignored) {
                // A preview must never break the settings page.
            } finally {
                previewPreset = null;
                pose.popMatrix();
            }
            previewSizes.put(p, new int[]{lastWidth, lastHeight});
            gg.shard.client.gui.Fonts.drawCentered(g, p.label(), gg.shard.client.gui.Fonts.Weight.MEDIUM, 10, cx + cellW / 2, cy + cellH - 13,
                    on ? gg.shard.client.gui.Theme.text() : gg.shard.client.gui.Theme.muted());
        }
        lastWidth = savedW;
        lastHeight = savedH;
        // "Use this look everywhere".
        String label = "Use this look for every HUD element";
        int bw = gg.shard.client.gui.Fonts.widthInt(label, gg.shard.client.gui.Fonts.Weight.MEDIUM, 11) + 20;
        int bx = x + (width - bw) / 2;
        int by = y + cellH + 10;
        matchButton[0] = bx;
        matchButton[1] = by;
        matchButton[2] = bw;
        matchButton[3] = 22;
        boolean hover = previewMouseX >= bx && previewMouseX < bx + bw && previewMouseY >= by && previewMouseY < by + 22;
        gg.shard.client.gui.Render2D.roundedRect(g, bx, by, bw, 22, 6, hover ? gg.shard.client.gui.Theme.surfaceHover() : gg.shard.client.gui.Theme.surfaceRaised());
        gg.shard.client.gui.Render2D.roundedOutline(g, bx, by, bw, 22, 6, gg.shard.client.gui.Theme.lineStrong());
        gg.shard.client.gui.Fonts.drawCentered(g, label, gg.shard.client.gui.Fonts.Weight.MEDIUM, 11, bx + bw / 2, by + (22 - gg.shard.client.gui.Fonts.lineHeight(11)) / 2,
                gg.shard.client.gui.Theme.text());
        return h;
    }

    @Override
    public String previewInput(int mx, int my, int button, boolean drag) {
        if (drag) return null;
        HudStyle.Preset[] presets = HudStyle.Preset.values();
        for (int i = 0; i < presets.length; i++) {
            int[] c = previewCells[i];
            if (mx >= c[0] && mx < c[0] + c[2] && my >= c[1] && my < c[1] + c[3]) {
                if (style.custom != null && !style.custom.get()) {
                    // Start from the inherited look so only the preset changes.
                    HudDefaultsModule defaults = ShardClient.hudDefaults();
                    if (defaults != null) style.copyLook(defaults.style());
                    style.custom.set(true);
                }
                style.preset.set(presets[i]);
                return null;
            }
        }
        if (mx >= matchButton[0] && mx < matchButton[0] + matchButton[2] && my >= matchButton[1] && my < matchButton[1] + matchButton[3]) {
            HudDefaultsModule defaults = ShardClient.hudDefaults();
            if (defaults == null) return null;
            HudStyle source = style.overrides() ? style : defaults.style();
            if (source != defaults.style()) defaults.style().copyLook(source);
            int n = 0;
            for (var m : ShardClient.modules().all()) {
                if (m instanceof HudModule hm && hm.style.custom != null && hm.style.custom.get()) {
                    hm.style.custom.set(false);
                    n++;
                }
            }
            return "Every HUD element now uses this look" + (n > 0 ? " (" + n + " custom styles reset)" : "");
        }
        return null;
    }

    public HudStyle styleSettings() {
        return style;
    }

    protected static int lineH() {
        return LINE;
    }

    protected static int textW(String text) {
        return Fonts.widthInt(text, WEIGHT, TEXT_SIZE);
    }

    /** Background for a {@code w} x {@code h} element according to the preset. */
    protected static void box(GuiGraphics g, HudStyle.Resolved st, int w, int h) {
        switch (st.preset()) {
            case CARD -> Render2D.roundedRect(g, 0, 0, w, h, st.radius(), st.background());
            case OUTLINED -> {
                Render2D.roundedRect(g, 0, 0, w, h, st.radius(), Colors.fade(st.background(), 0.5));
                Render2D.roundedOutline(g, 0, 0, w, h, st.radius(), Colors.withAlpha(st.text(), 0x60));
            }
            case MINIMAL -> {
            }
            case PILL -> Render2D.roundedRect(g, 0, 0, w, h, h / 2, st.background());
        }
    }

    /** Draws one HUD line whose box starts at {@code y} and is {@link #lineH()} tall. */
    protected static void text(GuiGraphics g, HudStyle.Resolved st, String text, int x, int y, int color) {
        Fonts.draw(g, text, WEIGHT, TEXT_SIZE, x, y + lineOffset(), color, st.shadow());
    }

    /** Shift from a HUD line box to the font's own (taller) line box so capitals stay centred. */
    protected static int lineOffset() {
        return (LINE - Fonts.lineHeight(TEXT_SIZE)) / 2;
    }

    /** The label from the style, with a trailing space, or "" when hidden. */
    protected static String labelText(HudStyle.Resolved st) {
        return st.hasLabel() ? st.label() + " " : "";
    }

    /** One line: optional label in the text colour, then the value. Sets the size. */
    protected void line(GuiGraphics g, String value, int valueColor) {
        HudStyle.Resolved st = style();
        if (st.brackets()) value = "[" + value + "]";
        String label = st.hasLabel() ? (st.labelAfter() ? " " + st.label() : st.label() + " ") : "";
        int pad = st.padding();
        int lw = textW(label);
        int vw = textW(value);
        int w = pad * 2 + lw + vw;
        int h = pad * 2 + lineH();
        if (st.preset() == HudStyle.Preset.PILL) {
            pad = Math.max(pad, h / 3);
            w = pad * 2 + lw + vw;
        }
        box(g, st, w, h);
        int vx = st.labelAfter() ? pad : pad + lw;
        int lx = st.labelAfter() ? pad + vw : pad;
        text(g, st, label, lx, st.padding(), st.text());
        text(g, st, value, vx, st.padding(), valueColor == 0 ? st.value() : valueColor);
        size(w, h);
    }

    /** Several lines of text, aligned inside the box per the style. Sets the size. */
    protected void lines(GuiGraphics g, List<String> lines, List<Integer> colors) {
        HudStyle.Resolved st = style();
        int pad = st.padding();
        int widest = 0;
        for (String l : lines) widest = Math.max(widest, textW(l));
        int w = pad * 2 + widest;
        int h = pad * 2 + lines.size() * lineH();
        box(g, st, w, h);
        for (int i = 0; i < lines.size(); i++) {
            int x = alignX(st, pad, widest, textW(lines.get(i)));
            text(g, st, lines.get(i), x, pad + i * lineH(), colors == null ? st.value() : colors.get(i));
        }
        size(w, h);
    }

    /**
     * A small bar graph: {@code values[0]} is the newest sample and is drawn on the right. Bars
     * over {@code warnAbove} use the warning colour; the scale tops out at {@code max}.
     */
    protected static void graph(GuiGraphics g, int x, int y, int w, int h, float[] values, int count, float max, float warnAbove,
                                int color, int warnColor, int background) {
        Render2D.roundedRect(g, x, y, w, h, 2, background);
        int bars = Math.min(count, w);
        for (int i = 0; i < bars; i++) {
            float v = values[i];
            int bh = Math.max(1, Math.min(h, Math.round(v / Math.max(0.001f, max) * h)));
            int bx = x + w - 1 - i;
            g.fill(bx, y + h - bh, bx + 1, y + h, v > warnAbove ? warnColor : color);
        }
    }

    protected static int alignX(HudStyle.Resolved st, int pad, int innerW, int textW) {
        return switch (st.align()) {
            case LEFT -> pad;
            case CENTER -> pad + (innerW - textW) / 2;
            case RIGHT -> pad + innerW - textW;
        };
    }

    protected static int accentOrText(boolean accent, HudStyle.Resolved st) {
        return accent ? Theme.accent() : st.value();
    }

    // ---- geometry ----------------------------------------------------------------------------

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

    /** Converts a fractional (default or 0.3.0) position to an anchored one for this GUI size. */
    public void resolve(int guiWidth, int guiHeight) {
        if (!fractional || guiWidth <= 0 || guiHeight <= 0) return;
        setPosition(fx * guiWidth, fy * guiHeight, guiWidth, guiHeight);
    }

    /** Left edge in GUI units (fractional so 1-pixel nudges at high GUI scales are kept). */
    public double posX(int guiWidth) {
        if (fractional) return fx * guiWidth;
        double x = HudGeometry.positionFor(anchorX, offX, scaledWidthExact(), guiWidth, HudManager.hudScale());
        return HudGeometry.clamp(x, scaledWidthExact(), guiWidth);
    }

    public double posY(int guiHeight) {
        if (fractional) return fy * guiHeight;
        double y = HudGeometry.positionFor(anchorY, offY, scaledHeightExact(), guiHeight, HudManager.hudScale());
        return HudGeometry.clamp(y, scaledHeightExact(), guiHeight);
    }

    public int pixelX(int guiWidth) {
        return (int) Math.round(posX(guiWidth));
    }

    public int pixelY(int guiHeight) {
        return (int) Math.round(posY(guiHeight));
    }

    /** Moves the element (GUI units, clamped to the screen) and re-anchors it to the nearest third. */
    public void setPosition(double x, double y, int guiWidth, int guiHeight) {
        double w = scaledWidthExact();
        double h = scaledHeightExact();
        double cx = HudGeometry.clamp(x, w, guiWidth);
        double cy = HudGeometry.clamp(y, h, guiHeight);
        double unit = HudManager.hudScale();
        anchorX = HudGeometry.anchorFor(cx, w, guiWidth);
        anchorY = HudGeometry.anchorFor(cy, h, guiHeight);
        offX = HudGeometry.offsetFor(anchorX, cx, w, guiWidth, unit);
        offY = HudGeometry.offsetFor(anchorY, cy, h, guiHeight, unit);
        fractional = false;
    }

    public void setPixelPosition(int x, int y, int guiWidth, int guiHeight) {
        setPosition(x, y, guiWidth, guiHeight);
    }

    public int anchorX() {
        return anchorX;
    }

    public int anchorY() {
        return anchorY;
    }

    /** Layout snapshot for undo and presets: {anchorX, anchorY, offX, offY, scale}, or fractions while unresolved. */
    public double[] layout() {
        return fractional ? new double[]{-1, -1, fx, fy, scale} : new double[]{anchorX, anchorY, offX, offY, scale};
    }

    public void applyLayout(double[] l) {
        if (l == null || l.length < 5) return;
        if (l[0] < 0) {
            fractional = true;
            fx = l[2];
            fy = l[3];
        } else {
            fractional = false;
            anchorX = (int) l[0];
            anchorY = (int) l[1];
            offX = l[2];
            offY = l[3];
        }
        setScale(l[4]);
    }

    public double scaledWidthExact() {
        return lastWidth * scale * HudManager.hudScale();
    }

    public double scaledHeightExact() {
        return lastHeight * scale * HudManager.hudScale();
    }

    /** Width in GUI units: HUD units times the global HUD scale times this element's scale. */
    public int scaledWidth() {
        return (int) Math.ceil(lastWidth * scale * HudManager.hudScale());
    }

    public int scaledHeight() {
        return (int) Math.ceil(lastHeight * scale * HudManager.hudScale());
    }

    /** Whether to draw when there is no player (main menu). HUD modules normally need a world. */
    public boolean needsPlayer() {
        return true;
    }

    // ---- persistence -------------------------------------------------------------------------

    @Override
    protected void saveExtra(JsonObject out) {
        JsonObject hud = new JsonObject();
        if (fractional) {
            hud.addProperty("x", fx);
            hud.addProperty("y", fy);
        } else {
            hud.addProperty("anchorX", anchorX);
            hud.addProperty("anchorY", anchorY);
            hud.addProperty("offsetX", offX);
            hud.addProperty("offsetY", offY);
        }
        hud.addProperty("scale", scale);
        out.add("hud", hud);
    }

    @Override
    protected void loadExtra(JsonObject in) {
        if (!in.has("hud") || !in.get("hud").isJsonObject()) return;
        JsonObject hud = in.getAsJsonObject("hud");
        if (hud.has("anchorX") && hud.has("offsetX")) {
            fractional = false;
            anchorX = Math.max(0, Math.min(2, hud.get("anchorX").getAsInt()));
            anchorY = Math.max(0, Math.min(2, hud.get("anchorY").getAsInt()));
            offX = hud.get("offsetX").getAsDouble();
            offY = hud.get("offsetY").getAsDouble();
        } else {
            // 0.3.0 and older: fractions, anchored on first draw.
            fractional = true;
            if (hud.has("x")) fx = Math.max(0, Math.min(1, hud.get("x").getAsDouble()));
            if (hud.has("y")) fy = Math.max(0, Math.min(1, hud.get("y").getAsDouble()));
        }
        if (hud.has("scale")) setScale(hud.get("scale").getAsDouble());
    }

    /**
     * 0.2.0 HUD modules had a "Show background" switch; off becomes the Minimal preset with a
     * custom style so the element keeps looking the way it did.
     */
    @Override
    protected void migrateSetting(String key, JsonElement value, JsonObject all, int version) {
        if (key.equals("show-background") || key.equals("background")) {
            if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean() && !value.getAsBoolean()) {
                style.custom.set(true);
                style.preset.set(HudStyle.Preset.MINIMAL);
            }
            return;
        }
        super.migrateSetting(key, value, all, version);
    }

    public void resetPosition(double defaultFx, double defaultFy) {
        this.fx = defaultFx;
        this.fy = defaultFy;
        this.fractional = true;
        this.scale = 1.0;
    }
}
