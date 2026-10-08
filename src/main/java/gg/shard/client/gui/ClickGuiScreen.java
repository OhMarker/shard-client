package gg.shard.client.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.mojang.blaze3d.platform.InputConstants;
import gg.shard.client.ShardClient;
import gg.shard.client.config.ConfigManager;
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
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.IntConsumer;

/**
 * Shard's menu (docs/DESIGN.md, direction A): a rail with search, filters and categories; a
 * list (or grid) of modules where the full name always fits; and a detail column with the
 * selected module's settings, always visible on wide windows. Search matches module names,
 * descriptions and individual settings. Everything is laid out in design units (see
 * {@link Scale}) on a 4-unit grid, so the page looks the same at every GUI scale; on medium
 * windows the detail replaces the list, on small ones the rail becomes a tab strip.
 *
 * <p>Input is routed through "hits": every control registers its rectangle while it renders,
 * so clicks, hover and keyboard focus all share one source of truth.
 */
public final class ClickGuiScreen extends DesignScreen {
    // ---- spacing grid (design units) ---------------------------------------------------------
    static final int PAD = 16;
    static final int RAIL_W = 176;
    static final int RAIL_ROW_H = 30;
    /** Below this page height the rail tightens its rows so every entry stays visible. */
    static final int COMPACT_HEIGHT = 420;
    /** From this width on the detail column sits next to the list instead of replacing it. */
    static final int WIDE_UNITS = 820;
    static final long DETAILS_DELAY_MS = 600;
    static final int GRID_GAP = 12;
    static final int LIST_ROW_H = 44;
    static final int CARD_H = 96;
    static final int CARD_MIN_W = 180;
    static final int DETAIL_W = 300;
    static final int PANEL_PAD = 16;
    static final int ROW_H = 32;
    static final int ROW_DESC_H = 44;
    static final int CONTROL_W = 150;
    static final int SECTION_ABOVE = 20;
    static final int SECTION_BELOW = 4;
    static final int SWITCH_W = 28;
    static final int SWITCH_H = 16;
    static final int BUTTON_H = 28;
    static final int FIELD_H = 28;
    static final int TOPBAR_H = 84;
    static final int SECTION_MAX_W = 600;
    // ---- typography ---------------------------------------------------------------------------
    static final int TITLE = 18;
    static final int NAME = 15;
    static final int SECTION = 13;
    static final int LABEL = 12;
    static final int DESC = 11;
    static final int HINT = 10;
    // ---- motion -------------------------------------------------------------------------------
    static final float HOVER_MS = 120f;
    static final float SWITCH_MS = 180f;
    static final float PANEL_MS = 220f;
    static final long FLASH_MS = 1600;

    private static final String KEY_SEARCH = "search";
    private static final String KEY_PROFILE_NAME = "profile-name";
    private static final String KEY_PROFILE_DESC = "profile-desc";
    private static final String KEY_SERVER_PATTERN = "server-pattern";

    /** Which modules the list shows when the search is empty. */
    private enum Filter { CATEGORY, FAVORITES, ENABLED }

    /** A clickable (and optionally focusable) rectangle registered during rendering. */
    private record Hit(String key, int x, int y, int w, int h, int[] clip, boolean focusable, IntConsumer onClick) {
        boolean contains(double mx, double my) {
            if (!(mx >= x && mx < x + w && my >= y && my < y + h)) return false;
            if (clip == null) return true;
            return mx >= clip[0] && mx < clip[2] && my >= clip[1] && my < clip[3];
        }
    }

    /** A setting found by the search, with the module that owns it. */
    private record SettingHit(Module module, Setting<?> setting, int score) {}

    /** What the smoke test checks across GUI scales. */
    public record LayoutInfo(int designWidth, int designHeight, boolean narrow, int gridColumns, int cardWidth, int cardHeight,
                             int sidebarWidth, int panelWidth, boolean panelOpen, double pageScale) {}

    private final Screen parent;
    private ModuleCategory category = ModuleCategory.COMBAT;
    private Filter filter = Filter.CATEGORY;
    private boolean settingsPage;
    private boolean gridView;
    private final Set<String> favorites = new LinkedHashSet<>();
    private final TextInput search = new TextInput(48).placeholder("Search").padLeft(30);
    private final TextInput profileName = new TextInput(32).placeholder("Profile name");
    private final TextInput profileDesc = new TextInput(80).placeholder("Description (optional)");
    private final TextInput serverPattern = new TextInput(64).placeholder("*.example.net or play.example.net:25566");
    private final Map<Setting<?>, TextInput> stringInputs = new HashMap<>();
    private final Map<Setting<?>, TextInput> numericInputs = new HashMap<>();
    private final Map<ServerBlacklist.Entry, TextInput> patternInputs = new HashMap<>();
    private TextInput activeInput;
    private String activeInputKey;
    private Runnable activeInputCommit;

    private Module panelModule;
    private float panelAnim;
    private float panelTarget;
    private float openProgress;
    private int gridScroll;
    private int panelScroll;
    private int pageScroll;
    private int sidebarScroll;
    private Setting<?> flashSetting;
    private long flashSince;
    private boolean flashScrollPending;

    private final List<Hit> hits = new ArrayList<>();
    private final Map<String, Setting<?>> focusTargets = new HashMap<>();
    private final List<String> cardOrder = new ArrayList<>();
    private int gridColumns = 1;
    private int cardWidth = CARD_MIN_W;
    private String focusKey;
    /** Focus rings only show while navigating with the keyboard (like CSS :focus-visible). */
    private boolean keyboardFocus;
    private int[] currentClip;
    private final Map<String, Float> anims = new HashMap<>();
    private Popover popover;
    private KeybindSetting listeningKey;
    private Module listeningModule;
    private Setting<?> sliding;
    private int slideX;
    private int slideW;
    private final Map<Setting<?>, int[]> sliderTracks = new HashMap<>();
    private String toast;
    private long toastUntil;
    private String hoverDetails;
    private String hoverDetailsShown;
    private long hoverDetailsSince;
    private int hoverDetailsX;
    private int hoverDetailsY;
    private final Set<ServerBlacklist.Entry> expandedRules = new HashSet<>();

    private int mouseX;
    private int mouseY;
    private boolean narrow;
    private boolean wide;
    private boolean panelReplacesGrid;
    private int contentX;
    private int contentY;
    private int contentW;
    private int contentH;
    private int listW;
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;

    /** Drawn inside another screen (the HUD editor): only the detail column, no page around it. */
    private boolean embedded;
    private Runnable embeddedClose = () -> {};

    public ClickGuiScreen(Screen parent) {
        super(Component.literal("Shard"));
        this.parent = parent;
    }

    /**
     * The detail column on its own, for the HUD editor's side panel. The host calls
     * {@code init}, {@code render} and forwards input while {@link #wantsInput} or
     * {@link #capturesKeys} says so; closing the column calls {@code onClose}.
     */
    public static ClickGuiScreen embedded(Runnable onClose) {
        ClickGuiScreen s = new ClickGuiScreen(null);
        s.embedded = true;
        s.embeddedClose = onClose;
        s.manageBlur = false;
        return s;
    }

    /** Whether a click at this GUI position belongs to the embedded column (or its popover). */
    public boolean wantsInput(double guiX, double guiY) {
        if (popover != null || sliding != null || listeningKey != null || listeningModule != null) return true;
        return panelModule != null && Render2D.hovered(toDesign(guiX), toDesign(guiY), panelX, panelY, panelW, panelH);
    }

    /** Whether key presses should go to the embedded column (typing, a popover, a key capture). */
    public boolean capturesKeys() {
        return popover != null || activeInput != null || listeningKey != null || listeningModule != null;
    }

    /** Left edge of the embedded column in GUI units, so the host can keep its chrome clear of it. */
    public int panelLeftGui() {
        return (int) Math.floor(Scale.toGui(panelX, pageScale));
    }

    // ---- lifecycle -----------------------------------------------------------------------------

    @Override
    protected void init() {
        if (anims.isEmpty() && openProgress == 0f) {
            var gui = ShardClient.config().gui();
            if (gui.has("category")) {
                String saved = gui.get("category").getAsString();
                switch (saved) {
                    case "settings" -> settingsPage = true;
                    case "favorites" -> filter = Filter.FAVORITES;
                    case "enabled" -> filter = Filter.ENABLED;
                    default -> {
                        try {
                            category = ModuleCategory.valueOf(saved);
                        } catch (IllegalArgumentException ignored) {
                            // keep default
                        }
                    }
                }
            }
            if (gui.has("view")) gridView = "grid".equals(gui.get("view").getAsString());
            if (gui.has("favorites") && gui.get("favorites").isJsonArray()) {
                for (JsonElement e : gui.getAsJsonArray("favorites")) favorites.add(e.getAsString());
            }
            if (gui.has("selected")) {
                String key = gui.get("selected").getAsString();
                for (Module m : ShardClient.modules().all()) if (m.key().equals(key) && !m.hidden()) panelModule = m;
            }
        }
        popover = null;
    }

