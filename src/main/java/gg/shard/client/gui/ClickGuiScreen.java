package gg.shard.client.gui;

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
import gg.shard.client.server.ServerBlacklist;
import gg.shard.client.util.Colors;
import gg.shard.client.util.Keys;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.IntConsumer;

/**
 * Shard's settings page: a sidebar of categories, a grid of mod cards with toggle switches and
 * a settings panel that slides in for the selected module. Everything is drawn in GUI units so
 * it scales with the game's GUI scale; the layout collapses to tabs on narrow screens.
 *
 * <p>Input is routed through "hits": every control registers its rectangle while it renders,
 * so clicks, hover and keyboard focus all share one source of truth.
 */
public final class ClickGuiScreen extends Screen {
    private static final int PAD = 10;
    private static final int SIDEBAR_W = 132;
    private static final int CARD_MIN_W = 150;
    private static final int CARD_H = 54;
    private static final int GAP = 8;
    private static final int ROW_H = 22;
    private static final int NARROW_WIDTH = 560;
    private static final int SWITCH_W = 26;
    private static final int SWITCH_H = 14;
    private static final int BUTTON_H = 16;
    private static final String KEY_SEARCH = "search";
    private static final String KEY_PROFILE_INPUT = "profile-input";

    /** A clickable (and optionally focusable) rectangle registered during rendering. */
    private record Hit(String key, int x, int y, int w, int h, int[] clip, boolean focusable, IntConsumer onClick) {
        boolean contains(double mx, double my) {
            if (!(mx >= x && mx < x + w && my >= y && my < y + h)) return false;
            if (clip == null) return true;
            return mx >= clip[0] && mx < clip[2] && my >= clip[1] && my < clip[3];
        }
    }

    private final Screen parent;
    private ModuleCategory category = ModuleCategory.HUD;
    private boolean settingsPage;
    private final TextInput search = new TextInput(48).placeholder("Search mods");
    private final TextInput profileInput = new TextInput(32).placeholder("New profile name");
    private final Map<Setting<?>, TextInput> stringInputs = new HashMap<>();
    private TextInput activeInput;
    private String activeInputKey;

    private Module panelModule;
    private float panelAnim;
    private float panelTarget;
    private float openProgress;
    private int gridScroll;
    private int panelScroll;
    private int pageScroll;
    private int sidebarScroll;

    private final List<Hit> hits = new ArrayList<>();
    private final Map<String, Setting<?>> focusTargets = new HashMap<>();
    private final List<String> cardOrder = new ArrayList<>();
    private int gridColumns = 1;
    private String focusKey;
    private int[] currentClip;
    private final Map<String, Float> anims = new HashMap<>();
    private Popover popover;
    private KeybindSetting listeningKey;
    private boolean listeningModuleKey;
    private Setting<?> sliding;
    private int slideX;
    private int slideW;
    private long lastFrameNs;
    private float dt;
    private String toast;
    private long toastUntil;

    private boolean narrow;
    private int contentX;
    private int contentY;
    private int contentW;
    private int contentH;
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;

    public ClickGuiScreen(Screen parent) {
        super(Component.literal("Shard"));
        this.parent = parent;
    }

    // ---- lifecycle ---------------------------------------------------------------------------

    @Override
    protected void init() {
        if (lastFrameNs == 0) {
            lastFrameNs = System.nanoTime();
            var gui = ShardClient.config().gui();
            if (gui.has("category")) {
                String saved = gui.get("category").getAsString();
                if (saved.equals("settings")) settingsPage = true;
                else {
                    try {
                        category = ModuleCategory.valueOf(saved);
                    } catch (IllegalArgumentException ignored) {
                        // keep default
                    }
                }
            }
        }
        popover = null;
    }

    @Override
    public void onClose() {
        blurInput();
        ShardClient.config().gui().addProperty("category", settingsPage ? "settings" : category.name());
        ShardClient.config().markDirty();
        ShardClient.config().save();
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Opens a module's settings panel (used by the smoke test and the HUD editor). */
    public void openModule(Module module) {
        panelModule = module;
        panelTarget = 1f;
        panelScroll = 0;
        if (!settingsPage && module != null && search.isEmpty() && module.category() != category) category = module.category();
    }

    /** Opens the colour picker for a colour setting of the open module (smoke test). */
    public void openColorPicker(ColorSetting setting) {
        popover = new ColorPopover(setting, panelX + 20, panelY + 60, panelY + 60, width, height);
    }

    public Module openModule() {
        return panelTarget > 0 ? panelModule : null;
    }

    // ---- animation helpers -------------------------------------------------------------------

    private float anim(String key, float target, float speed) {
        float current = anims.getOrDefault(key, target);
        float next = Render2D.approach(current, target, speed, dt);
        anims.put(key, next);
        return next;
    }

    private void showToast(String text) {
        toast = text;
        toastUntil = System.currentTimeMillis() + 2200;
    }

    // ---- hit registration --------------------------------------------------------------------

    private void hit(String key, int x, int y, int w, int h, boolean focusable, IntConsumer onClick) {
        hits.add(new Hit(key, x, y, w, h, currentClip, focusable, onClick));
    }

    private void clip(GuiGraphics g, int x, int y, int w, int h) {
        g.enableScissor(x, y, x + w, y + h);
        currentClip = new int[]{x, y, x + w, y + h};
    }

    private void unclip(GuiGraphics g) {
        g.disableScissor();
        currentClip = null;
    }

    private boolean focused(String key) {
        return key != null && key.equals(focusKey);
    }

    private Hit hitAt(double mx, double my) {
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (h.contains(mx, my)) return h;
        }
        return null;
    }

    private Hit hitFor(String key) {
        for (Hit h : hits) if (h.key.equals(key)) return h;
        return null;
    }

    private boolean hoverable(double mx, double my, Hit candidate) {
        if (popover != null) return false;
        // The settings panel renders after the grid, so a card under it must not light up.
        if (panelCovers(mx, my) && !Render2D.hovered(candidate.x + candidate.w / 2.0, candidate.y + candidate.h / 2.0, panelX, panelY, panelW, panelH)) {
            return false;
        }
        Hit top = hitAt(mx, my);
        return top != null && top.key.equals(candidate.key);
    }

    // ---- rendering ---------------------------------------------------------------------------

    /** Vanilla calls this before {@link #render}: blur the world once, then tint it. */
    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (minecraft.level == null) renderMenuBackground(g);
        else renderBlurredBackground(g);
        g.fill(0, 0, width, height, Colors.fade(Theme.overlay(), Math.max(0.35f, openProgress)));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        long now = System.nanoTime();
        dt = Math.min(0.1f, (now - lastFrameNs) / 1_000_000_000f) * 20f;
        lastFrameNs = now;
        openProgress = Render2D.approach(openProgress, 1f, 0.5f, dt);
        panelAnim = Render2D.approach(panelAnim, panelTarget, 0.55f, dt);
        if (panelTarget == 0f && panelAnim == 0f) panelModule = null;
        Theme.setGuiScale(minecraft.getWindow().getGuiScale());

