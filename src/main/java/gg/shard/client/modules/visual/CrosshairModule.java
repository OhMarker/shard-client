package gg.shard.client.modules.visual;

import gg.shard.client.gui.Fonts;
import gg.shard.client.gui.PanelPreview;
import gg.shard.client.gui.Render2D;
import gg.shard.client.gui.Theme;
import gg.shard.client.module.Module;
import gg.shard.client.module.ModuleCategory;
import gg.shard.client.module.setting.BoolSetting;
import gg.shard.client.module.setting.ColorSetting;
import gg.shard.client.module.setting.EnumSetting;
import gg.shard.client.module.setting.IntSetting;
import gg.shard.client.module.setting.Labeled;
import gg.shard.client.module.setting.StringSetting;
import gg.shard.client.util.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
//? if >=1.21.9 {
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
//?}
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.level.GameType;
import org.lwjgl.glfw.GLFW;

import com.google.gson.JsonObject;

import java.util.List;

/**
 * Shard's crosshair: thirteen shapes including one you draw pixel by pixel, drawn in real screen
 * pixels (so a 1-pixel line works at any GUI scale) or in GUI units, outline colour and
 * width, colours for aiming at a player or mob and at a crystal, dimming while your hit
 * recharges, a dynamic gap and a hit marker. Every shape is a pixel mask ({@link CrosshairShape})
 * so the outline follows it exactly. The settings panel shows it live on sample backgrounds at
 * real size and zoomed, doubles as the pixel editor, and copies or pastes share codes.
 */
public final class CrosshairModule extends Module implements PanelPreview {
    public enum Style implements Labeled {
        CROSS("Gap plus"), PLUS("Plus"), CROSS_DOT("Plus and dot"), DOT("Dot"), CIRCLE("Circle and dot"), RING("Circle"), CIRCLE_CROSS("Circle and plus"),
        SQUARE("Square"), T_SHAPE("T"), X("X"), X_DOT("X and dot"), CHEVRON("Chevron"), CUSTOM("Custom");

        private final String label;

        Style(String label) {
            this.label = label;
        }

        @Override
        public String label() {
            return label;
        }

        CrosshairShape.Kind kind() {
            return CrosshairShape.Kind.valueOf(name());
        }
    }

    private static final long GAP_MS = 150;
    private static final String[] ZOOM_CAPTIONS = {"", "Pixel grid", "Zoomed 2x", "Zoomed 3x", "Zoomed 4x"};
    private static final String[] BACKGROUNDS = {"Sky", "Grass", "Stone", "Nether", "End"};
    private static final Material[] BG_SPRITES = {null, block("grass_block_top"), block("stone"), block("netherrack"), block("end_stone")};