    @Override
    public void onClose() {
        blurInput();
        if (embedded) {
            popover = null;
            embeddedClose.run();
            return;
        }
        var gui = ShardClient.config().gui();
        String saved = settingsPage ? "settings" : switch (filter) {
            case FAVORITES -> "favorites";
            case ENABLED -> "enabled";
            case CATEGORY -> category.name();
        };
        gui.addProperty("category", saved);
        gui.addProperty("view", gridView ? "grid" : "list");
        JsonArray favs = new JsonArray();
        for (String f : favorites) favs.add(f);
        gui.add("favorites", favs);
        if (panelModule != null) gui.addProperty("selected", panelModule.key());
        ShardClient.config().markDirty();
        ShardClient.config().save();
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Opens a module's settings (used by the smoke test, the HUD editor and setting search results). */
    public void openModule(Module module) {
        panelModule = module;
        panelTarget = 1f;
        if (wide) panelAnim = 1f;
        panelScroll = 0;
        settingsPage = false;
        if (module != null && search.isEmpty() && filter == Filter.CATEGORY && module.category() != category) category = module.category();
    }

    /** Opens the colour picker for a colour setting of the open module (smoke test). */
    public void openColorPicker(ColorSetting setting) {
        int ax = panelModule != null ? panelX + PANEL_PAD : contentX + 40;
        popover = new ColorPopover(setting, ax, contentY + 120, contentY + 120, designW, designH);
    }

    /** Shows the Settings entry (smoke test). */
    public void openSettingsPage() {
        selectSettings();
    }

    /** Types into the search field (smoke test). */
    public void setSearch(String query) {
        search.setValue(query);
        search.cursorToEnd();
        gridScroll = 0;
    }

    /** Opens the n-th setting found by the current search (smoke test). */
    public void openSearchResult(int index) {
        List<SettingHit> results = settingResults();
        if (index < results.size()) jumpTo(results.get(index).module(), results.get(index).setting());
    }

    public void setGridView(boolean grid) {
        gridView = grid;
        gridScroll = 0;
    }

    public Module openModule() {
        return panelTarget > 0 ? panelModule : null;
    }

    public LayoutInfo layoutInfo() {
        int cardH = gridView ? CARD_H : LIST_ROW_H;
        return new LayoutInfo(designW, designH, narrow, gridColumns, cardWidth, cardH, narrow ? 0 : RAIL_W, panelW,
                panelModule != null && panelTarget > 0, pageScale);
    }

    // ---- animation helpers ---------------------------------------------------------------------

    /** Linear 0..1 progress toward {@code target} over {@code durationMs}; ease the result when drawing. */
    private float anim(String key, float target, float durationMs) {
        float current = anims.getOrDefault(key, target);
        float next = Render2D.step(current, target, dt, durationMs);
        anims.put(key, next);
        return next;
    }

    private void showToast(String text) {
        toast = text;
        toastUntil = System.currentTimeMillis() + 2400;
    }

    // ---- hit registration ----------------------------------------------------------------------

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
        return keyboardFocus && key != null && key.equals(focusKey);
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

    private boolean hoverable(Hit candidate) {
        if (popover != null) return false;
        // In the narrow layouts the panel renders over the list, so a row under it must not light up.
        if (panelCovers(mouseX, mouseY) && !Render2D.hovered(candidate.x + candidate.w / 2.0, candidate.y + candidate.h / 2.0, panelX, panelY, panelW, panelH)) {
            return false;
        }
        Hit top = hitAt(mouseX, mouseY);
        return top != null && top.key.equals(candidate.key);
    }

    // ---- rendering -----------------------------------------------------------------------------

    /** Vanilla calls this before {@link #render}: blur the world once, then tint it. */
    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float partialTick) {
        if (minecraft.level == null) renderMenuBackground(g);
        else renderBlurredBackground(g);
        g.fill(0, 0, width, height, Colors.fade(Theme.overlay(), Math.max(0.35f, Render2D.easeOut(openProgress))));
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick) {
        beginFrame();
        g0 = g;
        openProgress = Render2D.step(openProgress, 1f, dt, PANEL_MS);
        mouseX = (int) Math.floor(toDesign(mx));
        mouseY = (int) Math.floor(toDesign(my));

        hits.clear();
        focusTargets.clear();
        cardOrder.clear();
        sliderTracks.clear();
        currentClip = null;
        hoverDetails = null;

        pushDesign(g);
        // Open: fade in and grow from 98% to 100% around the centre.
        float open = Render2D.easeOut(openProgress);
        var pose = g.pose();
        pose.pushMatrix();
        float s = 0.98f + 0.02f * open;
        pose.translate(designW / 2f * (1 - s), designH / 2f * (1 - s));
        pose.scale(s, s);

        if (embedded) {
            narrow = false;
            wide = true;
            settingsPage = false;
            panelReplacesGrid = false;
            panelTarget = panelAnim = panelModule == null ? 0f : 1f;
            layoutPanel();
            contentX = panelX;
            contentY = panelY;
            contentW = panelW;
            contentH = panelH;
            if (panelModule != null) renderPanel(g);
        } else {
            layout();
            ensureSelection();
            panelAnim = wide ? panelTarget : Render2D.step(panelAnim, panelTarget, dt, PANEL_MS);
            if (!wide && panelTarget == 0f && panelAnim == 0f) panelModule = null;
            layoutPanel();

            if (narrow) renderTopBar(g);
            else renderRail(g);

            boolean hideList = panelReplacesGrid && panelAnim > 0.02f && !settingsPage;
            if (settingsPage) renderSettingsPage(g);
            else if (!hideList) renderList(g);
            if (!settingsPage && (wide || (panelModule != null && panelAnim > 0.001f))) renderPanel(g);
        }
        pose.popMatrix();

        if (popover != null) {
            g.nextStratum();
            popover.render(g, mouseX, mouseY, dt);
            if (popover.wantsClose()) popover = null;
        } else if (hoverDetails != null && detailsReady()) {
            g.nextStratum();
            renderDetails(g);
        }
        if (hoverDetails == null) hoverDetailsShown = null;
        renderToast(g);
        popDesign(g);
    }

    private void layout() {
        narrow = Scale.narrow(designW);
        wide = !narrow && designW >= WIDE_UNITS;
        if (narrow) {
            contentX = PAD;
            contentY = PAD + TOPBAR_H + GRID_GAP;
            contentW = designW - 2 * PAD;
            contentH = designH - contentY - PAD;
        } else {
            contentX = PAD + RAIL_W + 24;
            contentY = PAD + 4;
            contentW = designW - contentX - PAD;
            contentH = designH - contentY - PAD;
        }
        panelReplacesGrid = !wide;
        listW = wide && !settingsPage ? contentW - DETAIL_W - 24 : contentW;
        gridColumns = gridView && search.isEmpty() ? Scale.gridColumns(listW, CARD_MIN_W, GRID_GAP) : 1;
        cardWidth = Scale.cardWidth(listW, gridColumns, GRID_GAP);
    }

    /** Where the detail column sits: its own column on wide windows, sliding over the list otherwise. */
    private void layoutPanel() {
        if (wide) {
            panelW = DETAIL_W;
            panelX = designW - PAD - DETAIL_W;
            panelY = PAD;
            panelH = designH - 2 * PAD;
            return;
        }
        panelW = contentW;
        panelY = contentY;
        panelH = contentH;
        float eased = Render2D.easeOut(panelAnim);
        panelX = contentX + Math.round((1f - eased) * (panelW + PAD));
    }

    /** On wide windows something is always selected: the saved module, else the first in the list. */
    private void ensureSelection() {
        if (!wide || settingsPage || embedded) return;
        if (panelModule == null || panelModule.hidden()) {
            List<Module> visible = visibleModules();
            panelModule = visible.isEmpty() ? null : visible.get(0);
        }
        panelTarget = panelModule == null ? 0f : 1f;
    }

    private boolean panelCovers(double mx, double my) {
        if (wide || settingsPage) return false;
        return panelModule != null && panelAnim > 0.001f && Render2D.hovered(mx, my, panelX, panelY, panelW, panelH);
    }

    private List<ModuleCategory> categories() {
        List<ModuleCategory> out = new ArrayList<>();
        for (ModuleCategory c : ModuleCategory.values()) if (!ShardClient.modules().byCategory(c).isEmpty()) out.add(c);
        return out;
    }

    static String version() {
        return FabricLoader.getInstance().getModContainer(ShardClient.MOD_ID)
                .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("dev");
    }

    // ---- rail / top bar ------------------------------------------------------------------------

    private void renderRail(GuiGraphics g) {
        int x = PAD;
        int y = PAD;
        int w = RAIL_W;
        int h = designH - 2 * PAD;
        Render2D.panel(g, x, y, w, h, Theme.radiusLarge(), Theme.surface(), Theme.line());
        hit("rail", x, y, w, h, false, b -> {});
        int ix = x + 10;
        int iw = w - 20;

        // Brand: mark, name, version.
        int brandY = y + 14;
        Icons.draw(g, "logo", ix + 4, brandY, 18, Theme.accent());
        Fonts.draw(g, "Shard", Fonts.Weight.SEMIBOLD, SECTION, ix + 30, brandY + (18 - Fonts.lineHeight(SECTION)) / 2, Theme.text());
        Fonts.drawRight(g, version(), Fonts.Weight.MEDIUM, HINT, ix + iw - 4, brandY + (18 - Fonts.lineHeight(HINT)) / 2, Theme.subtle());

        boolean compact = designH < COMPACT_HEIGHT;
        int rowH = compact ? 26 : RAIL_ROW_H;
        int searchY = y + (compact ? 40 : 46);
        renderSearch(g, ix, searchY, iw, FIELD_H);

        int listY = searchY + FIELD_H + (compact ? 6 : 12);
        int bottomH = 2 * rowH + 2 + 10;
        int listH = y + h - bottomH - listY - 6;
        clip(g, x, listY, w, listH);
        int cy = listY - sidebarScroll;
        boolean noSearch = search.isEmpty();
        railRow(g, "nav:favorites", "Favorites", "favorites", noSearch && !settingsPage && filter == Filter.FAVORITES, ix, cy, iw, rowH, true,
                () -> selectFilter(Filter.FAVORITES));
        cy += rowH + 2;
        railRow(g, "nav:enabled", "Enabled", "enabled", noSearch && !settingsPage && filter == Filter.ENABLED, ix, cy, iw, rowH, true,
                () -> selectFilter(Filter.ENABLED));
        cy += rowH + (compact ? 6 : 14);
        if (!compact) {
            Fonts.draw(g, "CATEGORIES", Fonts.Weight.MEDIUM, HINT, ix + 8, cy, Theme.subtle());
            cy += Fonts.lineHeight(HINT) + 6;
        }
        for (ModuleCategory c : categories()) {
            railRow(g, "cat:" + c.name(), c.displayName(), Icons.categoryIcon(c), noSearch && !settingsPage && filter == Filter.CATEGORY && c == category,
                    ix, cy, iw, rowH, true, () -> selectCategory(c));
            cy += rowH + 2;
        }
        int contentHeight = cy + sidebarScroll - listY;
        sidebarScroll = Math.max(0, Math.min(sidebarScroll, contentHeight - listH));
        unclip(g);

        int by = y + h - 10 - 2 * rowH - 2;
        g.fill(ix + 6, by - 6, ix + iw - 6, by - 5, Theme.line());
        boolean canEdit = minecraft.player != null;
        railRow(g, "nav:hud-editor", "Edit HUD", "edit-hud", false, ix, by, iw, rowH, canEdit, () -> {
            if (canEdit) minecraft.setScreen(new HudEditorScreen(this, ShardClient.hud()));
        });
        railRow(g, "cat:settings", "Settings", "settings", settingsPage, ix, by + rowH + 2, iw, rowH, true, this::selectSettings);
    }

    private void railRow(GuiGraphics g, String key, String label, String icon, boolean selected,
                         int x, int y, int w, int h, boolean enabled, Runnable onSelect) {
        Hit probe = new Hit(key, x, y, w, h, currentClip, enabled, b -> {
            if (enabled) onSelect.run();
        });
        hits.add(probe);
        boolean hover = enabled && hoverable(probe);
        float sel = Render2D.easeInOut(anim(key + ":sel", selected ? 1f : 0f, HOVER_MS));
        float hov = Render2D.easeInOut(anim(key + ":hov", hover ? 1f : 0f, HOVER_MS));
        int fill = Colors.mix(Colors.mix(0x00000000, Theme.surfaceHover(), hov), Theme.control(), sel);
        if (Colors.alpha(fill) > 4) Render2D.roundedRect(g, x, y, w, h, Theme.radiusSmall(), fill);
        if (focused(key)) Render2D.roundedOutline(g, x, y, w, h, Theme.radiusSmall(), Theme.accentAlpha(0xA0));
        int textColor = !enabled ? Theme.subtle() : selected || hover ? Theme.text() : Theme.muted();
        Icons.draw(g, icon, x + 8, y + (h - 16) / 2, 16, selected ? Theme.accent() : enabled ? Theme.muted() : Theme.subtle());
        Fonts.drawClipped(g, label, Fonts.Weight.MEDIUM, LABEL, x + 34, y + (h - Fonts.lineHeight(LABEL)) / 2, w - 42, textColor);
    }

    private void renderTopBar(GuiGraphics g) {
        int x = PAD;
        int y = PAD;
        int w = designW - 2 * PAD;
        Render2D.panel(g, x, y, w, TOPBAR_H, Theme.radiusLarge(), Theme.surface(), Theme.line());
        hit("topbar", x, y, w, TOPBAR_H, false, b -> {});
        Icons.draw(g, "logo", x + 14, y + 12 + (FIELD_H - 18) / 2, 18, Theme.accent());
        Fonts.draw(g, "Shard", Fonts.Weight.SEMIBOLD, SECTION, x + 40, y + 12 + (FIELD_H - Fonts.lineHeight(SECTION)) / 2, Theme.text());
        int searchX = x + 40 + Fonts.widthInt("Shard", Fonts.Weight.SEMIBOLD, SECTION) + 16;
        renderSearch(g, searchX, y + 12, x + w - 12 - searchX, FIELD_H);

        int tabY = y + 12 + FIELD_H + 10;
        int tabH = 26;
        int tx = x + 10;
        List<ModuleCategory> cats = categories();
        int hudW = Fonts.widthInt("Edit HUD", Fonts.Weight.MEDIUM, DESC) + 20;
        int available = w - 20 - hudW - 8;
        int needed = Fonts.widthInt("Settings", Fonts.Weight.MEDIUM, DESC) + 20;
        for (ModuleCategory c : cats) needed += Fonts.widthInt(c.displayName(), Fonts.Weight.MEDIUM, DESC) + 20;
        boolean compact = needed > available;
        for (ModuleCategory c : cats) {
            String label = compact ? compactName(c) : c.displayName();
            tx += renderTab(g, "cat:" + c.name(), label, search.isEmpty() && !settingsPage && filter == Filter.CATEGORY && c == category, tx, tabY, tabH,
                    () -> selectCategory(c));
        }
        renderTab(g, "cat:settings", compact ? "More" : "Settings", settingsPage, tx, tabY, tabH, this::selectSettings);
        boolean canEdit = minecraft.player != null;
        button(g, "nav:hud-editor", x + w - 10 - hudW, tabY, hudW, tabH, "Edit HUD", false, canEdit, b -> {
            if (canEdit) minecraft.setScreen(new HudEditorScreen(this, ShardClient.hud()));
        });
    }

