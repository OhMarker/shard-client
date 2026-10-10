package gg.shard.client.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.mojang.blaze3d.platform.InputConstants;
import gg.shard.client.ShardClient;
import gg.shard.client.config.ConfigManager;
import gg.shard.client.cosmetics.CosmeticsTab;
import gg.shard.client.cosmetics.PlayerCosmetics;
import gg.shard.client.cosmetics.PreviewFit;
import gg.shard.client.cosmetics.ShardApi;
import gg.shard.client.modules.visual.CosmeticsModule;
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
import net.minecraft.resources.Identifier;
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
 * Shard's mod menu: one compact centred panel with a top bar (wordmark, Mods / Settings /
 * Cosmetics / Friends tabs, search), a row of category tabs, a six-column grid of mod tiles and
 * a footer. Clicking a tile toggles it; the gear or a right-click swaps the grid for that mod's
 * settings inside the same panel. Search matches module names, descriptions and individual
 * settings. The menu is drawn at one design unit per physical pixel on a 1080p screen (scaled
 * with the window height and Settings → Appearance → Interface size), so it keeps its
 * proportions at every GUI scale. The HUD editor embeds the settings column on its own.
 *
 * <p>Input is routed through "hits": every control registers its rectangle while it renders,
 * so clicks, hover and keyboard focus all share one source of truth.
 */
public final class ClickGuiScreen extends DesignScreen {
    // ---- spacing grid (design units) ---------------------------------------------------------
    static final int PAD = 16;
    static final long DETAILS_DELAY_MS = 600;
    static final int GRID_GAP = 12;
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
    static final int SECTION_MAX_W = 640;
    /** The Cosmetics tab's item grid is wider than a settings section. */
    static final int COSMETICS_MAX_W = 760;
    // ---- menu frame (design units = physical pixels at 1080p) -------------------------------
    static final int MENU_W = 1280;
    static final int MENU_H = 688;
    /** The smallest the menu gets before the whole page scales down (keeps 720p at 1:1). */
    static final int MENU_MIN_W = 1040;
    static final int MENU_MIN_H = 600;
    static final int SIDE_W = 240;
    static final int SIDE_PAD = 18;
    static final int NAV_H = 30;
    static final int MAIN_PAD = 24;
    static final int HEAD_H = 48;
    static final int FOOT_H = 30;
    static final int COLS = 2;
    static final int CARD_H = 114;
    static final int CARD_GAP = 10;
    static final int CARD_PAD = 14;
    static final int CARD_FOOT_H = 30;
    static final int BOX_GAP = 10;
    static final int GRID_PAD = 14;
    static final int TITLE = 20;
    // ---- typography ---------------------------------------------------------------------------
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
    private enum Filter { ALL, CATEGORY, FAVORITES, ENABLED }

    /** The top bar's tabs. */
    public enum Tab { MODS, SETTINGS, PROFILES, COSMETICS, FRIENDS }

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
    private Filter filter = Filter.ALL;
    private Tab tab = Tab.MODS;
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
    /** What is drawn: each scroll eases toward its target every frame, so the wheel glides. */
    private float gridShown;
    private float panelShown;
    private float pageShown;
    private int menuX;
    private int menuY;
    private int menuW;
    private int menuH;
    /** The main column right of the sidebar, and its inner (padded) area. */
    private int bodyX;
    private int bodyW;
    private int mainX;
    private int mainW;
    private int headY;
    private int footY;
    /** All / Enabled / Off at the top right of a mod list. */
    private int showOnly;
    /** Thin lines between setting rows (the menu's settings view). */
    private boolean rowDividers;
    private PanelPreview previewDragging;
    private int previewButton;
    private Setting<?> flashSetting;
    private long flashSince;
    private boolean flashScrollPending;