    private final EnumSetting<Style> style = add(new EnumSetting<>("Style", "Shape of the crosshair; Custom lets you draw it in the preview (right-click erases)", Style.CROSS).group("Shape"));
    private final BoolSetting pixelPerfect = add(new BoolSetting("Pixel-perfect", "Measure sizes in real screen pixels, so a 1-pixel line stays 1 pixel at any GUI scale", true).group("Shape")
            .details("Off measures in GUI pixels like vanilla's crosshair: 1 px is 3 screen pixels at GUI scale 3."));
    private final IntSetting size = add(new IntSetting("Size", "Length of each arm", 9, 1, 40, 1, "px").group("Shape"));
    private final IntSetting gap = add(new IntSetting("Gap", "Space around the centre", 4, 0, 20, 1, "px").group("Shape"));
    private final IntSetting thickness = add(new IntSetting("Thickness", "Line width", 2, 1, 10, 1, "px").group("Shape"));
    private final StringSetting pixels = add(new StringSetting("Pixels", "Your custom crosshair, drawn in the preview", CrosshairShape.encodePixels(defaultCustom()), 64).group("Shape"));
    private final BoolSetting dynamicGap = add(new BoolSetting("Dynamic gap", "Widen the gap for a moment when you attack", true).group("Shape")
            .details("Reacts to attacks you perform; it does not change aim or timing."));
    private final IntSetting gapKick = add(new IntSetting("Gap kick", "How far the gap widens on an attack", 4, 1, 16, 1, "px").group("Shape"));
    private final ColorSetting color = add(new ColorSetting("Colour", "Crosshair colour (alpha is opacity)", 0xFFFFFFFF).group("Colour"));
    private final BoolSetting outline = add(new BoolSetting("Outline", "An outline around the crosshair for contrast", true).group("Colour"));
    private final ColorSetting outlineColor = add(new ColorSetting("Outline colour", "Colour of the outline", 0xB0000000).group("Colour"));
    private final IntSetting outlineWidth = add(new IntSetting("Outline width", "Width of the outline", 1, 1, 4, 1, "px").group("Colour"));
    private final BoolSetting highlightTarget = add(new BoolSetting("Highlight target", "Change colour while aiming at a player or mob", true).group("Colour"));
    private final ColorSetting targetColor = add(new ColorSetting("Target colour", "Colour while aiming at a player or mob", 0xFFFB7185).group("Colour"));
    private final BoolSetting highlightCrystal = add(new BoolSetting("Highlight crystals", "Change colour while aiming at an end crystal", true).group("Colour"));
    private final ColorSetting crystalColor = add(new ColorSetting("Crystal colour", "Colour while aiming at an end crystal", 0xFFC084FC).group("Colour"));
    private final BoolSetting dimCharging = add(new BoolSetting("Dim while charging", "Half opacity while your next hit is still recharging", false).group("Colour")
            .details("Reads the same attack strength as vanilla's attack indicator."));
    private final BoolSetting hitMarker = add(new BoolSetting("Hit marker", "Flash an X around the crosshair when you hit a living target", true).group("Hit marker"));
    private final ColorSetting hitMarkerColor = add(new ColorSetting("Marker colour", "Colour of the hit marker", 0xFFFFFFFF, false).group("Hit marker"));
    private final IntSetting hitMarkerMs = add(new IntSetting("Marker time", "How long the marker stays", 250, 100, 600, 50, " ms").group("Hit marker"));

    private long attackAt = Long.MIN_VALUE / 2;
    private long hitAt = Long.MIN_VALUE / 2;
    private int background = 1;
    private int mouseX;
    private int mouseY;
    // Preview hit areas (design units), filled while rendering.
    private final int[] chips = new int[BACKGROUNDS.length * 4];
    private final int[] copyBtn = new int[4];
    private final int[] pasteBtn = new int[4];
    private final int[] editor = new int[3];
    // Mask cache.
    private String maskKey = "";
    private List<CrosshairShape.Run> mainRuns = List.of();
    private List<CrosshairShape.Run> outlineRuns = List.of();
    private int maskSize = 1;
    private static final int[][] CORNERS = {{-1, -1}, {1, -1}, {-1, 1}, {1, 1}};
    /** 0.6.0 defaults, for configs saved before Pixel-perfect existed (their sizes are GUI pixels). */
    private static final int LEGACY_SIZE = 5, LEGACY_GAP = 2, LEGACY_THICKNESS = 1, LEGACY_KICK = 3;
    private static final String UNITS_MARKER = "crosshairUnits";

    public CrosshairModule() {
        super("Crosshair", "Your own crosshair: shapes, a pixel editor, outline, target and crystal colours, hit marker.", ModuleCategory.VISUALS);
        targetColor.visibleWhen(highlightTarget::get);
        crystalColor.visibleWhen(highlightCrystal::get);
        gapKick.visibleWhen(dynamicGap::get);
        outlineColor.visibleWhen(outline::get);
        outlineWidth.visibleWhen(outline::get);
        hitMarkerColor.visibleWhen(hitMarker::get);
        hitMarkerMs.visibleWhen(hitMarker::get);
        pixels.visibleWhen(() -> false);
        size.visibleWhen(() -> style.get() != Style.CUSTOM && style.get() != Style.DOT);
        gap.visibleWhen(() -> style.get() != Style.CUSTOM && style.get() != Style.DOT && style.get() != Style.PLUS);
        thickness.visibleWhen(() -> style.get() != Style.CUSTOM);
    }

    private static Material block(String name) {
        return new Material(TextureAtlas.LOCATION_BLOCKS, Identifier.withDefaultNamespace("block/" + name));
    }