    private static String compactName(ModuleCategory c) {
        return switch (c) {
            case PERFORMANCE -> "Perf";
            default -> c.displayName();
        };
    }

    private int renderTab(GuiGraphics g, String key, String label, boolean selected, int x, int y, int h, Runnable onSelect) {
        int w = Fonts.widthInt(label, Fonts.Weight.MEDIUM, DESC) + 20;
        Hit probe = new Hit(key, x, y, w, h, currentClip, true, b -> onSelect.run());
        hits.add(probe);
        boolean hover = hoverable(probe);
        float sel = Render2D.easeInOut(anim(key + ":sel", selected ? 1f : 0f, HOVER_MS));
        int fill = Colors.mix(hover ? Theme.surfaceHover() : 0x00000000, Theme.control(), sel);
        if (Colors.alpha(fill) > 4) Render2D.roundedRect(g, x, y, w, h, Theme.radiusSmall(), fill);
        if (focused(key)) Render2D.roundedOutline(g, x, y, w, h, Theme.radiusSmall(), Theme.accentAlpha(0xA0));
        Fonts.draw(g, label, Fonts.Weight.MEDIUM, DESC, x + 10, y + (h - Fonts.lineHeight(DESC)) / 2, selected ? Theme.text() : Theme.muted());
        return w + 4;
    }

    private void renderSearch(GuiGraphics g, int x, int y, int w, int h) {
        boolean isFocused = activeInput == search;
        search.render(g, x, y, w, h, isFocused);
        Icons.draw(g, "search", x + 9, y + (h - 14) / 2, 14, isFocused ? Theme.accent() : Theme.subtle());
        if (search.isEmpty() && !isFocused) {
            String hint = "Ctrl F";
            int hw = Fonts.widthInt(hint, Fonts.Weight.MEDIUM, HINT) + 10;
            int hx = x + w - 6 - hw;
            int hy = y + (h - 16) / 2;
            Render2D.roundedOutline(g, hx, hy, hw, 16, 4, Theme.lineStrong());
            Fonts.draw(g, hint, Fonts.Weight.MEDIUM, HINT, hx + 5, hy + (16 - Fonts.lineHeight(HINT)) / 2, Theme.subtle());
        }
        if (focused(KEY_SEARCH) && !isFocused) Render2D.roundedOutline(g, x, y, w, h, Theme.radiusSmall(), Theme.accentAlpha(0xA0));
        textHit(KEY_SEARCH, search, x, y, w, h, null);
    }

    private void selectCategory(ModuleCategory c) {
        popover = null;
        settingsPage = false;
        filter = Filter.CATEGORY;
        category = c;
        gridScroll = 0;
        clearSearch();
        if (!wide && panelModule != null && panelModule.category() != c) closePanel();
        if (wide && panelModule != null && panelModule.category() != c) panelModule = null;
        focusKey = "cat:" + c.name();
    }

    private void selectFilter(Filter f) {
        popover = null;
        settingsPage = false;
        filter = f;
        gridScroll = 0;
        clearSearch();
        if (!wide) closePanel();
        else panelModule = null;
        focusKey = "nav:" + f.name().toLowerCase(Locale.ROOT);
    }

    private void selectSettings() {
        popover = null;
        settingsPage = true;
        pageScroll = 0;
        clearSearch();
        if (!wide) closePanel();
        focusKey = "cat:settings";
    }

    private void clearSearch() {
        if (activeInput == search) blurInput();
        search.setValue("");
    }

    private void closePanel() {
        panelTarget = 0f;
        popover = null;
        listeningKey = null;
        listeningModule = null;
        blurInput();
    }

    // ---- module list / grid --------------------------------------------------------------------

    private List<Module> visibleModules() {
        String q = search.value().trim();
        if (q.isEmpty()) {
            return switch (filter) {
                case CATEGORY -> ShardClient.modules().byCategory(category);
                case FAVORITES -> {
                    List<Module> out = new ArrayList<>();
                    for (Module m : ShardClient.modules().all()) if (!m.hidden() && favorites.contains(m.key())) out.add(m);
                    yield out;
                }
                case ENABLED -> {
                    List<Module> out = new ArrayList<>();
                    for (Module m : ShardClient.modules().all()) if (!m.hidden() && m.isToggledOn()) out.add(m);
                    yield out;
                }
            };
        }
        List<Module> out = new ArrayList<>();
        Map<Module, Integer> scores = new HashMap<>();
        for (Module m : ShardClient.modules().all()) {
            if (m.hidden()) continue;
            int score = SearchMatcher.score(q, m.name(), m.description(), m.category().displayName());
            if (score >= 0) {
                out.add(m);
                scores.put(m, score);
            }
        }
        out.sort(Comparator.comparingInt((Module m) -> -scores.get(m)));
        return out;
    }

    /** Individual settings matching the search, best first (hidden modules are the Settings page). */
    private List<SettingHit> settingResults() {
        String q = search.value().trim();
        List<SettingHit> out = new ArrayList<>();
        if (q.isEmpty()) return out;
        for (Module m : ShardClient.modules().all()) {
            for (Setting<?> s : m.settings()) {
                if (!s.isVisible()) continue;
                int score = SearchMatcher.score(q, s.name(), m.name(), s.group(), s.description());
                if (score >= 0) out.add(new SettingHit(m, s, score));
            }
        }
        out.sort(Comparator.comparingInt(h -> -h.score()));
        return out.size() > 12 ? new ArrayList<>(out.subList(0, 12)) : out;
    }

    private int headerHeight() {
        return Fonts.lineHeight(TITLE) + 2 + Fonts.lineHeight(DESC) + 14;
    }

    private void renderPageHeader(GuiGraphics g, String title, String sub, int w) {
        Fonts.drawClipped(g, title, Fonts.Weight.SEMIBOLD, TITLE, contentX, contentY, w, Theme.text());
        Fonts.drawClipped(g, sub, Fonts.Weight.REGULAR, DESC, contentX, contentY + Fonts.lineHeight(TITLE) + 2, w, Theme.muted());
    }

    private String listTitle(boolean searching) {
        if (searching) return "Results";
        return switch (filter) {
            case FAVORITES -> "Favorites";
            case ENABLED -> "Enabled";
            case CATEGORY -> category.displayName();
        };
    }

    private String listSubtitle(boolean searching, int modules, int settings) {
        if (searching) {
            int n = modules + settings;
            return n == 0 ? "Nothing matches \"" + search.value().trim() + "\"" : n + (n == 1 ? " match for \"" : " matches for \"") + search.value().trim() + "\"";
        }
        return switch (filter) {
            case FAVORITES -> "Star a module in its panel to pin it here.";
            case ENABLED -> "Everything that is switched on right now.";
            case CATEGORY -> category.description();
        };
    }

    private void renderList(GuiGraphics g) {
        boolean searching = !search.isEmpty();
        List<Module> modules = visibleModules();
        List<SettingHit> settings = settingResults();
        int toggleW = 54;
        renderPageHeader(g, listTitle(searching), listSubtitle(searching, modules.size(), settings.size()), listW - (searching ? 0 : toggleW + 12));
        if (!searching) renderViewToggle(g, contentX + listW - toggleW, contentY + 4, toggleW);

        int top = contentY + headerHeight();
        int viewH = contentY + contentH - top;
        if (modules.isEmpty() && settings.isEmpty()) {
            String icon = filter == Filter.FAVORITES && !searching ? "favorites" : "search";
            Icons.draw(g, icon, contentX + listW / 2 - 12, top + 32, 24, Theme.subtle());
            String msg = searching ? "Try another word" : filter == Filter.FAVORITES ? "No favorites yet" : "Nothing here yet";
            Fonts.drawCentered(g, msg, Fonts.Weight.MEDIUM, LABEL, contentX + listW / 2, top + 64, Theme.muted());
            return;
        }

        clip(g, contentX - 4, top, listW + 8, viewH);
        int y = top - gridScroll;
        if (gridView && search.isEmpty()) {
            for (int i = 0; i < modules.size(); i++) {
                Module m = modules.get(i);
                int cx = contentX + (i % gridColumns) * (cardWidth + GRID_GAP);
                int cy = y + (i / gridColumns) * (CARD_H + GRID_GAP);
                cardOrder.add("row:" + m.key());
                if (cy + CARD_H >= top && cy <= top + viewH) renderCard(g, m, cx, cy, cardWidth);
            }
            y += ((modules.size() + gridColumns - 1) / gridColumns) * (CARD_H + GRID_GAP);
        } else {
            for (Module m : modules) {
                cardOrder.add("row:" + m.key());
                if (y + LIST_ROW_H >= top && y <= top + viewH) renderModuleRow(g, m, contentX, y, listW);
                y += LIST_ROW_H + 2;
            }
        }
        if (!settings.isEmpty()) {
            y += 12;
            Fonts.draw(g, "SETTINGS", Fonts.Weight.MEDIUM, HINT, contentX + 12, y, Theme.subtle());
            y += Fonts.lineHeight(HINT) + 6;
            for (SettingHit sh : settings) {
                if (y + 36 >= top && y <= top + viewH) renderSettingResult(g, sh, contentX, y, listW);
                y += 38;
            }
        }
        int contentHeight = y + gridScroll - top + 8;
        gridScroll = Math.max(0, Math.min(gridScroll, contentHeight - viewH));
        // Fade the bottom edge when more is below.
        if (contentHeight - gridScroll > viewH + 2) {
            Render2D.gradientV(g, contentX - 4, top + viewH - 16, listW + 8, 16, 0x00000000, Colors.withAlpha(Theme.overlay(), 0xC0));
        }
        unclip(g);
    }

    private void renderViewToggle(GuiGraphics g, int x, int y, int w) {
        int h = 24;
        Render2D.panel(g, x, y, w, h, Theme.radiusSmall(), Theme.surfaceRaised(), Theme.line());
        int half = (w - 4) / 2;
        viewButton(g, "view:grid", "grid", gridView, x + 2, y + 2, half, h - 4, () -> setGridView(true));
        viewButton(g, "view:list", "list", !gridView, x + 2 + half, y + 2, half, h - 4, () -> setGridView(false));
    }

    private void viewButton(GuiGraphics g, String key, String icon, boolean on, int x, int y, int w, int h, Runnable onClick) {
        Hit probe = new Hit(key, x, y, w, h, currentClip, true, b -> onClick.run());
        hits.add(probe);
        boolean hover = hoverable(probe);
        if (on) Render2D.roundedRect(g, x, y, w, h, 4, Theme.control());
        else if (hover) Render2D.roundedRect(g, x, y, w, h, 4, Theme.surfaceHover());
        if (focused(key)) Render2D.roundedOutline(g, x, y, w, h, 4, Theme.accentAlpha(0xA0));
        Icons.draw(g, icon, x + (w - 14) / 2, y + (h - 14) / 2, 14, on ? Theme.text() : Theme.subtle());
    }

    private void selectModule(Module m) {
        if (panelModule != m) panelScroll = 0;
        openModule(m);
        focusKey = "row:" + m.key();
    }

