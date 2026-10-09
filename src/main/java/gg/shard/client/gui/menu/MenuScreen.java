package gg.shard.client.gui.menu;

import gg.shard.client.ShardClient;
import gg.shard.client.gui.DesignScreen;
import gg.shard.client.gui.Fonts;
import gg.shard.client.gui.Icons;
import gg.shard.client.gui.Render2D;
import gg.shard.client.gui.Scale;
import gg.shard.client.util.Colors;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;

/**
 * Base for Shard's title screen and server list: the mod menu's design language (neutral greys,
 * Inter, Lucide icons, rounded corners) drawn in design units at the mod menu's density, so text is
 * drawn 1:1 ({@link Scale#menuPixelsPerUnit}). Over the vanilla panorama, dimmed. Input goes through
 * "hits" registered while rendering, like the mod menu: the last one registered under the pointer
 * wins, so popovers drawn last take the click.
 */
public abstract class MenuScreen extends DesignScreen {
    // Palette (the mod menu's tokens).
    static final int BG = 0xFF141416;
    static final int SURFACE = 0xFF1A1A1D;
    static final int INPUT = 0xFF1C1C1F;
    static final int HOVER = 0xFF1F1F22;
    static final int ACTIVE = 0xFF232326;
    static final int BORDER = 0xFF232326;
    static final int BORDER_HOVER = 0xFF38383D;
    static final int TEXT = 0xFFEDEDED;
    static final int MUTED = 0xFF7C7C82;
    static final int SOFT = 0xFFA9A9AF;
    static final int SUBTLE = 0xFF55555B;
    static final int SUCCESS = 0xFF4ADE80;
    static final int WARNING = 0xFFFACC15;
    static final int DANGER = 0xFFF87171;
    static final float HOVER_MS = 120f;

    enum Style { PRIMARY, NORMAL, DANGER, GHOST }

    /** A clickable rectangle registered during rendering. */
    record Hit(String key, int x, int y, int w, int h, int[] clip, IntConsumer onClick) {
        boolean contains(double mx, double my) {
            if (!(mx >= x && mx < x + w && my >= y && my < y + h)) return false;
            return clip == null || (mx >= clip[0] && mx < clip[2] && my >= clip[1] && my < clip[3]);
        }
    }

    private final List<Hit> hits = new ArrayList<>();
    private final Map<String, Float> anims = new HashMap<>();
    protected final AccountSwitcher accounts = new AccountSwitcher(this);
    protected int mouseX = -1;
    protected int mouseY = -1;
    private int[] currentClip;
    private long lastClickMs;
    private String lastClickKey;
    /** Design units the layout needs; the density shrinks before the layout would not fit. */
    private final int needW;
    private final int needH;

    protected MenuScreen(Component title, int needW, int needH) {
        super(title);
        this.needW = needW;
        this.needH = needH;
    }

    @Override
    protected double pageScaleFor(int guiScale, double interfaceSize) {
        double perUnit = Scale.menuPixelsPerUnit(minecraft.getWindow().getWidth(), minecraft.getWindow().getHeight(), interfaceSize, needW, needH);
        return perUnit / Math.max(1, guiScale);
    }