    private static boolean[] defaultCustom() {
        boolean[] px = new boolean[CrosshairShape.CUSTOM * CrosshairShape.CUSTOM];
        int c = CrosshairShape.CUSTOM / 2;
        for (int d = 2; d <= 5; d++) {
            px[c * CrosshairShape.CUSTOM + c + d] = true;
            px[c * CrosshairShape.CUSTOM + c - d] = true;
            px[(c + d) * CrosshairShape.CUSTOM + c] = true;
        }
        px[c * CrosshairShape.CUSTOM + c] = true;
        return px;
    }

    @Override
    public String about() {
        return "Draws a crosshair of your choice in place of vanilla's and keeps vanilla's rules (first person only, hidden with the GUI, debug axes win). "
                + "The colours read what vanilla already picked under the cursor and your own attack strength; the gap kick and hit marker react to attacks you made. "
                + "Nothing aims or clicks for you. Share a crosshair with Copy code and Paste code in the preview.";
    }

    @Override
    public String icon() {
        return "crosshair";
    }

    @Override
    public List<String> conflictingMods() {
        return List.of("custom-crosshair-mod");
    }

    /** Called after vanilla sent an attack; living targets also start the hit marker. */
    public void onAttack(Entity target) {
        long now = System.currentTimeMillis();
        attackAt = now;
        if (target instanceof LivingEntity) hitAt = now;
    }

