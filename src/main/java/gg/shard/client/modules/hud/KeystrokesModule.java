package gg.shard.client.modules.hud;

import gg.shard.client.gui.Fonts;
import gg.shard.client.gui.Render2D;
import gg.shard.client.hud.HudModule;
import gg.shard.client.hud.HudStyle;
import gg.shard.client.module.setting.BoolSetting;
import gg.shard.client.module.setting.ColorSetting;
import gg.shard.client.module.setting.EnumSetting;
import gg.shard.client.module.setting.IntSetting;
import gg.shard.client.util.Colors;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;

public final class KeystrokesModule extends HudModule {
    public enum Layout implements gg.shard.client.module.setting.Labeled {
        WASD("WASD"), ARROWS("Arrows");

        private final String label;

        Layout(String label) {
            this.label = label;
        }

        @Override
        public String label() {
            return label;
        }
    }

    private final java.util.Map<String, Float> fade = new java.util.HashMap<>();
    private long lastFrameNs;
    private float frameDt;

    private final EnumSetting<Layout> layout = add(new EnumSetting<>("Layout", "WASD shows the keys you bound; Arrows shows arrow symbols", Layout.WASD).group("Keys"));
    private final BoolSetting labels = add(new BoolSetting("Key labels", "Print the key name on each key", true).group("Keys"));
    private final IntSetting keySize = add(new IntSetting("Key size", "Size of one key", 20, 14, 36, 1, "").group("Keys"));
    private final IntSetting gap = add(new IntSetting("Gap", "Space between keys", 2, 0, 8, 1, "").group("Keys"));
    private final BoolSetting mouse = add(new BoolSetting("Mouse buttons", "Show LMB and RMB", true).group("Keys"));
    private final BoolSetting cps = add(new BoolSetting("CPS on mouse keys", "Show clicks per second on LMB and RMB instead of the key names", true));
    private final BoolSetting space = add(new BoolSetting("Space bar", "Show the jump key", true).group("Keys"));
    private final ColorSetting pressed = add(new ColorSetting("Pressed", "Fill colour while a key is held", 0xFF22D3EE).group("Colours"));
    private final ColorSetting idle = add(new ColorSetting("Idle", "Fill colour while released", 0x80000000).group("Colours"));

    public KeystrokesModule() {
        super("Keystrokes", "WASD, mouse buttons and space, lit up as you press them.", 0.01, 0.20);
    }

    @Override
    public String about() {
        return "Lights up the movement keys, mouse buttons and space as you press them, reading the same key state vanilla does. It shows input; it never generates any.";
    }

    @Override
    public boolean defaultEnabled() {
        return true;
    }

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        long now = System.nanoTime();
        frameDt = lastFrameNs == 0 ? 0f : Math.min(0.1f, (now - lastFrameNs) / 1e9f);
        lastFrameNs = now;
        Options o = mc().options;
        HudStyle.Resolved st = style();
        int k = keySize.get();
        int gp = gap.get();
        int pad = st.padding();
        int inner = k * 3 + gp * 2;
        int rowsH = k * 2 + gp;
        int mouseH = Math.max(12, k - 6);
        if (mouse.get()) rowsH += gp + mouseH;
        if (space.get()) rowsH += gp + 10;
        int w = inner + pad * 2;
        int h = rowsH + pad * 2;
        box(g, st, w, h);
        boolean arrows = layout.get() == Layout.ARROWS;
        int y = pad;
        key(g, st, pad + k + gp, y, k, k, arrows ? "↑" : name(o.keyUp, "W"), o.keyUp.isDown());
        y += k + gp;
        key(g, st, pad, y, k, k, arrows ? "←" : name(o.keyLeft, "A"), o.keyLeft.isDown());
        key(g, st, pad + k + gp, y, k, k, arrows ? "↓" : name(o.keyDown, "S"), o.keyDown.isDown());
        key(g, st, pad + (k + gp) * 2, y, k, k, arrows ? "→" : name(o.keyRight, "D"), o.keyRight.isDown());
        y += k + gp;
        if (mouse.get()) {
            int half = (inner - gp) / 2;
            boolean showCps = cps.get();
            key(g, st, pad, y, half, mouseH, showCps ? cpsLabel(gg.shard.client.input.ClickTracker.left(), half) : "LMB", mc().mouseHandler.isLeftPressed());
            key(g, st, pad + half + gp, y, inner - half - gp, mouseH, showCps ? cpsLabel(gg.shard.client.input.ClickTracker.right(), inner - half - gp) : "RMB", mc().mouseHandler.isRightPressed());
            y += mouseH + gp;
        }
        if (space.get()) {
            boolean down = o.keyJump.isDown();
            key(g, st, pad, y, inner, 10, "", down);
            Render2D.fill(g, pad + 6, y + 4, inner - 12, 2, down ? Colors.contrastText(pressed.get()) : Colors.withAlpha(st.text(), 0x80));
        }
        size(w, h);
    }

    private static String name(KeyMapping mapping, String fallback) {
        String n = mapping.getTranslatedKeyMessage().getString();
        if (n.isEmpty()) return fallback;
        return n.length() <= 3 ? n.toUpperCase() : n;
    }

    /** "12 CPS" when it fits the key, else just "12". */
    private static String cpsLabel(int cps, int keyW) {
        String full = cps + " CPS";
        return textW(full) <= keyW - 4 ? full : String.valueOf(cps);
    }

    private void key(GuiGraphics g, HudStyle.Resolved st, int x, int y, int w, int h, String label, boolean down) {
        // Presses light up at once and fade out over 120 ms, so fast taps are still visible.
        String id = x + ":" + y;
        float t = down ? 1f : Math.max(0f, fade.getOrDefault(id, 0f) - frameDt / 0.12f);
        fade.put(id, t);
        Render2D.roundedRect(g, x, y, w, h, Math.min(st.radius(), Math.min(w, h) / 2), Colors.mix(idle.get(), pressed.get(), t));
        if (labels.get() && !label.isEmpty()) {
            int color = t > 0.5f ? Colors.contrastText(pressed.get()) : st.text();
            String shown = Fonts.clip(label, WEIGHT, TEXT_SIZE, w - 4);
            Fonts.draw(g, shown, WEIGHT, TEXT_SIZE, x + (w - Fonts.widthInt(shown, WEIGHT, TEXT_SIZE)) / 2, y + (h - lineH()) / 2 + lineOffset(), color, st.shadow());
        }
    }

    @Override
    public String icon() {
        return "keystrokes";
    }
}
