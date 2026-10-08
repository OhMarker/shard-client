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
    public enum Layout { WASD, ARROWS }

    private final EnumSetting<Layout> layout = add(new EnumSetting<>("Layout", "WASD shows the keys you bound; Arrows shows arrow symbols", Layout.WASD).group("Keys"));
    private final BoolSetting labels = add(new BoolSetting("Key labels", "Print the key name on each key", true).group("Keys"));
    private final IntSetting keySize = add(new IntSetting("Key size", "Size of one key", 20, 14, 36, 1, "").group("Keys"));
    private final IntSetting gap = add(new IntSetting("Gap", "Space between keys", 2, 0, 8, 1, "").group("Keys"));
    private final BoolSetting mouse = add(new BoolSetting("Mouse buttons", "Show LMB and RMB", true).group("Keys"));
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
            key(g, st, pad, y, half, mouseH, "LMB", mc().mouseHandler.isLeftPressed());
            key(g, st, pad + half + gp, y, inner - half - gp, mouseH, "RMB", mc().mouseHandler.isRightPressed());
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

    private void key(GuiGraphics g, HudStyle.Resolved st, int x, int y, int w, int h, String label, boolean down) {
        Render2D.roundedRect(g, x, y, w, h, Math.min(st.radius(), Math.min(w, h) / 2), down ? pressed.get() : idle.get());
        if (labels.get() && !label.isEmpty()) {
            int color = down ? Colors.contrastText(pressed.get()) : st.text();
            String shown = Fonts.clip(label, WEIGHT, TEXT_SIZE, w - 4);
            Fonts.draw(g, shown, WEIGHT, TEXT_SIZE, x + (w - Fonts.widthInt(shown, WEIGHT, TEXT_SIZE)) / 2, y + (h - lineH()) / 2 + lineOffset(), color, st.shadow());
        }
    }

    @Override
    public String icon() {
        return "glyph:keys";
    }
}
