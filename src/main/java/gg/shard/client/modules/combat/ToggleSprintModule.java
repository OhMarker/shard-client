package gg.shard.client.modules.combat;

import gg.shard.client.gui.Render2D;
import gg.shard.client.gui.Theme;
import gg.shard.client.hud.HudModule;
import gg.shard.client.module.ModuleCategory;
import gg.shard.client.module.setting.BoolSetting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;

/**
 * Keeps the sprint key held for you, like the vanilla "Sprint: Toggle" option without the
 * double-tap, and shows a small status line. It never moves you faster than vanilla sprinting.
 */
public final class ToggleSprintModule extends HudModule {
    private final BoolSetting showStatus = add(new BoolSetting("Status text", "Show [Sprinting (Toggled)] on screen", true));
    private final BoolSetting onlyWhenMoving = add(new BoolSetting("Only while moving", "Release sprint when not walking forward", true));

    public ToggleSprintModule() {
        super("Toggle Sprint", "Hold sprint for you while the module is on.", ModuleCategory.COMBAT, 0.01, 0.97);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null) return;
        boolean moving = !onlyWhenMoving.get() || p.input.keyPresses.forward();
        if (moving && !p.isUsingItem()) mc.options.keySprint.setDown(true);
    }

    @Override
    protected void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.options.keySprint.setDown(false);
    }

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        if (!showStatus.get()) {
            size(1, 1);
            return;
        }
        LocalPlayer p = mc().player;
        String text = p != null && p.isSprinting() ? "[Sprinting (Toggled)]" : "[Sprint (Toggled)]";
        Render2D.text(g, font(), text, 0, 0, Theme.muted(), true);
        size(font().width(text), 10);
    }

    @Override
    public String icon() {
        return "glyph:sprint";
    }

    @Override
    public java.util.List<String> conflictingMods() {
        return java.util.List.of("sprintbydefault");
    }
}
