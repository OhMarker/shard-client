package gg.shard.client.gui;

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.IntConsumer;

/**
 * Shard's settings page: a sidebar of categories, a grid of mod cards with toggle switches and
 * a settings panel that slides in for the selected module. Everything is laid out in design
 * units (see {@link Scale}) on a 4/8/12/16/24 spacing grid and drawn with Inter, so the page
 * looks the same at every GUI scale; the layout collapses to tabs only on genuinely small
 * windows.
 *
 * <p>Input is routed through "hits": every control registers its rectangle while it renders,
 * so clicks, hover and keyboard focus all share one source of truth.
 */
public final class ClickGuiScreen extends DesignScreen {
    // ---- spacing grid (design units) ---------------------------------------------------------
    static final int PAD = 24;
    static final int SIDEBAR_W = 176;
    static final int SIDEBAR_ROW_H = 32;
    static final int SIDEBAR_GAP = 12;
    /** Below this page height the sidebar tightens its row gaps so every entry stays visible. */
    static final int COMPACT_HEIGHT = 420;
    static final long DETAILS_DELAY_MS = 600;
    static final int GRID_GAP = 16;
    static final int CARD_H = 76;
    static final int CARD_MIN_W = 220;
    static final int ICON_WELL = 40;
    static final int PANEL_W = 360;
    static final int PANEL_PAD = 20;
    static final int ROW_H = 32;
    static final int ROW_DESC_H = 48;
    static final int CONTROL_W = 160;
    static final int SECTION_ABOVE = 24;
    static final int SECTION_BELOW = 8;
    static final int SWITCH_W = 44;
    static final int SWITCH_H = 24;
    static final int BUTTON_H = 32;
    static final int FIELD_H = 32;
    static final int TOPBAR_H = 92;
    static final int SECTION_MAX_W = 600;
    // ---- typography ---------------------------------------------------------------------------
    static final int TITLE = 18;
    static final int SECTION = 14;
    static final int LABEL = 13;
    static final int DESC = 12;
    static final int HINT = 11;
    // ---- motion -------------------------------------------------------------------------------
    static final float HOVER_MS = 150f;
    static final float SWITCH_MS = 180f;
    static final float PANEL_MS = 200f;

    private static final String KEY_SEARCH = "search";
    private static final String KEY_PROFILE_NAME = "profile-name";
    private static final String KEY_PROFILE_DESC = "profile-desc";
    private static final String KEY_SERVER_PATTERN = "server-pattern";

    /** A clickable (and optionally focusable) rectangle registered during rendering. */
    private record Hit(String key, int x, int y, int w, int h, int[] clip, boolean focusable, IntConsumer onClick) {
        boolean contains(double mx, double my) {
            if (!(mx >= x && mx < x + w && my >= y && my < y + h)) return false;
            if (clip == null) return true;
            return mx >= clip[0] && mx < clip[2] && my >= clip[1] && my < clip[3];
        }
    }

    /** What the smoke test checks across GUI scales. */
    public record LayoutInfo(int designWidth, int designHeight, boolean narrow, int gridColumns, int cardWidth, int cardHeight,
                             int sidebarWidth, int panelWidth, boolean panelOpen, double pageScale) {}

    private final Screen parent;
    private ModuleCategory category = ModuleCategory.HUD;
    private boolean settingsPage;
    private final TextInput search = new TextInput(48).placeholder("Search mods");
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
    private boolean panelReplacesGrid;
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

    // ---- lifecycle -----------------------------------------------------------------------------