    /** One list row: icon, full name, one line of description, switch. Right-click toggles. */
    private void renderModuleRow(GuiGraphics g, Module m, int x, int y, int w) {
        String key = "row:" + m.key();
        int h = LIST_ROW_H;
        Hit row = new Hit(key, x, y, w, h, currentClip, true, b -> {
            if (b == GLFW.GLFW_MOUSE_BUTTON_RIGHT) m.toggle();
            else selectModule(m);
        });
        hits.add(row);
        boolean hover = hoverable(row) || (popover == null && focused(key));
        boolean selected = panelModule == m && panelTarget > 0;
        float hov = Render2D.easeInOut(anim(key + ":hov", hover ? 1f : 0f, HOVER_MS));
        float sel = Render2D.easeInOut(anim(key + ":sel", selected ? 1f : 0f, HOVER_MS));
        int fill = Colors.mix(Colors.mix(0x00000000, Theme.surfaceRaised(), hov), Theme.surfaceRaised(), sel);
        if (Colors.alpha(fill) > 4) Render2D.roundedRect(g, x, y, w, h, Theme.radius(), fill);
        if (sel > 0.01f) Render2D.roundedRect(g, x, y + 10, 2, h - 20, 1, Colors.fade(Theme.accent(), sel));
        if (focused(key)) Render2D.roundedOutline(g, x, y, w, h, Theme.radius(), Theme.accentAlpha(0xA0));

        Icons.draw(g, m, x + 12, y + (h - 16) / 2, 16, m.isEnabled() ? Theme.accent() : Theme.muted());
        int textX = x + 40;
        int switchX = x + w - 12 - SWITCH_W;
        int textW = switchX - 12 - textX;
        int nameY = y + (h - Fonts.lineHeight(LABEL) - Fonts.lineHeight(DESC)) / 2;
        int nameW = Fonts.widthInt(m.name(), Fonts.Weight.SEMIBOLD, LABEL);
        Fonts.drawClipped(g, m.name(), Fonts.Weight.SEMIBOLD, LABEL, textX, nameY, textW, Theme.text());
        if (favorites.contains(m.key()) && nameW + 18 < textW) Icons.draw(g, "favorite-on", textX + nameW + 6, nameY + (Fonts.lineHeight(LABEL) - 10) / 2, 10, Theme.subtle());
        String notice = notice(m);
        int descY = nameY + Fonts.lineHeight(LABEL);
        if (notice != null) Fonts.drawClipped(g, notice, Fonts.Weight.REGULAR, DESC, textX, descY, textW, Theme.warning());
        else Fonts.drawClipped(g, m.description(), Fonts.Weight.REGULAR, DESC, textX, descY, textW, Theme.muted());
        renderSwitch(g, "sw:" + m.key(), switchX, y + (h - SWITCH_H) / 2, m.isToggledOn(), notice == null, m::toggle);
    }

    /** Grid card: icon and switch on top, the full name on its own line, two lines of description. */
    private void renderCard(GuiGraphics g, Module m, int x, int y, int w) {
        String key = "row:" + m.key();
        int h = CARD_H;
        Hit card = new Hit(key, x, y, w, h, currentClip, true, b -> {
            if (b == GLFW.GLFW_MOUSE_BUTTON_RIGHT) m.toggle();
            else selectModule(m);
        });
        hits.add(card);
        boolean hover = hoverable(card) || (popover == null && focused(key));
        boolean selected = panelModule == m && panelTarget > 0;
        float hov = Render2D.easeInOut(anim(key + ":hov", hover ? 1f : 0f, HOVER_MS));
        int lift = Math.round(hov);
        int fill = Colors.mix(Theme.surfaceRaised(), Theme.surfaceHover(), hov);
        Render2D.roundedRect(g, x, y - lift, w, h, Theme.radius(), fill);
        int border = selected ? Theme.accentAlpha(0x90) : m.isEnabled() ? Theme.accentAlpha(0x38) : Colors.mix(Theme.line(), Theme.lineStrong(), hov);
        if (focused(key)) border = Theme.accentAlpha(0xC0);
        Render2D.roundedOutline(g, x, y - lift, w, h, Theme.radius(), border);
        int top = y - lift + 12;
        Icons.draw(g, m, x + 12, top, 16, m.isEnabled() ? Theme.accent() : Theme.muted());
        String notice = notice(m);
        renderSwitch(g, "sw:" + m.key(), x + w - 12 - SWITCH_W, top, m.isToggledOn(), notice == null, m::toggle);
        int textW = w - 24;
        int nameY = top + 24;
        Fonts.drawClipped(g, m.name(), Fonts.Weight.SEMIBOLD, LABEL, x + 12, nameY, textW, Theme.text());
        int descY = nameY + Fonts.lineHeight(LABEL) + 2;
        if (notice != null) {
            Fonts.drawClipped(g, notice, Fonts.Weight.REGULAR, DESC, x + 12, descY, textW, Theme.warning());
            return;
        }
        List<String> lines = Fonts.wrap(m.description(), Fonts.Weight.REGULAR, DESC, textW);
        for (int i = 0; i < Math.min(2, lines.size()); i++) {
            String line = i == 1 && lines.size() > 2 ? Fonts.clip(lines.get(1) + " " + lines.get(2), Fonts.Weight.REGULAR, DESC, textW) : lines.get(i);
            Fonts.draw(g, line, Fonts.Weight.REGULAR, DESC, x + 12, descY + i * Fonts.lineHeight(DESC), Theme.muted());
        }
    }

    /** A setting found by the search; clicking opens its module and flashes the row. */
    private void renderSettingResult(GuiGraphics g, SettingHit sh, int x, int y, int w) {
        String key = "res:" + sh.module().key() + ":" + sh.setting().key();
        int h = 36;
        Hit row = new Hit(key, x, y, w, h, currentClip, true, b -> jumpTo(sh.module(), sh.setting()));
        hits.add(row);
        boolean hover = hoverable(row) || (popover == null && focused(key));
        float hov = Render2D.easeInOut(anim(key + ":hov", hover ? 1f : 0f, HOVER_MS));
        if (hov > 0.01f) Render2D.roundedRect(g, x, y, w, h, Theme.radius(), Colors.fade(Theme.surfaceRaised(), hov));
        if (focused(key)) Render2D.roundedOutline(g, x, y, w, h, Theme.radius(), Theme.accentAlpha(0xA0));
        Icons.draw(g, sh.module(), x + 12, y + (h - 16) / 2, 16, Theme.muted());
        int textW = w - 40 - 32;
        int nameY = y + (h - Fonts.lineHeight(LABEL) - Fonts.lineHeight(HINT)) / 2;
        Fonts.drawClipped(g, sh.setting().name(), Fonts.Weight.MEDIUM, LABEL, x + 40, nameY, textW, Theme.text());
        String where = sh.module().hidden() ? "Settings · " + sh.module().name() : sh.module().name();
        if (!sh.setting().group().isEmpty()) where += " · " + sh.setting().group();
        Fonts.drawClipped(g, where, Fonts.Weight.REGULAR, HINT, x + 40, nameY + Fonts.lineHeight(LABEL), textW, Theme.subtle());
        Icons.draw(g, "chevron-right", x + w - 12 - 14, y + (h - 14) / 2, 14, Theme.subtle());
    }

    private void jumpTo(Module m, Setting<?> s) {
        if (m.hidden()) {
            selectSettings();
            return;
        }
        openModule(m);
        panelScroll = 0;
        flashSetting = s;
        flashSince = System.currentTimeMillis();
        flashScrollPending = true;
        focusKey = "set:" + m.key() + ":" + s.key();
    }

    private static String notice(Module m) {
        if (m.blockedBy() != null) return "Off: " + m.blockedBy() + " is installed";
        if (m.isSuppressed()) return "Off on this server";
        return null;
    }

    // ---- controls ------------------------------------------------------------------------------

    private void renderSwitch(GuiGraphics g, String key, int x, int y, boolean on, boolean active, Runnable onToggle) {
        Hit probe = new Hit(key, x - 6, y - 6, SWITCH_W + 12, SWITCH_H + 12, currentClip, true, b -> onToggle.run());
        hits.add(probe);
        float knob = Render2D.easeInOut(anim(key, on ? 1f : 0f, SWITCH_MS));
        Render2D.toggle(g, x, y, SWITCH_W, SWITCH_H, knob, on, focused(key));
        if (!active) Render2D.roundedRect(g, x, y, SWITCH_W, SWITCH_H, SWITCH_H / 2, Colors.withAlpha(Theme.surface(), 0x90));
    }

    private void button(GuiGraphics g, String key, int x, int y, int w, int h, String label, boolean primary, boolean enabled, IntConsumer onClick) {
        button(g, key, x, y, w, h, label, primary, false, enabled, onClick);
    }

    private void button(GuiGraphics g, String key, int x, int y, int w, int h, String label, boolean primary, boolean danger, boolean enabled, IntConsumer onClick) {
        Hit probe = new Hit(key, x, y, w, h, currentClip, enabled, b -> {
            if (enabled) onClick.accept(b);
        });
        hits.add(probe);
        boolean hover = enabled && hoverable(probe);
        float hov = Render2D.easeInOut(anim(key + ":hov", hover ? 1f : 0f, HOVER_MS));
        int r = Theme.radiusSmall();
        int fill;
        int color;
        if (primary) {
            fill = Colors.mix(Theme.accent(), Theme.accentHover(), hov);
            color = Theme.accentText();
        } else if (danger) {
            fill = Colors.mix(Colors.withAlpha(Theme.danger(), 0x1A), Colors.withAlpha(Theme.danger(), 0x30), hov);
            color = Theme.danger();
        } else {
            fill = Colors.mix(Theme.surfaceRaised(), Theme.surfaceHover(), hov);
            color = enabled ? Theme.text() : Theme.subtle();
        }
        if (!enabled) fill = Colors.withAlpha(fill, 0x70);
        Render2D.roundedRect(g, x, y, w, h, r, fill);
        int outline = focused(key) ? Theme.accentAlpha(0xC0) : primary ? 0 : danger ? Colors.withAlpha(Theme.danger(), 0x40) : Theme.lineStrong();
        Render2D.roundedOutline(g, x, y, w, h, r, outline);
        if (!label.isEmpty()) {
            Fonts.drawCentered(g, Fonts.clip(label, Fonts.Weight.MEDIUM, LABEL, w - 16), Fonts.Weight.MEDIUM, LABEL, x + w / 2, y + (h - Fonts.lineHeight(LABEL)) / 2, color);
        }
    }

    /** Small square icon button (back, star). */
    private void iconButton(GuiGraphics g, String key, String icon, int x, int y, int size, int color, IntConsumer onClick) {
        Hit probe = new Hit(key, x, y, size, size, currentClip, true, onClick);
        hits.add(probe);
        boolean hover = hoverable(probe);
        if (hover) Render2D.roundedRect(g, x, y, size, size, Theme.radiusSmall(), Theme.surfaceHover());
        if (focused(key)) Render2D.roundedOutline(g, x, y, size, size, Theme.radiusSmall(), Theme.accentAlpha(0xA0));
        int is = Math.min(16, size - 8);
        Icons.draw(g, icon, x + (size - is) / 2, y + (size - is) / 2, is, color);
    }

    /** Registers a text field; clicking it focuses the field and places the cursor. */
    private void textHit(String key, TextInput input, int x, int y, int w, int h, Runnable commit) {
        if (focused(key) && activeInput != input) Render2D.roundedOutline(g0, x, y, w, h, Theme.radiusSmall(), Theme.accentAlpha(0xA0));
        hits.add(new Hit(key, x, y, w, h, currentClip, true, b -> {
            focusInput(input, key, commit);
            input.clickAt(x, w, mouseX);
        }));
    }

    private GuiGraphics g0;

    private void focusInput(TextInput input, String key, Runnable commit) {
        if (activeInput != null && activeInput != input) blurInput();
        activeInput = input;
        activeInputKey = key;
        activeInputCommit = commit;
        focusKey = key;
    }

