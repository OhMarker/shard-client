package gg.shard.client.hud;

import gg.shard.client.ShardClient;
import gg.shard.client.gui.ClickGuiScreen;
import gg.shard.client.gui.DesignScreen;
import gg.shard.client.gui.Fonts;
import gg.shard.client.gui.Icons;
import gg.shard.client.gui.Render2D;
import gg.shard.client.gui.Scale;
import gg.shard.client.gui.TextInput;
import gg.shard.client.gui.Theme;
import gg.shard.client.util.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The HUD editor: drag elements with smart snapping (screen edges, centre lines and other
 * elements, with guide lines; hold Alt to place freely), Shift-click or drag a box to select
 * several and align or distribute them, arrow keys nudge by one physical pixel (Shift: ten),
 * scroll scales, Ctrl+Z / Ctrl+Y undo and redo, G toggles a grid, Delete hides, right-click
 * resets. Clicking an element opens its settings in a side panel without leaving the editor.
 * Layout presets (three built in for crystal PvP, plus your own) live in the toolbar.
 *
 * <p>Elements are positioned in GUI units and stored as an anchor plus an offset
 * ({@link HudGeometry}); the editor's chrome is drawn in design units so it looks the same at
 * every GUI scale.
 */
public final class HudEditorScreen extends DesignScreen {
    private static final int GRID_UNITS = 8;
    private static final double SNAP_PX = 6;
    private static final int BAR_H = 36;
    private static final int BTN = 28;

    private record Hit(String key, int x, int y, int w, int h, Runnable onClick) {
        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    /** One toolbar item: kind is title, text, icon, sep or scale. */
    private record Item(String kind, String key, String label, Runnable action, boolean enabled, boolean on) {}

    private final Screen parent;
    private final HudManager hud;
    private final Set<HudModule> selection = new LinkedHashSet<>();
    private final UndoStack<Map<String, HudPresets.Entry>> undo = new UndoStack<>(64);
    private final List<Hit> hits = new ArrayList<>();
    private final TextInput presetName = new TextInput(32).placeholder("Save current as…");

    private HudModule primary;
    private boolean dragging;
    private boolean moved;
    private double pressX;
    private double pressY;
    private Map<String, HudPresets.Entry> pressSnapshot;
    private final Map<HudModule, double[]> startPos = new HashMap<>();
    private boolean marquee;
    private double mqX1;
    private double mqY1;
    private List<Double> guidesV = List.of();
    private List<Double> guidesH = List.of();
    private boolean gridOn;
    private boolean presetsOpen;
    private boolean presetInputFocused;
    private int presetsAnchorX;
    private int presetsAnchorY;
    private String toast;
    private long toastUntil;

    private ClickGuiScreen panel;
    private boolean panelOpen;
    private boolean panelPressed;
    private int mouseDX;
    private int mouseDY;

    public HudEditorScreen(Screen parent, HudManager hud) {
        super(Component.literal("Shard HUD editor"));
        this.parent = parent;
        this.hud = hud;
    }

    @Override
    protected void init() {
        var gui = ShardClient.config().gui();
        if (gui.has("hudGrid")) gridOn = gui.get("hudGrid").getAsBoolean();
        if (panel == null) panel = ClickGuiScreen.embedded(() -> panelOpen = false);
        //? if >=1.21.11 {
        panel.init(width, height);
        //?} else {
        /*panel.init(minecraft, width, height);
        *///?}
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0x55000000);
    }

    // ---- rendering -----------------------------------------------------------------------------

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        beginFrame();
        int guiScale = minecraft.getWindow().getGuiScale();
        HudManager.updateScale(minecraft);
        hits.clear();

        if (gridOn) {
            double step = GRID_UNITS * HudManager.hudScale();
            int line = 0x14FFFFFF;
            for (double x = 0; x < width; x += step) g.fill((int) x, 0, (int) x + 1, height, line);
            for (double y = 0; y < height; y += step) g.fill(0, (int) y, width, (int) y + 1, line);
        } else {
            // Centre lines are always faintly visible.
            g.fill(width / 2, 0, width / 2 + 1, height, 0x10FFFFFF);
            g.fill(0, height / 2, width, height / 2 + 1, 0x10FFFFFF);
        }