    @Override
    protected void init() {
        if (anims.isEmpty() && openProgress == 0f) {
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
        settingsPage = false;
        if (module != null && search.isEmpty() && module.category() != category) category = module.category();
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

    public Module openModule() {
        return panelTarget > 0 ? panelModule : null;
    }

    public LayoutInfo layoutInfo() {
        return new LayoutInfo(designW, designH, narrow, gridColumns, cardWidth, CARD_H, narrow ? 0 : SIDEBAR_W, panelW,
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
        // The settings panel renders after the grid, so a card under it must not light up.
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
        panelAnim = Render2D.step(panelAnim, panelTarget, dt, PANEL_MS);
        if (panelTarget == 0f && panelAnim == 0f) panelModule = null;
        mouseX = (int) Math.floor(toDesign(mx));
        mouseY = (int) Math.floor(toDesign(my));

        hits.clear();
        focusTargets.clear();
        cardOrder.clear();
        sliderTracks.clear();
        currentClip = null;
        hoverDetails = null;

        pushDesign(g);
        layout();
        if (narrow) renderTopBar(g);
        else renderSidebar(g);

        boolean hideGrid = panelReplacesGrid && panelAnim > 0.02f && !settingsPage;
        if (settingsPage) renderSettingsPage(g);
        else if (!hideGrid) renderGrid(g);
        if (!settingsPage && panelModule != null && panelAnim > 0.001f) renderPanel(g);

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
        if (narrow) {
            contentX = PAD;
            contentY = PAD + TOPBAR_H + GRID_GAP;
            contentW = designW - 2 * PAD;
            contentH = designH - contentY - PAD;
        } else {
            contentX = PAD + SIDEBAR_W + PAD;
            contentY = PAD;
            contentW = designW - contentX - PAD;
            contentH = designH - 2 * PAD;
        }
        panelReplacesGrid = narrow || contentW - PANEL_W - GRID_GAP < CARD_MIN_W;
        panelW = panelReplacesGrid ? contentW : PANEL_W;
        panelY = contentY;
        panelH = contentH;
        float eased = Render2D.easeOut(panelAnim);
        panelX = contentX + contentW - panelW + Math.round((1f - eased) * (panelW + PAD));
        gridColumns = Scale.gridColumns(contentW, CARD_MIN_W, GRID_GAP);
        cardWidth = Scale.cardWidth(contentW, gridColumns, GRID_GAP);
    }

    private boolean panelCovers(double mx, double my) {
        return panelModule != null && panelAnim > 0.001f && !settingsPage && Render2D.hovered(mx, my, panelX, panelY, panelW, panelH);
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

    // ---- sidebar / top bar ---------------------------------------------------------------------

    private void renderSidebar(GuiGraphics g) {
        int x = PAD;
        int y = PAD;
        int w = SIDEBAR_W;
        int h = designH - 2 * PAD;
        int r = Theme.radiusLarge();
        Render2D.panel(g, x, y, w, h, r, Theme.surface(), Theme.line());
        hit("sidebar", x, y, w, h, false, b -> {});

        // Header block: logo, name, version.
        Icons.draw(g, "logo", x + 16, y + 16, 32, Theme.accent());
        Fonts.draw(g, "Shard", Fonts.Weight.SEMIBOLD, SECTION, x + 56, y + 14, Theme.text());
        Fonts.drawClipped(g, "v" + version(), Fonts.Weight.REGULAR, HINT, x + 56, y + 14 + Fonts.lineHeight(SECTION), w - 72, Theme.subtle());

        boolean compact = designH < COMPACT_HEIGHT;
        int rowH = compact ? 28 : SIDEBAR_ROW_H;
        int rowGap = compact ? 4 : SIDEBAR_GAP;
        int edge = compact ? 8 : 12;
        int searchY = y + (compact ? 56 : 64);
        renderSearch(g, x + 12, searchY, w - 24, FIELD_H);

        int listY = searchY + FIELD_H + (compact ? 8 : SIDEBAR_GAP);
        int bottomReserved = edge + BUTTON_H + edge;
        int listH = h - (listY - y) - bottomReserved;
        clip(g, x, listY, w, listH);
        int cy = listY - sidebarScroll;
        for (ModuleCategory c : categories()) {
            renderCategoryRow(g, "cat:" + c.name(), c.displayName(), Icons.categoryGlyph(c), countLabel(c),
                    !settingsPage && c == category, x + 12, cy, w - 24, rowH, () -> selectCategory(c));
            cy += rowH + rowGap;
        }
        g.fill(x + 16, cy, x + w - 16, cy + 1, Theme.line());
        cy += rowGap + 1;
        renderCategoryRow(g, "cat:settings", "Settings", "settings", "", settingsPage, x + 12, cy, w - 24, rowH, this::selectSettings);
        cy += rowH;
        int contentHeight = cy + sidebarScroll - listY;
        sidebarScroll = Math.max(0, Math.min(sidebarScroll, contentHeight - listH));
        unclip(g);

        boolean canEdit = minecraft.player != null;
        button(g, "hud-editor", x + 12, y + h - edge - BUTTON_H, w - 24, BUTTON_H, "HUD editor", false, canEdit, b -> {
            if (canEdit) minecraft.setScreen(new HudEditorScreen(this, ShardClient.hud()));
        });
    }

    private String countLabel(ModuleCategory c) {
        int on = 0;
        List<Module> list = ShardClient.modules().byCategory(c);
        for (Module m : list) if (m.isEnabled()) on++;
        return on + "/" + list.size();
    }

    private void renderCategoryRow(GuiGraphics g, String key, String label, String glyph, String count, boolean selected,
                                   int x, int y, int w, int h, Runnable onSelect) {
        Hit probe = new Hit(key, x, y, w, h, currentClip, true, b -> onSelect.run());
        hits.add(probe);
        boolean hover = hoverable(probe);
        float sel = Render2D.easeInOut(anim(key + ":sel", selected ? 1f : 0f, HOVER_MS));
        float hov = Render2D.easeInOut(anim(key + ":hov", hover ? 1f : 0f, HOVER_MS));
        int fill = Colors.mix(Colors.mix(0x00000000, Theme.surfaceHover(), hov), Theme.surfaceRaised(), sel);
        if (Colors.alpha(fill) > 4) Render2D.roundedRect(g, x, y, w, h, Theme.radiusSmall(), fill);
        if (focused(key)) Render2D.roundedOutline(g, x, y, w, h, Theme.radiusSmall(), Theme.accentAlpha(0xA0));
        int textColor = selected || hover ? Theme.text() : Theme.muted();
        int glyphColor = selected ? Theme.accent() : Theme.muted();
        Icons.draw(g, glyph, x + 8, y + (h - 16) / 2, glyphColor);
        int countW = count.isEmpty() ? 0 : Fonts.widthInt(count, Fonts.Weight.MEDIUM, HINT) + 12;
        Fonts.drawClipped(g, label, Fonts.Weight.MEDIUM, LABEL, x + 32, y + (h - Fonts.lineHeight(LABEL)) / 2, w - 32 - countW - 8, textColor);
        if (!count.isEmpty()) Fonts.drawRight(g, count, Fonts.Weight.MEDIUM, HINT, x + w - 8, y + (h - Fonts.lineHeight(HINT)) / 2, Theme.subtle());
    }

    private void renderTopBar(GuiGraphics g) {
        int x = PAD;
        int y = PAD;
        int w = designW - 2 * PAD;
        int r = Theme.radiusLarge();
        Render2D.panel(g, x, y, w, TOPBAR_H, r, Theme.surface(), Theme.line());
        hit("topbar", x, y, w, TOPBAR_H, false, b -> {});
        Icons.draw(g, "logo", x + 16, y + 12, 32, Theme.accent());
        Fonts.draw(g, "Shard", Fonts.Weight.SEMIBOLD, SECTION, x + 56, y + 12 + (32 - Fonts.lineHeight(SECTION)) / 2, Theme.text());
        int searchX = x + 56 + Fonts.widthInt("Shard", Fonts.Weight.SEMIBOLD, SECTION) + 16;
        renderSearch(g, searchX, y + 12, x + w - 16 - searchX, FIELD_H);

        int tabY = y + 12 + FIELD_H + 12;
        int tabH = 28;
        int tx = x + 12;
        List<ModuleCategory> cats = categories();
        int hudW = Fonts.widthInt("HUD", Fonts.Weight.MEDIUM, DESC) + 24;
        // Use short labels when the full ones would collide with the HUD button.
        int available = w - 24 - hudW - 8;
        int needed = Fonts.widthInt("Settings", Fonts.Weight.MEDIUM, DESC) + 24;
        for (ModuleCategory c : cats) needed += Fonts.widthInt(c.displayName(), Fonts.Weight.MEDIUM, DESC) + 24;
        boolean compact = needed > available;
        for (ModuleCategory c : cats) {
            String label = compact ? compactName(c) : c.displayName();
            tx += renderTab(g, "cat:" + c.name(), label, !settingsPage && c == category, tx, tabY, tabH, () -> selectCategory(c));
        }
        renderTab(g, "cat:settings", compact ? "More" : "Settings", settingsPage, tx, tabY, tabH, this::selectSettings);
        boolean canEdit = minecraft.player != null;
        button(g, "hud-editor", x + w - 12 - hudW, tabY, hudW, tabH, "HUD", false, canEdit, b -> {
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

    private int renderTab(GuiGraphics g, String key, String label, boolean selected, int x, int y, int h, Runnable onSelect) {
        int w = Fonts.widthInt(label, Fonts.Weight.MEDIUM, DESC) + 24;
        Hit probe = new Hit(key, x, y, w, h, currentClip, true, b -> onSelect.run());
        hits.add(probe);
        boolean hover = hoverable(probe);
        float sel = Render2D.easeInOut(anim(key + ":sel", selected ? 1f : 0f, HOVER_MS));
        int fill = Colors.mix(hover ? Theme.surfaceHover() : 0x00000000, Theme.surfaceRaised(), sel);
        if (Colors.alpha(fill) > 4) Render2D.roundedRect(g, x, y, w, h, Theme.radiusSmall(), fill);
        if (focused(key)) Render2D.roundedOutline(g, x, y, w, h, Theme.radiusSmall(), Theme.accentAlpha(0xA0));
        Fonts.draw(g, label, Fonts.Weight.MEDIUM, DESC, x + 12, y + (h - Fonts.lineHeight(DESC)) / 2, selected ? Theme.text() : Theme.muted());
        return w + 4;
    }

    private void renderSearch(GuiGraphics g, int x, int y, int w, int h) {
        boolean isFocused = activeInput == search;
        search.render(g, x, y, w, h, isFocused);
        Icons.draw(g, "search", x + w - 12 - 16, y + (h - 16) / 2, isFocused ? Theme.accent() : Theme.subtle());
        if (focused(KEY_SEARCH) && !isFocused) Render2D.roundedOutline(g, x, y, w, h, Theme.radiusSmall(), Theme.accentAlpha(0xA0));
        textHit(KEY_SEARCH, search, x, y, w, h, null);
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
        popover = null;
        listeningKey = null;
        listeningModule = null;
        blurInput();
    }

    // ---- grid ----------------------------------------------------------------------------------

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

    private int headerHeight() {
        return Fonts.lineHeight(TITLE) + 2 + Fonts.lineHeight(DESC) + GRID_GAP;
    }

    private void renderPageHeader(GuiGraphics g, String title, String sub) {
        Fonts.drawClipped(g, title, Fonts.Weight.SEMIBOLD, TITLE, contentX, contentY, contentW, Theme.text());
        Fonts.drawClipped(g, sub, Fonts.Weight.REGULAR, DESC, contentX, contentY + Fonts.lineHeight(TITLE) + 2, contentW, Theme.muted());
    }

    private void renderGrid(GuiGraphics g) {
        List<Module> modules = visibleModules();
        boolean searching = !search.isEmpty();
        String title = searching ? "Results for \"" + search.value().trim() + "\"" : category.displayName();
        String sub = searching ? modules.size() + (modules.size() == 1 ? " match" : " matches") : category.description();
        renderPageHeader(g, title, sub);

        int gridY = contentY + headerHeight();
        int gridH = contentH - headerHeight();
        int rows = (modules.size() + gridColumns - 1) / gridColumns;
        int contentHeight = rows * (CARD_H + GRID_GAP);
        gridScroll = Math.max(0, Math.min(gridScroll, contentHeight - gridH));

        if (modules.isEmpty()) {
            Icons.draw(g, "search", contentX + contentW / 2 - 16, gridY + 40, 32, Theme.subtle());
            Fonts.drawCentered(g, "Nothing matches", Fonts.Weight.MEDIUM, LABEL, contentX + contentW / 2, gridY + 88, Theme.muted());
            return;
        }

        clip(g, contentX, gridY, contentW, gridH);
        for (int i = 0; i < modules.size(); i++) {
            Module m = modules.get(i);
            int col = i % gridColumns;
            int row = i / gridColumns;
            int cx = contentX + col * (cardWidth + GRID_GAP);
            int cy = gridY + row * (CARD_H + GRID_GAP) - gridScroll;
            cardOrder.add("card:" + m.key());
            if (cy + CARD_H < gridY || cy > gridY + gridH) continue;
            renderCard(g, m, cx, cy, cardWidth);
        }
        unclip(g);
    }

    private void renderCard(GuiGraphics g, Module m, int x, int y, int w) {
        String key = "card:" + m.key();
        int r = Theme.radius();
        Hit card = new Hit(key, x, y, w, CARD_H, currentClip, true, b -> {
            if (b == GLFW.GLFW_MOUSE_BUTTON_RIGHT) m.toggle();
            else openModule(m);
        });
        hits.add(card);
        boolean hover = hoverable(card) || (popover == null && focused(key));
        float hov = Render2D.easeInOut(anim(key + ":hov", hover ? 1f : 0f, HOVER_MS));
        boolean open = panelModule == m && panelTarget > 0;

        int fill = Colors.mix(Theme.surfaceRaised(), Theme.surfaceHover(), hov);
        Render2D.shadow(g, x, y, w, CARD_H, r, 0.35 + 0.45 * hov);
        Render2D.roundedRect(g, x, y, w, CARD_H, r, fill);
        int border = open ? Theme.accentAlpha(0x80) : Colors.mix(Theme.line(), Theme.lineStrong(), hov);
        if (focused(key)) border = Theme.accentAlpha(0xC0);
        Render2D.roundedOutline(g, x, y, w, CARD_H, r, border);

        // Icon in a tinted square.
        int wellX = x + 16;
        int wellY = y + (CARD_H - ICON_WELL) / 2;
        Render2D.roundedRect(g, wellX, wellY, ICON_WELL, ICON_WELL, Theme.radiusSmall() + 2, m.isEnabled() ? Theme.accentAlpha(0x22) : Theme.iconWell());
        Icons.draw(g, m, wellX + 4, wellY + 4, 32, m.isEnabled() ? Theme.accent() : Theme.muted());

        int switchX = x + w - 16 - SWITCH_W;
        int textX = wellX + ICON_WELL + 16;
        int textW = switchX - 16 - textX;
        int nameY = y + 12;
        Fonts.drawClipped(g, m.name(), Fonts.Weight.SEMIBOLD, SECTION, textX, nameY, textW, Theme.text());
        int descY = nameY + Fonts.lineHeight(SECTION) + 2;
        String notice = notice(m);
        if (notice != null) {
            Icons.draw(g, "warning", textX, descY, Theme.warning());
            Fonts.drawClipped(g, notice, Fonts.Weight.REGULAR, DESC, textX + 20, descY, textW - 20, Theme.warning());
        } else {
            List<String> lines = Fonts.wrap(m.description(), Fonts.Weight.REGULAR, DESC, textW);
            for (int i = 0; i < Math.min(2, lines.size()); i++) {
                String line = i == 1 && lines.size() > 2 ? Fonts.clip(lines.get(1) + " " + lines.get(2), Fonts.Weight.REGULAR, DESC, textW) : lines.get(i);
                Fonts.draw(g, line, Fonts.Weight.REGULAR, DESC, textX, descY + i * Fonts.lineHeight(DESC), Theme.muted());
            }
        }

        renderSwitch(g, "sw:" + m.key(), switchX, y + (CARD_H - SWITCH_H) / 2, m.isToggledOn(), notice == null, m::toggle);
    }

    private static String notice(Module m) {
        if (m.blockedBy() != null) return "Off: " + m.blockedBy() + " is installed";
        if (m.isSuppressed()) return "Off on this server";
        return null;
    }

    // ---- controls ------------------------------------------------------------------------------

    private void renderSwitch(GuiGraphics g, String key, int x, int y, boolean on, boolean active, Runnable onToggle) {
        Hit probe = new Hit(key, x - 4, y - 4, SWITCH_W + 8, SWITCH_H + 8, currentClip, true, b -> onToggle.run());
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
            fill = Colors.mix(Colors.withAlpha(Theme.danger(), 0x22), Colors.withAlpha(Theme.danger(), 0x3A), hov);
            color = Theme.danger();
        } else {
            fill = Colors.mix(Theme.control(), Theme.controlHover(), hov);
            color = enabled ? Theme.text() : Theme.subtle();
        }
        if (!enabled) fill = Colors.withAlpha(fill, 0x70);
        Render2D.roundedRect(g, x, y, w, h, r, fill);
        int outline = focused(key) ? Theme.accentAlpha(0xC0) : primary ? 0 : danger ? Colors.withAlpha(Theme.danger(), 0x50) : Theme.line();
        Render2D.roundedOutline(g, x, y, w, h, r, outline);
        if (!label.isEmpty()) {
            Fonts.drawCentered(g, Fonts.clip(label, Fonts.Weight.MEDIUM, LABEL, w - 16), Fonts.Weight.MEDIUM, LABEL, x + w / 2, y + (h - Fonts.lineHeight(LABEL)) / 2, color);
        }
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

    // ---- settings panel ------------------------------------------------------------------------

    private void renderPanel(GuiGraphics g) {
        Module m = panelModule;
        int r = Theme.radiusLarge();
        int x = panelX;
        int y = panelY;
        int w = panelW;
        int h = panelH;
        clip(g, contentX, contentY, contentW, contentH);
        Render2D.shadow(g, x, y, w, h, r, 0.6);
        Render2D.panel(g, x, y, w, h, r, Theme.surface(), Theme.lineStrong());
        hit("panel", x, y, w, h, false, b -> {});

        // Header: back/close, icon, name, about paragraph, notice.
        int innerX = x + PANEL_PAD;
        int innerW = w - 2 * PANEL_PAD;
        String backGlyph = panelReplacesGrid ? "back" : "close";
        int bx = panelReplacesGrid ? innerX : x + w - PANEL_PAD - 32;
        button(g, "panel-close", bx, y + 16, 32, 32, "", false, true, b -> closePanel());
        Icons.draw(g, backGlyph, bx + 8, y + 24, Theme.muted());
        int wellX = panelReplacesGrid ? innerX + 32 + 12 : innerX;
        Render2D.roundedRect(g, wellX, y + 12, ICON_WELL, ICON_WELL, Theme.radiusSmall() + 2, m.isEnabled() ? Theme.accentAlpha(0x22) : Theme.iconWell());
        Icons.draw(g, m, wellX + 4, y + 16, 32, m.isEnabled() ? Theme.accent() : Theme.muted());
        int titleX = wellX + ICON_WELL + 12;
        int titleW = (panelReplacesGrid ? x + w - PANEL_PAD : bx - 12) - titleX;
        Fonts.drawClipped(g, m.name(), Fonts.Weight.SEMIBOLD, TITLE, titleX, y + 12 + (ICON_WELL - Fonts.lineHeight(TITLE)) / 2, titleW, Theme.text());

        int cy = y + 12 + ICON_WELL + 12;
        for (String line : Fonts.wrap(m.about(), Fonts.Weight.REGULAR, DESC, innerW)) {
            Fonts.draw(g, line, Fonts.Weight.REGULAR, DESC, innerX, cy, Theme.muted());
            cy += Fonts.lineHeight(DESC);
        }
        String notice = notice(m);
        if (notice != null) {
            cy += 4;
            Icons.draw(g, "warning", innerX, cy, Theme.warning());
            for (String line : Fonts.wrap(notice, Fonts.Weight.REGULAR, DESC, innerW - 24)) {
                Fonts.draw(g, line, Fonts.Weight.REGULAR, DESC, innerX + 24, cy, Theme.warning());
                cy += Fonts.lineHeight(DESC);
            }
        }
        if (m instanceof PanelPreview preview) {
            int used = preview.renderPreview(g, innerX, cy + 8, innerW);
            if (used > 0) cy += used + 8;
        }
        cy += 12;
        g.fill(x + 1, cy, x + w - 1, cy + 1, Theme.line());
        cy += 1;

        // Scrollable rows.
        int rowsTop = cy;
        int rowsH = y + h - rowsTop - 1;
        clip(g, x + 1, rowsTop, w - 2, rowsH);
        int ry = rowsTop + 8 - panelScroll;

        ry += switchRow(g, "panel-enabled", "Enabled", notice != null ? "Blocked here, see above" : "", innerX, ry, innerW, m.isToggledOn(), notice == null, m::toggle);
        ry += keybindRow(g, "kb:" + m.key(), "Keybind", "Toggle the module in-game", Keys.name(m.keybind()), listeningModule == m, innerX, ry, innerW,
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
        ry += 8;
        button(g, "panel-reset", innerX, ry, Math.min(innerW, 176), BUTTON_H, "Reset to defaults", false, true, b -> {
            for (Setting<?> s : m.settings()) s.reset();
            stringInputs.clear();
            numericInputs.clear();
            showToast(m.name() + " reset");
        });
        ry += BUTTON_H + 8;

        ry = renderSettingRows(g, m, m.settings(), innerX, ry, innerW);
        ry += PANEL_PAD;
        int contentHeight = ry + panelScroll - rowsTop;
        panelScroll = Math.max(0, Math.min(panelScroll, contentHeight - rowsH));
        unclip(g);
        // Leave the outer (content area) scissor as well.
        g.disableScissor();
        currentClip = null;
    }

    /** Grouped setting rows with section titles; returns the y after the last row. */
    private int renderSettingRows(GuiGraphics g, Module m, List<Setting<?>> settings, int x, int y, int w) {
        String lastGroup = null;
        boolean first = true;
        for (Setting<?> s : settings) {
            if (!s.isVisible()) continue;
            if (!s.group().isEmpty() && !s.group().equals(lastGroup)) {
                y += first ? 8 : SECTION_ABOVE;
                Fonts.draw(g, s.group(), Fonts.Weight.SEMIBOLD, SECTION, x, y, Theme.text());
                y += Fonts.lineHeight(SECTION) + SECTION_BELOW;
                lastGroup = s.group();
            } else if (s.group().isEmpty() && lastGroup != null) {
                y += 8;
                lastGroup = null;
            }
            y += renderSettingRow(g, m, s, x, y, w);
            first = false;
        }
        return y;
    }

    private interface ControlRenderer {
        void render(int controlX, int controlY, int controlW, int controlH);
    }

    /** Label on the left (with an optional description below it), control right-aligned in a 160 column. */
    private int row(GuiGraphics g, String label, String description, String details, int x, int y, int w, ControlRenderer control) {
        boolean hasDesc = description != null && !description.isEmpty();
        int controlW = Math.min(w / 2, CONTROL_W);
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
        return row(g, label, description, null, x, y, w, (cx, cy, cw, ch) ->
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
            return row(g, s.name(), s.description(), s.details(), x, y, w, (cx, cy, cw, ch) ->
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

    private int section(GuiGraphics g, String glyph, String title, String sub, int x, int y, int w, SectionBody body) {
        int pad = PANEL_PAD;
        int innerX = x + pad;
        int innerW = w - 2 * pad;
        int headerH = pad + ICON_WELL + 12;
        // Body first (measures), then the card behind it: draw the card into a measured height by
        // rendering body twice would double hits, so the card is drawn with a lower stratum trick:
        // we render the panel background now with the height from the previous frame.
        String cacheKey = "section-h:" + title;
        int cachedH = Math.round(anims.getOrDefault(cacheKey, (float) (headerH + 40)));
        Render2D.panel(g, x, y, w, cachedH, Theme.radius(), Theme.surfaceRaised(), Theme.line());
        Render2D.roundedRect(g, innerX, y + pad, ICON_WELL, ICON_WELL, Theme.radiusSmall() + 2, Theme.accentAlpha(0x22));
        Icons.draw(g, glyph, innerX + 4, y + pad + 4, 32, Theme.accent());
        Fonts.drawClipped(g, title, Fonts.Weight.SEMIBOLD, SECTION, innerX + ICON_WELL + 12, y + pad + 2, innerW - ICON_WELL - 12, Theme.text());
        Fonts.drawClipped(g, sub, Fonts.Weight.REGULAR, DESC, innerX + ICON_WELL + 12, y + pad + 2 + Fonts.lineHeight(SECTION), innerW - ICON_WELL - 12, Theme.muted());
        int end = body.render(innerX, y + headerH, innerW);
        int h = end - y + pad;
        anims.put(cacheKey, (float) h);
        return h;
    }

    private void renderSettingsPage(GuiGraphics g) {
        renderPageHeader(g, "Settings", "Appearance, HUD, keybinds, profiles, server rules, export and reset");
        int top = contentY + headerHeight();
        int viewH = contentH - headerHeight();
        clip(g, contentX, top, contentW, viewH);
        int y = top - pageScroll;
        int sectionW = Math.min(contentW, SECTION_MAX_W);
        int x = contentX;

        y += section(g, "visuals", "Appearance", "Accent, interface size, font, blur and motion", x, y, sectionW,
                (ix, iy, iw) -> renderSettingRows(g, ShardClient.appearance(), ShardClient.appearance().settings(), ix, iy, iw)) + GRID_GAP;
        y += section(g, "hud", "HUD", "Scale and the style every HUD element inherits", x, y, sectionW,
                (ix, iy, iw) -> renderSettingRows(g, ShardClient.hudDefaults(), ShardClient.hudDefaults().settings(), ix, iy, iw)) + GRID_GAP;
        y += section(g, "keys", "Keybinds", "Every module keybind in one list, with conflict warnings", x, y, sectionW, this::renderKeybindsBody) + GRID_GAP;
        y += section(g, "folder", "Profiles", "Save the whole config under a name and switch between them", x, y, sectionW, this::renderProfilesBody) + GRID_GAP;
        y += section(g, "globe", "Server rules", "Modules kept off on matching addresses; * matches subdomains", x, y, sectionW, this::renderServersBody) + GRID_GAP;
        y += section(g, "generic", "Export and import", "Copy the config as JSON to the clipboard, or paste one back", x, y, sectionW, this::renderExportBody) + GRID_GAP;
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
                Icons.draw(g, "warning", x, rowY + 6 + Fonts.lineHeight(LABEL), Theme.warning());
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

    private void applySlider(double mx) {
        double frac = slideW <= 0 ? 0 : (mx - slideX) / (double) slideW;
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
        if (!narrow && Render2D.hovered(mx, my, PAD, PAD, SIDEBAR_W, designH - 2 * PAD)) {
            sidebarScroll = Math.max(0, sidebarScroll - step);
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
            if (h.key.startsWith("card:")) gridScroll = Math.max(0, gridScroll - delta);
            else if (settingsPage) pageScroll = Math.max(0, pageScroll - delta);
            else panelScroll = Math.max(0, panelScroll - delta);
        } else if (h.y + h.h > h.clip[3]) {
            int delta = h.y + h.h - h.clip[3] + 8;
            if (h.key.startsWith("card:")) gridScroll += delta;
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
        return super.charTyped(event);
    }
}