    private void blurInput() {
        if (activeInput == null) return;
        TextInput was = activeInput;
        Runnable commit = activeInputCommit;
        activeInput = null;
        activeInputKey = null;
        activeInputCommit = null;
        if (commit != null) commit.run();
        for (Map.Entry<Setting<?>, TextInput> e : stringInputs.entrySet()) {
            if (e.getValue() == was && e.getKey() instanceof StringSetting s) s.set(was.value());
        }
    }

    // ---- detail column -------------------------------------------------------------------------

    private void renderPanel(GuiGraphics g) {
        Module m = panelModule;
        int r = Theme.radiusLarge();
        int x = panelX;
        int y = panelY;
        int w = panelW;
        int h = panelH;
        if (!wide) clip(g, contentX, contentY, contentW, contentH);
        else Render2D.shadow(g, x, y, w, h, r, 0.5);
        Render2D.panel(g, x, y, w, h, r, Theme.surface(), Theme.line());
        hit("panel", x, y, w, h, false, b -> {});
        int innerX = x + PANEL_PAD;
        int innerW = w - 2 * PANEL_PAD;
        if (m == null) {
            Fonts.drawCentered(g, "Pick a module on the left", Fonts.Weight.MEDIUM, LABEL, x + w / 2, y + h / 2 - 8, Theme.subtle());
            return;
        }

        // Header: (back), icon, name, category and key, favourite star, switch.
        int hy = y + PANEL_PAD;
        int headH = 36;
        int hx = innerX;
        if (!wide) {
            iconButton(g, "panel-back", "chevron-left", hx - 4, hy + (headH - 28) / 2, 28, Theme.muted(), b -> closePanel());
            hx += 28;
        } else if (embedded) {
            iconButton(g, "panel-back", "close", hx - 4, hy + (headH - 28) / 2, 28, Theme.muted(), b -> onClose());
            hx += 28;
        }
        Icons.draw(g, m, hx, hy + (headH - 20) / 2, 20, m.isEnabled() ? Theme.accent() : Theme.muted());
        String notice = notice(m);
        int switchX = x + w - PANEL_PAD - SWITCH_W;
        renderSwitch(g, "panel-enabled", switchX, hy + (headH - SWITCH_H) / 2, m.isToggledOn(), notice == null, m::toggle);
        boolean fav = favorites.contains(m.key());
        int starX = switchX - 8 - 24;
        iconButton(g, "panel-star", fav ? "favorite-on" : "favorites", starX, hy + (headH - 24) / 2, 24, fav ? Theme.warning() : Theme.subtle(), b -> {
            if (!favorites.remove(m.key())) favorites.add(m.key());
        });
        int titleX = hx + 30;
        int titleW = starX - 6 - titleX;
        Fonts.drawClipped(g, m.name(), Fonts.Weight.SEMIBOLD, NAME, titleX, hy, titleW, Theme.text());
        String key = Keys.name(m.keybind());
        String sub = m.category().displayName() + (m.keybind() >= 0 ? " · " + key + " toggles" : "");
        Fonts.drawClipped(g, sub, Fonts.Weight.REGULAR, HINT, titleX, hy + Fonts.lineHeight(NAME), titleW, Theme.subtle());

        int cy = hy + headH + 10;
        for (String line : Fonts.wrap(m.about(), Fonts.Weight.REGULAR, DESC, innerW)) {
            Fonts.draw(g, line, Fonts.Weight.REGULAR, DESC, innerX, cy, Theme.muted());
            cy += Fonts.lineHeight(DESC);
        }
        if (notice != null) {
            cy += 6;
            Icons.draw(g, "warning", innerX, cy, 14, Theme.warning());
            for (String line : Fonts.wrap(notice, Fonts.Weight.REGULAR, DESC, innerW - 22)) {
                Fonts.draw(g, line, Fonts.Weight.REGULAR, DESC, innerX + 22, cy, Theme.warning());
                cy += Fonts.lineHeight(DESC);
            }
        }
        if (m instanceof PanelPreview preview) {
            int used = preview.renderPreview(g, innerX, cy + 10, innerW);
            if (used > 0) cy += used + 10;
        }
        cy += 12;
        g.fill(x + 1, cy, x + w - 1, cy + 1, Theme.line());
        cy += 1;

        // Scrollable settings.
        int rowsTop = cy;
        int rowsH = y + h - rowsTop - 1;
        clip(g, x + 1, rowsTop, w - 2, rowsH);
        int ry = rowsTop + 4 - panelScroll;
        ry = renderSettingRows(g, m, m.settings(), innerX, ry, innerW, rowsTop);

        ry += SECTION_ABOVE;
        Fonts.draw(g, "GENERAL", Fonts.Weight.MEDIUM, HINT, innerX, ry, Theme.subtle());
        ry += Fonts.lineHeight(HINT) + SECTION_BELOW;
        List<Module> bindable = new ArrayList<>();
        for (Module other : ShardClient.modules().all()) if (!other.hidden()) bindable.add(other);
        String conflict = conflictFor(m, bindable);
        ry += keybindRow(g, "kb:" + m.key(), "Keybind", conflict != null ? conflict : "Toggles the module in-game", Keys.name(m.keybind()), listeningModule == m, innerX, ry, innerW,
                () -> {
                    listeningModule = m;
                    listeningKey = null;
                }, () -> m.setKeybind(Keys.NONE));
        String server = ShardClient.modules().currentServer();
        if (server != null) {
            boolean off = ShardClient.modules().blacklist().isDisabled(server, m.key());
            ry += switchRow(g, "panel-server", "Disable on " + ServerBlacklist.normalize(server), "Kept off whenever you play here", innerX, ry, innerW, off, true,
                    () -> ShardClient.modules().setDisabledOnCurrentServer(m, !off));
        }
        ry += 10;
        button(g, "panel-reset", innerX, ry, Math.min(innerW, 132), BUTTON_H, "Reset module", false, true, b -> {
            for (Setting<?> s : m.settings()) s.reset();
            stringInputs.clear();
            numericInputs.clear();
            showToast(m.name() + " reset");
        });
        ry += BUTTON_H + PANEL_PAD;
        int contentHeight = ry + panelScroll - rowsTop;
        panelScroll = Math.max(0, Math.min(panelScroll, contentHeight - rowsH));
        if (contentHeight - panelScroll > rowsH + 2) {
            Render2D.gradientV(g, x + 1, rowsTop + rowsH - 16, w - 2, 16, 0x00000000, Colors.withAlpha(Theme.surface(), 0xF0));
        }
        unclip(g);
        if (!wide) {
            // Leave the outer (content area) scissor as well.
            g.disableScissor();
            currentClip = null;
        }
    }

    /**
     * Grouped setting rows with small caption headings; returns the y after the last row. While a
     * search result is being flashed, its row is highlighted and scrolled into view once.
     */
    private int renderSettingRows(GuiGraphics g, Module m, List<Setting<?>> settings, int x, int y, int w) {
        return renderSettingRows(g, m, settings, x, y, w, Integer.MIN_VALUE);
    }

    private int renderSettingRows(GuiGraphics g, Module m, List<Setting<?>> settings, int x, int y, int w, int viewTop) {
        String lastGroup = null;
        boolean first = true;
        for (Setting<?> s : settings) {
            if (!s.isVisible()) continue;
            if (!s.group().isEmpty() && !s.group().equals(lastGroup)) {
                y += first ? 8 : SECTION_ABOVE;
                Fonts.draw(g, s.group().toUpperCase(Locale.ROOT), Fonts.Weight.MEDIUM, HINT, x, y, Theme.subtle());
                y += Fonts.lineHeight(HINT) + SECTION_BELOW;
                lastGroup = s.group();
            } else if (s.group().isEmpty() && lastGroup != null) {
                y += 8;
                lastGroup = null;
            }
            if (s == flashSetting) {
                long age = System.currentTimeMillis() - flashSince;
                if (flashScrollPending && viewTop != Integer.MIN_VALUE) {
                    panelScroll = Math.max(0, panelScroll + (y - viewTop) - 24);
                    flashScrollPending = false;
                }
                if (age < FLASH_MS) {
                    // Height measured on the previous frame, so the highlight sits behind the row.
                    float a = 1f - age / (float) FLASH_MS;
                    int fh = Math.round(anims.getOrDefault("flash-h", (float) ROW_DESC_H));
                    Render2D.roundedRect(g, x - 8, y, w + 16, fh, Theme.radiusSmall(), Colors.fade(Theme.accentAlpha(0x2A), a));
                } else flashSetting = null;
            }
            int rowH = renderSettingRow(g, m, s, x, y, w);
            if (s == flashSetting) anims.put("flash-h", (float) rowH);
            y += rowH;
            first = false;
        }
        return y;
    }

    private interface ControlRenderer {
        void render(int controlX, int controlY, int controlW, int controlH);
    }

    /** Label on the left (with an optional description below it), control right-aligned in a 150 column. */
    private int row(GuiGraphics g, String label, String description, String details, int x, int y, int w, ControlRenderer control) {
        return row(g, label, description, details, x, y, w, CONTROL_W, control);
    }

    /** As above with a control column of {@code columnW}; switches use a narrow one so labels get the room. */
    private int row(GuiGraphics g, String label, String description, String details, int x, int y, int w, int columnW, ControlRenderer control) {
        boolean hasDesc = description != null && !description.isEmpty();
        int controlW = Math.min(w / 2, columnW);
        int labelW = w - controlW - 12;
        // Descriptions wrap (up to three lines) instead of being cut off; the row grows to fit.
        List<String> descLines = hasDesc ? Fonts.wrap(description, Fonts.Weight.REGULAR, DESC, labelW) : List.of();
        if (descLines.size() > 3) {
            descLines = new ArrayList<>(descLines.subList(0, 3));
            descLines.set(2, Fonts.clip(descLines.get(2) + " …", Fonts.Weight.REGULAR, DESC, labelW));
        }
        int h = hasDesc ? Math.max(ROW_DESC_H, 6 + Fonts.lineHeight(LABEL) + 2 + descLines.size() * Fonts.lineHeight(DESC) + 6) : ROW_H;
        int labelY = hasDesc ? y + 6 : y + (ROW_H - Fonts.lineHeight(LABEL)) / 2;
        Fonts.drawClipped(g, label, Fonts.Weight.MEDIUM, LABEL, x, labelY, labelW, Theme.text());
        for (int i = 0; i < descLines.size(); i++) {
            Fonts.draw(g, descLines.get(i), Fonts.Weight.REGULAR, DESC, x, labelY + Fonts.lineHeight(LABEL) + 2 + i * Fonts.lineHeight(DESC), Theme.muted());
        }
        if (details != null && !details.isEmpty() && popover == null && Render2D.hovered(mouseX, mouseY, x, y, labelW, h)
                && (currentClip == null || Render2D.hovered(mouseX, mouseY, currentClip[0], currentClip[1], currentClip[2] - currentClip[0], currentClip[3] - currentClip[1]))) {
            hoverDetails = details;
            hoverDetailsX = mouseX + 12;
            hoverDetailsY = mouseY + 16;
        }
        control.render(x + w - controlW, y, controlW, h);
        return h;
    }

    private int switchRow(GuiGraphics g, String key, String label, String description, int x, int y, int w, boolean on, boolean active, Runnable onToggle) {
        return row(g, label, description, null, x, y, w, SWITCH_W + 4, (cx, cy, cw, ch) ->
                renderSwitch(g, key, cx + cw - SWITCH_W, cy + (ch - SWITCH_H) / 2, on, active, onToggle));
    }

    private int keybindRow(GuiGraphics g, String key, String label, String description, String shown, boolean listening,
                           int x, int y, int w, Runnable onListen, Runnable onClear) {
        return row(g, label, description, "Right-click the key to unbind it.", x, y, w, (cx, cy, cw, ch) -> {
            String text = listening ? "Press a key…" : shown;
            int by = cy + (ch - BUTTON_H) / 2;
            button(g, key, cx, by, cw, BUTTON_H, "", false, true, b -> {
                if (b == GLFW.GLFW_MOUSE_BUTTON_RIGHT) onClear.run();
                else onListen.run();
            });
            Fonts.drawCentered(g, Fonts.clip(text, Fonts.Weight.MEDIUM, LABEL, cw - 16), Fonts.Weight.MEDIUM, LABEL, cx + cw / 2, by + (BUTTON_H - Fonts.lineHeight(LABEL)) / 2,
                    listening ? Theme.warning() : Theme.text());
        });
    }

