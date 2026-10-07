package gg.shard.client.gui;

import com.google.gson.JsonObject;
import gg.shard.client.ShardClient;
import gg.shard.client.hud.HudEditorScreen;
import gg.shard.client.module.Module;
import gg.shard.client.module.ModuleCategory;
import gg.shard.client.module.setting.BoolSetting;
import gg.shard.client.module.setting.ColorSetting;
import gg.shard.client.module.setting.DoubleSetting;
import gg.shard.client.module.setting.EnumSetting;
import gg.shard.client.module.setting.IntSetting;
import gg.shard.client.module.setting.KeybindSetting;
import gg.shard.client.module.setting.Setting;
import gg.shard.client.module.setting.StringSetting;
import gg.shard.client.util.Colors;
import gg.shard.client.util.Keys;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The Shard click GUI: one draggable glass panel per category, module rows with inline settings,
 * live search, keyboard navigation. Panel positions persist through the config's "gui" object.
 */
public final class ClickGuiScreen extends Screen {
    static final int PANEL_W = 118;
    static final int HEADER_H = 18;
    static final int ROW_H = 14;
    static final int SETTING_H = 13;
    static final int PAD = 4;

    private final Screen parent;
    private final List<Panel> panels = new ArrayList<>();
    private Panel dragging;
    private int dragDx;
    private int dragDy;
    private String search = "";
    private boolean searchFocused;
    private KeybindSetting listeningKey;
    private Module listeningModule;
    private StringSetting editingString;
    private Setting<?> sliding;
    private Module hoveredModule;
    private Setting<?> hoveredSetting;
    private float openProgress;
    private long lastFrameNs;
    private boolean twoRows;

    public ClickGuiScreen(Screen parent) {
        super(Component.literal("Shard"));
        this.parent = parent;
    }

    // ---- lifecycle -----------------------------------------------------------------------

    @Override
    protected void init() {
        panels.clear();
        JsonObject saved = ShardClient.config().gui().has("panels") && ShardClient.config().gui().get("panels").isJsonObject()
                ? ShardClient.config().gui().getAsJsonObject("panels")
                : new JsonObject();
        int i = 0;
        // Panels wrap onto a second row when the screen (large GUI scale) cannot fit them side by
        // side; each row then gets half the height and bodies scroll inside it.
        int perRow = Math.max(1, (width - 16) / (PANEL_W + 10));
        List<ModuleCategory> categories = new ArrayList<>();
        for (ModuleCategory c : ModuleCategory.values()) {
            if (!ShardClient.modules().byCategory(c).isEmpty()) categories.add(c);
        }
        twoRows = categories.size() > perRow;
        for (ModuleCategory category : categories) {
            int col = i % perRow;
            int row = i / perRow;
            Panel p = new Panel(category, 16 + col * (PANEL_W + 10), row == 0 ? 32 : height / 2 + 6);
            JsonObject s = saved.has(category.name()) ? saved.getAsJsonObject(category.name()) : null;
            if (s != null) {
                p.x = s.has("x") ? s.get("x").getAsInt() : p.x;
                p.y = s.has("y") ? s.get("y").getAsInt() : p.y;
                p.open = !s.has("open") || s.get("open").getAsBoolean();
            }
            p.x = Math.max(0, Math.min(width - PANEL_W, p.x));
            p.y = Math.max(0, Math.min(height - HEADER_H, p.y));
            p.openAnim = p.open ? 1f : 0f;
            panels.add(p);
            i++;
        }
        lastFrameNs = System.nanoTime();
    }

    private void persistPanels() {
        JsonObject out = new JsonObject();
        for (Panel p : panels) {
            JsonObject s = new JsonObject();
            s.addProperty("x", p.x);
            s.addProperty("y", p.y);
            s.addProperty("open", p.open);
            out.add(p.category.name(), s);
        }
        ShardClient.config().gui().add("panels", out);
        ShardClient.config().markDirty();
    }

