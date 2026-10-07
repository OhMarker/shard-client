package gg.shard.client.modules.hud;

import gg.shard.client.gui.Render2D;
import gg.shard.client.gui.Theme;
import gg.shard.client.hud.HudModule;
import gg.shard.client.module.setting.BoolSetting;
import gg.shard.client.module.setting.ColorSetting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;

public final class KeystrokesModule extends HudModule {
    private static final int KEY = 20;
    private static final int GAP = 2;

    private final BoolSetting mouse = add(new BoolSetting("Mouse buttons", "Show LMB and RMB with CPS", true));
    private final BoolSetting space = add(new BoolSetting("Space bar", "Show the jump key", true));
    private final ColorSetting pressed = add(new ColorSetting("Pressed", "Fill colour while a key is held", 0xFF22D3EE));
    private final ColorSetting idle = add(new ColorSetting("Idle", "Fill colour while released", 0x80000000));

    public KeystrokesModule() {
        super("Keystrokes", "WASD, mouse buttons and space, lit up as you press them.", 0.01, 0.20);
    }

    @Override
    public boolean defaultEnabled() {
        return true;
    }

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        Options o = mc().options;
        int width = KEY * 3 + GAP * 2;
        int y = 0;
        key(g, KEY + GAP, y, KEY, KEY, "W", o.keyUp);
        y += KEY + GAP;
        key(g, 0, y, KEY, KEY, "A", o.keyLeft);
        key(g, KEY + GAP, y, KEY, KEY, "S", o.keyDown);
        key(g, (KEY + GAP) * 2, y, KEY, KEY, "D", o.keyRight);
        y += KEY + GAP;
        if (mouse.get()) {
            int half = (width - GAP) / 2;
            boolean l = mc().mouseHandler.isLeftPressed();
            boolean r = mc().mouseHandler.isRightPressed();
            box(g, 0, y, half, KEY - 4, "LMB", l);
            box(g, half + GAP, y, half, KEY - 4, "RMB", r);
            y += KEY - 4 + GAP;
        }
        if (space.get()) {
            box(g, 0, y, width, 8, "", o.keyJump.isDown());
            if (o.keyJump.isDown()) Render2D.fill(g, 4, y + 3, width - 8, 2, Theme.accentText());
            else Render2D.fill(g, 4, y + 3, width - 8, 2, 0x80FFFFFF);
            y += 8;
        }
        size(width, y);
    }

    private void key(GuiGraphics g, int x, int y, int w, int h, String label, KeyMapping mapping) {
        box(g, x, y, w, h, label, mapping.isDown());
    }

    private void box(GuiGraphics g, int x, int y, int w, int h, String label, boolean down) {
        Render2D.rounded(g, x, y, w, h, down ? pressed.get() : idle.get());
        if (!label.isEmpty()) {
            int color = down ? Theme.accentText() : Theme.text();
            Render2D.textCentered(g, font(), label, x + w / 2, y + (h - 8) / 2, color, false);
        }
    }

    @Override
    public String icon() {
        return "glyph:keys";
    }
}