    /** True when vanilla's crosshair should be cancelled and ours drawn instead. */
    public boolean replacesVanilla() {
        if (!isEnabled()) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null) return false;
        //? if >=1.21.9 {
        if (mc.debugEntries != null && mc.debugEntries.isCurrentlyEnabled(DebugScreenEntries.THREE_DIMENSIONAL_CROSSHAIR)) return false;
        //?} else {
        /*// Before 1.21.9 the F3 screen always brings the 3D crosshair (first person, full debug info).
        //? if >=1.21.6 {
        if (mc.gui.shouldRenderDebugCrosshair()) return false;
        //?} else {
        /^if (mc.getDebugOverlay().showDebugScreen() && mc.options.getCameraType().isFirstPerson()
                && !mc.player.isReducedDebugInfo() && !mc.options.reducedDebugInfo().get()) return false;
        ^///?}
        *///?}
        return true;
    }

    // ---- shape -----------------------------------------------------------------------------------

    private void updateMask(int extraGap) {
        String key = style.get() + ":" + size.get() + ":" + (gap.get() + extraGap) + ":" + thickness.get() + ":" + pixels.get() + ":" + outline.get() + ":" + outlineWidth.get();
        if (key.equals(maskKey)) return;
        maskKey = key;
        CrosshairShape.Mask m = CrosshairShape.mask(style.get().kind(), size.get(), gap.get() + extraGap, thickness.get(), CrosshairShape.decodePixels(pixels.get()));
        maskSize = m.size();
        mainRuns = CrosshairShape.runs(m);
        outlineRuns = outline.get() ? CrosshairShape.runs(CrosshairShape.grow(m, outlineWidth.get())) : List.of();
    }

    /** Draws the crosshair centred on (cx, cy), {@code px} units per crosshair pixel. */
    private void drawShape(GuiGraphics g, int cx, int cy, int px, int c) {
        for (CrosshairShape.Run r : outlineRuns) g.fill(cx + r.x() * px, cy + r.y() * px, cx + (r.x() + r.w()) * px, cy + (r.y() + 1) * px, outlineColor.get());
        for (CrosshairShape.Run r : mainRuns) g.fill(cx + r.x() * px, cy + r.y() * px, cx + (r.x() + r.w()) * px, cy + (r.y() + 1) * px, c);
    }

    /** Draws the crosshair in the HUD layer; respects vanilla's first-person and spectator rules. */
    public void render(GuiGraphics g) {
        if (!replacesVanilla()) return;
        Minecraft mc = Minecraft.getInstance();
        if (!mc.options.getCameraType().isFirstPerson()) return;
        if (mc.gameMode != null && mc.gameMode.getPlayerMode() == GameType.SPECTATOR && mc.crosshairPickEntity == null) return;
        int c = color.get();
        Entity picked = mc.crosshairPickEntity;
        if (highlightCrystal.get() && picked instanceof EndCrystal) c = crystalColor.get();
        else if (highlightTarget.get() && picked instanceof LivingEntity) c = targetColor.get();
        if (dimCharging.get() && mc.player != null && mc.player.getAttackStrengthScale(0f) < 1f) c = Colors.fade(c, 0.5f);
        long now = System.currentTimeMillis();
        int kick = 0;
        if (dynamicGap.get() && style.get() != Style.CUSTOM) {
            float progress = Math.min(1f, (now - attackAt) / (float) GAP_MS);
            kick = Math.round(gapKick.get() * (1f - Render2D.easeOut(progress)));
        }
        updateMask(kick);
        if (pixelPerfect.get()) {
            // One unit per screen pixel: undo the GUI scale and centre on the framebuffer's middle pixel.
            float gs = (float) mc.getWindow().getGuiScale();
            int cx = mc.getWindow().getWidth() / 2;
            int cy = mc.getWindow().getHeight() / 2;
            g.pose().pushMatrix();
            g.pose().scale(1f / gs, 1f / gs);
            drawShape(g, cx, cy, 1, c);
            if (hitMarker.get()) drawHitMarker(g, cx, cy, now);
            g.pose().popMatrix();
        } else {
            int cx = g.guiWidth() / 2;
            int cy = g.guiHeight() / 2;
            drawShape(g, cx, cy, 1, c);
            if (hitMarker.get()) drawHitMarker(g, cx, cy, now);
        }
    }

    private void drawHitMarker(GuiGraphics g, int cx, int cy, long now) {
        long age = now - hitAt;
        if (age < 0 || age >= hitMarkerMs.get()) return;
        float alpha = 1f - age / (float) hitMarkerMs.get();
        int mc2 = Colors.fade(hitMarkerColor.get(), alpha);
        int t = Math.max(1, thickness.get());
        int r0 = gap.get() + size.get() + 3;
        int len = pixelPerfect.get() ? Math.max(4, size.get() / 2 + 2) : 5;
        int half = t / 2;
        int ow = outlineWidth.get();
        for (int i = 0; i < len; i++) {
            int d = r0 + i;
            if (outline.get()) {
                int oc = Colors.fade(outlineColor.get(), alpha);
                for (int[] s : CORNERS) Render2D.fill(g, cx + s[0] * d - half - ow, cy + s[1] * d - half - ow, t + 2 * ow, t + 2 * ow, oc);
            }
            for (int[] s : CORNERS) Render2D.fill(g, cx + s[0] * d - half, cy + s[1] * d - half, t, t, mc2);
        }
    }

    // ---- preview -------------------------------------------------------------------------------

    @Override
    public void previewMouse(int x, int y) {
        mouseX = x;
        mouseY = y;
    }

    @Override
    public int renderPreview(GuiGraphics g, int x, int y, int width) {
        int h = 176;
        Render2D.roundedRect(g, x, y, width, h, Theme.radius(), 0xFF0B0F18);
        Render2D.roundedOutline(g, x, y, width, h, Theme.radius(), Theme.line());
        // Background chips.
        int cxp = x + 8;
        for (int i = 0; i < BACKGROUNDS.length; i++) {
            int w = Fonts.widthInt(BACKGROUNDS[i], Fonts.Weight.MEDIUM, 10) + 10;
            boolean on = i == background;
            boolean hover = inside(cxp, y + 8, w, 18);
            if (on) Render2D.roundedRect(g, cxp, y + 8, w, 18, 4, Theme.control());
            else if (hover) Render2D.roundedRect(g, cxp, y + 8, w, 18, 4, Theme.surfaceHover());
            Fonts.draw(g, BACKGROUNDS[i], Fonts.Weight.MEDIUM, 10, cxp + 5, y + 8 + (18 - Fonts.lineHeight(10)) / 2, on ? Theme.text() : Theme.muted());
            chips[i * 4] = cxp;
            chips[i * 4 + 1] = y + 8;
            chips[i * 4 + 2] = w;
            chips[i * 4 + 3] = 18;
            cxp += w + 2;
        }
        // Copy / paste share codes, bottom right.
        int bw = 74;
        smallButton(g, copyBtn, x + width - 8 - bw * 2 - 4, y + h - 26, bw, "Copy code");
        smallButton(g, pasteBtn, x + width - 8 - bw, y + h - 26, bw, "Paste code");

        int areaY = y + 32;
        int areaH = h - 32 - 34;
        int half = (width - 24) / 2;
        int lx = x + 8;
        int rx = lx + half + 8;
        backdrop(g, lx, areaY, half, areaH);
        backdrop(g, rx, areaY, half, areaH);
        updateMask(0);
        if (pixelPerfect.get()) {
            // Actual size: one crosshair pixel is one physical pixel of this screen.
            float ppu = (float) Render2D.pixelsPerUnit();
            g.pose().pushMatrix();
            g.pose().scale(1f / ppu, 1f / ppu);
            drawShape(g, Math.round((lx + half / 2) * ppu), Math.round((areaY + areaH / 2) * ppu), 1, color.get());
            g.pose().popMatrix();
        } else {
            // Actual size: one crosshair pixel is one GUI unit, which is one design unit at GUI scale 2.
            drawShape(g, lx + half / 2, areaY + areaH / 2, 1, color.get());
        }
        if (style.get() == Style.CUSTOM) {
            drawEditor(g, rx, areaY, half, areaH);
            caption(g, "Click to draw", rx, areaY);
        } else {
            editor[2] = 0;
            int zoom = Math.max(1, Math.min(4, (areaH - 20) / Math.max(1, maskSize)));
            g.enableScissor(rx, areaY, rx + half, areaY + areaH);
            drawShape(g, rx + half / 2 - zoom / 2, areaY + areaH / 2 - zoom / 2, zoom, color.get());
            g.disableScissor();
            caption(g, ZOOM_CAPTIONS[zoom], rx, areaY);
        }
        caption(g, "Actual size", lx, areaY);
        return h;
    }

    private static void caption(GuiGraphics g, String text, int x, int y) {
        int w = Fonts.widthInt(text, Fonts.Weight.MEDIUM, 10) + 8;
        Render2D.roundedRect(g, x + 3, y + 3, w, Fonts.lineHeight(10) + 2, 3, 0xA0000000);
        Fonts.draw(g, text, Fonts.Weight.MEDIUM, 10, x + 7, y + 4, 0xFFE5E7EB);
    }

    private void backdrop(GuiGraphics g, int x, int y, int w, int h) {
        g.enableScissor(x, y, x + w, y + h);
        if (background == 0 || BG_SPRITES[background] == null) {
            Render2D.gradientV(g, x, y, w, h, 0xFF6FA0F0, 0xFFBFD6FF);
        } else {
            try {
                TextureAtlasSprite sprite = gg.shard.client.compat.Atlases.sprite(BG_SPRITES[background]);
                int tint = background == 1 ? 0xFF79C05A : 0xFFFFFFFF;
                for (int ty = y; ty < y + h; ty += 32) for (int tx = x; tx < x + w; tx += 32) g.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, tx, ty, 32, 32, tint);
            } catch (RuntimeException e) {
                g.fill(x, y, x + w, y + h, 0xFF404040);
            }
        }
        g.disableScissor();
        Render2D.roundedOutline(g, x, y, w, h, 4, Theme.line());
    }

    private void drawEditor(GuiGraphics g, int x, int y, int w, int h) {
        int n = CrosshairShape.CUSTOM;
        int cell = Math.max(3, Math.min((w - 8) / n, (h - 8) / n));
        int gw = cell * n;
        int gx = x + (w - gw) / 2;
        int gy = y + (h - gw) / 2;
        editor[0] = gx;
        editor[1] = gy;
        editor[2] = cell;
        g.fill(gx, gy, gx + gw, gy + gw, 0x66000000);
        boolean[] px = CrosshairShape.decodePixels(pixels.get());
        for (int j = 0; j < n; j++) {
            for (int i = 0; i < n; i++) {
                if (px[j * n + i]) g.fill(gx + i * cell, gy + j * cell, gx + (i + 1) * cell, gy + (j + 1) * cell, color.get());
            }
        }
        for (int k = 0; k <= n; k++) {
            int line = k == n / 2 || k == n / 2 + 1 ? 0x55FFFFFF : 0x22FFFFFF;
            g.fill(gx + k * cell, gy, gx + k * cell + 1, gy + gw, line);
            g.fill(gx, gy + k * cell, gx + gw, gy + k * cell + 1, line);
        }
    }

    private void smallButton(GuiGraphics g, int[] rect, int x, int y, int w, String label) {
        rect[0] = x;
        rect[1] = y;
        rect[2] = w;
        rect[3] = 18;
        boolean hover = inside(x, y, w, 18);
        Render2D.roundedRect(g, x, y, w, 18, 4, hover ? Theme.surfaceHover() : Theme.surfaceRaised());
        Render2D.roundedOutline(g, x, y, w, 18, 4, Theme.lineStrong());
        Fonts.drawCentered(g, label, Fonts.Weight.MEDIUM, 10, x + w / 2, y + (18 - Fonts.lineHeight(10)) / 2, Theme.text());
    }

    private boolean inside(int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    private static boolean in(int[] r, int mx, int my) {
        return mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3];
    }

    @Override
    public String previewInput(int mx, int my, int button, boolean drag) {
        if (!drag) {
            for (int i = 0; i < BACKGROUNDS.length; i++) {
                if (mx >= chips[i * 4] && mx < chips[i * 4] + chips[i * 4 + 2] && my >= chips[i * 4 + 1] && my < chips[i * 4 + 1] + chips[i * 4 + 3]) {
                    background = i;
                    return null;
                }
            }
            if (in(copyBtn, mx, my)) {
                Minecraft.getInstance().keyboardHandler.setClipboard(CrosshairShape.encode(spec()));
                return "Crosshair code copied";
            }
            if (in(pasteBtn, mx, my)) {
                CrosshairShape.Spec s = CrosshairShape.decode(Minecraft.getInstance().keyboardHandler.getClipboard());
                if (s == null) return "The clipboard has no crosshair code";
                apply(s);
                return "Crosshair pasted";
            }
        }
        int cell = editor[2];
        if (cell > 0 && style.get() == Style.CUSTOM) {
            int i = (mx - editor[0]) / cell;
            int j = (my - editor[1]) / cell;
            int n = CrosshairShape.CUSTOM;
            if (mx >= editor[0] && my >= editor[1] && i < n && j < n) {
                boolean[] px = CrosshairShape.decodePixels(pixels.get());
                boolean paint = button != GLFW.GLFW_MOUSE_BUTTON_RIGHT;
                if (px[j * n + i] != paint) {
                    px[j * n + i] = paint;
                    pixels.set(CrosshairShape.encodePixels(px));
                }
            }
        }
        return null;
    }

    private CrosshairShape.Spec spec() {
        return new CrosshairShape.Spec(style.get().kind(), size.get(), gap.get(), thickness.get(), color.get(), outline.get(),
                outlineColor.get(), outlineWidth.get(), pixels.get(), pixelPerfect.get());
    }

    private void apply(CrosshairShape.Spec s) {
        style.set(Style.valueOf(s.kind().name()));
        size.set(s.size());
        gap.set(s.gap());
        thickness.set(s.thickness());
        color.set(s.color());
        outline.set(s.outline());
        outlineColor.set(s.outlineColor());
        outlineWidth.set(s.outlineWidth());
        if (s.pixels() != null && !s.pixels().isEmpty()) pixels.set(s.pixels());
        pixelPerfect.set(s.pixelPerfect());
    }

    // ---- config ------------------------------------------------------------------------------

    @Override
    protected void saveExtra(JsonObject out) {
        out.addProperty(UNITS_MARKER, true);
    }

    /**
     * Configs from before Pixel-perfect stored their sizes in GUI pixels. When such a file
     * changed any size, keep drawing in GUI pixels and keep the old defaults for the sizes it
     * left alone, so the crosshair looks exactly as it did.
     */
    @Override
    protected void loadExtra(JsonObject in) {
        if (in.has(UNITS_MARKER)) return;
        JsonObject values = in.has("settings") && in.get("settings").isJsonObject() ? in.getAsJsonObject("settings") : new JsonObject();
        if (!values.has("size") && !values.has("gap") && !values.has("thickness") && !values.has("gap-kick")) return;
        pixelPerfect.set(false);
        if (!values.has("size")) size.set(LEGACY_SIZE);
        if (!values.has("gap")) gap.set(LEGACY_GAP);
        if (!values.has("thickness")) thickness.set(LEGACY_THICKNESS);
        if (!values.has("gap-kick")) gapKick.set(LEGACY_KICK);
    }
}