    /** Panorama, blurred when {@link #blurBackground}, then a quiet dim. */
    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float partialTick) {
        if (minecraft.level == null) renderPanorama(g, partialTick);
        if (blurBackground() || minecraft.level != null) renderBlurredBackground(g);
        g.fill(0, 0, width, height, dimColor());
    }

    protected boolean blurBackground() {
        return false;
    }

    protected int dimColor() {
        return 0x66000000;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick) {
        beginFrame();
        mouseX = (int) Math.floor(toDesign(mx));
        mouseY = (int) Math.floor(toDesign(my));
        hits.clear();
        currentClip = null;
        accounts.tick();
        pushDesign(g);
        renderPage(g, partialTick);
        if (accounts.open()) {
            g.nextStratum();
            accounts.renderDropdown(g);
        }
        renderTooltip(g);
        popDesign(g);
    }

    /** Draws the page in design units; register every control with {@link #hit}. */
    protected abstract void renderPage(GuiGraphics g, float partialTick);

    // ---- input --------------------------------------------------------------------------------

    @Override
    protected boolean designClicked(double x, double y, int button, boolean doubleClick) {
        Hit hit = hitAt(x, y);
        if (accounts.open() && (hit == null || !hit.key().startsWith("acct"))) {
            accounts.close();
            return true;
        }
        if (hit == null) return clickedNothing(button);
        long now = System.currentTimeMillis();
        boolean dbl = doubleClick || (hit.key().equals(lastClickKey) && now - lastClickMs < 300);
        lastClickKey = hit.key();
        lastClickMs = now;
        hit.onClick().accept(dbl ? button | DOUBLE : button);
        playClick();
        return true;
    }

    /** Added to the button number passed to a hit's handler on a double click. */
    static final int DOUBLE = 0x100;

    protected boolean clickedNothing(int button) {
        return false;
    }

    protected void playClick() {
        minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0f));
    }

    // ---- hits, hover and motion -----------------------------------------------------------------

    protected void hit(String key, int x, int y, int w, int h, IntConsumer onClick) {
        hits.add(new Hit(key, x, y, w, h, currentClip, onClick));
    }

    private Hit hitAt(double mx, double my) {
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (h.contains(mx, my)) return h;
        }
        return null;
    }

    /** True when the pointer is over this rectangle and nothing registered later covers it. */
    protected boolean hovered(String key, int x, int y, int w, int h) {
        if (!Render2D.hovered(mouseX, mouseY, x, y, w, h)) return false;
        if (currentClip != null && !Render2D.hovered(mouseX, mouseY, currentClip[0], currentClip[1], currentClip[2] - currentClip[0], currentClip[3] - currentClip[1])) {
            return false;
        }
        return !accounts.open() || key.startsWith("acct");
    }

    /** Eased 0..1 hover amount for {@code key}. */
    protected float hoverAnim(String key, boolean hover) {
        return Render2D.easeInOut(anim(key + ":hov", hover ? 1f : 0f, HOVER_MS));
    }

    protected float anim(String key, float target, float durationMs) {
        float current = anims.getOrDefault(key, target);
        float next = Render2D.step(current, target, dt, durationMs);
        anims.put(key, next);
        return next;
    }

    protected void setAnim(String key, float value) {
        anims.put(key, value);
    }

    protected void clip(GuiGraphics g, int x, int y, int w, int h) {
        g.enableScissor(x, y, x + w, y + h);
        currentClip = new int[]{x, y, x + w, y + h};
    }

    protected void unclip(GuiGraphics g) {
        g.disableScissor();
        currentClip = null;
    }

    // ---- controls -----------------------------------------------------------------------------

    /** A rounded button with an optional leading icon; returns nothing, clicks go to {@code onClick}. */
    protected void button(GuiGraphics g, String key, int x, int y, int w, int h, String icon, String label, int textSize,
                          Style style, boolean enabled, IntConsumer onClick) {
        if (enabled) hit(key, x, y, w, h, onClick);
        float hov = hoverAnim(key, enabled && hovered(key, x, y, w, h));
        int fill;
        int border;
        int color;
        switch (style) {
            case PRIMARY -> {
                fill = Colors.mix(TEXT, 0xFFFFFFFF, hov);
                border = 0;
                color = BG;
            }
            case DANGER -> {
                fill = Colors.mix(Colors.withAlpha(DANGER, 0x1A), Colors.withAlpha(DANGER, 0x30), hov);
                border = Colors.withAlpha(DANGER, 0x40);
                color = DANGER;
            }
            case GHOST -> {
                fill = Colors.withAlpha(HOVER, Math.round(0xE0 * hov));
                border = 0;
                color = Colors.mix(SOFT, TEXT, hov);
            }
            default -> {
                fill = Colors.mix(Colors.withAlpha(SURFACE, 0xEB), Colors.withAlpha(ACTIVE, 0xFA), hov);
                border = Colors.mix(BORDER, BORDER_HOVER, hov);
                color = TEXT;
            }
        }
        if (!enabled) {
            fill = Colors.withAlpha(fill, Colors.alpha(fill) / 2);
            color = SUBTLE;
        }
        int r = 6;
        Render2D.roundedRect(g, x, y, w, h, r, fill);
        if (border != 0) Render2D.roundedOutline(g, x, y, w, h, r, border);
        int iconSize = textSize + 3;
        boolean hasLabel = label != null && !label.isEmpty();
        if (!hasLabel) {
            if (icon != null) Icons.draw(g, icon, x + (w - iconSize) / 2, y + (h - iconSize) / 2, iconSize, color);
            return;
        }
        int gap = icon == null ? 0 : iconSize + 8;
        String shown = Fonts.clip(label, Fonts.Weight.MEDIUM, textSize, w - 20 - gap);
        int tw = Fonts.widthInt(shown, Fonts.Weight.MEDIUM, textSize);
        int start = x + (w - tw - gap) / 2;
        if (icon != null) Icons.draw(g, icon, start, y + (h - iconSize) / 2, iconSize, style == Style.PRIMARY || !enabled ? color : Colors.mix(SOFT, TEXT, hov));
        Fonts.draw(g, shown, Fonts.Weight.MEDIUM, textSize, start + gap, y + (h - Fonts.lineHeight(textSize)) / 2, color);
    }

    /** Square icon-only button that lights up on hover. */
    protected void iconButton(GuiGraphics g, String key, String icon, int x, int y, int size, int iconSize, String tooltip, IntConsumer onClick) {
        hit(key, x, y, size, size, onClick);
        boolean hover = hovered(key, x, y, size, size);
        float hov = hoverAnim(key, hover);
        Render2D.roundedRect(g, x, y, size, size, 6, Colors.mix(Colors.withAlpha(SURFACE, 0xC8), Colors.withAlpha(ACTIVE, 0xFA), hov));
        Render2D.roundedOutline(g, x, y, size, size, 6, Colors.mix(BORDER, BORDER_HOVER, hov));
        Icons.draw(g, icon, x + (size - iconSize) / 2, y + (size - iconSize) / 2, iconSize, Colors.mix(SOFT, TEXT, hov));
        if (hover && tooltip != null) pendingTooltip = new Object[]{tooltip, x + size / 2, y};
    }

    /** One small label tooltip per frame, drawn by {@link #renderTooltip} above everything. */
    protected Object[] pendingTooltip;

    protected void renderTooltip(GuiGraphics g) {
        if (pendingTooltip == null) return;
        String text = (String) pendingTooltip[0];
        int cx = (int) pendingTooltip[1];
        int top = (int) pendingTooltip[2];
        pendingTooltip = null;
        int tw = Fonts.widthInt(text, Fonts.Weight.MEDIUM, 11) + 16;
        int th = 24;
        int x = Math.max(4, Math.min(designW - tw - 4, cx - tw / 2));
        int y = top - th - 6;
        if (y < 4) y = top + 6;
        g.nextStratum();
        Render2D.panel(g, x, y, tw, th, 6, SURFACE, BORDER_HOVER);
        Fonts.draw(g, text, Fonts.Weight.MEDIUM, 11, x + 8, y + (th - Fonts.lineHeight(11)) / 2, TEXT);
    }

    /**
     * A formatted Component (a server's MOTD) in Inter, keeping its colours, wrapped to
     * {@code maxWidth} and cut to {@code maxLines}.
     */
    protected void drawComponent(GuiGraphics g, Component text, int size, int x, int y, int maxWidth, int maxLines, int color) {
        if (text == null) return;
        Component styled = Fonts.smooth() ? Component.empty().append(text).withStyle(Fonts.style(Fonts.Weight.REGULAR, size)) : text;
        List<FormattedCharSequence> lines = font.split(styled, Math.max(1, maxWidth));
        int line = Fonts.lineHeight(size);
        for (int i = 0; i < Math.min(maxLines, lines.size()); i++) {
            int ly = y + i * line;
            if (Fonts.smooth()) g.drawString(font, lines.get(i), x, ly + Fonts.baseline(size) - 7, color, false);
            else g.drawString(font, lines.get(i), x, ly + (line - 8) / 2, color, false);
        }
    }

    /** Width of a Component drawn by {@link #drawComponent}. */
    protected int componentWidth(Component text, int size) {
        Component styled = Fonts.smooth() ? Component.empty().append(text).withStyle(Fonts.style(Fonts.Weight.REGULAR, size)) : text;
        return font.width(styled);
    }

    // ---- smoke test hooks ------------------------------------------------------------------------

    /** Centre of the control registered as {@code key} in the last frame, in design units, or null. */
    public int[] hitCentre(String key) {
        for (Hit h : hits) if (h.key().equals(key)) return new int[]{h.x() + h.w() / 2, h.y() + h.h() / 2};
        return null;
    }

    /** Clicks the control registered as {@code key} in the last frame; false when there is none. */
    public boolean clickHit(String key) {
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (h.key().equals(key)) {
                h.onClick().accept(0);
                return true;
            }
        }
        return false;
    }

    public float animValue(String key) {
        return anims.getOrDefault(key, -1f);
    }

    /** The pointer in design units as of the last frame (smoke test). */
    public int[] mouseDesign() {
        return new int[]{mouseX, mouseY};
    }

    public double pixelsPerUnitNow() {
        return Render2D.pixelsPerUnit();
    }

    public boolean accountsOpen() {
        return accounts.open();
    }

    public void toggleAccounts() {
        accounts.toggle();
    }

    static String shardVersion() {
        return net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer(ShardClient.MOD_ID)
                .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("dev");
    }
}