        HudModule hovered = dragging ? null : at(mouseX, mouseY);
        for (HudModule m : hud.hudModules()) {
            if (!m.isEnabled()) continue;
            HudManager.renderOne(g, minecraft.getDeltaTracker(), m);
            double x = m.posX(width);
            double y = m.posY(height);
            double w = m.scaledWidthExact();
            double h = m.scaledHeightExact();
            boolean sel = selection.contains(m);
            boolean hov = m == hovered;
            int fill = sel ? Theme.accentAlpha(0x26) : hov ? 0x14FFFFFF : 0x0AFFFFFF;
            int border = sel ? Theme.accent() : hov ? 0x88FFFFFF : 0x33FFFFFF;
            fillD(g, x - 2, y - 2, w + 4, h + 4, fill);
            outlineD(g, x - 2, y - 2, w + 4, h + 4, border);
        }

        int guide = Theme.accentAlpha(0xC0);
        for (double v : guidesV) fillD(g, v - 0.5 / guiScale, 0, 1.0 / guiScale, height, guide);
        for (double hy : guidesH) fillD(g, 0, hy - 0.5 / guiScale, width, 1.0 / guiScale, guide);
        if (marquee) {
            double x0 = Math.min(pressX, mqX1), y0 = Math.min(pressY, mqY1);
            double w = Math.abs(mqX1 - pressX), h = Math.abs(mqY1 - pressY);
            fillD(g, x0, y0, w, h, Theme.accentAlpha(0x18));
            outlineD(g, x0, y0, w, h, Theme.accentAlpha(0xA0));
        }

        // Chrome in design units.
        Render2D.setPixelsPerUnit(Scale.pixelsPerUnit(pageScale, guiScale));
        pushDesign(g);
        mouseDX = (int) Math.floor(toDesign(mouseX));
        mouseDY = (int) Math.floor(toDesign(mouseY));
        HudModule labelled = selection.size() == 1 ? selection.iterator().next() : hovered;
        if (labelled != null && labelled.isEnabled()) renderLabel(g, labelled);
        if (!dragging && !marquee) {
            renderToolbar(g);
            if (presetsOpen) renderPresets(g);
        }
        renderToast(g);
        popDesign(g);