    private int renderSettingRow(GuiGraphics g, Module m, Setting<?> s, int x, int y, int w) {
        String key = "set:" + m.key() + ":" + s.key();
        if (s instanceof BoolSetting b) {
            focusTargets.put(key, s);
            return row(g, s.name(), s.description(), s.details(), x, y, w, SWITCH_W + 4, (cx, cy, cw, ch) ->
                    renderSwitch(g, key, cx + cw - SWITCH_W, cy + (ch - SWITCH_H) / 2, b.get(), true, b::toggle));
        }
        if (s instanceof IntSetting || s instanceof DoubleSetting) {
            focusTargets.put(key, s);
            return row(g, s.name(), s.description(), s.details(), x, y, w, (cx, cy, cw, ch) -> {
                int fieldW = 56;
                int trackW = cw - fieldW - 12;
                int trackX = cx;
                int trackY = cy + (ch - 16) / 2;
                double frac = s instanceof IntSetting i ? i.fraction() : ((DoubleSetting) s).fraction();
                Hit probe = new Hit(key, trackX - 6, trackY - 6, trackW + 12, 28, currentClip, true, b -> {
                    if (b == GLFW.GLFW_MOUSE_BUTTON_RIGHT) s.reset();
                });
                hits.add(probe);
                boolean active = sliding == s;
                boolean hover = hoverable(probe);
                Render2D.slider(g, trackX, trackY, trackW, 16, frac, active || hover, focused(key));
                if (hover || active) sliderTracks.put(s, new int[]{trackX, trackW});
                // Numeric field: click to type a value.
                TextInput field = numericInputs.computeIfAbsent(s, unused -> new TextInput(12).rightAlign(true));
                String fieldKey = key + ":num";
                if (activeInput != field) field.sync(s.display());
                int fx = cx + cw - fieldW;
                int fy = cy + (ch - FIELD_H) / 2;
                field.render(g, fx, fy, fieldW, FIELD_H, activeInput == field);
                textHit(fieldKey, field, fx, fy, fieldW, FIELD_H, () -> {
                    String typed = field.value().trim();
                    if (!typed.isEmpty()) {
                        String number = typed.replaceAll("[^0-9.+-]", "");
                        if (!s.parse(number)) showToast("\"" + typed + "\" is not a valid " + s.name().toLowerCase(Locale.ROOT));
                    }
                    field.sync(s.display());
                });
            });
        }
        if (s instanceof EnumSetting<?> e && segmentedFits(e)) {
            focusTargets.put(key, s);
            return row(g, s.name(), s.description(), s.details(), x, y, w, (cx, cy, cw, ch) -> segmented(g, key, e, cx, cy + (ch - 24) / 2, cw));
        }
        if (s instanceof EnumSetting<?> e) {
            focusTargets.put(key, s);
            return row(g, s.name(), s.description(), s.details(), x, y, w, (cx, cy, cw, ch) -> {
                int by = cy + (ch - BUTTON_H) / 2;
                button(g, key, cx, by, cw, BUTTON_H, "", false, true, b -> {
                    if (b == GLFW.GLFW_MOUSE_BUTTON_RIGHT) e.cycle(true);
                    else popover = new DropdownPopover(e, cx, by + BUTTON_H, by, cw, designW, designH);
                });
                Fonts.drawClipped(g, e.display(), Fonts.Weight.MEDIUM, LABEL, cx + 12, by + (BUTTON_H - Fonts.lineHeight(LABEL)) / 2, cw - 44, Theme.text());
                Icons.draw(g, "chevron-down", cx + cw - 12 - 16, by + (BUTTON_H - 16) / 2, Theme.muted());
            });
        }
        if (s instanceof ColorSetting c) {
            focusTargets.put(key, s);
            return row(g, s.name(), s.description(), s.details(), x, y, w, (cx, cy, cw, ch) -> {
                int editW = 52;
                int editH = 28;
                int swW = 32;
                int swH = 24;
                int editX = cx + cw - editW;
                int editY = cy + (ch - editH) / 2;
                int swX = editX - 8 - swW;
                int swY = cy + (ch - swH) / 2;
                IntConsumer open = b -> {
                    if (b == GLFW.GLFW_MOUSE_BUTTON_RIGHT) c.reset();
                    else popover = new ColorPopover(c, editX + editW - 228, editY + editH, editY, designW, designH);
                };
                button(g, key, editX, editY, editW, editH, "Edit", false, true, open);
                hits.add(new Hit(key + ":swatch", swX - 2, swY - 2, swW + 4, swH + 4, currentClip, false, open));
                Render2D.checker(g, swX, swY, swW, swH, 6);
                Render2D.roundedRect(g, swX, swY, swW, swH, Theme.radiusSmall(), c.get());
                Render2D.roundedOutline(g, swX, swY, swW, swH, Theme.radiusSmall(), Theme.lineStrong());
                Fonts.drawRight(g, c.display(), Fonts.Weight.REGULAR, DESC, swX - 10, cy + (ch - Fonts.lineHeight(DESC)) / 2, Theme.muted());
            });
        }
        if (s instanceof KeybindSetting k) {
            return keybindRow(g, key, s.name(), s.description(), k.display(), listeningKey == k, x, y, w, () -> {
                listeningKey = k;
                listeningModule = null;
            }, () -> k.set(Keys.NONE));
        }
        if (s instanceof StringSetting str) {
            TextInput input = stringInputs.computeIfAbsent(s, unused -> new TextInput(str.maxLength()).onCommit(this::blurInput));
            if (activeInput != input) input.sync(str.get());
            return row(g, s.name(), s.description(), s.details(), x, y, w, (cx, cy, cw, ch) -> {
                int fy = cy + (ch - FIELD_H) / 2;
                input.render(g, cx, fy, cw, FIELD_H, activeInput == input);
                textHit(key, input, cx, fy, cw, FIELD_H, null);
            });
        }
        return 0;
    }

    /** Two or three options whose labels fit side by side in the control column. */
    private static boolean segmentedFits(EnumSetting<?> e) {
        Enum<?>[] values = e.values();
        if (values.length < 2 || values.length > 3) return false;
        int need = 4;
        for (Enum<?> v : values) need += Fonts.widthInt(EnumSetting.pretty(v), Fonts.Weight.MEDIUM, HINT) + 16;
        return need <= CONTROL_W;
    }

    /** Segmented control: one button per option, the current one raised. */
    private <E extends Enum<E>> void segmented(GuiGraphics g, String key, EnumSetting<?> setting, int x, int y, int w) {
        @SuppressWarnings("unchecked") EnumSetting<E> e = (EnumSetting<E>) setting;
        E[] values = e.values();
        int h = 24;
        Render2D.panel(g, x, y, w, h, Theme.radiusSmall(), Theme.surfaceRaised(), Theme.line());
        if (focused(key)) Render2D.roundedOutline(g, x - 2, y - 2, w + 4, h + 4, Theme.radiusSmall() + 2, Theme.accentAlpha(0xA0));
        int segW = (w - 4) / values.length;
        for (int i = 0; i < values.length; i++) {
            E v = values[i];
            int sx = x + 2 + i * segW;
            int sw = i == values.length - 1 ? x + w - 2 - sx : segW;
            boolean on = e.get() == v;
            Hit probe = new Hit(key + ":" + i, sx, y + 2, sw, h - 4, currentClip, false, b -> e.set(v));
            hits.add(probe);
            boolean hover = hoverable(probe);
            if (on) Render2D.roundedRect(g, sx, y + 2, sw, h - 4, 4, Theme.control());
            else if (hover) Render2D.roundedRect(g, sx, y + 2, sw, h - 4, 4, Theme.surfaceHover());
            String label = Fonts.clip(EnumSetting.pretty(v), Fonts.Weight.MEDIUM, HINT, sw - 6);
            Fonts.drawCentered(g, label, Fonts.Weight.MEDIUM, HINT, sx + sw / 2, y + (h - Fonts.lineHeight(HINT)) / 2, on ? Theme.text() : Theme.muted());
        }
        // The whole control is one focus stop; arrows change the value.
        hits.add(new Hit(key, x, y, w, h, currentClip, true, b -> {}));
    }

    /** Details appear only after the pointer rests on the same row for a moment, like a tooltip. */
    private boolean detailsReady() {
        long now = System.currentTimeMillis();
        if (!hoverDetails.equals(hoverDetailsShown)) {
            hoverDetailsShown = hoverDetails;
            hoverDetailsSince = now;
        }
        return now - hoverDetailsSince >= DETAILS_DELAY_MS;
    }

    private void renderDetails(GuiGraphics g) {
        int maxW = Math.min(300, designW - 32);
        List<String> lines = Fonts.wrap(hoverDetails, Fonts.Weight.REGULAR, DESC, maxW - 24);
        int w = 0;
        for (String l : lines) w = Math.max(w, Fonts.widthInt(l, Fonts.Weight.REGULAR, DESC));
        w += 24;
        int h = lines.size() * Fonts.lineHeight(DESC) + 16;
        int x = Math.max(8, Math.min(designW - w - 8, hoverDetailsX));
        int y = hoverDetailsY + 4;
        if (y + h > designH - 8) y = hoverDetailsY - h - 24;
        Render2D.shadow(g, x, y, w, h, Theme.radiusSmall(), 0.5);
        Render2D.panel(g, x, y, w, h, Theme.radiusSmall(), Theme.popover(), Theme.lineStrong());
        for (int i = 0; i < lines.size(); i++) {
            Fonts.draw(g, lines.get(i), Fonts.Weight.REGULAR, DESC, x + 12, y + 8 + i * Fonts.lineHeight(DESC), Theme.text());
        }
    }

    // ---- settings page -------------------------------------------------------------------------

    private interface SectionBody {
        int render(int x, int y, int w);
    }

    private int section(GuiGraphics g, String icon, String title, String sub, int x, int y, int w, SectionBody body) {
        int pad = PANEL_PAD;
        int innerX = x + pad;
        int innerW = w - 2 * pad;
        int headerH = pad + Fonts.lineHeight(SECTION) + 2 + Fonts.lineHeight(DESC) + 8;
        // The card is drawn behind the body using the height measured on the previous frame, so
        // the body renders (and registers its hits) exactly once.
        String cacheKey = "section-h:" + title;
        int cachedH = Math.round(anims.getOrDefault(cacheKey, (float) (headerH + 40)));
        Render2D.panel(g, x, y, w, cachedH, Theme.radius(), Theme.surfaceRaised(), Theme.line());
        Icons.draw(g, icon, innerX, y + pad + (Fonts.lineHeight(SECTION) - 16) / 2, 16, Theme.accent());
        Fonts.drawClipped(g, title, Fonts.Weight.SEMIBOLD, SECTION, innerX + 26, y + pad, innerW - 26, Theme.text());
        Fonts.drawClipped(g, sub, Fonts.Weight.REGULAR, DESC, innerX + 26, y + pad + Fonts.lineHeight(SECTION) + 2, innerW - 26, Theme.muted());
        int end = body.render(innerX, y + headerH, innerW);
        int h = end - y + pad;
        anims.put(cacheKey, (float) h);
        return h;
    }

