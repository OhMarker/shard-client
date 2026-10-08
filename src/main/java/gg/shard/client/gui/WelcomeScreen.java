package gg.shard.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import gg.shard.client.ShardClient;
import gg.shard.client.input.Keybinds;
import gg.shard.client.util.Keys;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * First-run welcome: three skippable steps (a one-click setup, an accent colour, the key that
 * opens the menu). Shown once, the first time you join a world; Settings → Quick setup offers the
 * setups again.
 */
public final class WelcomeScreen extends DesignScreen {
    private static final int[] ACCENTS = {0xFF22D3EE, 0xFFF5F5F5, 0xFFF43F5E, 0xFFA78BFA, 0xFFA3E635, 0xFFFBBF24};
    private static final String[] ACCENT_NAMES = {"Shard cyan", "Mono", "Crimson", "Violet", "Lime", "Gold"};

    private record Hit(int x, int y, int w, int h, Runnable action) {
        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    private final List<Hit> hits = new ArrayList<>();
    private int step;
    private String chosen;
    private boolean listening;
    private int mx;
    private int my;

    public WelcomeScreen() {
        super(Component.literal("Welcome to Shard"));
    }

    /** Whether the welcome still needs showing. */
    public static boolean needed() {
        var gui = ShardClient.config().gui();
        return !gui.has("welcomed") || !gui.get("welcomed").getAsBoolean();
    }

    public static void markDone() {
        ShardClient.config().gui().addProperty("welcomed", true);
        ShardClient.config().markDirty();
        ShardClient.config().save();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (minecraft.level == null) renderMenuBackground(g);
        else renderBlurredBackground(g);
        g.fill(0, 0, width, height, Theme.overlay());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        beginFrame();
        hits.clear();
        mx = (int) toDesign(mouseX);
        my = (int) toDesign(mouseY);
        pushDesign(g);
        int w = Math.min(520, designW - 32);
        int h = 300;
        int x = (designW - w) / 2;
        int y = (designH - h) / 2;
        Render2D.shadow(g, x, y, w, h, Theme.radiusLarge(), 0.6);
        Render2D.panel(g, x, y, w, h, Theme.radiusLarge(), Theme.surface(), Theme.line());
        Icons.draw(g, "logo", x + 20, y + 20, 20, Theme.accent());
        String[] titles = {"Welcome to Shard", "Pick your colour", "Your menu key"};
        String[] subs = {"Start from a setup made for crystal PvP. You can change everything later.",
                "Used for switches, focus and highlights across Shard.", "Press it any time to open Shard."};
        Fonts.draw(g, titles[step], Fonts.Weight.SEMIBOLD, 18, x + 50, y + 18, Theme.text());
        Fonts.drawClipped(g, subs[step], Fonts.Weight.REGULAR, 11, x + 20, y + 50, w - 40, Theme.muted());
        for (int i = 0; i < 3; i++) Render2D.circle(g, x + w - 52 + i * 12, y + 28, 3, i == step ? Theme.accent() : Theme.lineStrong());

        int by = y + 74;
        switch (step) {
            case 0 -> {
                int cy = by;
                for (QuickSetup.Choice c : QuickSetup.choices()) {
                    boolean sel = c.name().equals(chosen);
                    option(g, x + 20, cy, w - 40, 50, sel, () -> chosen = sel ? null : c.name());
                    Icons.draw(g, c.icon(), x + 32, cy + 17, 16, sel ? Theme.accent() : Theme.muted());
                    Fonts.draw(g, c.name(), Fonts.Weight.SEMIBOLD, 12, x + 58, cy + 8, Theme.text());
                    Fonts.drawClipped(g, c.description(), Fonts.Weight.REGULAR, 10, x + 58, cy + 26, w - 100, Theme.muted());
                    cy += 56;
                }
            }
            case 1 -> {
                int cols = 3;
                int cw = (w - 40 - 16) / cols;
                for (int i = 0; i < ACCENTS.length; i++) {
                    int cx = x + 20 + (i % cols) * (cw + 8);
                    int cy = by + (i / cols) * 58;
                    int c = ACCENTS[i];
                    boolean sel = !ShardClient.appearance().launcherAccent.get() && ShardClient.appearance().accent.get() == c;
                    option(g, cx, cy, cw, 50, sel, () -> {
                        ShardClient.appearance().launcherAccent.set(false);
                        ShardClient.appearance().accent.set(c);
                    });
                    Render2D.circle(g, cx + 24, cy + 25, 10, c);
                    Fonts.draw(g, ACCENT_NAMES[i], Fonts.Weight.MEDIUM, 12, cx + 44, cy + 18, Theme.text());
                }
                boolean launcher = ShardClient.appearance().launcherAccent.get();
                option(g, x + 20, by + 120, w - 40, 32, launcher, () -> ShardClient.appearance().launcherAccent.set(true));
                Fonts.draw(g, "Use Shard Launcher's colour", Fonts.Weight.MEDIUM, 12, x + 34, by + 129, Theme.text());
            }
            default -> {
                String key = listening ? "Press a key..." : Keys.name(net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper.getBoundKeyOf(Keybinds.openGui).getValue());
                option(g, x + w / 2 - 110, by + 30, 220, 56, listening, () -> listening = !listening);
                Fonts.drawCentered(g, key, Fonts.Weight.SEMIBOLD, 18, x + w / 2, by + 46, listening ? Theme.warning() : Theme.text());
                Fonts.drawCentered(g, "Click, then press the key you want", Fonts.Weight.REGULAR, 11, x + w / 2, by + 100, Theme.subtle());
            }
        }

        int fy = y + h - 20 - 30;
        button(g, x + 20, fy, 80, 30, "Skip", false, this::finish);
        if (step > 0) button(g, x + w - 20 - 100 - 8 - 80, fy, 80, 30, "Back", false, () -> step--);
        button(g, x + w - 20 - 100, fy, 100, 30, step == 2 ? "Done" : "Next", true, this::next);
        popDesign(g);
    }