        hits.clear();
        focusTargets.clear();
        cardOrder.clear();
        currentClip = null;

        layout();
        if (narrow) renderTopBar(g, mouseX, mouseY);
        else renderSidebar(g, mouseX, mouseY);

        boolean panelReplacesGrid = narrow && panelAnim > 0.02f;
        if (settingsPage) renderSettingsPage(g, mouseX, mouseY);
        else if (!panelReplacesGrid) renderGrid(g, mouseX, mouseY);
        if (!settingsPage && panelModule != null && panelAnim > 0.001f) renderPanel(g, mouseX, mouseY);

        if (popover != null) {
            g.nextStratum();
            popover.render(g, mouseX, mouseY, dt);
            if (popover.wantsClose()) popover = null;
        }
        renderToast(g);
    }

    private void layout() {
        narrow = width < NARROW_WIDTH;
        if (narrow) {
            contentX = PAD;
            contentY = PAD + 46;
            contentW = width - 2 * PAD;
            contentH = height - contentY - PAD;
        } else {
            contentX = PAD + SIDEBAR_W + PAD;
            contentY = PAD;
            contentW = width - contentX - PAD;
            contentH = height - 2 * PAD;
        }
        panelW = narrow ? contentW : Math.max(220, Math.min(300, (int) (contentW * 0.46)));
        if (panelW > contentW) panelW = contentW;
        panelY = contentY;
        panelH = contentH;
        float eased = Render2D.easeOut(panelAnim);
        panelX = contentX + contentW - panelW + Math.round((1f - eased) * (panelW + PAD));
    }

    private boolean panelCovers(double mx, double my) {
        return panelModule != null && panelAnim > 0.001f && !settingsPage && Render2D.hovered(mx, my, panelX, panelY, panelW, panelH);
    }

    private List<ModuleCategory> categories() {
        List<ModuleCategory> out = new ArrayList<>();
        for (ModuleCategory c : ModuleCategory.values()) if (!ShardClient.modules().byCategory(c).isEmpty()) out.add(c);
        return out;
    }

    private static String version() {
        return FabricLoader.getInstance().getModContainer(ShardClient.MOD_ID)
                .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("dev");
    }

    // ---- sidebar / top bar --------------------------------------------------------------------

    private void renderSidebar(GuiGraphics g, int mouseX, int mouseY) {
        int x = PAD;
        int y = PAD;
        int w = SIDEBAR_W;
        int h = height - 2 * PAD;
        int r = Theme.radius();
        Render2D.panel(g, x, y, w, h, r, Theme.surface(), Theme.line());
        hit("sidebar", x, y, w, h, false, b -> {});

        Glyphs.draw(g, "logo", x + 9, y + 9, Theme.accent());
        Render2D.text(g, font, "Shard", x + 28, y + 9, Theme.text(), false);
        Render2D.textClipped(g, font, "v" + version(), x + 28, y + 19, w - 36, Theme.subtle(), false);

        int searchY = y + 36;
        renderSearch(g, x + 8, searchY, w - 16, 16);

        int listY = searchY + 24;
        int bottomReserved = 34;
        int listH = h - (listY - y) - bottomReserved;
        clip(g, x, listY, w, listH);
        int cy = listY - sidebarScroll;
        for (ModuleCategory c : categories()) {
            cy += renderCategoryRow(g, "cat:" + c.name(), c.displayName(), Icons.categoryGlyph(c), countLabel(c),
                    !settingsPage && c == category, x + 6, cy, w - 12, mouseX, mouseY, () -> selectCategory(c));
        }
        cy += 4;
        g.fill(x + 10, cy, x + w - 10, cy + 1, Theme.line());
        cy += 6;
        cy += renderCategoryRow(g, "cat:settings", "Settings", "settings", "", settingsPage, x + 6, cy, w - 12, mouseX, mouseY, this::selectSettings);
        int contentHeight = cy + sidebarScroll - listY;
        sidebarScroll = Math.max(0, Math.min(sidebarScroll, contentHeight - listH));
        unclip(g);

        boolean canEdit = minecraft.player != null;
        button(g, "hud-editor", x + 8, y + h - 8 - BUTTON_H, w - 16, BUTTON_H, "HUD editor", false, canEdit, mouseX, mouseY, b -> {
            if (canEdit) minecraft.setScreen(new HudEditorScreen(this, ShardClient.hud()));
        });
    }

    private String countLabel(ModuleCategory c) {
        int on = 0;
        List<Module> list = ShardClient.modules().byCategory(c);
        for (Module m : list) if (m.isEnabled()) on++;
        return on + "/" + list.size();
    }

    private int renderCategoryRow(GuiGraphics g, String key, String label, String glyph, String count, boolean selected,
                                  int x, int y, int w, int mouseX, int mouseY, Runnable onSelect) {
        int h = 18;
        Hit probe = new Hit(key, x, y, w, h, currentClip, true, b -> onSelect.run());
        hits.add(probe);
        boolean hover = hoverable(mouseX, mouseY, probe);
        float sel = anim(key + ":sel", selected ? 1f : 0f, 0.6f);
        float hov = anim(key + ":hov", hover ? 1f : 0f, 0.8f);
        int fill = Colors.mix(Colors.mix(0x00000000, Theme.surfaceHover(), hov), Theme.surfaceRaised(), sel);
        if (Colors.alpha(fill) > 4) Render2D.roundedRect(g, x, y, w, h, Theme.radiusSmall(), fill);
        if (focused(key)) Render2D.roundedOutline(g, x, y, w, h, Theme.radiusSmall(), Theme.accentAlpha(0xA0));
        int textColor = selected ? Theme.text() : hover ? Theme.text() : Theme.muted();
        int glyphColor = selected ? Theme.accent() : Theme.muted();
        Glyphs.draw(g, glyph, x + 4, y + 1, glyphColor);
        Render2D.textClipped(g, font, label, x + 24, y + 5, w - 24 - (count.isEmpty() ? 6 : font.width(count) + 10), textColor, false);
        if (!count.isEmpty()) Render2D.textRight(g, font, count, x + w - 6, y + 5, Theme.subtle(), false);
        return h + 2;
    }

    private void renderTopBar(GuiGraphics g, int mouseX, int mouseY) {
        int x = PAD;
        int y = PAD;
        int w = width - 2 * PAD;
        int r = Theme.radius();
        Render2D.panel(g, x, y, w, 40, r, Theme.surface(), Theme.line());
        hit("topbar", x, y, w, 40, false, b -> {});
        Glyphs.draw(g, "logo", x + 8, y + 4, Theme.accent());
        Render2D.text(g, font, "Shard", x + 26, y + 8, Theme.text(), false);
        renderSearch(g, x + 64, y + 4, w - 72, 16);

        int tabY = y + 23;
        int tx = x + 6;
        List<ModuleCategory> cats = categories();
        int hudW = font.width("HUD") + 12;
        // Use short labels when the full ones would collide with the HUD button.
        int available = w - 12 - hudW - 6;
        int needed = font.width("Settings") + 16;
        for (ModuleCategory c : cats) needed += font.width(c.displayName()) + 16;
        boolean compact = needed > available;
        for (ModuleCategory c : cats) {
            String label = compact ? compactName(c) : c.displayName();
            tx += renderTab(g, "cat:" + c.name(), label, !settingsPage && c == category, tx, tabY, mouseX, mouseY, () -> selectCategory(c));
        }
        renderTab(g, "cat:settings", compact ? "More" : "Settings", settingsPage, tx, tabY, mouseX, mouseY, this::selectSettings);
        boolean canEdit = minecraft.player != null;
        button(g, "hud-editor", x + w - 6 - hudW, tabY, hudW, 14, "HUD", false, canEdit, mouseX, mouseY, b -> {
            if (canEdit) minecraft.setScreen(new HudEditorScreen(this, ShardClient.hud()));
        });
    }

    private static String compactName(ModuleCategory c) {
        return switch (c) {
            case COMBAT -> "Combat";
            case PERFORMANCE -> "Perf";
            default -> c.displayName();
        };
    }

    private int renderTab(GuiGraphics g, String key, String label, boolean selected, int x, int y, int mouseX, int mouseY, Runnable onSelect) {
        int w = font.width(label) + 12;
        int h = 14;
        Hit probe = new Hit(key, x, y, w, h, currentClip, true, b -> onSelect.run());
        hits.add(probe);
        boolean hover = hoverable(mouseX, mouseY, probe);
        float sel = anim(key + ":sel", selected ? 1f : 0f, 0.6f);
        int fill = Colors.mix(hover ? Theme.surfaceHover() : 0x00000000, Theme.surfaceRaised(), sel);
        if (Colors.alpha(fill) > 4) Render2D.roundedRect(g, x, y, w, h, Theme.radiusSmall(), fill);
        if (focused(key)) Render2D.roundedOutline(g, x, y, w, h, Theme.radiusSmall(), Theme.accentAlpha(0xA0));
        Render2D.text(g, font, label, x + 6, y + 3, selected ? Theme.text() : Theme.muted(), false);
        return w + 4;
    }

    private void renderSearch(GuiGraphics g, int x, int y, int w, int h) {
        boolean isFocused = activeInput == search;
        search.render(g, font, x, y, w, h, isFocused);
        Glyphs.draw(g, "search", x + w - 18, y + (h - 16) / 2, isFocused ? Theme.accent() : Theme.subtle());
        if (focused(KEY_SEARCH) && !isFocused) Render2D.roundedOutline(g, x, y, w, h, Theme.radiusSmall(), Theme.accentAlpha(0xA0));
        hit(KEY_SEARCH, x, y, w, h, true, b -> {
            focusInput(search, KEY_SEARCH);
            search.cursorToEnd();
        });
    }

    private void selectCategory(ModuleCategory c) {
        settingsPage = false;
        category = c;
        gridScroll = 0;
        if (panelModule != null && panelModule.category() != c) closePanel();
        focusKey = "cat:" + c.name();
    }

    private void selectSettings() {
        settingsPage = true;
        pageScroll = 0;
        closePanel();
        focusKey = "cat:settings";
    }

    private void closePanel() {
        panelTarget = 0f;
        if (popover != null) popover = null;
        listeningKey = null;
        listeningModuleKey = false;
        blurInput();
    }

    // ---- grid --------------------------------------------------------------------------------

    private List<Module> visibleModules() {
        String q = search.value().trim().toLowerCase(Locale.ROOT);
        List<Module> out = new ArrayList<>();
        if (q.isEmpty()) return ShardClient.modules().byCategory(category);
        for (Module m : ShardClient.modules().all()) {
            if (m.hidden()) continue;
            if (m.name().toLowerCase(Locale.ROOT).contains(q) || m.description().toLowerCase(Locale.ROOT).contains(q)
                    || m.category().displayName().toLowerCase(Locale.ROOT).contains(q)) out.add(m);
        }
        return out;
    }

    private void renderGrid(GuiGraphics g, int mouseX, int mouseY) {
        List<Module> modules = visibleModules();
        boolean searching = !search.isEmpty();
        int headerH = 20;
        String title = searching ? "Results for \"" + search.value().trim() + "\"" : category.displayName();
        String sub = searching ? modules.size() + (modules.size() == 1 ? " match" : " matches") : category.description();
        Render2D.text(g, font, title, contentX + 2, contentY + 2, Theme.text(), false);
        Render2D.textClipped(g, font, sub, contentX + 2, contentY + 12, contentW - 4, Theme.subtle(), false);

        int gridY = contentY + headerH + 6;
        int gridH = contentH - headerH - 6;
        gridColumns = Math.max(1, (contentW + GAP) / (CARD_MIN_W + GAP));
        int cardW = (contentW - (gridColumns - 1) * GAP) / gridColumns;
        int rows = (modules.size() + gridColumns - 1) / gridColumns;
        int contentHeight = rows * (CARD_H + GAP);
        gridScroll = Math.max(0, Math.min(gridScroll, contentHeight - gridH));

        if (modules.isEmpty()) {
            Glyphs.draw(g, "search", contentX + contentW / 2 - 16, gridY + 30, Theme.subtle(), 2);
            Render2D.textCentered(g, font, "Nothing matches", contentX + contentW / 2, gridY + 70, Theme.muted(), false);
            return;
        }

        clip(g, contentX, gridY, contentW, gridH);
        for (int i = 0; i < modules.size(); i++) {
            Module m = modules.get(i);
            int col = i % gridColumns;
            int row = i / gridColumns;
            int cx = contentX + col * (cardW + GAP);
            int cy = gridY + row * (CARD_H + GAP) - gridScroll;
            cardOrder.add("card:" + m.key());
            if (cy + CARD_H < gridY || cy > gridY + gridH) continue;
            renderCard(g, m, cx, cy, cardW, mouseX, mouseY);
        }
        unclip(g);
    }

    private void renderCard(GuiGraphics g, Module m, int x, int y, int w, int mouseX, int mouseY) {
        String key = "card:" + m.key();
        int r = Theme.radius();
        Hit card = new Hit(key, x, y, w, CARD_H, currentClip, true, b -> {
            if (b == GLFW.GLFW_MOUSE_BUTTON_RIGHT) m.toggle();
            else openModule(m);
        });
        hits.add(card);
        boolean hover = hoverable(mouseX, mouseY, card) || (popover == null && focused(key));
        float hov = anim(key + ":hov", hover ? 1f : 0f, 0.7f);
        boolean open = panelModule == m && panelTarget > 0;

        int lift = Math.round(hov * 1f);
        int fill = Colors.mix(Theme.surfaceRaised(), Theme.surfaceHover(), hov);
        Render2D.roundedRect(g, x + 1, y + 2, w, CARD_H, r, Colors.fade(Theme.shadow(), 0.4 + 0.6 * hov));
        Render2D.roundedRect(g, x, y - lift, w, CARD_H, r, fill);
        int border = open ? Theme.accentAlpha(0x80) : Colors.mix(Theme.line(), Theme.lineStrong(), hov);
        if (focused(key)) border = Theme.accentAlpha(0xC0);
        Render2D.roundedOutline(g, x, y - lift, w, CARD_H, r, border);

        int iy = y - lift;
        Icons.draw(g, m, x + 10, iy + (CARD_H - 16) / 2, m.isEnabled() ? Theme.accent() : Theme.muted());

        int switchX = x + w - 10 - SWITCH_W;
        int textW = switchX - (x + 34) - 6;
        Render2D.textClipped(g, font, m.name(), x + 34, iy + 15, textW, Theme.text(), false);
        String notice = notice(m);
        if (notice != null) {
            Glyphs.draw(g, "warning", x + 33, iy + 24, Theme.warning());
            Render2D.textClipped(g, font, notice, x + 46, iy + 28, textW - 12, Theme.warning(), false);
        } else {
            Render2D.textClipped(g, font, m.description(), x + 34, iy + 28, textW, Theme.muted(), false);
        }

        renderSwitch(g, "sw:" + m.key(), switchX, iy + (CARD_H - SWITCH_H) / 2, m.isToggledOn(), notice == null, mouseX, mouseY, m::toggle);
    }

    private static String notice(Module m) {
        if (m.blockedBy() != null) return "Off: " + m.blockedBy() + " is installed";
        if (m.isSuppressed()) return "Off on this server";
        return null;
    }

    // ---- controls ----------------------------------------------------------------------------

    private void renderSwitch(GuiGraphics g, String key, int x, int y, boolean on, boolean active, int mouseX, int mouseY, Runnable onToggle) {
        Hit probe = new Hit(key, x - 2, y - 2, SWITCH_W + 4, SWITCH_H + 4, currentClip, true, b -> onToggle.run());
        hits.add(probe);
        float knob = anim(key, on ? 1f : 0f, 0.75f);
        Render2D.toggle(g, x, y, SWITCH_W, SWITCH_H, knob, on, focused(key));
        if (!active) Render2D.roundedRect(g, x, y, SWITCH_W, SWITCH_H, SWITCH_H / 2, Colors.withAlpha(Theme.surface(), 0x90));
    }

    private void button(GuiGraphics g, String key, int x, int y, int w, int h, String label, boolean primary, boolean enabled,
                        int mouseX, int mouseY, IntConsumer onClick) {
        Hit probe = new Hit(key, x, y, w, h, currentClip, enabled, b -> {
            if (enabled) onClick.accept(b);
        });
        hits.add(probe);
        boolean hover = enabled && hoverable(mouseX, mouseY, probe);
        float hov = anim(key + ":hov", hover ? 1f : 0f, 0.8f);
        int r = Theme.radiusSmall();
        int fill = primary ? Colors.mix(Theme.accent(), Theme.accentHover(), hov) : Colors.mix(Theme.control(), Theme.controlHover(), hov);
        if (!enabled) fill = Colors.withAlpha(fill, 0x70);
        Render2D.roundedRect(g, x, y, w, h, r, fill);
        Render2D.roundedOutline(g, x, y, w, h, r, focused(key) ? Theme.accentAlpha(0xC0) : primary ? 0 : Theme.line());
        int color = primary ? Theme.accentText() : enabled ? Theme.text() : Theme.subtle();
        Render2D.textCentered(g, font, label, x + w / 2, y + (h - 8) / 2, color, false);
    }

    private void focusInput(TextInput input, String key) {
        if (activeInput != null && activeInput != input) blurInput();
        activeInput = input;
        activeInputKey = key;
        focusKey = key;
    }

    private void blurInput() {
        if (activeInput == null) return;
        TextInput was = activeInput;
        activeInput = null;
        activeInputKey = null;
        if (was != search && was != profileInput) {
            for (Map.Entry<Setting<?>, TextInput> e : stringInputs.entrySet()) {
                if (e.getValue() == was && e.getKey() instanceof StringSetting s) s.set(was.value());
            }
        }
    }

    // ---- settings panel ----------------------------------------------------------------------

    private void renderPanel(GuiGraphics g, int mouseX, int mouseY) {
        Module m = panelModule;
        int r = Theme.radius();
        int x = panelX;
        int y = panelY;
        int w = panelW;
        int h = panelH;
        clip(g, contentX, contentY, contentW, contentH);
        Render2D.roundedRect(g, x - 2, y + 2, w + 2, h, r, Theme.shadow());
        Render2D.panel(g, x, y, w, h, r, Theme.surface(), Theme.lineStrong());
        hit("panel", x, y, w, h, false, b -> {});

        // Header: back/close, name, description, notice.
        String backGlyph = narrow ? "back" : "close";
        int bx = narrow ? x + 8 : x + w - 24;
        button(g, "panel-close", bx, y + 8, 16, 16, "", false, true, mouseX, mouseY, b -> closePanel());
        Glyphs.draw(g, backGlyph, bx, y + 8, Theme.muted());
        int titleX = narrow ? x + 30 : x + 12;
        int titleW = narrow ? w - 42 : w - 48;
        Icons.draw(g, m, titleX, y + 8, Theme.accent());
        Render2D.textClipped(g, font, m.name(), titleX + 22, y + 12, titleW - 22, Theme.text(), false);

        int cy = y + 30;
        int innerX = x + 12;
        int innerW = w - 24;
        List<String> desc = Render2D.wrap(font, m.description(), innerW);
        for (String line : desc) {
            Render2D.text(g, font, line, innerX, cy, Theme.muted(), false);
            cy += 10;
        }
        String notice = notice(m);
        if (notice != null) {
            cy += 2;
            Glyphs.draw(g, "warning", innerX, cy - 4, Theme.warning());
            for (String line : Render2D.wrap(font, notice, innerW - 16)) {
                Render2D.text(g, font, line, innerX + 16, cy, Theme.warning(), false);
                cy += 10;
            }
        }
        cy += 6;
        g.fill(x + 1, cy, x + w - 1, cy + 1, Theme.line());
        cy += 1;

        // Scrollable rows.
        int rowsTop = cy;
        int rowsH = y + h - rowsTop - 1;
        clip(g, x + 1, rowsTop, w - 2, rowsH);
        int ry = rowsTop + 6 - panelScroll;
        int keyPrefix = m.key().hashCode();

        final int enabledRowY = ry;
        ry += row(g, "Enabled", "", innerX, ry, innerW, mouseX, mouseY, (cx, cw) ->
                renderSwitch(g, "panel-enabled", cx + cw - SWITCH_W, ry0(enabledRowY), m.isToggledOn(), notice == null, mouseX, mouseY, m::toggle));
        ry += renderKeybindRow(g, "Keybind", "Toggle the module in-game", "kb:" + m.key(), Keys.name(m.keybind()), listeningModuleKey,
                innerX, ry, innerW, mouseX, mouseY, () -> {
                    listeningModuleKey = true;
                    listeningKey = null;
                }, () -> m.setKeybind(Keys.NONE));
        String server = ShardClient.modules().currentServer();
        if (server != null) {
            boolean off = ShardClient.modules().blacklist().isDisabled(server, m.key());
            String label = "Disable on " + ServerBlacklist.normalize(server);
            final int serverRowY = ry;
            ry += row(g, label, "Kept off whenever you play here", innerX, ry, innerW, mouseX, mouseY, (cx, cw) ->
                    renderSwitch(g, "panel-server", cx + cw - SWITCH_W, ry0(serverRowY), off, true, mouseX, mouseY,
                            () -> ShardClient.modules().setDisabledOnCurrentServer(m, !off)));
        }
        button(g, "panel-reset", innerX, ry + 3, 110, BUTTON_H, "Reset to defaults", false, true, mouseX, mouseY, b -> {
            for (Setting<?> s : m.settings()) s.reset();
            stringInputs.clear();
            showToast(m.name() + " reset");
        });
        ry += ROW_H + 4;

        List<Setting<?>> settings = m.settings();
        if (!settings.isEmpty()) {
            g.fill(innerX, ry + 2, innerX + innerW, ry + 3, Theme.line());
            ry += 8;
        }
        String lastGroup = null;
        for (Setting<?> s : settings) {
            if (!s.isVisible()) continue;
            if (!s.group().isEmpty() && !s.group().equals(lastGroup)) {
                ry += 2;
                Render2D.text(g, font, s.group(), innerX, ry + 3, Theme.subtle(), false);
                int lineX = innerX + font.width(s.group()) + 6;
                g.fill(lineX, ry + 7, innerX + innerW, ry + 8, Theme.line());
                ry += 15;
                lastGroup = s.group();
            }
            ry += renderSettingRow(g, m, s, keyPrefix, innerX, ry, innerW, mouseX, mouseY);
        }
        ry += 6;
        int contentHeight = ry + panelScroll - rowsTop;
        panelScroll = Math.max(0, Math.min(panelScroll, contentHeight - rowsH));
        unclip(g);
        // Leave the outer (content area) scissor as well.
        g.disableScissor();
        currentClip = null;
    }

    /** Vertical offset of a control inside a row: rows are ROW_H tall, controls sit centred. */
    private static int ry0(int rowY) {
        return rowY + (ROW_H - SWITCH_H) / 2;
    }

    private interface ControlRenderer {
        void render(int controlX, int controlW);
    }

    /** Label on the left (with an optional description below it), control on the right. */
    private int row(GuiGraphics g, String label, String description, int x, int y, int w, int mouseX, int mouseY, ControlRenderer control) {
        boolean hasDesc = description != null && !description.isEmpty();
        int labelY = hasDesc ? y + 2 : y + (ROW_H - 8) / 2;
        int controlW = Math.min(w / 2, 140);
        Render2D.textClipped(g, font, label, x, labelY, w - controlW - 6, Theme.text(), false);
        if (hasDesc) Render2D.textClipped(g, font, description, x, y + 12, w - controlW - 6, Theme.subtle(), false);
        control.render(x + w - controlW, controlW);
        return ROW_H;
    }

    private int renderKeybindRow(GuiGraphics g, String label, String description, String key, String shown, boolean listening,
                                 int x, int y, int w, int mouseX, int mouseY, Runnable onListen, Runnable onClear) {
        return row(g, label, description, x, y, w, mouseX, mouseY, (cx, cw) -> {
            int bw = Math.min(cw, 86);
            String text = listening ? "Press a key…" : shown;
            button(g, key, cx + cw - bw, ry0(y) - 1, bw, BUTTON_H, "", false, true, mouseX, mouseY, b -> {
                if (b == GLFW.GLFW_MOUSE_BUTTON_RIGHT) onClear.run();
                else onListen.run();
            });
            Render2D.textClipped(g, font, text, cx + cw - bw + 6, ry0(y) + 3, bw - 12, listening ? Theme.warning() : Theme.text(), false);
        });
    }

    private int renderSettingRow(GuiGraphics g, Module m, Setting<?> s, int prefix, int x, int y, int w, int mouseX, int mouseY) {
        String key = "set:" + m.key() + ":" + s.key();
        if (s instanceof BoolSetting b) {
            return row(g, s.name(), s.description(), x, y, w, mouseX, mouseY, (cx, cw) ->
                    renderSwitch(g, key, cx + cw - SWITCH_W, ry0(y), b.get(), true, mouseX, mouseY, b::toggle));
        }
        if (s instanceof IntSetting || s instanceof DoubleSetting) {
            focusTargets.put(key, s);
            return row(g, s.name(), s.description(), x, y, w, mouseX, mouseY, (cx, cw) -> {
                String value = s.display();
                int valueW = Math.max(28, font.width(value) + 4);
                int trackW = cw - valueW - 4;
                int trackX = cx;
                double frac = s instanceof IntSetting i ? i.fraction() : ((DoubleSetting) s).fraction();
                Hit probe = new Hit(key, trackX - 4, y, trackW + 8, ROW_H, currentClip, true, b -> {
                    if (b == GLFW.GLFW_MOUSE_BUTTON_RIGHT) s.reset();
                });
                hits.add(probe);
                boolean active = sliding == s;
                boolean hover = hoverable(mouseX, mouseY, probe);
                Render2D.slider(g, trackX, ry0(y) + 1, trackW, frac, active || hover, focused(key));
                Render2D.textRight(g, font, value, cx + cw, ry0(y) + 3, Theme.muted(), false);
                if (hover || active) {
                    // Remember the track so drags can start here.
                    sliderTracks.put(s, new int[]{trackX, trackW});
                }
            });
        }
        if (s instanceof EnumSetting<?> e) {
            focusTargets.put(key, s);
            return row(g, s.name(), s.description(), x, y, w, mouseX, mouseY, (cx, cw) -> {
                int bw = Math.min(cw, 110);
                int bx = cx + cw - bw;
                int by = ry0(y) - 1;
                button(g, key, bx, by, bw, BUTTON_H, "", false, true, mouseX, mouseY, b -> {
                    if (b == GLFW.GLFW_MOUSE_BUTTON_RIGHT) e.cycle(true);
                    else popover = new DropdownPopover(e, bx, by + BUTTON_H, by, bw, width, height);
                });
                Render2D.textClipped(g, font, e.display(), bx + 6, by + 4, bw - 24, Theme.text(), false);
                Glyphs.draw(g, "chevron-down", bx + bw - 18, by, Theme.muted());
            });
        }
        if (s instanceof ColorSetting c) {
            return row(g, s.name(), s.description(), x, y, w, mouseX, mouseY, (cx, cw) -> {
                int sw = 26;
                int sx = cx + cw - sw;
                int sy = ry0(y);
                Hit probe = new Hit(key, sx - 2, sy - 2, sw + 4, SWITCH_H + 4, currentClip, true, b -> {
                    if (b == GLFW.GLFW_MOUSE_BUTTON_RIGHT) c.reset();
                    else popover = new ColorPopover(c, sx + sw - 150, sy + SWITCH_H, sy, width, height);
                });
                hits.add(probe);
                Render2D.checker(g, sx, sy, sw, SWITCH_H, 3);
                Render2D.roundedRect(g, sx, sy, sw, SWITCH_H, Theme.radiusSmall(), c.get());
                Render2D.roundedOutline(g, sx, sy, sw, SWITCH_H, Theme.radiusSmall(), focused(key) ? Theme.accent() : Theme.lineStrong());
                Render2D.textRight(g, font, c.display(), sx - 6, sy + 3, Theme.muted(), false);
            });
        }
        if (s instanceof KeybindSetting k) {
            return renderKeybindRow(g, s.name(), s.description(), key, k.display(), listeningKey == k, x, y, w, mouseX, mouseY, () -> {
                listeningKey = k;
                listeningModuleKey = false;
            }, () -> k.set(Keys.NONE));
        }
        if (s instanceof StringSetting str) {
            TextInput input = stringInputs.computeIfAbsent(s, unused -> new TextInput(str.maxLength()).onCommit(this::blurInput));
            if (activeInput != input) input.sync(str.get());
            return row(g, s.name(), s.description(), x, y, w, mouseX, mouseY, (cx, cw) -> {
                int fw = cw;
                int fx = cx;
                int fy = ry0(y) - 1;
                input.render(g, font, fx, fy, fw, BUTTON_H, activeInput == input);
                if (focused(key) && activeInput != input) Render2D.roundedOutline(g, fx, fy, fw, BUTTON_H, Theme.radiusSmall(), Theme.accentAlpha(0xA0));
                hit(key, fx, fy, fw, BUTTON_H, true, b -> {
                    focusInput(input, key);
                    input.cursorToEnd();
                });
            });
        }
        return 0;
    }

    private final Map<Setting<?>, int[]> sliderTracks = new HashMap<>();

    // ---- settings page (profiles, servers, about) ----------------------------------------------

    private void renderSettingsPage(GuiGraphics g, int mouseX, int mouseY) {
        Render2D.text(g, font, "Settings", contentX + 2, contentY + 2, Theme.text(), false);
        Render2D.textClipped(g, font, "Profiles, server rules and about", contentX + 2, contentY + 12, contentW - 4, Theme.subtle(), false);
        int top = contentY + 26;
        int viewH = contentH - 26;
        clip(g, contentX, top, contentW, viewH);
        int y = top - pageScroll;
        int sectionW = Math.min(contentW, 420);
        int r = Theme.radius();

        // Profiles.
        List<String> profiles = ShardClient.config().profiles();
        int profilesH = 46 + profiles.size() * 20 + 8;
        Render2D.panel(g, contentX, y, sectionW, profilesH, r, Theme.surfaceRaised(), Theme.line());
        Glyphs.draw(g, "folder", contentX + 10, y + 8, Theme.accent());
        Render2D.text(g, font, "Profiles", contentX + 32, y + 12, Theme.text(), false);
        int py = y + 28;
        if (profiles.isEmpty()) {
            Render2D.text(g, font, "No saved profiles yet", contentX + 12, py + 4, Theme.subtle(), false);
            py += 20;
        }
        for (String p : profiles) {
            Render2D.textClipped(g, font, p, contentX + 12, py + 4, sectionW - 150, Theme.text(), false);
            int bx = contentX + sectionW - 12 - 50;
            button(g, "profile-delete:" + p, bx, py, 50, BUTTON_H, "Delete", false, true, mouseX, mouseY, b -> {
                if (ShardClient.config().deleteProfile(p)) showToast("Deleted " + p);
            });
            button(g, "profile-load:" + p, bx - 46, py, 42, BUTTON_H, "Load", false, true, mouseX, mouseY, b -> {
                if (ShardClient.config().loadProfile(p)) {
                    stringInputs.clear();
                    showToast("Loaded " + p);
                }
            });
            py += 20;
        }
        int inputW = sectionW - 24 - 56;
        profileInput.render(g, font, contentX + 12, py, inputW, BUTTON_H, activeInput == profileInput);
        if (focused(KEY_PROFILE_INPUT) && activeInput != profileInput) Render2D.roundedOutline(g, contentX + 12, py, inputW, BUTTON_H, Theme.radiusSmall(), Theme.accentAlpha(0xA0));
        hit(KEY_PROFILE_INPUT, contentX + 12, py, inputW, BUTTON_H, true, b -> {
            focusInput(profileInput, KEY_PROFILE_INPUT);
            profileInput.cursorToEnd();
        });
        button(g, "profile-save", contentX + sectionW - 12 - 50, py, 50, BUTTON_H, "Save", true, true, mouseX, mouseY, b -> saveProfileFromInput());
        y += profilesH + GAP;

        // Servers.
        ServerBlacklist list = ShardClient.modules().blacklist();
        String current = ShardClient.modules().currentServer();
        int serversH = 30 + 12 + (list.entries().isEmpty() ? 20 : list.entries().size() * 20) + 26 + 8;
        Render2D.panel(g, contentX, y, sectionW, serversH, r, Theme.surfaceRaised(), Theme.line());
        Glyphs.draw(g, "globe", contentX + 10, y + 8, Theme.accent());
        Render2D.text(g, font, "Server rules", contentX + 32, y + 12, Theme.text(), false);
        int sy = y + 28;
        String hint = current == null ? "Join a server, then open a mod and flip \"Disable on <server>\"."
                : "Connected to " + ServerBlacklist.normalize(current) + ". Rules apply automatically on join.";
        for (String line : Render2D.wrap(font, hint, sectionW - 24)) {
            Render2D.text(g, font, line, contentX + 12, sy, Theme.subtle(), false);
            sy += 10;
        }
        sy += 4;
        if (list.entries().isEmpty()) {
            Render2D.text(g, font, "No rules yet", contentX + 12, sy + 4, Theme.subtle(), false);
            sy += 20;
        }
        for (ServerBlacklist.Entry e : new ArrayList<>(list.entries())) {
            Render2D.textClipped(g, font, e.pattern(), contentX + 12, sy + 4, sectionW - 150, Theme.text(), false);
            String mods = e.modules().size() + (e.modules().size() == 1 ? " mod off" : " mods off");
            Render2D.textRight(g, font, mods, contentX + sectionW - 12 - 56, sy + 4, Theme.muted(), false);
            button(g, "server-remove:" + e.pattern(), contentX + sectionW - 12 - 50, sy, 50, BUTTON_H, "Remove", false, true, mouseX, mouseY, b -> {
                list.remove(e);
                ShardClient.modules().refreshSuppression();
                ShardClient.config().markDirty();
            });
            sy += 20;
        }
        button(g, "server-add", contentX + 12, sy + 4, 120, BUTTON_H, "Add current server", false, current != null, mouseX, mouseY, b -> {
            if (current != null) {
                list.getOrCreate(current);
                ShardClient.config().markDirty();
                showToast("Rule added for " + ServerBlacklist.normalize(current));
            }
        });
        y += serversH + GAP;

        // About.
        int aboutH = 56;
        Render2D.panel(g, contentX, y, sectionW, aboutH, r, Theme.surfaceRaised(), Theme.line());
        Glyphs.draw(g, "logo", contentX + 10, y + 8, Theme.accent());
        Render2D.text(g, font, "Shard Client " + version(), contentX + 32, y + 12, Theme.text(), false);
        var info = ShardClient.launcherInfo();
        String launcher = info.present() ? "Launched by Shard Launcher " + info.launcherVersion() : "Not launched by Shard Launcher";
        Render2D.textClipped(g, font, launcher, contentX + 12, y + 28, sectionW - 24, Theme.muted(), false);
        Render2D.textClipped(g, font, "Right Shift opens this menu · \".help\" in chat lists commands", contentX + 12, y + 40, sectionW - 24, Theme.subtle(), false);
        y += aboutH + GAP;

        int contentHeight = y + pageScroll - top;
        pageScroll = Math.max(0, Math.min(pageScroll, contentHeight - viewH));
        unclip(g);
    }

    private void saveProfileFromInput() {
        String name = profileInput.value().trim();
        if (name.isEmpty()) {
            ShardClient.config().save();
            showToast("Config saved");
            return;
        }
        if (ShardClient.config().saveProfile(name)) {
            showToast("Saved profile " + name);
            profileInput.setValue("");
        } else {
            showToast("Use letters, digits, - and _ only");
        }
    }

    private void renderToast(GuiGraphics g) {
        if (toast == null) return;
        long left = toastUntil - System.currentTimeMillis();
        if (left <= 0) {
            toast = null;
            return;
        }
        float alpha = Math.min(1f, left / 400f);
        int w = font.width(toast) + 20;
        int x = (width - w) / 2;
        int y = height - PAD - 24;
        Render2D.roundedRect(g, x, y, w, 18, Theme.radiusSmall(), Colors.fade(Theme.popover(), alpha));
        Render2D.roundedOutline(g, x, y, w, 18, Theme.radiusSmall(), Colors.fade(Theme.lineStrong(), alpha));
        Render2D.textCentered(g, font, toast, x + w / 2, y + 5, Colors.fade(Theme.text(), alpha), false);
    }

    // ---- mouse -------------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x();
        double my = event.y();
        int button = event.button();

        if (popover != null) {
            if (popover.contains(mx, my)) {
                popover.mouseClicked(mx, my, button);
                if (popover != null && popover.wantsClose()) popover = null;
                return true;
            }
            popover = null;
            return true;
        }
        if (listeningKey != null || listeningModuleKey) {
            listeningKey = null;
            listeningModuleKey = false;
            return true;
        }

        Hit target = hitAt(mx, my);
        if (activeInput != null && (target == null || !target.key.equals(activeInputKey))) blurInput();
        if (target == null) return super.mouseClicked(event, doubleClick);

        if (target.focusable) focusKey = target.key;
        Setting<?> slider = focusTargets.get(target.key);
        if (slider != null && (slider instanceof IntSetting || slider instanceof DoubleSetting) && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            int[] track = sliderTracks.get(slider);
            if (track != null) {
                sliding = slider;
                slideX = track[0];
                slideW = track[1];
                applySlider(mx);
                return true;
            }
        }
        if (target.key.equals(KEY_SEARCH)) {
            focusInput(search, KEY_SEARCH);
            search.clickAt(font, target.x, mx);
            return true;
        }
        if (target.key.equals(KEY_PROFILE_INPUT)) {
            focusInput(profileInput, KEY_PROFILE_INPUT);
            profileInput.clickAt(font, target.x, mx);
            return true;
        }
        target.onClick.accept(button);
        if (activeInput != null && target.key.equals(activeInputKey)) activeInput.clickAt(font, target.x, mx);
        return true;
    }

    private void applySlider(double mx) {
        double frac = slideW <= 0 ? 0 : (mx - slideX) / (double) slideW;
        frac = Math.max(0, Math.min(1, frac));
        if (sliding instanceof IntSetting i) i.setFraction(frac);
        else if (sliding instanceof DoubleSetting d) d.setFraction(frac);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (popover != null) return popover.mouseDragged(event.x(), event.y()) || true;
        if (sliding != null) {
            applySlider(event.x());
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (popover != null) {
            popover.mouseReleased();
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
        if (popover != null) {
            if (popover.contains(mx, my)) return popover.mouseScrolled(mx, my, sy);
            return true;
        }
        int step = (int) Math.round(sy * 24);
        if (panelCovers(mx, my)) {
            panelScroll = Math.max(0, panelScroll - step);
            return true;
        }
        if (!narrow && Render2D.hovered(mx, my, PAD, PAD, SIDEBAR_W, height - 2 * PAD)) {
            sidebarScroll = Math.max(0, sidebarScroll - step);
            return true;
        }
        if (Render2D.hovered(mx, my, contentX, contentY, contentW, contentH)) {
            if (settingsPage) pageScroll = Math.max(0, pageScroll - step);
            else gridScroll = Math.max(0, gridScroll - step);
            return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    // ---- keyboard ----------------------------------------------------------------------------

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        int mods = event.modifiers();
        boolean ctrl = (mods & GLFW.GLFW_MOD_CONTROL) != 0;
        boolean shift = (mods & GLFW.GLFW_MOD_SHIFT) != 0;

        if (popover != null) {
            if (popover.keyPressed(key, mods)) {
                if (popover != null && popover.wantsClose()) popover = null;
                return true;
            }
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                popover = null;
                return true;
            }
            return true;
        }
        if (listeningKey != null || listeningModuleKey) {
            int bound = key == GLFW.GLFW_KEY_ESCAPE ? Integer.MIN_VALUE
                    : key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE ? Keys.NONE : key;
            if (bound != Integer.MIN_VALUE) {
                if (listeningKey != null) listeningKey.set(bound);
                else if (panelModule != null) panelModule.setKeybind(bound);
            }
            listeningKey = null;
            listeningModuleKey = false;
            return true;
        }
        if (activeInput != null) {
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                blurInput();
                return true;
            }
            if (key == GLFW.GLFW_KEY_TAB) {
                blurInput();
                moveFocus(shift ? -1 : 1);
                return true;
            }
            if (activeInput.keyPressed(key, mods)) {
                if (activeInput == search) gridScroll = 0;
                return true;
            }
            return true;
        }

        switch (key) {
            case GLFW.GLFW_KEY_ESCAPE -> {
                if (panelTarget > 0) closePanel();
                else onClose();
                return true;
            }
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> {
                onClose();
                return true;
            }
            case GLFW.GLFW_KEY_TAB -> {
                moveFocus(shift ? -1 : 1);
                return true;
            }
            case GLFW.GLFW_KEY_SLASH -> {
                focusInput(search, KEY_SEARCH);
                search.cursorToEnd();
                return true;
            }
            case GLFW.GLFW_KEY_F -> {
                if (ctrl) {
                    focusInput(search, KEY_SEARCH);
                    search.cursorToEnd();
                    return true;
                }
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER, GLFW.GLFW_KEY_SPACE -> {
                if (activate()) return true;
            }
            case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_DOWN -> {
                if (arrow(key)) return true;
            }
            default -> {
            }
        }
        return super.keyPressed(event);
    }

    private boolean activate() {
        if (focusKey == null) return false;
        Hit h = hitFor(focusKey);
        if (h == null) return false;
        if (focusKey.equals(KEY_SEARCH)) {
            focusInput(search, KEY_SEARCH);
            search.cursorToEnd();
            return true;
        }
        if (focusKey.equals(KEY_PROFILE_INPUT)) {
            focusInput(profileInput, KEY_PROFILE_INPUT);
            profileInput.cursorToEnd();
            return true;
        }
        Setting<?> target = focusTargets.get(focusKey);
        if (target instanceof IntSetting || target instanceof DoubleSetting) return true;
        h.onClick.accept(GLFW.GLFW_MOUSE_BUTTON_LEFT);
        return true;
    }

    private boolean arrow(int key) {
        if (focusKey == null) return false;
        Setting<?> target = focusTargets.get(focusKey);
        boolean forward = key == GLFW.GLFW_KEY_RIGHT || key == GLFW.GLFW_KEY_UP;
        if (target instanceof IntSetting i) {
            i.set(i.get() + (forward ? i.step() : -i.step()));
            return true;
        }
        if (target instanceof DoubleSetting d) {
            d.set(d.get() + (forward ? d.step() : -d.step()));
            return true;
        }
        if (target instanceof EnumSetting<?> e) {
            e.cycle(forward);
            return true;
        }
        if (focusKey.startsWith("card:") && !cardOrder.isEmpty()) {
            int idx = cardOrder.indexOf(focusKey);
            if (idx < 0) return false;
            int next = switch (key) {
                case GLFW.GLFW_KEY_LEFT -> idx - 1;
                case GLFW.GLFW_KEY_RIGHT -> idx + 1;
                case GLFW.GLFW_KEY_UP -> idx - gridColumns;
                default -> idx + gridColumns;
            };
            if (next >= 0 && next < cardOrder.size()) {
                focusKey = cardOrder.get(next);
                scrollFocusIntoView();
            }
            return true;
        }
        if (focusKey.startsWith("cat:")) {
            List<String> keys = new ArrayList<>();
            for (Hit h : hits) if (h.key.startsWith("cat:")) keys.add(h.key);
            int idx = keys.indexOf(focusKey);
            int next = idx + (forward && key != GLFW.GLFW_KEY_RIGHT ? -1 : key == GLFW.GLFW_KEY_LEFT ? -1 : 1);
            if (next >= 0 && next < keys.size()) {
                focusKey = keys.get(next);
                hitFor(focusKey).onClick.accept(GLFW.GLFW_MOUSE_BUTTON_LEFT);
                focusKey = keys.get(next);
            }
            return true;
        }
        return false;
    }

    private void moveFocus(int direction) {
        List<String> order = new ArrayList<>();
        for (Hit h : hits) if (h.focusable) order.add(h.key);
        if (order.isEmpty()) return;
        int idx = focusKey == null ? -1 : order.indexOf(focusKey);
        int next = idx < 0 ? (direction > 0 ? 0 : order.size() - 1) : Math.floorMod(idx + direction, order.size());
        focusKey = order.get(next);
        scrollFocusIntoView();
    }

    private void scrollFocusIntoView() {
        Hit h = hitFor(focusKey);
        if (h == null || h.clip == null) return;
        if (h.y < h.clip[1]) {
            int delta = h.clip[1] - h.y + 6;
            if (h.key.startsWith("card:")) gridScroll = Math.max(0, gridScroll - delta);
            else if (settingsPage) pageScroll = Math.max(0, pageScroll - delta);
            else panelScroll = Math.max(0, panelScroll - delta);
        } else if (h.y + h.h > h.clip[3]) {
            int delta = h.y + h.h - h.clip[3] + 6;
            if (h.key.startsWith("card:")) gridScroll += delta;
            else if (settingsPage) pageScroll += delta;
            else panelScroll += delta;
        }
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        String ch = event.codepointAsString();
        if (popover != null) return popover.charTyped(ch) || true;
        if (listeningKey != null || listeningModuleKey) return true;
        if (activeInput != null) {
            if (activeInput.charTyped(ch) && activeInput == search) gridScroll = 0;
            return true;
        }
        return super.charTyped(event);
    }
}