    private void renderSettingsPage(GuiGraphics g) {
        renderPageHeader(g, "Settings", "Appearance, HUD, keybinds, profiles, server rules, export and reset", contentW);
        int top = contentY + headerHeight();
        int viewH = contentH - headerHeight();
        clip(g, contentX, top, contentW, viewH);
        int y = top - pageScroll;
        int sectionW = Math.min(contentW, SECTION_MAX_W);
        int x = contentX;

        y += section(g, "appearance", "Appearance", "Accent, interface size, font, blur and motion", x, y, sectionW,
                (ix, iy, iw) -> renderSettingRows(g, ShardClient.appearance(), ShardClient.appearance().settings(), ix, iy, iw)) + GRID_GAP;
        y += section(g, "hud", "HUD", "Scale and the style every HUD element inherits", x, y, sectionW,
                (ix, iy, iw) -> renderSettingRows(g, ShardClient.hudDefaults(), ShardClient.hudDefaults().settings(), ix, iy, iw)) + GRID_GAP;
        y += section(g, "keybind", "Keybinds", "Every module keybind in one list, with conflict warnings", x, y, sectionW, this::renderKeybindsBody) + GRID_GAP;
        y += section(g, "profile", "Profiles", "Save the whole config under a name and switch between them", x, y, sectionW, this::renderProfilesBody) + GRID_GAP;
        y += section(g, "server-rule", "Server rules", "Modules kept off on matching addresses; * matches subdomains", x, y, sectionW, this::renderServersBody) + GRID_GAP;
        y += section(g, "download", "Export and import", "Copy the config as JSON to the clipboard, or paste one back", x, y, sectionW, this::renderExportBody) + GRID_GAP;
        y += section(g, "reset", "Reset", "Back to a fresh install, keeping server rules", x, y, sectionW, this::renderResetBody) + GRID_GAP;
        y += section(g, "logo", "About", "Shard Client " + version(), x, y, sectionW, this::renderAboutBody) + GRID_GAP;

        int contentHeight = y + pageScroll - top;
        pageScroll = Math.max(0, Math.min(pageScroll, contentHeight - viewH));
        unclip(g);
    }

    private int renderKeybindsBody(int x, int y, int w) {
        GuiGraphics g = g0;
        List<Module> all = new ArrayList<>();
        for (Module m : ShardClient.modules().all()) if (!m.hidden()) all.add(m);
        for (Module m : all) {
            String conflict = conflictFor(m, all);
            String desc = conflict != null ? conflict : m.category().displayName();
            int rowY = y;
            y += keybindRow(g, "bind:" + m.key(), m.name(), desc, Keys.name(m.keybind()), listeningModule == m, x, y, w,
                    () -> {
                        listeningModule = m;
                        listeningKey = null;
                    }, () -> m.setKeybind(Keys.NONE));
            if (conflict != null) {
                // Repaint the description in the warning colour.
                int labelW = w - Math.min(w / 2, CONTROL_W) - 12;
                Render2D.fill(g, x, rowY + 6 + Fonts.lineHeight(LABEL) + 2, labelW, Fonts.lineHeight(DESC), Theme.surfaceRaised());
                Icons.draw(g, "warning", x, rowY + 6 + Fonts.lineHeight(LABEL), 14, Theme.warning());
                Fonts.drawClipped(g, conflict, Fonts.Weight.REGULAR, DESC, x + 20, rowY + 6 + Fonts.lineHeight(LABEL) + 2, labelW - 20, Theme.warning());
            }
        }
        return y;
    }

    /** "Also bound to <module>" or "Vanilla <action> uses this key", or null when the key is free. */
    private String conflictFor(Module m, List<Module> all) {
        int key = m.keybind();
        if (key < 0) return null;
        for (Module other : all) if (other != m && other.keybind() == key) return "Also bound to " + other.name();
        for (KeyMapping km : minecraft.options.keyMappings) {
            InputConstants.Key bound = KeyBindingHelper.getBoundKeyOf(km);
            if (bound.getType() == InputConstants.Type.KEYSYM && bound.getValue() == key) {
                String name = Component.translatable(km.getName()).getString();
                if (name.startsWith("key.shard")) continue;
                return "Vanilla \"" + name + "\" uses this key";
            }
        }
        return null;
    }

    private int renderProfilesBody(int x, int y, int w) {
        GuiGraphics g = g0;
        List<ConfigManager.ProfileInfo> profiles = ShardClient.config().profileInfos();
        if (profiles.isEmpty()) {
            Fonts.draw(g, "No saved profiles yet", Fonts.Weight.REGULAR, DESC, x, y + 8, Theme.subtle());
            y += Fonts.lineHeight(DESC) + 16;
        }
        int btnW = 72;
        for (ConfigManager.ProfileInfo p : profiles) {
            int rowH = p.description().isEmpty() ? ROW_H : ROW_DESC_H;
            int textW = w - (btnW * 2 + 8) - 12;
            int labelY = p.description().isEmpty() ? y + (ROW_H - Fonts.lineHeight(LABEL)) / 2 : y + 6;
            Fonts.drawClipped(g, p.name(), Fonts.Weight.MEDIUM, LABEL, x, labelY, textW, Theme.text());
            if (!p.description().isEmpty()) Fonts.drawClipped(g, p.description(), Fonts.Weight.REGULAR, DESC, x, labelY + Fonts.lineHeight(LABEL) + 2, textW, Theme.muted());
            int by = y + (rowH - BUTTON_H) / 2;
            button(g, "profile-load:" + p.name(), x + w - btnW * 2 - 8, by, btnW, BUTTON_H, "Load", false, true, b -> {
                if (ShardClient.config().loadProfile(p.name())) {
                    stringInputs.clear();
                    numericInputs.clear();
                    showToast("Loaded " + p.name());
                }
            });
            button(g, "profile-delete:" + p.name(), x + w - btnW, by, btnW, BUTTON_H, "Delete", false, true, true, b ->
                    popover = new ConfirmPopover("Delete profile", "Delete the saved profile \"" + p.name() + "\"? This cannot be undone.", "Delete", true, () -> {
                        if (ShardClient.config().deleteProfile(p.name())) showToast("Deleted " + p.name());
                    }, designW, designH));
            y += rowH;
        }
        y += 8;
        int saveW = 80;
        int nameW = Math.min(200, (w - saveW - 24) / 2);
        int descW = w - nameW - saveW - 24;
        profileName.render(g, x, y, nameW, FIELD_H, activeInput == profileName);
        textHit(KEY_PROFILE_NAME, profileName, x, y, nameW, FIELD_H, null);
        profileDesc.render(g, x + nameW + 12, y, descW, FIELD_H, activeInput == profileDesc);
        textHit(KEY_PROFILE_DESC, profileDesc, x + nameW + 12, y, descW, FIELD_H, null);
        button(g, "profile-save", x + w - saveW, y, saveW, BUTTON_H, "Save", true, true, b -> saveProfileFromInput());
        return y + FIELD_H;
    }

    private int renderServersBody(int x, int y, int w) {
        GuiGraphics g = g0;
        ServerBlacklist list = ShardClient.modules().blacklist();
        String current = ShardClient.modules().currentServer();
        String hint = current == null ? "Not connected. Add a pattern below, or join a server and use \"Disable on <server>\" in a module's panel."
                : "Connected to " + ServerBlacklist.normalize(current) + ". Rules apply automatically every time you join.";
        for (String line : Fonts.wrap(hint, Fonts.Weight.REGULAR, DESC, w)) {
            Fonts.draw(g, line, Fonts.Weight.REGULAR, DESC, x, y, Theme.muted());
            y += Fonts.lineHeight(DESC);
        }
        y += 12;
        if (list.entries().isEmpty()) {
            Fonts.draw(g, "No rules yet", Fonts.Weight.REGULAR, DESC, x, y + 4, Theme.subtle());
            y += Fonts.lineHeight(DESC) + 12;
        }
        List<Module> all = new ArrayList<>();
        for (Module m : ShardClient.modules().all()) if (!m.hidden()) all.add(m);
        for (ServerBlacklist.Entry e : new ArrayList<>(list.entries())) {
            String ek = "rule:" + e.pattern();
            boolean expanded = expandedRules.contains(e);
            int removeW = 84;
            int fieldW = w - removeW - 12 - 36;
            TextInput field = patternInputs.computeIfAbsent(e, unused -> new TextInput(64));
            if (activeInput != field) field.sync(e.pattern());
            // Expand chevron.
            button(g, ek + ":expand", x, y, 28, FIELD_H, "", false, true, b -> {
                if (!expandedRules.remove(e)) expandedRules.add(e);
            });
            Icons.draw(g, expanded ? "chevron-down" : "chevron-right", x + 6, y + (FIELD_H - 16) / 2, Theme.muted());
            field.render(g, x + 36, y, fieldW, FIELD_H, activeInput == field);
            textHit(ek + ":pattern", field, x + 36, y, fieldW, FIELD_H, () -> {
                String typed = field.value().trim();
                if (typed.isEmpty() || ServerBlacklist.normalize(typed).equals(e.pattern())) return;
                if (list.rename(e, typed)) {
                    patternInputs.remove(e);
                    expandedRules.remove(e);
                    ShardClient.modules().refreshSuppression();
                    ShardClient.config().markDirty();
                    showToast("Rule renamed to " + ServerBlacklist.normalize(typed));
                } else {
                    showToast("\"" + typed + "\" is not a valid pattern or already exists");
                    field.sync(e.pattern());
                }
            });
            button(g, ek + ":remove", x + w - removeW, y, removeW, BUTTON_H, "Remove", false, true, true, b -> {
                list.remove(e);
                patternInputs.remove(e);
                expandedRules.remove(e);
                ShardClient.modules().refreshSuppression();
                ShardClient.config().markDirty();
            });
            y += FIELD_H + 4;
            int n = e.modules().size();
            Fonts.draw(g, n == 0 ? "No modules off yet, expand to pick some" : n + (n == 1 ? " module off" : " modules off"), Fonts.Weight.REGULAR, HINT, x + 36, y, Theme.subtle());
            y += Fonts.lineHeight(HINT) + 8;
            if (expanded) {
                int cols = w >= 480 ? 2 : 1;
                int colW = (w - 36 - (cols - 1) * 12) / cols;
                int startY = y;
                for (int i = 0; i < all.size(); i++) {
                    Module m = all.get(i);
                    int col = i % cols;
                    int rowI = i / cols;
                    int rx = x + 36 + col * (colW + 12);
                    int ryy = startY + rowI * ROW_H;
                    boolean off = e.modules().contains(m.key());
                    Fonts.drawClipped(g, m.name(), Fonts.Weight.MEDIUM, DESC, rx, ryy + (ROW_H - Fonts.lineHeight(DESC)) / 2, colW - SWITCH_W - 12, off ? Theme.text() : Theme.muted());
                    renderSwitch(g, ek + ":m:" + m.key(), rx + colW - SWITCH_W, ryy + (ROW_H - SWITCH_H) / 2, off, true, () -> {
                        e.setDisabled(m.key(), !off);
                        ShardClient.modules().refreshSuppression();
                        ShardClient.config().markDirty();
                    });
                }
                y = startY + ((all.size() + cols - 1) / cols) * ROW_H + 8;
            }
            y += 8;
        }
        // Add a pattern.
        int addW = 72;
        int curW = current != null ? 160 : 0;
        int fieldW = w - addW - 12 - (curW > 0 ? curW + 12 : 0);
        serverPattern.render(g, x, y, fieldW, FIELD_H, activeInput == serverPattern);
        textHit(KEY_SERVER_PATTERN, serverPattern, x, y, fieldW, FIELD_H, null);
        button(g, "server-add-pattern", x + fieldW + 12, y, addW, BUTTON_H, "Add", true, true, b -> addPatternFromInput());
        if (current != null) {
            button(g, "server-add", x + w - curW, y, curW, BUTTON_H, "Add current server", false, true, b -> {
                list.getOrCreate(current);
                ShardClient.config().markDirty();
                showToast("Rule added for " + ServerBlacklist.normalize(current));
            });
        }
        return y + FIELD_H;
    }

    private void addPatternFromInput() {
        String typed = serverPattern.value().trim();
        if (typed.isEmpty()) return;
        ServerBlacklist list = ShardClient.modules().blacklist();
        ServerBlacklist.Entry e = list.add(typed);
        if (e == null) {
            showToast("\"" + typed + "\" is not a valid pattern or already exists");
            return;
        }
        expandedRules.add(e);
        serverPattern.setValue("");
        ShardClient.config().markDirty();
        showToast("Rule added for " + e.pattern());
    }