        //? if >=26.1 {
        /*if (panelOpen) panel.extractRenderState(g, mouseX, mouseY, partialTick);
        *///?} else {
        if (panelOpen) panel.render(g, mouseX, mouseY, partialTick);
        //?}

    }

    /** A fill at fractional GUI coordinates (GuiGraphics.fill only takes ints). */
    private static void fillD(GuiGraphics g, double x, double y, double w, double h, int color) {
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate((float) x, (float) y);
        pose.scale((float) w, (float) h);
        g.fill(0, 0, 1, 1, color);
        pose.popMatrix();
    }

    /** One-physical-pixel outline. */
    private static void outlineD(GuiGraphics g, double x, double y, double w, double h, int color) {
        double t = 1.0 / Math.max(1, Minecraft.getInstance().getWindow().getGuiScale());
        fillD(g, x, y, w, t, color);
        fillD(g, x, y + h - t, w, t, color);
        fillD(g, x, y, t, h, color);
        fillD(g, x + w - t, y, t, h, color);
    }

    private void renderLabel(GuiGraphics g, HudModule m) {
        String label = m.name() + (m.scale() != 1.0 ? "  " + Math.round(m.scale() * 100) + "%" : "");
        int size = 11;
        int lw = Fonts.widthInt(label, Fonts.Weight.MEDIUM, size) + 14;
        int lh = Fonts.lineHeight(size) + 6;
        int lx = (int) Math.round(toDesign(m.posX(width)));
        int ly = (int) Math.round(toDesign(m.posY(height))) - lh - 6;
        if (ly < 4) ly = (int) Math.round(toDesign(m.posY(height) + m.scaledHeightExact())) + 6;
        lx = Math.max(4, Math.min(designW - lw - 4, lx));
        Render2D.roundedRect(g, lx, ly, lw, lh, Theme.radiusSmall(), Theme.popover());
        Fonts.draw(g, label, Fonts.Weight.MEDIUM, size, lx + 7, ly + 3, Theme.text());
    }

    /** Right edge available to the chrome: the side panel takes the right of the screen. */
    private int chromeRight() {
        return panelOpen ? (int) Math.floor(toDesign(panel.panelLeftGui())) - 8 : designW;
    }

    private List<Item> toolbarItems(List<HudModule> sel) {
        List<Item> items = new ArrayList<>();
        items.add(new Item("title", "title", "HUD editor", null, true, false));
        items.add(new Item("text", "presets", "Presets", () -> {
            presetsOpen = !presetsOpen;
            presetInputFocused = false;
        }, true, presetsOpen));
        items.add(new Item("sep", "", "", null, true, false));
        items.add(new Item("icon", "grid", gridOn ? "grid-on" : "grid", this::toggleGrid, true, gridOn));
        items.add(new Item("icon", "undo", "undo", this::undo, undo.canUndo(), false));
        items.add(new Item("icon", "redo", "redo", this::redo, undo.canRedo(), false));
        if (sel.size() >= 2) {
            items.add(new Item("sep", "", "", null, true, false));
            String[][] aligns = {{"align-left", "LEFT"}, {"align-center-x", "CENTER_X"}, {"align-right", "RIGHT"},
                    {"align-top", "TOP"}, {"align-center-y", "CENTER_Y"}, {"align-bottom", "BOTTOM"}};
            for (String[] a : aligns) {
                HudGeometry.Align mode = HudGeometry.Align.valueOf(a[1]);
                items.add(new Item("icon", a[0], a[0], () -> align(mode), true, false));
            }
            if (sel.size() >= 3) {
                items.add(new Item("icon", "distribute-h", "distribute-h", () -> distribute(true), true, false));
                items.add(new Item("icon", "distribute-v", "distribute-v", () -> distribute(false), true, false));
            }
        }
        if (!sel.isEmpty()) {
            items.add(new Item("sep", "", "", null, true, false));
            items.add(new Item("icon", "smaller", "minus", () -> scaleSelection(-0.05), true, false));
            items.add(new Item("scale", "scale", "", null, true, false));
            items.add(new Item("icon", "bigger", "plus", () -> scaleSelection(0.05), true, false));
            items.add(new Item("icon", "reset", "reset", this::resetSelection, true, false));
        }
        items.add(new Item("sep", "", "", null, true, false));
        items.add(new Item("text", "done", "Done", this::onClose, true, false));
        return items;
    }

    /** Icon-only title when the toolbar would not fit (the side panel takes the right of the screen). */
    private boolean compactBar;

    private int itemWidth(Item it) {
        return switch (it.kind()) {
            case "title" -> compactBar ? 24 : Fonts.widthInt(it.label(), Fonts.Weight.SEMIBOLD, 13) + 30;
            case "text" -> Fonts.widthInt(it.label(), Fonts.Weight.MEDIUM, 12) + 24 + ("presets".equals(it.key()) ? 16 : 0);
            case "sep" -> 9;
            case "scale" -> 40;
            default -> BTN;
        };
    }

    private void renderToolbar(GuiGraphics g) {
        List<HudModule> sel = selectedEnabled();
        List<Item> items = toolbarItems(sel);
        int right = chromeRight();
        compactBar = false;
        int barW = 16;
        for (Item it : items) barW += itemWidth(it) + 2;
        if (barW > right - 16) {
            compactBar = true;
            barW = 16;
            for (Item it : items) barW += itemWidth(it) + 2;
        }
        // Still too wide beside the side panel: wrap onto further bars so every button stays visible.
        List<List<Item>> rows = new ArrayList<>();
        List<Item> row = new ArrayList<>();
        int rowW = 16;
        for (Item it : items) {
            int w = itemWidth(it) + 2;
            if (!row.isEmpty() && rowW + w > right - 16) {
                rows.add(row);
                row = new ArrayList<>();
                rowW = 16;
            }
            if (row.isEmpty() && "sep".equals(it.kind())) continue;
            row.add(it);
            rowW += w;
        }
        if (!row.isEmpty()) rows.add(row);
        int by = 10;
        for (List<Item> r : rows) {
            renderToolbarRow(g, r, sel, right, by);
            by += BAR_H + 6;
        }
        by -= BAR_H + 6;

        String hint = selection.isEmpty()
                ? "Drag to move  ·  Shift-click or drag a box to select several  ·  Scroll to scale  ·  Click to edit settings"
                : "Arrows nudge 1 px (Shift: 10)  ·  Ctrl Z / Ctrl Y  ·  G grid  ·  Alt drags freely  ·  Delete hides";
        int hy = by + BAR_H + 6;
        int hw = Fonts.widthInt(hint, Fonts.Weight.REGULAR, 11);
        if (hw + 16 <= right - 16 && !presetsOpen && !compactBar) {
            Render2D.roundedRect(g, (right - hw) / 2 - 8, hy, hw + 16, Fonts.lineHeight(11) + 6, Theme.radiusSmall(), Colors.withAlpha(Theme.surface(), 0xC8));
            Fonts.draw(g, hint, Fonts.Weight.REGULAR, 11, (right - hw) / 2, hy + 3, Theme.muted());
        }
    }

    private void renderToolbarRow(GuiGraphics g, List<Item> items, List<HudModule> sel, int right, int by) {
        int barW = 16;
        for (Item it : items) barW += itemWidth(it) + 2;
        int bx = Math.max(8, (right - barW) / 2);
        Render2D.shadow(g, bx, by, barW, BAR_H, Theme.radiusLarge(), 0.5);
        Render2D.panel(g, bx, by, barW, BAR_H, Theme.radiusLarge(), Theme.surface(), Theme.lineStrong());
        hits.add(new Hit("bar", bx, by, barW, BAR_H, () -> {}));
        int cx = bx + 8;
        int cy = by + (BAR_H - BTN) / 2;
        for (Item it : items) {
            int w = itemWidth(it);
            switch (it.kind()) {
                case "title" -> {
                    Icons.draw(g, "edit-hud", cx + 4, by + (BAR_H - 14) / 2, 14, Theme.accent());
                    if (!compactBar) Fonts.draw(g, it.label(), Fonts.Weight.SEMIBOLD, 13, cx + 24, by + (BAR_H - Fonts.lineHeight(13)) / 2, Theme.text());
                }
                case "text" -> {
                    if ("presets".equals(it.key())) {
                        presetsAnchorX = cx;
                        presetsAnchorY = by + BAR_H + 4;
                    }
                    button(g, it.key(), cx, cy, w, BTN, it.label(), "done".equals(it.key()), it.on(), it.action());
                    if ("presets".equals(it.key())) Icons.draw(g, "chevron-down", cx + w - 20, cy + (BTN - 12) / 2, 12, Theme.muted());
                }
                case "sep" -> g.fill(cx + 4, by + 9, cx + 5, by + BAR_H - 9, Theme.line());
                case "scale" -> {
                    String pct = sel.size() == 1 ? Math.round(sel.get(0).scale() * 100) + "%" : "Scale";
                    Fonts.drawCentered(g, pct, Fonts.Weight.MEDIUM, 11, cx + w / 2, by + (BAR_H - Fonts.lineHeight(11)) / 2, Theme.muted());
                }
                default -> iconButton(g, it.key(), it.label(), cx, cy, it.enabled(), it.on(), it.action());
            }
            cx += w + 2;
        }
    }

    private void renderPresets(GuiGraphics g) {
        List<String> builtIn = HudPresets.builtIn();
        List<String> saved = HudPresets.savedNames();
        int w = 240;
        int itemH = 28;
        int capH = Fonts.lineHeight(10) + 4;
        int h = 8 + capH + builtIn.size() * itemH + (saved.isEmpty() ? 0 : 8 + capH + saved.size() * itemH) + 12 + BTN + 10;
        int x = Math.max(8, Math.min(chromeRight() - w - 8, presetsAnchorX));
        int y = presetsAnchorY;
        Render2D.shadow(g, x, y, w, h, Theme.radiusLarge(), 0.6);
        Render2D.panel(g, x, y, w, h, Theme.radiusLarge(), Theme.popover(), Theme.lineStrong());
        hits.add(new Hit("presets-panel", x, y, w, h, () -> {}));
        int cy = y + 8;
        Fonts.draw(g, "BUILT IN", Fonts.Weight.MEDIUM, 10, x + 12, cy, Theme.subtle());
        cy += capH;
        for (String name : builtIn) {
            presetRow(g, name, x + 4, cy, w - 8, itemH, false);
            cy += itemH;
        }
        if (!saved.isEmpty()) {
            cy += 8;
            Fonts.draw(g, "YOURS", Fonts.Weight.MEDIUM, 10, x + 12, cy, Theme.subtle());
            cy += capH;
            for (String name : saved) {
                presetRow(g, name, x + 4, cy, w - 8, itemH, true);
                cy += itemH;
            }
        }
        cy += 6;
        g.fill(x + 1, cy, x + w - 1, cy + 1, Theme.line());
        cy += 6;
        int fieldW = w - 24 - BTN - 6;
        presetName.render(g, x + 12, cy, fieldW, BTN, presetInputFocused);
        hits.add(new Hit("preset-name", x + 12, cy, fieldW, BTN, () -> {
            presetInputFocused = true;
            presetName.cursorToEnd();
        }));
        iconButton(g, "preset-save", "save", x + w - 12 - BTN, cy, !presetName.isEmpty(), false, this::savePreset);
    }

    private void presetRow(GuiGraphics g, String name, int x, int y, int w, int h, boolean deletable) {
        Hit row = new Hit("preset:" + name, x, y, w - (deletable ? h : 0), h, () -> applyPreset(name));
        hits.add(row);
        boolean hover = row.contains(mouseDX, mouseDY);
        if (hover) Render2D.roundedRect(g, x, y, w, h, Theme.radiusSmall(), Theme.surfaceHover());
        Icons.draw(g, deletable ? "profile" : "hud", x + 8, y + (h - 14) / 2, 14, Theme.muted());
        Fonts.drawClipped(g, name, Fonts.Weight.MEDIUM, 12, x + 30, y + (h - Fonts.lineHeight(12)) / 2, w - 30 - (deletable ? h + 4 : 8), Theme.text());
        if (deletable) iconButton(g, "preset-del:" + name, "trash", x + w - h, y, true, false, () -> {
            HudPresets.delete(name);
            showToast("Deleted " + name);
        });
    }

    private void button(GuiGraphics g, String key, int x, int y, int w, int h, String label, boolean primaryBtn, boolean active, Runnable onClick) {
        Hit hit = new Hit(key, x, y, w, h, onClick);
        hits.add(hit);
        boolean hover = hit.contains(mouseDX, mouseDY);
        int fill = primaryBtn ? (hover ? Theme.accentHover() : Theme.accent()) : active ? Theme.control() : hover ? Theme.surfaceHover() : 0x00000000;
        if (Colors.alpha(fill) > 0) Render2D.roundedRect(g, x, y, w, h, Theme.radiusSmall(), fill);
        int color = primaryBtn ? Theme.accentText() : Theme.text();
        int tw = Fonts.widthInt(label, Fonts.Weight.MEDIUM, 12);
        int tx = "presets".equals(key) ? x + 12 : x + (w - tw) / 2;
        Fonts.draw(g, label, Fonts.Weight.MEDIUM, 12, tx, y + (h - Fonts.lineHeight(12)) / 2, color);
    }

    private void iconButton(GuiGraphics g, String key, String icon, int x, int y, boolean enabled, boolean on, Runnable onClick) {
        Hit hit = new Hit(key, x, y, BTN, BTN, enabled && onClick != null ? onClick : () -> {});
        hits.add(hit);
        boolean hover = enabled && hit.contains(mouseDX, mouseDY);
        if (on) Render2D.roundedRect(g, x, y, BTN, BTN, Theme.radiusSmall(), Theme.control());
        else if (hover) Render2D.roundedRect(g, x, y, BTN, BTN, Theme.radiusSmall(), Theme.surfaceHover());
        int color = !enabled ? Colors.withAlpha(Theme.subtle(), 0x90) : on ? Theme.accent() : hover ? Theme.text() : Theme.muted();
        Icons.draw(g, icon, x + (BTN - 16) / 2, y + (BTN - 16) / 2, 16, color);
    }

    private void renderToast(GuiGraphics g) {
        if (toast == null) return;
        long left = toastUntil - System.currentTimeMillis();
        if (left <= 0) {
            toast = null;
            return;
        }
        float a = Math.min(1f, left / 300f);
        int w = Fonts.widthInt(toast, Fonts.Weight.MEDIUM, 12) + 28;
        int h = Fonts.lineHeight(12) + 14;
        int x = (chromeRight() - w) / 2;
        int y = designH - 16 - h;
        Render2D.roundedRect(g, x, y, w, h, Theme.radiusSmall(), Colors.fade(Theme.popover(), a));
        Render2D.roundedOutline(g, x, y, w, h, Theme.radiusSmall(), Colors.fade(Theme.lineStrong(), a));
        Fonts.drawCentered(g, toast, Fonts.Weight.MEDIUM, 12, x + w / 2, y + 7, Colors.fade(Theme.text(), a));
    }

    private void showToast(String text) {
        toast = text;
        toastUntil = System.currentTimeMillis() + 2000;
    }

    // ---- actions -------------------------------------------------------------------------------

    private List<HudModule> selectedEnabled() {
        List<HudModule> out = new ArrayList<>();
        for (HudModule m : selection) if (m.isEnabled()) out.add(m);
        return out;
    }

    private Map<String, HudPresets.Entry> snapshot() {
        return HudPresets.snapshot(hud);
    }

    private void record(String mergeKey) {
        undo.record(snapshot(), mergeKey, System.currentTimeMillis(), 700);
    }

    public void undo() {
        Map<String, HudPresets.Entry> s = undo.undo(snapshot());
        if (s != null) {
            HudPresets.restore(hud, s);
            showToast("Undone");
        }
    }

    public void redo() {
        Map<String, HudPresets.Entry> s = undo.redo(snapshot());
        if (s != null) {
            HudPresets.restore(hud, s);
            showToast("Redone");
        }
    }

    private void toggleGrid() {
        gridOn = !gridOn;
        ShardClient.config().gui().addProperty("hudGrid", gridOn);
        ShardClient.config().markDirty();
    }

    private HudGeometry.Rect rect(HudModule m) {
        return new HudGeometry.Rect(m.posX(width), m.posY(height), m.scaledWidthExact(), m.scaledHeightExact());
    }

    private void align(HudGeometry.Align mode) {
        List<HudModule> sel = selectedEnabled();
        if (sel.size() < 2) return;
        record(null);
        List<HudGeometry.Rect> rects = new ArrayList<>();
        for (HudModule m : sel) rects.add(rect(m));
        List<double[]> pos = HudGeometry.align(rects, mode);
        for (int i = 0; i < sel.size(); i++) sel.get(i).setPosition(pos.get(i)[0], pos.get(i)[1], width, height);
        ShardClient.config().markDirty();
    }

    private void distribute(boolean horizontal) {
        List<HudModule> sel = selectedEnabled();
        if (sel.size() < 3) return;
        record(null);
        List<HudGeometry.Rect> rects = new ArrayList<>();
        for (HudModule m : sel) rects.add(rect(m));
        List<double[]> pos = HudGeometry.distribute(rects, horizontal);
        for (int i = 0; i < sel.size(); i++) sel.get(i).setPosition(pos.get(i)[0], pos.get(i)[1], width, height);
        ShardClient.config().markDirty();
    }

    private void scaleSelection(double delta) {
        List<HudModule> sel = selectedEnabled();
        if (sel.isEmpty()) return;
        record("scale");
        for (HudModule m : sel) {
            double x = m.posX(width), y = m.posY(height);
            m.setScale(m.scale() + delta);
            m.setPosition(x, y, width, height);
        }
        ShardClient.config().markDirty();
    }

    private void resetSelection() {
        List<HudModule> sel = selectedEnabled();
        if (sel.isEmpty()) return;
        record(null);
        for (HudModule m : sel) m.resetExtra();
        ShardClient.config().markDirty();
    }

    /** Applies a preset (presets menu and smoke test); undoable. */
    public void applyPreset(String name) {
        record(null);
        if (HudPresets.apply(hud, name)) {
            selection.clear();
            showToast("Applied " + name);
        }
        presetsOpen = false;
    }

    private void savePreset() {
        String name = presetName.value().trim();
        if (HudPresets.save(hud, name)) {
            showToast("Saved " + name);
            presetName.setValue("");
            presetInputFocused = false;
        } else showToast(name.isEmpty() ? "Type a name first" : "That name is taken by a built-in preset");
    }

    /** Opens the side panel for a module (also used by the smoke test). */
    public void openPanel(HudModule m) {
        //? if >=1.21.11 {
        panel.init(width, height);
        //?} else {
        /*panel.init(minecraft, width, height);
        *///?}
        panel.openModule(m);
        panelOpen = true;
    }

    public void select(HudModule... modules) {
        selection.clear();
        selection.addAll(List.of(modules));
    }

    public void setPresetsOpen(boolean open) {
        presetsOpen = open;
    }

    /** Smoke test: starts a drag of the first selected element and moves it by (dx, dy) GUI units, leaving the guides up. */
    public void previewDrag(double dx, double dy) {
        if (selection.isEmpty()) return;
        HudModule m = selection.iterator().next();
        pressX = m.posX(width);
        pressY = m.posY(height);
        startDrag(m);
        dragTo(pressX + dx, pressY + dy);
    }

    /** Smoke test: ends a {@link #previewDrag}. */
    public void endPreviewDrag() {
        dragging = false;
        primary = null;
        guidesV = List.of();
        guidesH = List.of();
    }

    // ---- mouse ---------------------------------------------------------------------------------

    private HudModule at(double mx, double my) {
        List<HudModule> list = hud.hudModules();
        for (int i = list.size() - 1; i >= 0; i--) {
            HudModule m = list.get(i);
            if (!m.isEnabled()) continue;
            double x = m.posX(width), y = m.posY(height);
            if (mx >= x - 2 && mx <= x + m.scaledWidthExact() + 2 && my >= y - 2 && my <= y + m.scaledHeightExact() + 2) return m;
        }
        return null;
    }

    private Hit chromeAt(double dx, double dy) {
        for (int i = hits.size() - 1; i >= 0; i--) if (hits.get(i).contains(dx, dy)) return hits.get(i);
        return null;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        panelPressed = false;
        if (panelOpen && panel.wantsInput(mx, my)) {
            panelPressed = true;
            presetInputFocused = false;
            return panel.mouseClicked(event, doubleClick);
        }
        Hit chrome = chromeAt(toDesign(mx), toDesign(my));
        if (chrome != null) {
            if (!chrome.key().equals("preset-name")) presetInputFocused = false;
            if (!chrome.key().startsWith("preset") && !chrome.key().equals("presets")) presetsOpen = false;
            chrome.onClick().run();
            return true;
        }
        presetsOpen = false;
        presetInputFocused = false;
        HudModule m = at(mx, my);
        boolean shift = (event.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0;
        if (m == null) {
            if (!shift) selection.clear();
            marquee = event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT;
            pressX = mx;
            pressY = my;
            mqX1 = mx;
            mqY1 = my;
            return true;
        }
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            record(null);
            m.resetExtra();
            ShardClient.config().markDirty();
            return true;
        }
        if (shift) {
            if (!selection.remove(m)) selection.add(m);
        } else if (!selection.contains(m)) {
            selection.clear();
            selection.add(m);
        }
        pressX = mx;
        pressY = my;
        startDrag(m);
        return true;
    }

    private void startDrag(HudModule m) {
        primary = m;
        dragging = true;
        moved = false;
        pressSnapshot = snapshot();
        startPos.clear();
        for (HudModule s : selection) if (s.isEnabled()) startPos.put(s, new double[]{s.posX(width), s.posY(height)});
        startPos.put(m, new double[]{m.posX(width), m.posY(height)});
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (panelPressed) return panel.mouseDragged(event, dx, dy);
        if (marquee) {
            mqX1 = event.x();
            mqY1 = event.y();
            return true;
        }
        if (!dragging || primary == null) return false;
        dragTo(event.x(), event.y());
        return true;
    }

    private void dragTo(double mx, double my) {
        if (!moved && Math.hypot(mx - pressX, my - pressY) < 1.5) return;
        if (!moved) {
            undo.record(pressSnapshot);
            moved = true;
        }
        double[] start = startPos.get(primary);
        double nx = start[0] + (mx - pressX);
        double ny = start[1] + (my - pressY);
        double guiScale = minecraft.getWindow().getGuiScale();
        if (gridOn) {
            double step = GRID_UNITS * HudManager.hudScale();
            nx = Math.round(nx / step) * step;
            ny = Math.round(ny / step) * step;
        }
        guidesV = List.of();
        guidesH = List.of();
        if (!minecraft.hasAltDown()) {
            List<HudGeometry.Rect> others = new ArrayList<>();
            for (HudModule o : hud.hudModules()) if (o.isEnabled() && !startPos.containsKey(o)) others.add(rect(o));
            HudGeometry.Snap snap = HudGeometry.snap(new HudGeometry.Rect(nx, ny, primary.scaledWidthExact(), primary.scaledHeightExact()),
                    width, height, others, SNAP_PX / guiScale);
            nx = snap.x();
            ny = snap.y();
            guidesV = snap.verticalGuides();
            guidesH = snap.horizontalGuides();
        }
        // Whole physical pixels.
        nx = Math.round(nx * guiScale) / guiScale;
        ny = Math.round(ny * guiScale) / guiScale;
        double ddx = nx - start[0], ddy = ny - start[1];
        for (Map.Entry<HudModule, double[]> e : startPos.entrySet()) {
            e.getKey().setPosition(e.getValue()[0] + ddx, e.getValue()[1] + ddy, width, height);
        }
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (panelPressed) {
            panelPressed = false;
            return panel.mouseReleased(event);
        }
        if (marquee) {
            marquee = false;
            double x0 = Math.min(pressX, mqX1), y0 = Math.min(pressY, mqY1);
            double x1 = Math.max(pressX, mqX1), y1 = Math.max(pressY, mqY1);
            for (HudModule m : hud.hudModules()) {
                if (!m.isEnabled()) continue;
                HudGeometry.Rect r = rect(m);
                if (r.x() < x1 && r.x() + r.w() > x0 && r.y() < y1 && r.y() + r.h() > y0) selection.add(m);
            }
            return true;
        }
        if (dragging) {
            dragging = false;
            guidesV = List.of();
            guidesH = List.of();
            if (moved) ShardClient.config().markDirty();
            else if (primary != null && (event.modifiers() & GLFW.GLFW_MOD_SHIFT) == 0) openPanel(primary);
            primary = null;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (panelOpen && panel.wantsInput(mx, my)) return panel.mouseScrolled(mx, my, sx, sy);
        HudModule m = at(mx, my);
        if (m == null) return false;
        record("scale:" + m.key());
        double x = m.posX(width), y = m.posY(height);
        m.setScale(m.scale() + (sy > 0 ? 0.05 : -0.05));
        m.setPosition(x, y, width, height);
        ShardClient.config().markDirty();
        return true;
    }

    // ---- keyboard ------------------------------------------------------------------------------

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        int mods = event.modifiers();
        boolean ctrl = (mods & GLFW.GLFW_MOD_CONTROL) != 0;
        boolean shift = (mods & GLFW.GLFW_MOD_SHIFT) != 0;
        if (presetInputFocused) {
            if (key == GLFW.GLFW_KEY_ESCAPE) presetInputFocused = false;
            else if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) savePreset();
            else presetName.keyPressed(key, mods);
            return true;
        }
        if (panelOpen && panel.capturesKeys()) return panel.keyPressed(event);
        if (ctrl && key == GLFW.GLFW_KEY_Z) {
            if (shift) redo();
            else undo();
            return true;
        }
        if (ctrl && key == GLFW.GLFW_KEY_Y) {
            redo();
            return true;
        }
        if (ctrl && key == GLFW.GLFW_KEY_A) {
            selection.clear();
            for (HudModule m : hud.hudModules()) if (m.isEnabled()) selection.add(m);
            return true;
        }
        switch (key) {
            case GLFW.GLFW_KEY_ESCAPE -> {
                if (presetsOpen) presetsOpen = false;
                else if (panelOpen) panelOpen = false;
                else if (!selection.isEmpty()) selection.clear();
                else onClose();
                return true;
            }
            case GLFW.GLFW_KEY_G -> {
                toggleGrid();
                return true;
            }
            case GLFW.GLFW_KEY_DELETE, GLFW.GLFW_KEY_BACKSPACE -> {
                List<HudModule> sel = selectedEnabled();
                if (!sel.isEmpty()) {
                    record(null);
                    for (HudModule m : sel) m.toggle();
                    selection.clear();
                    showToast(sel.size() == 1 ? sel.get(0).name() + " hidden" : sel.size() + " elements hidden");
                    ShardClient.config().markDirty();
                }
                return true;
            }
            default -> {
            }
        }
        int dx = 0, dy = 0;
        switch (key) {
            case GLFW.GLFW_KEY_LEFT -> dx = -1;
            case GLFW.GLFW_KEY_RIGHT -> dx = 1;
            case GLFW.GLFW_KEY_UP -> dy = -1;
            case GLFW.GLFW_KEY_DOWN -> dy = 1;
            default -> {
            }
        }
        if ((dx != 0 || dy != 0) && !selection.isEmpty()) {
            record("nudge");
            double px = (shift ? 10.0 : 1.0) / minecraft.getWindow().getGuiScale();
            for (HudModule m : selectedEnabled()) m.setPosition(m.posX(width) + dx * px, m.posY(height) + dy * px, width, height);
            ShardClient.config().markDirty();
            return true;
        }
        if (panelOpen) return panel.keyPressed(event);
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (presetInputFocused) {
            presetName.charTyped(event.codepointAsString());
            return true;
        }
        if (panelOpen && panel.capturesKeys()) return panel.charTyped(event);
        return super.charTyped(event);
    }

    //? if >=1.21.11 {
    @Override
    public void resize(int w, int h) {
        super.resize(w, h);
        if (panel != null) panel.init(w, h);
    }
    //?} else {
    /*@Override
    public void resize(net.minecraft.client.Minecraft mc, int w, int h) {
        super.resize(mc, w, h);
        if (panel != null) panel.init(mc, w, h);
    }
    *///?}

    @Override
    public void onClose() {
        ShardClient.config().save();
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