    @Override
    public void onClose() {
        persistPanels();
        ShardClient.config().save();
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ---- rendering ------------------------------------------------------------------------

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - lastFrameNs) / 1_000_000_000f) * 20f; // in ticks
        lastFrameNs = now;
        openProgress = Render2D.approach(openProgress, 1f, 0.6f, dt);

        g.fill(0, 0, width, height, Colors.fade(0x990A0E16, openProgress));
        hoveredModule = null;
        hoveredSetting = null;

        renderTopBar(g, mouseX, mouseY);
        for (Panel p : panels) {
            p.openAnim = Render2D.approach(p.openAnim, p.open ? 1f : 0f, 0.7f, dt);
            p.hover = Render2D.approach(p.hover, p.headerHovered(mouseX, mouseY) ? 1f : 0f, 0.8f, dt);
            renderPanel(g, p, mouseX, mouseY);
        }
        renderTooltip(g);
    }

    /** Search box width adapts to the screen so the top bar never collides at large GUI scales. */
    private int barWidth() {
        return Math.max(90, Math.min(260, width - 2 * 70));
    }

    private int barX() {
        return (width - barWidth()) / 2;
    }

    private void renderTopBar(GuiGraphics g, int mouseX, int mouseY) {
        int barW = barWidth();
        int x = barX();
        int y = 8;
        Render2D.panel(g, x, y, barW, 16, Theme.panel(), searchFocused ? Theme.accent() : Theme.line());
        String shown = search.isEmpty() && !searchFocused ? "Search modules…" : search + (searchFocused && (System.currentTimeMillis() / 500) % 2 == 0 ? "_" : "");
        if (font.width(shown) > barW - 10) shown = font.plainSubstrByWidth(shown, barW - 10);
        Render2D.text(g, font, shown, x + 6, y + 4, search.isEmpty() && !searchFocused ? Theme.subtle() : Theme.text(), false);
        Render2D.text(g, font, "Shard", 8, 12, Theme.accent(), true);
        String hint = "RShift closes · Right-click for settings · Middle-click binds";
        if (x + barW + 12 + font.width(hint) <= width - 8) Render2D.textRight(g, font, hint, width - 8, 12, Theme.subtle(), false);
        String hud = "[ HUD editor ]";
        int hx = width - 8 - font.width(hud);
        boolean hov = Render2D.hovered(mouseX, mouseY, hx, 24, font.width(hud), 10);
        Render2D.text(g, font, hud, hx, 24, hov ? Theme.accent() : Theme.muted(), false);
    }

    private void renderPanel(GuiGraphics g, Panel p, int mouseX, int mouseY) {
        List<Module> rows = p.visibleModules();
        int contentH = p.contentHeight(rows);
        int visibleH = p.visibleHeight(rows);
        p.scroll = Math.max(0, Math.min(p.scroll, contentH - visibleH));
        int shownH = Math.round(visibleH * easeOut(p.openAnim));
        int totalH = HEADER_H + shownH;

        Render2D.panel(g, p.x, p.y, PANEL_W, totalH, Theme.panel(), Theme.line());
        int headerFill = Colors.mix(Theme.header(), Theme.panelHover(), p.hover);
        Render2D.rounded(g, p.x, p.y, PANEL_W, HEADER_H, headerFill);
        g.fill(p.x, p.y + HEADER_H - 1, p.x + PANEL_W, p.y + HEADER_H, Theme.accentAlpha(0x66));
        Render2D.text(g, font, p.category.displayName(), p.x + PAD + 2, p.y + 5, Theme.text(), true);
        String arrow = p.open ? "–" : "+";
        Render2D.textRight(g, font, arrow, p.x + PANEL_W - PAD - 1, p.y + 5, Theme.muted(), false);

        if (shownH <= 0) return;
        g.enableScissor(p.x, p.y + HEADER_H, p.x + PANEL_W, p.y + HEADER_H + shownH);
        int y = p.y + HEADER_H - p.scroll;
        for (Module m : rows) {
            boolean rowHover = Render2D.hovered(mouseX, mouseY, p.x, y, PANEL_W, ROW_H)
                    && mouseY >= p.y + HEADER_H && mouseY < p.y + totalH;
            if (rowHover) hoveredModule = m;
            int fill = m.isEnabled() ? Theme.accentAlpha(rowHover ? 0x55 : 0x3C) : (rowHover ? 0x14FFFFFF : 0);
            if (fill != 0) g.fill(p.x + 1, y, p.x + PANEL_W - 1, y + ROW_H, fill);
            if (m.isEnabled()) g.fill(p.x + 1, y + 2, p.x + 3, y + ROW_H - 2, Theme.accent());
            Render2D.text(g, font, m.name(), p.x + PAD + 3, y + 3, m.isEnabled() ? Theme.text() : Theme.muted(), false);
            if (m.keybind() >= 0 || listeningModule == m) {
                String k = listeningModule == m ? "…" : Keys.name(m.keybind());
                Render2D.textRight(g, font, k, p.x + PANEL_W - PAD - 7, y + 3, Theme.subtle(), false);
            }
            boolean expanded = p.expanded.contains(m.key()) && !m.settings().isEmpty();
            Render2D.textRight(g, font, m.settings().isEmpty() ? "" : expanded ? "▾" : "▸", p.x + PANEL_W - PAD, y + 3, Theme.subtle(), false);
            y += ROW_H;
            if (expanded) {
                for (Setting<?> s : m.settings()) {
                    if (!s.isVisible()) continue;
                    boolean sHover = Render2D.hovered(mouseX, mouseY, p.x, y, PANEL_W, SETTING_H)
                            && mouseY >= p.y + HEADER_H && mouseY < p.y + totalH;
                    if (sHover) {
                        hoveredSetting = s;
                        hoveredModule = m;
                    }
                    renderSetting(g, m, s, p.x, y, sHover);
                    y += SETTING_H;
                }
                y += 2;
            }
        }
        g.disableScissor();
    }

    private void renderSetting(GuiGraphics g, Module m, Setting<?> s, int px, int y, boolean hover) {
        int x = px + PAD + 6;
        int w = PANEL_W - PAD * 2 - 8;
        if (hover) g.fill(px + 1, y, px + PANEL_W - 1, y + SETTING_H, 0x10FFFFFF);
        int labelColor = hover ? Theme.text() : Theme.muted();
        if (s instanceof BoolSetting b) {
            Render2D.text(g, font, s.name(), x, y + 3, labelColor, false);
            int bx = px + PANEL_W - PAD - 11;
            Render2D.rounded(g, bx, y + 3, 8, 8, b.get() ? Theme.accent() : 0x33FFFFFF);
            if (b.get()) Render2D.text(g, font, "✓", bx + 1, y + 2, Theme.accentText(), false);
        } else if (s instanceof IntSetting || s instanceof DoubleSetting) {
            double frac = s instanceof IntSetting i ? i.fraction() : ((DoubleSetting) s).fraction();
            Render2D.text(g, font, s.name(), x, y + 1, labelColor, false);
            Render2D.textRight(g, font, s.display(), px + PANEL_W - PAD - 1, y + 1, Theme.subtle(), false);
            Render2D.bar(g, x, y + SETTING_H - 3, w, 2, frac, Theme.accent());
            int knob = x + (int) Math.round(frac * (w - 3));
            Render2D.fill(g, knob, y + SETTING_H - 4, 3, 4, sliding == s || hover ? Theme.accentHover() : Theme.text());
        } else if (s instanceof EnumSetting<?> e) {
            Render2D.text(g, font, s.name(), x, y + 3, labelColor, false);
            Render2D.textRight(g, font, e.display(), px + PANEL_W - PAD - 1, y + 3, Theme.accent(), false);
        } else if (s instanceof ColorSetting c) {
            Render2D.text(g, font, s.name(), x, y + 3, labelColor, false);
            int sw = px + PANEL_W - PAD - 24;
            Render2D.rounded(g, sw, y + 2, 22, 9, c.get());
            Render2D.outline(g, sw, y + 2, 22, 9, Theme.lineStrong());
        } else if (s instanceof KeybindSetting k) {
            Render2D.text(g, font, s.name(), x, y + 3, labelColor, false);
            String shown = listeningKey == k ? "press a key" : k.display();
            Render2D.textRight(g, font, shown, px + PANEL_W - PAD - 1, y + 3, listeningKey == k ? Theme.warning() : Theme.accent(), false);
        } else if (s instanceof StringSetting str) {
            Render2D.text(g, font, s.name(), x, y + 3, labelColor, false);
            String shown = editingString == str ? str.get() + "_" : str.display();
            if (font.width(shown) > w / 2) shown = font.plainSubstrByWidth(shown, w / 2);
            Render2D.textRight(g, font, shown, px + PANEL_W - PAD - 1, y + 3, Theme.subtle(), false);
        }
    }

    private void renderTooltip(GuiGraphics g) {
        String text = null;
        if (hoveredSetting != null) text = hoveredSetting.name() + ": " + hoveredSetting.description();
        else if (hoveredModule != null) text = hoveredModule.description();
        if (text == null || text.isEmpty()) return;
        int w = Math.min(260, font.width(text) + 10);
        List<String> lines = wrap(text, w - 10);
        int h = lines.size() * 10 + 6;
        int x = 8;
        int y = height - h - 8;
        Render2D.panel(g, x, y, w, h, Theme.header(), Theme.lineStrong());
        for (int i = 0; i < lines.size(); i++) Render2D.text(g, font, lines.get(i), x + 5, y + 3 + i * 10, Theme.text(), false);
    }

    private List<String> wrap(String text, int maxWidth) {
        List<String> out = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (font.width(candidate) > maxWidth && !line.isEmpty()) {
                out.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (!line.isEmpty()) out.add(line.toString());
        return out;
    }

    private static float easeOut(float t) {
        return 1f - (float) Math.pow(1f - t, 3);
    }

    // ---- input ----------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int mx = (int) event.x();
        int my = (int) event.y();
        int button = event.button();

        if (listeningKey != null) {
            listeningKey = null;
            return true;
        }
        if (listeningModule != null) {
            listeningModule = null;
            return true;
        }
        editingString = null;

        // Top bar: search box and HUD editor link.
        searchFocused = Render2D.hovered(mx, my, barX(), 8, barWidth(), 16);
        String hud = "[ HUD editor ]";
        int hx = width - 8 - font.width(hud);
        if (Render2D.hovered(mx, my, hx, 24, font.width(hud), 10)) {
            persistPanels();
            minecraft.setScreen(new HudEditorScreen(this, ShardClient.hud()));
            return true;
        }

        for (int i = panels.size() - 1; i >= 0; i--) {
            Panel p = panels.get(i);
            if (p.headerHovered(mx, my)) {
                if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                    dragging = p;
                    dragDx = mx - p.x;
                    dragDy = my - p.y;
                } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                    p.open = !p.open;
                }
                panels.remove(p);
                panels.add(p);
                return true;
            }
            if (p.open && p.bodyHovered(mx, my)) {
                handleBodyClick(p, mx, my, button);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void handleBodyClick(Panel p, int mx, int my, int button) {
        List<Module> rows = p.visibleModules();
        int y = p.y + HEADER_H - p.scroll;
        for (Module m : rows) {
            if (my >= y && my < y + ROW_H) {
                if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) m.toggle();
                else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && !m.settings().isEmpty()) {
                    if (!p.expanded.remove(m.key())) p.expanded.add(m.key());
                } else if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) listeningModule = m;
                return;
            }
            y += ROW_H;
            if (p.expanded.contains(m.key()) && !m.settings().isEmpty()) {
                for (Setting<?> s : m.settings()) {
                    if (!s.isVisible()) continue;
                    if (my >= y && my < y + SETTING_H) {
                        handleSettingClick(m, s, p.x, mx, button);
                        return;
                    }
                    y += SETTING_H;
                }
                y += 2;
            }
        }
    }

    private void handleSettingClick(Module m, Setting<?> s, int px, int mx, int button) {
        if (s instanceof BoolSetting b) {
            b.toggle();
        } else if (s instanceof IntSetting || s instanceof DoubleSetting) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                s.reset();
                return;
            }
            sliding = s;
            applySlider(s, px, mx);
        } else if (s instanceof EnumSetting<?> e) {
            e.cycle(button != GLFW.GLFW_MOUSE_BUTTON_RIGHT);
        } else if (s instanceof ColorSetting c) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                c.reset();
            } else if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
                c.setAlpha(c.alpha() >= 0xFF ? 0x80 : 0xFF);
            } else {
                // Walk a palette of accent-friendly colours.
                int[] palette = {0xFF22D3EE, 0xFFA78BFA, 0xFF34D399, 0xFFFB7185, 0xFFFBBF24, 0xFF93C5FD, 0xFFA3E635, 0xFFE879F9, 0xFFFFFFFF, 0xFF000000};
                int idx = 0;
                for (int i = 0; i < palette.length; i++) if ((palette[i] & 0xFFFFFF) == c.rgb()) idx = i + 1;
                c.setRgb(palette[idx % palette.length] & 0xFFFFFF);
            }
        } else if (s instanceof KeybindSetting k) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) k.set(Keys.NONE);
            else listeningKey = k;
        } else if (s instanceof StringSetting str) {
            editingString = str;
            searchFocused = false;
        }
    }

    private void applySlider(Setting<?> s, int px, int mx) {
        int x = px + PAD + 6;
        int w = PANEL_W - PAD * 2 - 8;
        double frac = (mx - x) / (double) (w - 3);
        frac = Math.max(0, Math.min(1, frac));
        if (s instanceof IntSetting i) i.setFraction(frac);
        else if (s instanceof DoubleSetting d) d.setFraction(frac);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (dragging != null) {
            dragging.x = Math.max(0, Math.min(width - PANEL_W, (int) event.x() - dragDx));
            dragging.y = Math.max(0, Math.min(height - HEADER_H, (int) event.y() - dragDy));
            return true;
        }
        if (sliding != null) {
            for (Panel p : panels) {
                if (p.contains(sliding)) {
                    applySlider(sliding, p.x, (int) event.x());
                    return true;
                }
            }
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging != null) {
            dragging = null;
            persistPanels();
            return true;
        }
        if (sliding != null) {
            sliding = null;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        for (int i = panels.size() - 1; i >= 0; i--) {
            Panel p = panels.get(i);
            if (p.open && (p.bodyHovered(mx, my) || p.headerHovered(mx, my))) {
                List<Module> rows = p.visibleModules();
                int maxScroll = Math.max(0, p.contentHeight(rows) - p.visibleHeight(rows));
                p.scroll = Math.max(0, Math.min(maxScroll, p.scroll - (int) (sy * 12)));
                return true;
            }
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (listeningKey != null) {
            listeningKey.set(key == GLFW.GLFW_KEY_ESCAPE ? Keys.NONE : key);
            listeningKey = null;
            return true;
        }
        if (listeningModule != null) {
            listeningModule.setKeybind(key == GLFW.GLFW_KEY_ESCAPE ? Keys.NONE : key);
            listeningModule = null;
            return true;
        }
        if (editingString != null) {
            if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_ENTER) {
                editingString = null;
            } else if (key == GLFW.GLFW_KEY_BACKSPACE) {
                String v = editingString.get();
                if (!v.isEmpty()) editingString.set(v.substring(0, v.length() - 1));
            }
            return true;
        }
        if (searchFocused) {
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                searchFocused = false;
                return true;
            }
            if (key == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) {
                search = search.substring(0, search.length() - 1);
                return true;
            }
            if (key == GLFW.GLFW_KEY_ENTER) {
                searchFocused = false;
                return true;
            }
        }
        if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            onClose();
            return true;
        }
        if (key == GLFW.GLFW_KEY_SLASH || (key == GLFW.GLFW_KEY_F && (event.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0)) {
            searchFocused = true;
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        String ch = event.codepointAsString();
        if (editingString != null) {
            editingString.set(editingString.get() + ch);
            return true;
        }
        if (searchFocused) {
            if (!ch.equals("/")) search += ch;
            return true;
        }
        return super.charTyped(event);
    }

    // ---- panel model ----------------------------------------------------------------------

    private final class Panel {
        final ModuleCategory category;
        int x;
        int y;
        boolean open = true;
        float openAnim = 1f;
        float hover;
        int scroll;
        final List<String> expanded = new ArrayList<>();

        Panel(ModuleCategory category, int x, int y) {
            this.category = category;
            this.x = x;
            this.y = y;
        }

        List<Module> visibleModules() {
            List<Module> all = ShardClient.modules().byCategory(category);
            if (search.isEmpty()) return all;
            String q = search.toLowerCase(Locale.ROOT);
            List<Module> out = new ArrayList<>();
            for (Module m : all) {
                if (m.name().toLowerCase(Locale.ROOT).contains(q) || m.description().toLowerCase(Locale.ROOT).contains(q)) out.add(m);
            }
            return out;
        }

        int contentHeight(List<Module> rows) {
            int h = 0;
            for (Module m : rows) {
                h += ROW_H;
                if (expanded.contains(m.key()) && !m.settings().isEmpty()) {
                    for (Setting<?> s : m.settings()) if (s.isVisible()) h += SETTING_H;
                    h += 2;
                }
            }
            return h == 0 ? ROW_H : h + 2;
        }

        /** Body height actually shown: the content, capped to the row's share of the screen. */
        int visibleHeight(List<Module> rows) {
            int contentH = contentHeight(rows);
            int rowBottom = twoRows && y < height / 2 ? height / 2 - 4 : height - 6;
            int cap = Math.max(ROW_H * 3, rowBottom - y - HEADER_H);
            return Math.min(contentH, cap);
        }

        boolean headerHovered(double mx, double my) {
            return Render2D.hovered(mx, my, x, y, PANEL_W, HEADER_H);
        }

        boolean bodyHovered(double mx, double my) {
            int shown = Math.round(visibleHeight(visibleModules()) * easeOut(openAnim));
            return Render2D.hovered(mx, my, x, y + HEADER_H, PANEL_W, shown);
        }

        boolean contains(Setting<?> s) {
            for (Module m : visibleModules()) if (m.settings().contains(s)) return true;
            return false;
        }
    }

    /** Exposed for tests of the layout math. */
    static Map<ModuleCategory, Integer> defaultColumns() {
        Map<ModuleCategory, Integer> out = new EnumMap<>(ModuleCategory.class);
        int i = 0;
        for (ModuleCategory c : ModuleCategory.values()) out.put(c, 16 + (i++) * (PANEL_W + 10));
        return out;
    }
}