    private int renderExportBody(int x, int y, int w) {
        GuiGraphics g = g0;
        int bw = Math.min(200, (w - 12) / 2);
        button(g, "export-copy", x, y, bw, BUTTON_H, "Copy config to clipboard", false, true, b -> {
            minecraft.keyboardHandler.setClipboard(ShardClient.config().exportJson());
            showToast("Config copied to the clipboard");
        });
        button(g, "export-paste", x + bw + 12, y, bw, BUTTON_H, "Paste config from clipboard", false, true, b -> {
            String json = minecraft.keyboardHandler.getClipboard();
            if (json == null || json.isBlank()) {
                showToast("The clipboard is empty");
                return;
            }
            popover = new ConfirmPopover("Import config", "Replace every module setting, keybind and HUD position with the config on the clipboard?",
                    "Import", false, () -> {
                        String error = ShardClient.config().importJson(json);
                        if (error != null) showToast(error);
                        else {
                            stringInputs.clear();
                            numericInputs.clear();
                            showToast("Config imported");
                        }
                    }, designW, designH);
        });
        y += BUTTON_H + 8;
        Fonts.drawClipped(g, "Profiles and the launcher's shared config use the same JSON.", Fonts.Weight.REGULAR, HINT, x, y, w, Theme.subtle());
        return y + Fonts.lineHeight(HINT);
    }

    private int renderResetBody(int x, int y, int w) {
        GuiGraphics g = g0;
        button(g, "reset-all", x, y, Math.min(w, 200), BUTTON_H, "Reset all settings", false, true, true, b ->
                popover = new ConfirmPopover("Reset everything", "Every module goes back to its defaults, keybinds are cleared and HUD positions reset. Server rules and profiles are kept.",
                        "Reset all", true, () -> {
                            ShardClient.config().resetAll();
                            stringInputs.clear();
                            numericInputs.clear();
                            showToast("Everything reset");
                        }, designW, designH));
        return y + BUTTON_H;
    }

    private int renderAboutBody(int x, int y, int w) {
        GuiGraphics g = g0;
        var info = ShardClient.launcherInfo();
        String launcher = info.present() ? "Launched by Shard Launcher " + info.launcherVersion() : "Not launched by Shard Launcher";
        Fonts.drawClipped(g, launcher, Fonts.Weight.REGULAR, DESC, x, y, w, Theme.muted());
        y += Fonts.lineHeight(DESC);
        Fonts.drawClipped(g, "Config schema " + ConfigManager.VERSION + " · Right Shift opens this menu · \".help\" in chat lists commands", Fonts.Weight.REGULAR, HINT, x, y, w, Theme.subtle());
        y += Fonts.lineHeight(HINT);
        Fonts.drawClipped(g, "Inter font © The Inter Project Authors, SIL Open Font License 1.1", Fonts.Weight.REGULAR, HINT, x, y, w, Theme.subtle());
        y += Fonts.lineHeight(HINT);
        Fonts.drawClipped(g, "Icons from Lucide (lucide.dev), ISC licence", Fonts.Weight.REGULAR, HINT, x, y, w, Theme.subtle());
        return y + Fonts.lineHeight(HINT);
    }

    private void saveProfileFromInput() {
        String name = profileName.value().trim();
        if (name.isEmpty()) {
            ShardClient.config().save();
            showToast("Config saved");
            return;
        }
        if (ShardClient.config().saveProfile(name, profileDesc.value())) {
            showToast("Saved profile " + name);
            profileName.setValue("");
            profileDesc.setValue("");
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
        float alpha = Math.min(1f, left / 300f);
        int w = Fonts.widthInt(toast, Fonts.Weight.MEDIUM, LABEL) + 32;
        int h = Fonts.lineHeight(LABEL) + 16;
        int x = (designW - w) / 2;
        int y = designH - PAD - h;
        Render2D.roundedRect(g, x, y, w, h, Theme.radiusSmall(), Colors.fade(Theme.popover(), alpha));
        Render2D.roundedOutline(g, x, y, w, h, Theme.radiusSmall(), Colors.fade(Theme.lineStrong(), alpha));
        Fonts.drawCentered(g, toast, Fonts.Weight.MEDIUM, LABEL, x + w / 2, y + 8, Colors.fade(Theme.text(), alpha));
    }

    // ---- mouse (design units) ------------------------------------------------------------------

    @Override
    protected boolean designClicked(double mx, double my, int button, boolean doubleClick) {
        if (popover != null) {
            if (popover.contains(mx, my)) {
                popover.mouseClicked(mx, my, button);
                if (popover != null && popover.wantsClose()) popover = null;
                return true;
            }
            if (!popover.modal()) popover = null;
            return true;
        }
        if (listeningKey != null || listeningModule != null) {
            listeningKey = null;
            listeningModule = null;
            return true;
        }

        Hit target = hitAt(mx, my);
        if (activeInput != null && (target == null || !target.key.equals(activeInputKey))) blurInput();
        if (target == null) return true;

        keyboardFocus = false;
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
        target.onClick.accept(button);
        return true;
    }

    /** Shift held while dragging moves the value at a quarter of the speed. */
    private double slideAnchorMouse = Double.NaN;
    private double slideAnchorFrac;

    private void applySlider(double mx) {
        boolean fine = minecraft.hasShiftDown();
        if (!fine) slideAnchorMouse = Double.NaN;
        else if (Double.isNaN(slideAnchorMouse)) {
            slideAnchorMouse = mx;
            slideAnchorFrac = sliding instanceof IntSetting i ? i.fraction() : ((DoubleSetting) sliding).fraction();
        }
        double frac = slideW <= 0 ? 0 : fine ? slideAnchorFrac + (mx - slideAnchorMouse) / (4.0 * slideW) : (mx - slideX) / (double) slideW;
        frac = Math.max(0, Math.min(1, frac));
        if (sliding instanceof IntSetting i) i.setFraction(frac);
        else if (sliding instanceof DoubleSetting d) d.setFraction(frac);
    }

    @Override
    protected boolean designDragged(double mx, double my, int button, double dx, double dy) {
        if (popover != null) {
            popover.mouseDragged(mx, my);
            return true;
        }
        if (sliding != null) {
            applySlider(mx);
            return true;
        }
        return false;
    }

    @Override
    protected boolean designReleased(double mx, double my, int button) {
        if (popover != null) {
            popover.mouseReleased();
            return true;
        }
        if (sliding != null) {
            sliding = null;
            slideAnchorMouse = Double.NaN;
            return true;
        }
        return false;
    }

    @Override
    protected boolean designScrolled(double mx, double my, double sx, double sy) {
        if (popover != null) {
            if (popover.contains(mx, my)) return popover.mouseScrolled(mx, my, sy);
            return true;
        }
        int step = (int) Math.round(sy * 48);
        if (panelCovers(mx, my)) {
            panelScroll = Math.max(0, panelScroll - step);
            return true;
        }
        if (!narrow && Render2D.hovered(mx, my, PAD, PAD, RAIL_W, designH - 2 * PAD)) {
            sidebarScroll = Math.max(0, sidebarScroll - step);
            return true;
        }
        if (wide && !settingsPage && Render2D.hovered(mx, my, panelX, panelY, panelW, panelH)) {
            panelScroll = Math.max(0, panelScroll - step);
            return true;
        }
        if (Render2D.hovered(mx, my, contentX, contentY, contentW, contentH)) {
            if (settingsPage) pageScroll = Math.max(0, pageScroll - step);
            else gridScroll = Math.max(0, gridScroll - step);
            return true;
        }
        return true;
    }

    // ---- keyboard ------------------------------------------------------------------------------

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key == GLFW.GLFW_KEY_TAB || key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_RIGHT
                || key == GLFW.GLFW_KEY_UP || key == GLFW.GLFW_KEY_DOWN) keyboardFocus = true;
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
        if (listeningKey != null || listeningModule != null) {
            int bound = key == GLFW.GLFW_KEY_ESCAPE ? Integer.MIN_VALUE
                    : key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE ? Keys.NONE : key;
            if (bound != Integer.MIN_VALUE) {
                if (listeningKey != null) listeningKey.set(bound);
                else listeningModule.setKeybind(bound);
            }
            listeningKey = null;
            listeningModule = null;
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
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                if (activeInput == profileName || activeInput == profileDesc) {
                    saveProfileFromInput();
                    blurInput();
                } else if (activeInput == serverPattern) {
                    addPatternFromInput();
                    blurInput();
                } else blurInput();
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
                if (!search.isEmpty()) clearSearch();
                else if (!wide && panelTarget > 0) closePanel();
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
                focusInput(search, KEY_SEARCH, null);
                search.cursorToEnd();
                return true;
            }
            case GLFW.GLFW_KEY_F -> {
                if (ctrl) {
                    focusInput(search, KEY_SEARCH, null);
                    search.cursorToEnd();
                    return true;
                }
            }
            case GLFW.GLFW_KEY_SPACE -> {
                if (focusKey != null && focusKey.startsWith("row:")) {
                    Module m = moduleFor(focusKey);
                    if (m != null) m.toggle();
                    return true;
                }
                if (activate()) return true;
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                if (focusKey != null && focusKey.startsWith("row:")) {
                    Module m = moduleFor(focusKey);
                    if (m != null) {
                        selectModule(m);
                        // Move into the detail column: first control of the module.
                        for (Hit h : hits) {
                            if (h.focusable && h.key.startsWith("set:" + m.key() + ":")) {
                                focusKey = h.key;
                                break;
                            }
                        }
                        keyboardFocus = true;
                    }
                    return true;
                }
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
        if (focusKey.startsWith("row:") && !cardOrder.isEmpty()) {
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
                // On wide windows the detail column follows the keyboard, like a mail client.
                if (wide) {
                    Module m = moduleFor(focusKey);
                    if (m != null) selectModule(m);
                }
                scrollFocusIntoView();
            }
            return true;
        }
        if (focusKey.startsWith("cat:")) {
            List<String> keys = new ArrayList<>();
            for (Hit h : hits) if (h.key.startsWith("cat:")) keys.add(h.key);
            int idx = keys.indexOf(focusKey);
            int next = idx + (key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_UP ? -1 : 1);
            if (next >= 0 && next < keys.size()) {
                hitFor(keys.get(next)).onClick.accept(GLFW.GLFW_MOUSE_BUTTON_LEFT);
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
            int delta = h.clip[1] - h.y + 8;
            if (h.key.startsWith("row:") || h.key.startsWith("res:")) gridScroll = Math.max(0, gridScroll - delta);
            else if (settingsPage) pageScroll = Math.max(0, pageScroll - delta);
            else panelScroll = Math.max(0, panelScroll - delta);
        } else if (h.y + h.h > h.clip[3]) {
            int delta = h.y + h.h - h.clip[3] + 8;
            if (h.key.startsWith("row:") || h.key.startsWith("res:")) gridScroll += delta;
            else if (settingsPage) pageScroll += delta;
            else panelScroll += delta;
        }
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        String ch = event.codepointAsString();
        if (popover != null) {
            popover.charTyped(ch);
            return true;
        }
        if (listeningKey != null || listeningModule != null) return true;
        if (activeInput != null) {
            if (activeInput.charTyped(ch) && activeInput == search) gridScroll = 0;
            return true;
        }
        // Type anywhere to search.
        if (!embedded && !ch.isEmpty() && Character.isLetterOrDigit(ch.codePointAt(0))) {
            focusInput(search, KEY_SEARCH, null);
            search.cursorToEnd();
            if (search.charTyped(ch)) gridScroll = 0;
            return true;
        }
        return super.charTyped(event);
    }

    private Module moduleFor(String key) {
        String k = key.substring(key.indexOf(':') + 1);
        for (Module m : ShardClient.modules().all()) if (m.key().equals(k)) return m;
        return null;
    }
}