    private void option(GuiGraphics g, int x, int y, int w, int h, boolean selected, Runnable action) {
        hits.add(new Hit(x, y, w, h, action));
        boolean hover = mx >= x && mx < x + w && my >= y && my < y + h;
        Render2D.roundedRect(g, x, y, w, h, Theme.radius(), selected ? Theme.accentAlpha(0x22) : hover ? Theme.surfaceHover() : Theme.surfaceRaised());
        Render2D.roundedOutline(g, x, y, w, h, Theme.radius(), selected ? Theme.accentAlpha(0xC0) : Theme.line());
    }

    private void button(GuiGraphics g, int x, int y, int w, int h, String label, boolean primary, Runnable action) {
        hits.add(new Hit(x, y, w, h, action));
        boolean hover = mx >= x && mx < x + w && my >= y && my < y + h;
        int fill = primary ? (hover ? Theme.accentHover() : Theme.accent()) : hover ? Theme.surfaceHover() : Theme.surfaceRaised();
        Render2D.roundedRect(g, x, y, w, h, Theme.radiusSmall(), fill);
        if (!primary) Render2D.roundedOutline(g, x, y, w, h, Theme.radiusSmall(), Theme.lineStrong());
        Fonts.drawCentered(g, label, Fonts.Weight.MEDIUM, 12, x + w / 2, y + (h - Fonts.lineHeight(12)) / 2, primary ? Theme.accentText() : Theme.text());
    }

    private void next() {
        if (step == 0 && chosen != null) QuickSetup.apply(chosen);
        if (step < 2) step++;
        else finish();
    }

    private void finish() {
        listening = false;
        markDone();
        onClose();
    }

    /** Smoke test: show a step. */
    public void showStep(int s, String choice) {
        step = Math.max(0, Math.min(2, s));
        chosen = choice;
    }

    @Override
    protected boolean designClicked(double x, double y, int button, boolean doubleClick) {
        for (int i = hits.size() - 1; i >= 0; i--) {
            if (hits.get(i).contains(x, y)) {
                hits.get(i).action().run();
                return true;
            }
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (listening) {
            int key = event.key();
            if (key != GLFW.GLFW_KEY_ESCAPE) {
                KeyMapping open = Keybinds.openGui;
                open.setKey(InputConstants.Type.KEYSYM.getOrCreate(key));
                KeyMapping.resetMapping();
                minecraft.options.save();
            }
            listening = false;
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            finish();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(null);
    }
}