    private final List<Hit> hits = new ArrayList<>();
    private final Map<String, Setting<?>> focusTargets = new HashMap<>();
    private final List<String> cardOrder = new ArrayList<>();
    private int gridColumns = 1;
    private int cardWidth;
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
    private boolean wide;
    private int contentX;
    private int contentY;
    private int contentW;
    private int contentH;
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
            if (gui.has("category") && ShardClient.appearance().rememberTab.get()) {
                String saved = gui.get("category").getAsString();
                // The menu always opens on Mods; only the Mods category is remembered. A saved
                // Settings/Cosmetics/Profiles/Friends tab keeps the default Mods > All.
                switch (saved) {
                    case "settings", "cosmetics", "profiles", "friends" -> { }
                    case "all" -> filter = Filter.ALL;
                    case "favorites" -> filter = Filter.FAVORITES;
                    case "enabled" -> filter = Filter.ENABLED;
                    default -> {
                        try {
                            category = ModuleCategory.valueOf(saved);
                            filter = Filter.CATEGORY;
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
        String saved = switch (filter) {
            case ALL -> "all";
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

    /**
     * Physical pixels per design unit for the menu: 1.25 on a 1080p screen, more on taller windows,
     * times Interface size, but never so large that the panel no longer fits. Rounded down to a
     * quarter so it is always one of the font raster densities and text is drawn 1:1. The HUD
     * editor's embedded column keeps the usual density.
     */
    @Override
    protected double pageScaleFor(int guiScale, double interfaceSize) {
        if (embedded) return super.pageScaleFor(guiScale, interfaceSize);
        double perUnit = Scale.menuPixelsPerUnit(minecraft.getWindow().getWidth(), minecraft.getWindow().getHeight(), interfaceSize,
                MENU_MIN_W + 2 * PAD, MENU_MIN_H + 2 * PAD);
        return perUnit / Math.max(1, guiScale);
    }

    /** Switches the top bar's tab (a click on it, and the smoke test). */
    public void selectTab(Tab t) {
        popover = null;
        if (t != Tab.MODS) clearSearch();
        closePanel();
        tab = t;
        pageScroll = 0;
        pageShown = 0;
        focusKey = "tab:" + t.name().toLowerCase(Locale.ROOT);
    }

    private boolean modSettingsOpen() {
        return tab == Tab.MODS && panelModule != null && panelTarget > 0;
    }

    /** Opens a module's settings (used by the smoke test, the HUD editor and setting search results). */
    public void openModule(Module module) {
        panelModule = module;
        panelTarget = 1f;
        if (wide) panelAnim = 1f;
        panelScroll = 0;
        panelShown = 0;
        tab = Tab.MODS;
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
        searchEdited();
    }

    /** Shows one category, as a click on it in the rail would (smoke test). */
    public void showCategory(ModuleCategory c) {
        selectCategory(c);
    }

    /**
     * Clicks the middle of every switch, option button and segment drawn last frame and reports
     * the ones whose value did not change; values are put back afterwards (smoke test).
     */
    public List<String> clickAudit() {
        List<String> dead = new ArrayList<>();
        for (Hit h : new ArrayList<>(hits)) {
            Setting<?> s = focusTargets.get(h.key);
            boolean segment = false;
            if (s == null && h.key.startsWith("set:") && h.key.matches(".*:[0-9]+$")) {
                s = focusTargets.get(h.key.substring(0, h.key.lastIndexOf(':')));
                segment = true;
            }
            if (!(s instanceof BoolSetting) && !(s instanceof EnumSetting<?>)) continue;
            if (s instanceof EnumSetting<?> && !segment) continue; // dropdowns open a popover instead
            com.google.gson.JsonElement before = s.toJson();
            double cx = h.x + h.w / 2.0;
            double cy = h.y + h.h / 2.0;
            if (!h.contains(cx, cy)) continue; // scrolled out of view
            mouseX = (int) cx;
            mouseY = (int) cy;
            designClicked(cx, cy, GLFW.GLFW_MOUSE_BUTTON_LEFT, false);
            popover = null;
            boolean changed = !before.equals(s.toJson());
            // A segment that is already selected legitimately does not change.
            if (!changed && segment && s instanceof EnumSetting<?> e && h.key.endsWith(":" + e.get().ordinal())) changed = true;
            if (!changed) dead.add(h.key);
            s.fromJson(before);
        }
        return dead;
    }

    /** Shows every mod (the All tab). */
    public void showAll() {
        selectFilter(Filter.ALL);
    }

    /** Centre of the n-th card (two per row), in design units (smoke test). */
    public int[] tileCentre(int index) {
        int col = index % COLS;
        int row = index / COLS;
        return new int[]{contentX + col * (cardWidth + CARD_GAP) + cardWidth / 2, contentY + row * (CARD_H + CARD_GAP) + CARD_H / 2};
    }

    /** Centre (design units) of the last frame's first control whose key matches {@code regex}, or null (smoke test). */
    public int[] hitCentre(String regex) {
        for (Hit h : new ArrayList<>(hits)) {
            if (h.key.matches(regex) && h.contains(h.x + h.w / 2.0, h.y + h.h / 2.0)) return new int[]{h.x + h.w / 2, h.y + h.h / 2};
        }
        return null;
    }

    /** Clicks the control with this key as the last frame drew it; false when it is not on screen (smoke test). */
    public boolean press(String key) {
        Hit h = hitFor(key);
        if (h == null) return false;
        h.onClick.accept(GLFW.GLFW_MOUSE_BUTTON_LEFT);
        return true;
    }

    /** The tab showing now (smoke test). */
    public Tab currentTab() {
        return tab;
    }

    /** Physical pixels per design unit this frame (smoke test). */
    public double pixelsPerUnitNow() {
        return Render2D.pixelsPerUnit();
    }

    /** Below the wide layout, slides the detail column away so the list shows (smoke test). */
    public void showList() {
        if (!wide) closePanel();
    }

    /** Opens the n-th setting found by the current search (smoke test). */
    public void openSearchResult(int index) {
        List<SettingHit> results = settingResults();
        if (index < results.size()) jumpTo(results.get(index).module(), results.get(index).setting());
    }

    public void setGridView(boolean grid) {
        gridView = grid;
        gridScroll = 0;
        gridShown = 0;
    }

    public Module openModule() {
        return panelTarget > 0 ? panelModule : null;
    }

    public LayoutInfo layoutInfo() {
        return new LayoutInfo(designW, designH, false, COLS, cardWidth, CARD_H, SIDE_W, panelW, modSettingsOpen(), pageScale);
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
        if (candidate == null || popover != null) return false;
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
            wide = true;
            tab = Tab.MODS;
            panelTarget = panelAnim = panelModule == null ? 0f : 1f;
            layoutPanel();
            contentX = panelX;
            contentY = panelY;
            contentW = panelW;
            contentH = panelH;
            if (panelModule != null) renderPanel(g);
        } else {
            wide = true;
            panelAnim = panelTarget;
            layoutMenu();
            renderMenu(g);
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

    /**
     * The centred panel: a sidebar on the left, and on the right a heading, the scrolling content
     * and (on the mod pages) a footer row. Everything is laid out in design units.
     */
    private void layoutMenu() {
        menuW = Math.min(MENU_W, designW - 2 * PAD);
        menuH = Math.min(MENU_H, designH - 2 * PAD);
        menuX = (designW - menuW) / 2;
        menuY = (designH - menuH) / 2;
        bodyX = menuX + SIDE_W;
        bodyW = menuW - SIDE_W;
        mainX = bodyX + MAIN_PAD;
        mainW = bodyW - 2 * MAIN_PAD;
        headY = menuY + MAIN_PAD;
        boolean modsFooter = tab == Tab.MODS;
        footY = menuY + menuH - MAIN_PAD - FOOT_H;
        contentX = mainX;
        contentW = mainW;
        contentY = headY + HEAD_H;
        int bottom = modsFooter ? footY - 14 : menuY + menuH - MAIN_PAD;
        contentH = bottom - contentY;
        gridColumns = COLS;
        cardWidth = (contentW - (COLS - 1) * CARD_GAP) / COLS;
        panelX = mainX;
        panelY = headY;
        panelW = mainW;
        panelH = bottom - headY;
    }

    /** One frame of scroll easing toward {@code target} (instant with Reduce motion). */
    private float ease(float shown, int target) {
        if (Theme.reduceMotion()) return target;
        float next = shown + (target - shown) * (1f - (float) Math.exp(-dt * 18f));
        return Math.abs(target - next) < 0.5f ? target : next;
    }

    private boolean panelCovers(double mx, double my) {
        if (wide || (tab == Tab.SETTINGS)) return false;
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

    // ---- menu chrome ---------------------------------------------------------------------------

    private void renderMenu(GuiGraphics g) {
        Render2D.panel(g, menuX, menuY, menuW, menuH, Theme.radiusLarge(), Theme.surface(), Theme.line());
        hit("menu", menuX, menuY, menuW, menuH, false, b -> {});
        renderSidebar(g);
        switch (tab) {
            case MODS -> {
                if (modSettingsOpen()) renderModulePage(g);
                else {
                    renderModsHeader(g);
                    renderTiles(g);
                    renderModsFooter(g);
                }
            }
            case SETTINGS -> {
                renderPageHeader(g, "Settings", "How the Shard menu and client behave.");
                renderSettingsPage(g);
            }
            case PROFILES -> {
                renderPageHeader(g, "Profiles", "Save mods, settings and HUD layout under a name, then switch in one click.");
                renderProfilesPage(g);
            }
            case COSMETICS -> {
                renderPageHeader(g, "Cosmetics", "What you wear, and what you see on other players.");
                renderCosmeticsPage(g);
            }
            case FRIENDS -> {
                renderPageHeader(g, "Friends", "Who is online, and requests waiting for you.");
                renderFriendsPage(g);
            }
        }
    }

    private static String tabName(Tab t) {
        return switch (t) {
            case MODS -> "Mods";
            case SETTINGS -> "Settings";
            case PROFILES -> "Profiles";
            case COSMETICS -> "Cosmetics";
            case FRIENDS -> "Friends";
        };
    }

    /**
     * Wordmark and version, search, All / Favorites / Enabled, the categories with their mod
     * counts, and at the bottom the other pages and who is signed in.
     */
    private void renderSidebar(GuiGraphics g) {
        g.fill(bodyX - 1, menuY + 1, bodyX, menuY + menuH - 1, Theme.line());
        int x = menuX + SIDE_PAD;
        int w = SIDE_W - 2 * SIDE_PAD;
        int y = menuY + 22;
        Icons.draw(g, "logo", x + 6, y, 20, Theme.accent());
        Fonts.draw(g, "Shard", Fonts.Weight.SEMIBOLD, 15, x + 34, y + (20 - Fonts.lineHeight(15)) / 2, Theme.text());
        Fonts.drawRight(g, version(), Fonts.Weight.REGULAR, HINT, x + w - 6, y + (20 - Fonts.lineHeight(HINT)) / 2, Theme.subtle());
        y += 38;
        renderSearch(g, x, y, w, 34);
        y += 34 + 14;

        boolean noSearch = search.isEmpty();
        boolean mods = tab == Tab.MODS && noSearch;
        int all = 0;
        int on = 0;
        int favCount = 0;
        for (Module m : ShardClient.modules().all()) {
            if (m.hidden()) continue;
            all++;
            if (m.isToggledOn()) on++;
            if (favorites.contains(m.key())) favCount++;
        }
        y += navItem(g, "cat:all", "grid", "All mods", String.valueOf(all), mods && filter == Filter.ALL, x, y, w, () -> selectFilter(Filter.ALL));
        y += navItem(g, "nav:favorites", "favorites", "Favorites", String.valueOf(favCount), mods && filter == Filter.FAVORITES, x, y, w, () -> selectFilter(Filter.FAVORITES));
        y += navItem(g, "nav:enabled", "enabled", "Enabled", String.valueOf(on), mods && filter == Filter.ENABLED, x, y, w, () -> selectFilter(Filter.ENABLED));
        y += 10;
        Fonts.draw(g, "CATEGORIES", Fonts.Weight.SEMIBOLD, HINT, x + 12, y, Theme.subtle());
        y += Fonts.lineHeight(HINT) + 6;
        for (ModuleCategory c : categories()) {
            boolean sel = mods && filter == Filter.CATEGORY && c == category;
            y += navItem(g, "cat:" + c.name(), Icons.categoryIcon(c), c.displayName(), String.valueOf(ShardClient.modules().byCategory(c).size()), sel, x, y, w, () -> selectCategory(c));
        }

        // Bottom: who is signed in, and above it the other pages.
        int userH = 36;
        int uy = menuY + menuH - SIDE_PAD - userH;
        Render2D.roundedRect(g, x + 6, uy + (userH - 28) / 2, 28, 28, Theme.radiusSmall(), Theme.iconWell());
        Icons.draw(g, "user", x + 12, uy + (userH - 16) / 2, 16, Theme.icon());
        String name = minecraft.getUser().getName();
        CosmeticsModule cosmetics = ShardClient.modules().get(CosmeticsModule.class);
        ShardApi.Me me = cosmetics == null ? null : cosmetics.account();
        int balanceW = 0;
        if (me != null) {
            String shards = String.format(Locale.ROOT, "%,d", me.tokens());
            balanceW = Fonts.widthInt(shards, Fonts.Weight.MEDIUM, HINT) + 16;
            Fonts.drawRight(g, shards, Fonts.Weight.MEDIUM, HINT, x + w - 6, uy + (userH - Fonts.lineHeight(HINT)) / 2, Theme.muted());
            Icons.draw(g, "crystal", x + w - 6 - balanceW + 2, uy + (userH - 11) / 2, 11, Theme.accent());
        }
        Fonts.drawClipped(g, name, Fonts.Weight.SEMIBOLD, LABEL, x + 42, uy + (userH - Fonts.lineHeight(LABEL)) / 2, w - 48 - balanceW, Theme.text());
        int by = uy - 8 - 5 * NAV_H;
        g.fill(x, by - 9, x + w, by - 8, Theme.line());
        by += navItem(g, "tab:cosmetics", "cape", "Cosmetics", null, tab == Tab.COSMETICS, x, by, w, () -> selectTab(Tab.COSMETICS));
        by += navItem(g, "tab:friends", "user-plus", "Friends", null, tab == Tab.FRIENDS, x, by, w, () -> selectTab(Tab.FRIENDS));
        by += navItem(g, "tab:profiles", "profile", "Profiles", null, tab == Tab.PROFILES, x, by, w, () -> selectTab(Tab.PROFILES));
        boolean canEdit = minecraft.player != null;
        by += navItem(g, "nav:hud-editor", "edit-hud", "Edit HUD", null, false, x, by, w, () -> {
            if (canEdit) minecraft.setScreen(new HudEditorScreen(this, ShardClient.hud()));
        });
        navItem(g, "tab:settings", "settings", "Settings", null, tab == Tab.SETTINGS, x, by, w, () -> selectTab(Tab.SETTINGS));
    }

    /** One sidebar row: icon, label, an optional count on the right; returns its height. */
    private int navItem(GuiGraphics g, String key, String icon, String label, String count, boolean selected, int x, int y, int w, Runnable onSelect) {
        Hit probe = new Hit(key, x, y, w, NAV_H, currentClip, true, b -> onSelect.run());
        hits.add(probe);
        float sel = Render2D.easeInOut(anim(key + ":sel", selected ? 1f : 0f, HOVER_MS));
        float hov = Render2D.easeInOut(anim(key + ":hov", hoverable(probe) ? 1f : 0f, HOVER_MS));
        int fill = Colors.mix(Colors.mix(0x00000000, Theme.surfaceHover(), hov), Theme.accentAlpha(0x24), sel);
        if (Colors.alpha(fill) > 4) Render2D.roundedRect(g, x, y, w, NAV_H, Theme.radiusSmall(), fill);
        if (focused(key)) Render2D.roundedOutline(g, x, y, w, NAV_H, Theme.radiusSmall(), Theme.accentAlpha(0xA0));
        int iconColor = Colors.mix(Colors.mix(Theme.muted(), Theme.icon(), hov), Theme.accent(), sel);
        Icons.draw(g, icon, x + 12, y + (NAV_H - 15) / 2, 15, iconColor);
        int color = Colors.mix(Colors.mix(Theme.soft(), Theme.text(), hov), Theme.text(), sel);
        Fonts.drawClipped(g, label, Fonts.Weight.MEDIUM, LABEL, x + 36, y + (NAV_H - Fonts.lineHeight(LABEL)) / 2, w - 70, color);
        if (count != null) {
            Fonts.drawRight(g, count, Fonts.Weight.MEDIUM, HINT, x + w - 12, y + (NAV_H - Fonts.lineHeight(HINT)) / 2, Colors.mix(Theme.subtle(), Theme.accent(), sel));
        }
        return NAV_H;
    }

    /** Big title and one line under it, at the top of the main column. */
    private void renderPageHeader(GuiGraphics g, String title, String sub) {
        Fonts.drawClipped(g, title, Fonts.Weight.SEMIBOLD, TITLE, mainX, headY, mainW - 260, Theme.text());
        Fonts.drawClipped(g, sub, Fonts.Weight.REGULAR, DESC, mainX, headY + Fonts.lineHeight(TITLE) + 2, mainW - 260, Theme.muted());
    }

    /** The mod list's title (category, filter or search) and the All / Enabled / Off control. */
    private void renderModsHeader(GuiGraphics g) {
        String q = search.value().trim();
        String title;
        String sub;
        if (!q.isEmpty()) {
            title = "Results for “" + q + "”";
            int settings = settingResults().size();
            int modCount = shownModules().size();
            sub = modCount + (modCount == 1 ? " mod" : " mods") + " · " + settings + (settings == 1 ? " setting" : " settings");
        } else {
            switch (filter) {
                case ALL -> { title = "All mods"; sub = "Every Shard mod, by category."; }
                case FAVORITES -> { title = "Favorites"; sub = "Your starred mods, from every category."; }
                case ENABLED -> { title = "Enabled"; sub = "The mods that are on right now."; }
                default -> { title = category.displayName(); sub = category.description(); }
            }
        }
        renderPageHeader(g, title, sub);
        String[] labels = {"All", "Enabled", "Off"};
        int segW = 6;
        for (String l : labels) segW += Fonts.widthInt(l, Fonts.Weight.MEDIUM, LABEL) + 22;
        int sx = mainX + mainW - segW;
        int sy = headY + 2;
        int h = 30;
        Render2D.panel(g, sx, sy, segW, h, Theme.radiusSmall(), Theme.surfaceRaised(), Theme.line());
        int px = sx + 3;
        for (int i = 0; i < labels.length; i++) {
            int lw = Fonts.widthInt(labels[i], Fonts.Weight.MEDIUM, LABEL) + 22;
            final int value = i;
            String key = "show:" + i;
            Hit probe = new Hit(key, px, sy + 3, lw, h - 6, currentClip, true, b -> {
                showOnly = value;
                gridScroll = 0;
                gridShown = 0;
            });
            hits.add(probe);
            boolean on = showOnly == i;
            if (on) Render2D.roundedRect(g, px, sy + 3, lw, h - 6, 5, Theme.control());
            else if (hoverable(probe)) Render2D.roundedRect(g, px, sy + 3, lw, h - 6, 5, Theme.surfaceHover());
            if (focused(key)) Render2D.roundedOutline(g, px, sy + 3, lw, h - 6, 5, Theme.accentAlpha(0xA0));
            Fonts.drawCentered(g, labels[i], Fonts.Weight.MEDIUM, LABEL, px + lw / 2, sy + (h - Fonts.lineHeight(LABEL)) / 2, on ? Theme.text() : Theme.muted());
            px += lw;
        }
    }

    /** How many are on, and Reset category / Edit HUD Layout. */
    private void renderModsFooter(GuiGraphics g) {
        int y = footY;
        int on = 0;
        int total = 0;
        for (Module m : visibleModules()) {
            total++;
            if (m.isToggledOn()) on++;
        }
        Icons.draw(g, "enabled", mainX, y + (FOOT_H - 14) / 2, 14, Theme.subtle());
        Fonts.draw(g, on + " of " + total + " on · Right-click a card for its settings", Fonts.Weight.REGULAR, DESC, mainX + 22,
                y + (FOOT_H - Fonts.lineHeight(DESC)) / 2, Theme.subtle());
        boolean canEdit = minecraft.player != null;
        int bx = mainX + mainW;
        int bw = labelButtonWidth("Edit HUD Layout");
        bx -= bw;
        labelButton(g, "nav:hud-layout", "edit-hud", "Edit HUD Layout", bx, y, bw, FOOT_H, true, false, canEdit, b -> {
            if (canEdit) minecraft.setScreen(new HudEditorScreen(this, ShardClient.hud()));
        });
        if (search.isEmpty() && filter == Filter.CATEGORY) {
            String label = "Reset category";
            int rw = labelButtonWidth(label);
            bx -= 10 + rw;
            ModuleCategory c = category;
            labelButton(g, "nav:reset-category", "reset", label, bx, y, rw, FOOT_H, false, false, true, b ->
                    popover = new ConfirmPopover("Reset " + c.displayName() + "?",
                            "Every " + c.displayName() + " mod goes back to its default settings. Which mods are on stays the same.",
                            "Reset", true, () -> {
                                for (Module m : ShardClient.modules().byCategory(c)) for (Setting<?> s : m.settings()) s.reset();
                                stringInputs.clear();
                                numericInputs.clear();
                                showToast(c.displayName() + " reset");
                            }, designW, designH));
        }
    }

    private static int labelButtonWidth(String label) {
        return Fonts.widthInt(label, Fonts.Weight.MEDIUM, LABEL) + 48;
    }

    /** A button with an icon before its label. */
    private void labelButton(GuiGraphics g, String key, String icon, String label, int x, int y, int w, int h, boolean primary, boolean danger, boolean enabled, IntConsumer onClick) {
        button(g, key, x, y, w, h, "", primary, danger, enabled, onClick);
        int color = primary ? Theme.accentText() : danger ? Theme.danger() : enabled ? Theme.text() : Theme.subtle();
        int tw = Fonts.widthInt(label, Fonts.Weight.MEDIUM, LABEL);
        int tx = x + (w - 20 - tw) / 2;
        Icons.draw(g, icon, tx, y + (h - 14) / 2, 14, color);
        Fonts.draw(g, label, Fonts.Weight.MEDIUM, LABEL, tx + 20, y + (h - Fonts.lineHeight(LABEL)) / 2, color);
    }

    private void renderSearch(GuiGraphics g, int x, int y, int w, int h) {
        boolean isFocused = activeInput == search;
        search.render(g, x, y, w, h, isFocused);
        Icons.draw(g, "search", x + 10, y + (h - 13) / 2, 13, isFocused ? Theme.muted() : Theme.subtle());
        if (search.isEmpty() && !isFocused && w >= 180) {
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
        tab = Tab.MODS;
        filter = Filter.CATEGORY;
        category = c;
        gridScroll = 0;
        gridShown = 0;
        clearSearch();
        if (!wide && panelModule != null && panelModule.category() != c) closePanel();
        if (wide && panelModule != null && panelModule.category() != c) panelModule = null;
        focusKey = "cat:" + c.name();
    }

    private void selectFilter(Filter f) {
        popover = null;
        tab = Tab.MODS;
        filter = f;
        gridScroll = 0;
        gridShown = 0;
        clearSearch();
        if (!wide) closePanel();
        else panelModule = null;
        focusKey = f == Filter.ALL ? "cat:all" : "nav:" + f.name().toLowerCase(Locale.ROOT);
    }

    private void selectSettings() {
        selectTab(Tab.SETTINGS);
    }

    /**
     * After the query changes: results start at the top, and they must be visible, so a query
     * leaves the Settings page and, below the wide layout, slides the detail column away (it
     * would cover the results). The search field keeps focus.
     */
    private void searchEdited() {
        gridScroll = 0;
        gridShown = 0;
        if (embedded || search.isEmpty()) return;
        tab = Tab.MODS;
        if (panelTarget > 0f) {
            panelTarget = 0f;
            popover = null;
        }
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
                case ALL -> {
                    List<Module> out = new ArrayList<>();
                    for (ModuleCategory c : ModuleCategory.values()) out.addAll(ShardClient.modules().byCategory(c));
                    yield out;
                }
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

    private void selectModule(Module m) {
        if (panelModule != m) panelScroll = 0;
        openModule(m);
        focusKey = "row:" + m.key();
    }

    /** The list after the All / Enabled / Off control. */
    private List<Module> shownModules() {
        List<Module> out = visibleModules();
        if (showOnly == 0) return out;
        List<Module> kept = new ArrayList<>();
        for (Module m : out) if (m.isToggledOn() == (showOnly == 1)) kept.add(m);
        return kept;
    }

    /** Two columns of mod cards (or search results: matching cards, then matching settings). */
    private void renderTiles(GuiGraphics g) {
        List<Module> modules = shownModules();
        List<SettingHit> settings = settingResults();
        int top = contentY;
        int viewH = contentH;
        if (modules.isEmpty() && settings.isEmpty()) {
            boolean searching = !search.isEmpty();
            boolean favs = filter == Filter.FAVORITES && !searching && visibleModules().isEmpty();
            String icon = favs ? "favorites" : "search";
            int cx = bodyX + bodyW / 2;
            Icons.draw(g, icon, cx - 11, top + viewH / 2 - 40, 22, Theme.subtle());
            String msg = searching ? "Nothing matches \"" + search.value().trim() + "\""
                    : favs ? "No favorites yet"
                    : showOnly == 1 ? "Nothing here is on" : showOnly == 2 ? "Everything here is on" : "Nothing here yet";
            Fonts.drawCentered(g, msg, Fonts.Weight.MEDIUM, LABEL, cx, top + viewH / 2 - 8, Theme.muted());
            if (favs) {
                Fonts.drawCentered(g, "Open a mod's settings and star it to pin it here", Fonts.Weight.REGULAR, DESC, cx, top + viewH / 2 + 10, Theme.subtle());
            }
            return;
        }
        gridShown = ease(gridShown, gridScroll);
        int scroll = Math.round(gridShown);
        clip(g, bodyX + 1, top - 4, bodyW - 2, viewH + 4);
        int y = top - scroll;
        for (int i = 0; i < modules.size(); i++) {
            Module m = modules.get(i);
            int tx = contentX + (i % COLS) * (cardWidth + CARD_GAP);
            int ty = y + (i / COLS) * (CARD_H + CARD_GAP);
            cardOrder.add("row:" + m.key());
            if (ty + CARD_H >= top - 4 && ty <= top + viewH) renderTile(g, m, tx, ty, cardWidth, CARD_H);
        }
        y += ((modules.size() + COLS - 1) / COLS) * (CARD_H + CARD_GAP);
        if (!settings.isEmpty()) {
            y += 6;
            Fonts.draw(g, "SETTINGS", Fonts.Weight.SEMIBOLD, HINT, contentX + 2, y, Theme.subtle());
            y += Fonts.lineHeight(HINT) + 8;
            for (SettingHit sh : settings) {
                if (y + 36 >= top && y <= top + viewH) renderSettingResult(g, sh, contentX, y, contentW);
                y += 38;
            }
        }
        int contentHeight = y + scroll - top - CARD_GAP;
        int max = Math.max(0, contentHeight - viewH);
        gridScroll = Math.max(0, Math.min(gridScroll, max));
        gridShown = Math.max(0, Math.min(gridShown, max));
        if (scroll < max) Render2D.gradientV(g, bodyX + 1, top + viewH - 28, bodyW - 2, 28, 0x00000000, Colors.withAlpha(Theme.surface(), 0xF0));
        unclip(g);
    }

    /**
     * One mod card: icon, name (and a star for favourites) with its switch; one line of what it
     * does; then the status, its key and a Settings link. A click on the card toggles it, a
     * right-click or the Settings link opens its settings page.
     */
    private void renderTile(GuiGraphics g, Module m, int x, int y, int w, int h) {
        String key = "row:" + m.key();
        String gearKey = "gear:" + m.key();
        String switchKey = "sw:" + m.key();
        hits.add(new Hit(key, x, y, w, h, currentClip, true, b -> {
            if (b == GLFW.GLFW_MOUSE_BUTTON_RIGHT) selectModule(m);
            else m.toggle();
        }));
        String notice = notice(m);
        boolean on = m.isEnabled();

        int footTop = y + h - CARD_FOOT_H;
        String link = "Settings";
        int linkW = Fonts.widthInt(link, Fonts.Weight.MEDIUM, HINT) + 18;
        int linkX = x + w - CARD_PAD - linkW;
        hits.add(new Hit(gearKey, linkX - 6, footTop + 2, linkW + 12, CARD_FOOT_H - 4, currentClip, false, b -> selectModule(m)));
        int switchX = x + w - CARD_PAD - SWITCH_W;
        int iconBox = 32;
        int hy = y + 12;
        // The switch's hit goes in now (above the card's), and it is drawn after the card.
        Hit switchHit = new Hit(switchKey, switchX - 6, hy + (iconBox - SWITCH_H) / 2 - 6, SWITCH_W + 12, SWITCH_H + 12, currentClip, true, b -> m.toggle());
        hits.add(switchHit);

        Hit topHit = popover == null ? hitAt(mouseX, mouseY) : null;
        boolean hover = topHit != null && (topHit.key.equals(key) || topHit.key.equals(gearKey) || topHit.key.equals(switchKey));
        boolean linkHover = topHit != null && topHit.key.equals(gearKey);
        float hov = Render2D.easeInOut(anim(key + ":hov", hover ? 1f : 0f, HOVER_MS));
        boolean selected = panelModule == m;

        int fill = selected ? Colors.mix(Theme.surfaceRaised(), Theme.accent(), 0.05f) : Colors.mix(Theme.surfaceRaised(), Theme.surfaceHover(), hov * 0.5f);
        Render2D.roundedRect(g, x, y, w, h, Theme.radius(), fill);
        int rest = selected ? Theme.accentAlpha(0x70) : on ? Theme.accentAlpha(0x38) : Theme.line();
        int border = focused(key) ? Theme.accentAlpha(0xC0) : Colors.mix(rest, Theme.lineHover(), hov * 0.6f);
        Render2D.roundedOutline(g, x, y, w, h, Theme.radius(), border);

        // Head: icon well, name, star, switch.
        float knob = Render2D.easeInOut(anim(switchKey, m.isToggledOn() ? 1f : 0f, SWITCH_MS));
        int switchY = hy + (iconBox - SWITCH_H) / 2;
        Render2D.toggle(g, switchX, switchY, SWITCH_W, SWITCH_H, knob, m.isToggledOn(), focused(switchKey));
        if (notice != null) Render2D.roundedRect(g, switchX, switchY, SWITCH_W, SWITCH_H, SWITCH_H / 2, Colors.withAlpha(Theme.surface(), 0x90));
        int ix = x + CARD_PAD;
        Render2D.roundedRect(g, ix, hy, iconBox, iconBox, Theme.radiusSmall(), on ? Theme.accentAlpha(0x22) : Theme.iconWell());
        Icons.draw(g, m, ix + (iconBox - 16) / 2, hy + (iconBox - 16) / 2, 16, on ? Theme.accent() : Theme.icon());
        int nameX = ix + iconBox + 12;
        int nameMax = switchX - 12 - nameX - 18;
        String name = Fonts.clip(m.name(), Fonts.Weight.SEMIBOLD, LABEL, nameMax);
        int nameY = hy + (iconBox - Fonts.lineHeight(LABEL)) / 2;
        Fonts.draw(g, name, Fonts.Weight.SEMIBOLD, LABEL, nameX, nameY, Theme.text());
        if (favorites.contains(m.key())) {
            Icons.draw(g, "favorite-on", nameX + Fonts.widthInt(name, Fonts.Weight.SEMIBOLD, LABEL) + 7, hy + (iconBox - 11) / 2, 11, Theme.warning());
        }

        // What it does, on as many lines as fit above the foot (two on a normal card).
        int descY = hy + iconBox + 8;
        int descW = w - 2 * CARD_PAD;
        int maxLines = Math.max(1, (footTop - 4 - descY) / Fonts.lineHeight(DESC));
        List<String> lines = Fonts.wrap(m.description(), Fonts.Weight.REGULAR, DESC, descW);
        for (int i = 0; i < Math.min(maxLines, lines.size()); i++) {
            String line = lines.get(i);
            if (i == maxLines - 1 && lines.size() > maxLines) line = Fonts.clip(line + " …", Fonts.Weight.REGULAR, DESC, descW);
            Fonts.draw(g, line, Fonts.Weight.REGULAR, DESC, ix, descY + i * Fonts.lineHeight(DESC), Theme.muted());
        }

        // Foot: status, key, Settings link.
        g.fill(ix, footTop, x + w - CARD_PAD, footTop + 1, Theme.line());
        String status = notice != null ? (m.blockedBy() != null ? "BLOCKED" : "OFF HERE") : on ? "ENABLED" : "DISABLED";
        int statusColor = notice != null ? Theme.warning() : on ? Theme.accent() : Theme.subtle();
        int fy = footTop + (CARD_FOOT_H - Fonts.lineHeight(HINT)) / 2;
        Fonts.draw(g, status, Fonts.Weight.SEMIBOLD, HINT, ix, fy, statusColor);
        if (m.keybind() >= 0) {
            String bind = Keys.name(m.keybind());
            int kx = ix + Fonts.widthInt(status, Fonts.Weight.SEMIBOLD, HINT) + 10;
            int kw = Fonts.widthInt(bind, Fonts.Weight.MEDIUM, HINT) + 12;
            int ky = footTop + (CARD_FOOT_H - 18) / 2;
            Render2D.roundedOutline(g, kx, ky, kw, 18, 4, Theme.lineStrong());
            Fonts.draw(g, bind, Fonts.Weight.MEDIUM, HINT, kx + 6, ky + (18 - Fonts.lineHeight(HINT)) / 2, Theme.muted());
        }
        int linkColor = linkHover ? Theme.text() : Theme.subtle();
        Icons.draw(g, "settings", linkX, footTop + (CARD_FOOT_H - 12) / 2, 12, linkColor);
        Fonts.draw(g, link, Fonts.Weight.MEDIUM, HINT, linkX + 18, fy, linkColor);

        if (hover && notice != null) {
            hoverDetails = notice;
            hoverDetailsX = mouseX + 12;
            hoverDetailsY = mouseY + 16;
        }
    }

    // ---- a mod's settings page -----------------------------------------------------------------

    /** One box of settings on a mod's page: a caption and what goes in it. */
    private record SettingsBox(String id, String title, List<Setting<?>> settings, SectionBody extra) {}

    /**
     * A mod's settings as a page of its own: a back button and breadcrumb, a card with the icon,
     * name, what it does, its key, Favorite and the switch, then the settings in boxes (one per
     * group) laid out in two columns, and Reset module / Disable on this server at the bottom.
     */
    private void renderModulePage(GuiGraphics g) {
        Module m = panelModule;
        if (m == null) return;
        hit("panel", panelX, panelY, panelW, panelH, false, b -> {});
        int x = mainX;
        int w = mainW;
        int y = headY;

        // Back and breadcrumb.
        int backS = 28;
        Render2D.roundedRect(g, x, y, backS, backS, Theme.radiusSmall(), Theme.surfaceRaised());
        Render2D.roundedOutline(g, x, y, backS, backS, Theme.radiusSmall(), Theme.line());
        iconButton(g, "panel-back", "chevron-left", x, y, backS, Theme.text(), b -> closePanel());
        int cy = y + (backS - Fonts.lineHeight(LABEL)) / 2;
        int cx = x + backS + 12;
        String crumb = m.category().displayName();
        int crumbW = Fonts.widthInt(crumb, Fonts.Weight.MEDIUM, LABEL);
        Hit crumbHit = new Hit("crumb:category", cx - 4, y, crumbW + 8, backS, currentClip, true, b -> {
            ModuleCategory c = m.category();
            closePanel();
            selectCategory(c);
        });
        hits.add(crumbHit);
        Fonts.draw(g, crumb, Fonts.Weight.MEDIUM, LABEL, cx, cy, hoverable(crumbHit) ? Theme.accentHover() : Theme.accent());
        cx += crumbW + 8;
        Fonts.draw(g, "/", Fonts.Weight.REGULAR, LABEL, cx, cy, Theme.subtle());
        cx += Fonts.widthInt("/", Fonts.Weight.REGULAR, LABEL) + 8;
        Fonts.drawClipped(g, m.name(), Fonts.Weight.REGULAR, LABEL, cx, cy, x + w - cx, Theme.muted());
        y += backS + 12;

        // The mod's card.
        String notice = notice(m);
        int pad = 16;
        int iconBox = 44;
        boolean fav = favorites.contains(m.key());
        String favLabel = "Favorite";
        int favW = labelButtonWidth(favLabel);
        String bind = m.keybind() >= 0 ? Keys.name(m.keybind()) : null;
        int bindW = bind == null ? 0 : Fonts.widthInt(bind, Fonts.Weight.MEDIUM, HINT) + 14;
        int rightW = SWITCH_W + 14 + favW + (bind == null ? 0 : bindW + 12);
        int textX = x + pad + iconBox + 16;
        int textW = x + w - pad - rightW - 24 - textX;
        List<String> about = Fonts.wrap(m.about(), Fonts.Weight.REGULAR, DESC, textW);
        if (about.size() > 3) about = new ArrayList<>(about.subList(0, 3));
        List<String> noticeLines = notice == null ? List.of() : Fonts.wrap(notice, Fonts.Weight.REGULAR, DESC, textW - 20);
        int textH = Fonts.lineHeight(18) + 4 + about.size() * Fonts.lineHeight(DESC) + (noticeLines.isEmpty() ? 0 : 6 + noticeLines.size() * Fonts.lineHeight(DESC));
        int cardH = Math.max(iconBox, textH) + 2 * pad;
        Render2D.roundedRect(g, x, y, w, cardH, Theme.radius(), Colors.mix(Theme.surfaceRaised(), Theme.accent(), 0.05f));
        Render2D.roundedOutline(g, x, y, w, cardH, Theme.radius(), Theme.accentAlpha(0x60));
        int iy = y + (cardH - iconBox) / 2;
        Render2D.roundedRect(g, x + pad, iy, iconBox, iconBox, Theme.radius(), Theme.accentAlpha(0x22));
        Icons.draw(g, m, x + pad + (iconBox - 22) / 2, iy + (iconBox - 22) / 2, 22, m.isEnabled() ? Theme.accent() : Theme.icon());
        int ty = y + (cardH - textH) / 2;
        Fonts.drawClipped(g, m.name(), Fonts.Weight.SEMIBOLD, 18, textX, ty, textW, Theme.text());
        ty += Fonts.lineHeight(18) + 4;
        for (String line : about) {
            Fonts.draw(g, line, Fonts.Weight.REGULAR, DESC, textX, ty, Theme.muted());
            ty += Fonts.lineHeight(DESC);
        }
        if (!noticeLines.isEmpty()) {
            ty += 6;
            Icons.draw(g, "warning", textX, ty, 14, Theme.warning());
            for (String line : noticeLines) {
                Fonts.draw(g, line, Fonts.Weight.REGULAR, DESC, textX + 20, ty, Theme.warning());
                ty += Fonts.lineHeight(DESC);
            }
        }
        int rx = x + w - pad - SWITCH_W;
        int midY = y + cardH / 2;
        renderSwitch(g, "panel-enabled", rx, midY - SWITCH_H / 2, m.isToggledOn(), notice == null, m::toggle);
        rx -= 14 + favW;
        int bh = 30;
        button(g, "panel-star", rx, midY - bh / 2, favW, bh, "", false, true, b -> {
            if (!favorites.remove(m.key())) favorites.add(m.key());
        });
        int favColor = fav ? Theme.warning() : Theme.text();
        int ftw = Fonts.widthInt(favLabel, Fonts.Weight.MEDIUM, LABEL);
        int fx = rx + (favW - 20 - ftw) / 2;
        Icons.draw(g, fav ? "favorite-on" : "favorites", fx, midY - 7, 14, fav ? Theme.warning() : Theme.muted());
        Fonts.draw(g, favLabel, Fonts.Weight.MEDIUM, LABEL, fx + 20, midY - Fonts.lineHeight(LABEL) / 2, favColor);
        if (bind != null) {
            rx -= 12 + bindW;
            Render2D.roundedOutline(g, rx, midY - 10, bindW, 20, 4, Theme.lineStrong());
            Fonts.draw(g, bind, Fonts.Weight.MEDIUM, HINT, rx + 7, midY - Fonts.lineHeight(HINT) / 2, Theme.muted());
        }
        y += cardH + 12;

        // Settings boxes in two columns, scrolling together.
        List<SettingsBox> boxes = new ArrayList<>();
        if (m instanceof PanelPreview preview) {
            boxes.add(new SettingsBox("preview", "PREVIEW", List.of(), (bx, by, bw) -> {
                preview.previewMouse(mouseX, mouseY);
                int used = preview.renderPreview(g, bx, by, bw);
                if (used > 0) {
                    hits.add(new Hit("preview", bx, by, bw, used, currentClip, false, b -> {
                        String msg = preview.previewInput(mouseX, mouseY, b, false);
                        if (msg != null) showToast(msg);
                        previewDragging = preview;
                        previewButton = b;
                    }));
                }
                return by + Math.max(0, used);
            }));
        }
        Map<String, List<Setting<?>>> groups = new java.util.LinkedHashMap<>();
        for (Setting<?> s : m.settings()) {
            if (!s.isVisible()) continue;
            groups.computeIfAbsent(s.group().isEmpty() ? "Settings" : s.group(), k -> new ArrayList<>()).add(s);
        }
        for (var e : groups.entrySet()) boxes.add(new SettingsBox("g:" + e.getKey(), e.getKey().toUpperCase(Locale.ROOT), e.getValue(), null));
        List<Module> bindable = new ArrayList<>();
        for (Module other : ShardClient.modules().all()) if (!other.hidden()) bindable.add(other);
        String conflict = conflictFor(m, bindable);
        boxes.add(new SettingsBox("general", "KEYBIND", List.of(), (bx, by, bw) -> by + keybindRow(g, "kb:" + m.key(), "Toggle key",
                conflict != null ? conflict : "Turns the mod on and off in-game", Keys.name(m.keybind()), listeningModule == m, bx, by, bw,
                () -> {
                    listeningModule = m;
                    listeningKey = null;
                }, () -> m.setKeybind(Keys.NONE))));

        int rowsTop = y;
        int rowsH = footY - 14 - rowsTop;
        clip(g, bodyX + 1, rowsTop, bodyW - 2, rowsH);
        panelShown = ease(panelShown, panelScroll);
        int scroll = Math.round(panelShown);
        int colW = (w - BOX_GAP) / 2;
        int[] colY = {rowsTop - scroll, rowsTop - scroll};
        for (SettingsBox box : boxes) {
            String cacheKey = "box-h:" + m.key() + ":" + box.id();
            int col = colY[1] < colY[0] ? 1 : 0;
            int bx = x + col * (colW + BOX_GAP);
            int bh2 = renderSettingsBox(g, m, box, bx, colY[col], colW, cacheKey, rowsTop);
            colY[col] += bh2 + BOX_GAP;
        }
        int contentHeight = Math.max(colY[0], colY[1]) + scroll - rowsTop - BOX_GAP;
        int max = Math.max(0, contentHeight - rowsH);
        panelScroll = Math.max(0, Math.min(panelScroll, max));
        panelShown = Math.max(0, Math.min(panelShown, max));
        if (scroll < max) Render2D.gradientV(g, bodyX + 1, rowsTop + rowsH - 24, bodyW - 2, 24, 0x00000000, Colors.withAlpha(Theme.surface(), 0xF0));
        unclip(g);

        // Footer.
        Fonts.draw(g, "Changes save as you make them", Fonts.Weight.REGULAR, DESC, x, footY + (FOOT_H - Fonts.lineHeight(DESC)) / 2, Theme.subtle());
        int fbx = x + w;
        String server = ShardClient.modules().currentServer();
        if (server != null) {
            boolean off = ShardClient.modules().blacklist().isDisabled(server, m.key());
            String label = off ? "Allow on this server" : "Disable on this server";
            int sw = labelButtonWidth(label);
            fbx -= sw;
            labelButton(g, "panel-server", "server-rule", label, fbx, footY, sw, FOOT_H, false, !off, true,
                    b -> ShardClient.modules().setDisabledOnCurrentServer(m, !off));
            fbx -= 10;
        }
        String reset = "Reset module";
        int resetW = labelButtonWidth(reset);
        fbx -= resetW;
        labelButton(g, "panel-reset", "reset", reset, fbx, footY, resetW, FOOT_H, false, false, true, b -> {
            for (Setting<?> s : m.settings()) s.reset();
            stringInputs.clear();
            numericInputs.clear();
            showToast(m.name() + " reset");
        });
    }

    /** One settings box (height measured last frame, so the frame is drawn behind its rows); returns its height. */
    private int renderSettingsBox(GuiGraphics g, Module m, SettingsBox box, int x, int y, int w, String cacheKey, int viewTop) {
        int pad = 16;
        int cachedH = Math.round(anims.getOrDefault(cacheKey, 120f));
        Render2D.panel(g, x, y, w, cachedH, Theme.radius(), Theme.surfaceRaised(), Theme.line());
        Fonts.draw(g, box.title(), Fonts.Weight.SEMIBOLD, HINT, x + pad, y + 14, Theme.subtle());
        int ry = y + 14 + Fonts.lineHeight(HINT) + 6;
        int ix = x + pad;
        int iw = w - 2 * pad;
        if (box.extra() != null) ry = box.extra().render(ix, ry, iw);
        List<Setting<?>> list = box.settings();
        for (int i = 0; i < list.size(); i++) {
            Setting<?> s = list.get(i);
            if (s == flashSetting) {
                long age = System.currentTimeMillis() - flashSince;
                if (flashScrollPending) {
                    panelScroll = Math.max(0, panelScroll + (ry - viewTop) - 24);
                    flashScrollPending = false;
                }
                if (age < FLASH_MS) {
                    float a = 1f - age / (float) FLASH_MS;
                    int fh = Math.round(anims.getOrDefault("flash-h", (float) ROW_DESC_H));
                    Render2D.roundedRect(g, ix - 8, ry, iw + 16, fh, Theme.radiusSmall(), Colors.fade(Theme.accentAlpha(0x2A), a));
                } else flashSetting = null;
            }
            int rh = renderSettingRow(g, m, s, ix, ry, iw);
            if (s == flashSetting) anims.put("flash-h", (float) rh);
            ry += rh;
            if (rh > 0 && i < list.size() - 1) {
                g.fill(ix, ry, ix + iw, ry + 1, Theme.line());
                ry += 1;
            }
        }
        int h = ry - y + 8;
        anims.put(cacheKey, (float) h);
        return h;
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
        panelShown = 0;
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
        boolean inline = !embedded;
        if (!inline) {
            Render2D.shadow(g, x, y, w, h, r, 0.5);
            Render2D.panel(g, x, y, w, h, r, Theme.surface(), Theme.line());
        }
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
        if (inline) {
            iconButton(g, "panel-back", "chevron-left", hx - 4, hy + (headH - 28) / 2, 28, Theme.muted(), b -> closePanel());
            hx += 28;
        } else if (embedded) {
            iconButton(g, "panel-back", "close", hx - 4, hy + (headH - 28) / 2, 28, Theme.muted(), b -> onClose());
            hx += 28;
        }
        if (!inline) Icons.draw(g, m, hx, hy + (headH - 20) / 2, 20, m.isEnabled() ? Theme.accent() : Theme.muted());
        String notice = notice(m);
        int switchX = x + w - PANEL_PAD - SWITCH_W;
        renderSwitch(g, "panel-enabled", switchX, hy + (headH - SWITCH_H) / 2, m.isToggledOn(), notice == null, m::toggle);
        boolean fav = favorites.contains(m.key());
        int starX = switchX - 8 - 24;
        iconButton(g, "panel-star", fav ? "favorite-on" : "favorites", starX, hy + (headH - 24) / 2, 24, fav ? Theme.warning() : Theme.subtle(), b -> {
            if (!favorites.remove(m.key())) favorites.add(m.key());
        });
        int titleX = inline ? hx + 4 : hx + 30;
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
            preview.previewMouse(mouseX, mouseY);
            int used = preview.renderPreview(g, innerX, cy + 10, innerW);
            if (used > 0) {
                hits.add(new Hit("preview", innerX, cy + 10, innerW, used, currentClip, false, b -> {
                    String msg = preview.previewInput(mouseX, mouseY, b, false);
                    if (msg != null) showToast(msg);
                    previewDragging = preview;
                    previewButton = b;
                }));
                cy += used + 10;
            }
        }
        cy += 12;
        g.fill(x + 1, cy, x + w - 1, cy + 1, Theme.line());
        cy += 1;

        // Scrollable settings.
        int rowsTop = cy;
        int rowsH = y + h - rowsTop - 1;
        clip(g, x + 1, rowsTop, w - 2, rowsH);
        panelShown = ease(panelShown, panelScroll);
        int ry = rowsTop + 4 - Math.round(panelShown);
        rowDividers = inline;
        ry = renderSettingRows(g, m, m.settings(), innerX, ry, innerW, rowsTop);

        ry += SECTION_ABOVE;
        Fonts.draw(g, "General", Fonts.Weight.SEMIBOLD, DESC, innerX, ry, Theme.soft());
        ry += Fonts.lineHeight(DESC) + SECTION_BELOW;
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
        rowDividers = false;
        ry += BUTTON_H + PANEL_PAD;
        int contentHeight = ry + Math.round(panelShown) - rowsTop;
        panelScroll = Math.max(0, Math.min(panelScroll, contentHeight - rowsH));
        panelShown = Math.max(0, Math.min(panelShown, Math.max(0, contentHeight - rowsH)));
        if (contentHeight - panelScroll > rowsH + 2) {
            Render2D.gradientV(g, x + 1, rowsTop + rowsH - 16, w - 2, 16, 0x00000000, Colors.withAlpha(Theme.surface(), 0xF0));
        }
        unclip(g);
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
                Fonts.draw(g, s.group(), Fonts.Weight.SEMIBOLD, DESC, x, y, Theme.soft());
                y += Fonts.lineHeight(DESC) + SECTION_BELOW;
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
            if (rowDividers && rowH > 0) {
                g.fill(x, y, x + w, y + 1, Theme.line());
                y += 1;
            }
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
            // The whole-control hit for `key` is only added after this loop, so test the segment's own
            // probe (looking up `key` here returned null and crashed on hover, 0.10.0).
            boolean hover = popover == null && Render2D.hovered(mouseX, mouseY, sx, y + 2, sw, h - 4) && hoverable(probe);
            if (on) Render2D.roundedRect(g, sx, y + 2, sw, h - 4, 4, Theme.control());
            else if (hover) Render2D.roundedRect(g, sx, y + 2, sw, h - 4, 4, Theme.surfaceHover());
            String label = Fonts.clip(EnumSetting.pretty(v), Fonts.Weight.MEDIUM, HINT, sw - 6);
            Fonts.drawCentered(g, label, Fonts.Weight.MEDIUM, HINT, sx + sw / 2, y + (h - Fonts.lineHeight(HINT)) / 2, on ? Theme.text() : Theme.muted());
        }
        // The whole control is one focus stop (arrows change the value). It is registered last, so
        // it is the hit a click finds: pick the segment under the pointer here, or clicks on the
        // segments would do nothing.
        hits.add(new Hit(key, x, y, w, h, currentClip, true, b -> {
            int i = Math.max(0, Math.min(values.length - 1, (mouseX - x - 2) / Math.max(1, segW)));
            e.set(values[i]);
        }));
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
        Icons.draw(g, icon, innerX, y + pad + (Fonts.lineHeight(SECTION) - 16) / 2, 16, Theme.muted());
        Fonts.drawClipped(g, title, Fonts.Weight.SEMIBOLD, SECTION, innerX + 26, y + pad, innerW - 26, Theme.text());
        Fonts.drawClipped(g, sub, Fonts.Weight.REGULAR, DESC, innerX + 26, y + pad + Fonts.lineHeight(SECTION) + 2, innerW - 26, Theme.muted());
        int end = body.render(innerX, y + headerH, innerW);
        int h = end - y + pad;
        anims.put(cacheKey, (float) h);
        return h;
    }

    private void renderSettingsPage(GuiGraphics g) {
        int top = contentY;
        int viewH = contentH;
        clip(g, bodyX + 1, top - GRID_PAD + 1, bodyW - 2, viewH + GRID_PAD - 1);
        pageShown = ease(pageShown, pageScroll);
        int y = top - Math.round(pageShown);
        int sectionW = Math.min(contentW, SECTION_MAX_W);
        int x = mainX;

        y += section(g, "favorites", "Quick setup", "One click to a setup made for crystal PvP; change anything afterwards", x, y, sectionW, this::renderQuickSetupBody) + GRID_GAP;
        y += section(g, "appearance", "Appearance", "Accent, interface size, font, blur and motion", x, y, sectionW,
                (ix, iy, iw) -> renderSettingRows(g, ShardClient.appearance(), ShardClient.appearance().settings(), ix, iy, iw)) + GRID_GAP;
        var screens = ShardClient.modules().get(gg.shard.client.modules.settings.MenuScreensModule.class);
        y += section(g, "window", "Menus", "Shard's title screen and server list, or vanilla's", x, y, sectionW,
                (ix, iy, iw) -> renderSettingRows(g, screens, screens.settings(), ix, iy, iw)) + GRID_GAP;
        var display = ShardClient.modules().get(gg.shard.client.modules.utility.DisplayModule.class);
        y += section(g, "window", "Window", "Borderless fullscreen, frame caps, window title, raw input and VSync", x, y, sectionW,
                (ix, iy, iw) -> renderSettingRows(g, display, display.settings(), ix, iy, iw)) + GRID_GAP;
        y += section(g, "hud", "HUD", "Scale and the style every HUD element inherits", x, y, sectionW,
                (ix, iy, iw) -> renderSettingRows(g, ShardClient.hudDefaults(), ShardClient.hudDefaults().settings(), ix, iy, iw)) + GRID_GAP;
        y += section(g, "keybind", "Keybinds", "Every module keybind in one list, with conflict warnings", x, y, sectionW, this::renderKeybindsBody) + GRID_GAP;
        y += section(g, "server-rule", "Server rules", "Modules kept off on matching addresses; * matches subdomains", x, y, sectionW, this::renderServersBody) + GRID_GAP;
        y += section(g, "download", "Export and import", "Copy the config as JSON to the clipboard, or paste one back", x, y, sectionW, this::renderExportBody) + GRID_GAP;
        y += section(g, "reset", "Reset", "Back to a fresh install, keeping server rules", x, y, sectionW, this::renderResetBody) + GRID_GAP;
        y += section(g, "logo", "About", "Shard Client " + version(), x, y, sectionW, this::renderAboutBody) + GRID_GAP;

        int contentHeight = y + Math.round(pageShown) - top;
        pageScroll = Math.max(0, Math.min(pageScroll, contentHeight - viewH));
        pageShown = Math.max(0, Math.min(pageShown, Math.max(0, contentHeight - viewH)));
        unclip(g);
    }

    /** Saved configs: make your own profiles and switch between them. */
    private void renderProfilesPage(GuiGraphics g) {
        int top = contentY;
        int viewH = contentH;
        clip(g, bodyX + 1, top - GRID_PAD + 1, bodyW - 2, viewH + GRID_PAD - 1);
        pageShown = ease(pageShown, pageScroll);
        int w = Math.min(contentW, SECTION_MAX_W);
        int x = mainX;
        int y = top - Math.round(pageShown);
        y = renderProfilesBody(x, y, w);
        int contentHeight = y + Math.round(pageShown) - top + GRID_PAD;
        pageScroll = Math.max(0, Math.min(pageScroll, contentHeight - viewH));
        pageShown = Math.max(0, Math.min(pageShown, Math.max(0, contentHeight - viewH)));
        unclip(g);
    }

    /** Cosmetics are always on; this tab holds what they show and the token options. */
    /** Which slot the Cosmetics tab lists (null = all), and the item key being saved (buttons wait for it). */
    private String cosmeticFilter;
    private String cosmeticBusy;

    /**
     * Cosmetics: what you wear in each slot, every cape, shield and bandana (owned first) with
     * Equip / Take off, and the display switches. Equipping saves to your Shard account.
     */
    private void renderCosmeticsPage(GuiGraphics g) {
        CosmeticsModule cosmetics = ShardClient.modules().get(CosmeticsModule.class);
        int top = contentY;
        int viewH = contentH;
        clip(g, bodyX + 1, top - GRID_PAD + 1, bodyW - 2, viewH + GRID_PAD - 1);
        pageShown = ease(pageShown, pageScroll);
        int w = Math.min(contentW, COSMETICS_MAX_W);
        int x = mainX;
        int y = top - Math.round(pageShown);
        Map<String, PlayerCosmetics.Item> catalogue = cosmetics.catalogue();
        ShardApi.Me me = cosmetics.account();
        boolean signedIn = cosmetics.signedIn();
        Set<String> owned = me == null ? Set.of() : new HashSet<>(me.owned());

        for (String line : Fonts.wrap("Equip anything you own. It saves to your Shard account: you wear it straight away, other Shard players see it within a minute, and the launcher shows it too.",
                Fonts.Weight.REGULAR, DESC, w)) {
            Fonts.draw(g, line, Fonts.Weight.REGULAR, DESC, x, y, Theme.muted());
            y += Fonts.lineHeight(DESC);
        }
        y += 4;
        Fonts.drawClipped(g, cosmetics.accountStatus(), Fonts.Weight.REGULAR, DESC, x, y, w, signedIn ? Theme.subtle() : Theme.warning());
        y += Fonts.lineHeight(DESC) + 14;

        // What you wear, one card per slot.
        Fonts.draw(g, "Wearing", Fonts.Weight.SEMIBOLD, SECTION, x, y, Theme.text());
        y += Fonts.lineHeight(SECTION) + 8;
        int slotGap = 8;
        int slotW = (w - 2 * slotGap) / 3;
        int slotH = 64;
        for (int i = 0; i < CosmeticsTab.SLOTS.size(); i++) {
            String slot = CosmeticsTab.SLOTS.get(i);
            int sx = x + i * (slotW + slotGap);
            Render2D.panel(g, sx, y, slotW, slotH, Theme.radius(), Theme.surfaceRaised(), Theme.line());
            String id = cosmetics.wearing(slot);
            PlayerCosmetics.Item item = id == null ? null : catalogue.get(id);
            int ps = slotH - 16;
            cosmeticPicture(g, cosmetics, id, slot, sx + 8, y + 8, ps, true);
            int tx = sx + 8 + ps + 10;
            int tw = slotW - (tx - sx) - 8;
            Fonts.drawClipped(g, CosmeticsTab.slotLabel(slot), Fonts.Weight.MEDIUM, HINT, tx, y + 8, tw, Theme.subtle());
            Fonts.drawClipped(g, item != null ? item.name() : id != null ? id : "Nothing", Fonts.Weight.MEDIUM, LABEL, tx, y + 8 + Fonts.lineHeight(HINT) + 1, tw,
                    id == null ? Theme.muted() : Theme.text());
            if (id != null) {
                String key = "cos-off:" + slot;
                button(g, key, tx, y + slotH - 8 - 20, Math.min(tw, 80), 20, cosmeticBusy != null && cosmeticBusy.equals(key) ? "Saving" : "Take off", false,
                        signedIn && cosmeticBusy == null, b -> equipFromTab(cosmetics, key, slot, null, null));
            }
        }
        y += slotH + 18;

        // Your cosmetics, with a slot filter on the right.
        Fonts.draw(g, "Your cosmetics", Fonts.Weight.SEMIBOLD, SECTION, x, y + (24 - Fonts.lineHeight(SECTION)) / 2, Theme.text());
        String[][] filters = {{null, "All"}, {"cape", "Capes"}, {"shield", "Shields"}, {"bandana", "Bandanas"}};
        int[] fws = new int[filters.length];
        int fx = x + w;
        for (int i = 0; i < filters.length; i++) {
            fws[i] = Math.max(56, Fonts.widthInt(filters[i][1], Fonts.Weight.MEDIUM, LABEL) + 28);
            fx -= fws[i] + (i > 0 ? 4 : 0);
        }
        for (int i = 0; i < filters.length; i++) {
            String[] f = filters[i];
            int fw = fws[i];
            boolean on = java.util.Objects.equals(cosmeticFilter, f[0]);
            button(g, "cos-filter:" + f[1], fx, y, fw, 24, f[1], on, true, b -> {
                cosmeticFilter = f[0];
                pageScroll = 0;
            });
            fx += fw + 4;
        }
        y += 24 + 10;

        List<PlayerCosmetics.Item> items = CosmeticsTab.list(catalogue.values(), owned, cosmeticFilter);
        if (items.isEmpty()) {
            String empty = catalogue.isEmpty() ? "Loading the cosmetics catalogue..." : "Nothing in this slot yet.";
            Fonts.draw(g, empty, Fonts.Weight.REGULAR, DESC, x, y + 4, Theme.subtle());
            y += Fonts.lineHeight(DESC) + 16;
        } else {
            int gap = 8;
            int cols = Math.max(2, (w + gap) / (132 + gap));
            int tileW = (w - (cols - 1) * gap) / cols;
            int ps = Math.min(tileW - 16, 96);
            int tileH = 8 + ps + 8 + Fonts.lineHeight(LABEL) + 2 + Fonts.lineHeight(HINT) + 8 + 22 + 8;
            boolean anyLocked = false;
            for (int i = 0; i < items.size(); i++) {
                PlayerCosmetics.Item item = items.get(i);
                int tx = x + (i % cols) * (tileW + gap);
                int ty = y + (i / cols) * (tileH + gap);
                boolean has = owned.contains(item.id());
                anyLocked |= !has;
                boolean on = item.id().equals(cosmetics.wearing(item.slot()));
                Render2D.panel(g, tx, ty, tileW, tileH, Theme.radius(), Theme.surfaceRaised(), on ? Theme.accentAlpha(0xC0) : Theme.line());
                cosmeticPicture(g, cosmetics, item.id(), item.slot(), tx + (tileW - ps) / 2, ty + 8, ps, has);
                if (!has) Icons.draw(g, "lock", tx + tileW - 8 - 14, ty + 8, 14, Theme.muted());
                if (on) Icons.draw(g, "check", tx + 8, ty + 8, 14, Theme.accent());
                int ny = ty + 8 + ps + 8;
                Fonts.drawClipped(g, item.name(), Fonts.Weight.MEDIUM, LABEL, tx + 8, ny, tileW - 16, has ? Theme.text() : Theme.muted());
                ny += Fonts.lineHeight(LABEL) + 2;
                Fonts.drawClipped(g, CosmeticsTab.slotLabel(item.rarity()) + " " + CosmeticsTab.slotLabel(item.slot()).toLowerCase(Locale.ROOT),
                        Fonts.Weight.REGULAR, HINT, tx + 8, ny, tileW - 16, rarityColor(item.rarity()));
                int by = ty + tileH - 8 - 22;
                String key = "cos-item:" + item.id();
                if (!has) {
                    button(g, key, tx + 8, by, tileW - 16, 22, "In the shop", false, false, b -> {});
                } else {
                    String label = cosmeticBusy != null && cosmeticBusy.equals(key) ? "Saving" : on ? "Take off" : "Equip";
                    button(g, key, tx + 8, by, tileW - 16, 22, label, !on, signedIn && cosmeticBusy == null,
                            b -> equipFromTab(cosmetics, key, item.slot(), on ? null : item.id(), item.name()));
                }
            }
            y += ((items.size() + cols - 1) / cols) * (tileH + gap);
            if (anyLocked) {
                Fonts.draw(g, "Locked items are in the Shop in Shard Launcher; spend the Shards you earn by playing, or get more in its Store.", Fonts.Weight.REGULAR, HINT, x, y + 2, Theme.subtle());
                y += Fonts.lineHeight(HINT) + 6;
            }
        }
        y += 14;

        Fonts.draw(g, "Display", Fonts.Weight.SEMIBOLD, SECTION, x, y, Theme.text());
        y += Fonts.lineHeight(SECTION) + 8;
        g.fill(x, y, x + w, y + 1, Theme.line());
        y += 1;
        rowDividers = true;
        y = renderSettingRows(g, cosmetics, cosmetics.settings(), x, y, w);
        rowDividers = false;
        int contentHeight = y + Math.round(pageShown) - top + GRID_PAD;
        pageScroll = Math.max(0, Math.min(pageScroll, contentHeight - viewH));
        pageShown = Math.max(0, Math.min(pageShown, Math.max(0, contentHeight - viewH)));
        unclip(g);
    }

    /** The item's preview picture in a rounded well, or the slot's icon while it loads (or when there is none). */
    private void cosmeticPicture(GuiGraphics g, CosmeticsModule cosmetics, String id, String slot, int x, int y, int size, boolean bright) {
        Render2D.roundedRect(g, x, y, size, size, Theme.radiusSmall(), Theme.iconWell());
        Identifier tex = id == null ? null : cosmetics.preview(id);
        if (tex != null) {
            Render2D.image(g, tex, x + 2, y + 2, size - 4, PreviewFit.SIZE, bright ? 0xFFFFFFFF : 0x80FFFFFF);
            return;
        }
        int is = Math.min(24, size / 2);
        Icons.draw(g, "bandana".equals(slot) ? "user" : slot, x + (size - is) / 2, y + (size - is) / 2, is, id == null ? Theme.subtle() : Theme.muted());
    }

    /** Saves a slot change; the buttons wait (show "Saving") until the account answers. */
    private void equipFromTab(CosmeticsModule cosmetics, String key, String slot, String id, String name) {
        cosmeticBusy = key;
        cosmetics.equip(slot, id).whenComplete((me, error) -> {
            cosmeticBusy = null;
            if (error != null) showToast("Could not save: " + error.getMessage());
            else showToast(id == null ? "Took off your " + slot : "Equipped " + name);
        });
    }

    private static int rarityColor(String rarity) {
        return switch (rarity == null ? "" : rarity) {
            case "special" -> 0xFFF08A3C;
            case "mythic" -> 0xFFE5484D;
            case "legendary" -> 0xFFF5B83D;
            case "epic" -> 0xFFB07CF7;
            case "rare" -> 0xFF4FA3F7;
            case "uncommon" -> 0xFF5CC97B;
            default -> Theme.muted();
        };
    }

    /** Friends are managed in the launcher; the tab points there. */
    private void renderFriendsPage(GuiGraphics g) {
        int cx = bodyX + bodyW / 2;
        int cy = contentY + contentH / 2 - 30;
        Icons.draw(g, "user", cx - 11, cy - 32, 22, Theme.subtle());
        Fonts.drawCentered(g, "Friends live in Shard Launcher", Fonts.Weight.MEDIUM, LABEL, cx, cy, Theme.text());
        Fonts.drawCentered(g, "Add players, answer requests and see who is in game on the launcher's Friends page.", Fonts.Weight.REGULAR, DESC, cx,
                cy + Fonts.lineHeight(LABEL) + 4, Theme.muted());
    }


    private int renderQuickSetupBody(int x, int y, int w) {
        GuiGraphics g = g0;
        int cols = w >= 420 ? 3 : 1;
        int bw = (w - (cols - 1) * 8) / cols;
        int i = 0;
        int rowY = y;
        for (QuickSetup.Choice c : QuickSetup.choices()) {
            int bx = x + (i % cols) * (bw + 8);
            int by = rowY + (i / cols) * (BUTTON_H + 8);
            button(g, "quick:" + c.name(), bx, by, bw, BUTTON_H, c.name(), i == 0, true, b -> {
                if (QuickSetup.apply(c.name())) showToast("Applied " + c.name());
            });
            i++;
        }
        int rows = (QuickSetup.choices().size() + cols - 1) / cols;
        y = rowY + rows * (BUTTON_H + 8);
        // What each setup does is worth reading, so it wraps instead of being cut off.
        for (String line : Fonts.wrap("Pro turns on the fight modules and the minimal HUD; Minimal keeps things vanilla; Recording is clean for videos.",
                Fonts.Weight.REGULAR, HINT, w)) {
            Fonts.draw(g, line, Fonts.Weight.REGULAR, HINT, x, y, Theme.muted());
            y += Fonts.lineHeight(HINT);
        }
        return y;
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
        Fonts.drawClipped(g, "Made by OhMarker with the help of SwxyzX2", Fonts.Weight.MEDIUM, LABEL, x, y, w, Theme.text());
        y += Fonts.lineHeight(LABEL) + 2;
        Fonts.drawClipped(g, "Shard Client is open source under the MIT License (github.com/OhMarker/shard-client)", Fonts.Weight.REGULAR, DESC, x, y, w, Theme.muted());
        y += Fonts.lineHeight(DESC) + 6;
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
        if (ShardClient.appearance().clickSounds.get() && (target.key.startsWith("row:") || target.key.startsWith("sw:") || target.key.startsWith("set:")
                || target.key.startsWith("tab:") || target.key.startsWith("cat:") || target.key.startsWith("nav:") || target.key.startsWith("panel"))) {
            minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 1.0f, 0.25f));
        }
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
        if (previewDragging != null) {
            previewDragging.previewInput((int) Math.floor(mx), (int) Math.floor(my), previewButton, true);
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
        if (previewDragging != null) previewDragging = null;
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
        int step = (int) Math.round(sy * 56);
        if (embedded) {
            if (Render2D.hovered(mx, my, panelX, panelY, panelW, panelH)) panelScroll = Math.max(0, panelScroll - step);
            return true;
        }
        if (tab == Tab.MODS) {
            if (modSettingsOpen()) panelScroll = Math.max(0, panelScroll - step);
            else gridScroll = Math.max(0, gridScroll - step);
        } else pageScroll = Math.max(0, pageScroll - step);
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
                if (activeInput == search) searchEdited();
                return true;
            }
            return true;
        }

        switch (key) {
            case GLFW.GLFW_KEY_ESCAPE -> {
                if (!search.isEmpty()) clearSearch();
                else if (!embedded && panelTarget > 0) closePanel();
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
            else if ((tab == Tab.SETTINGS)) pageScroll = Math.max(0, pageScroll - delta);
            else panelScroll = Math.max(0, panelScroll - delta);
        } else if (h.y + h.h > h.clip[3]) {
            int delta = h.y + h.h - h.clip[3] + 8;
            if (h.key.startsWith("row:") || h.key.startsWith("res:")) gridScroll += delta;
            else if ((tab == Tab.SETTINGS)) pageScroll += delta;
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
            if (activeInput.charTyped(ch) && activeInput == search) searchEdited();
            return true;
        }
        // Type anywhere to search.
        if (!embedded && !ch.isEmpty() && Character.isLetterOrDigit(ch.codePointAt(0))) {
            focusInput(search, KEY_SEARCH, null);
            search.cursorToEnd();
            if (search.charTyped(ch)) searchEdited();
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
