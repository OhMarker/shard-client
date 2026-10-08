package gg.shard.client.modules.visual;

import gg.shard.client.module.Module;
import gg.shard.client.module.ModuleCategory;
import gg.shard.client.module.setting.BoolSetting;
import gg.shard.client.module.setting.ColorSetting;
import gg.shard.client.module.setting.IntSetting;
import gg.shard.client.util.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

/** Low Health Warning: a pulsing vignette (and optional heartbeat) when your health drops low. */
public final class LowHealthModule extends Module {
    private final IntSetting hearts = add(new IntSetting("Warn at", "Hearts at or below which the warning shows", 4, 1, 10, 1, " hearts"));
    private final ColorSetting color = add(new ColorSetting("Colour", "Colour of the edge glow", 0xFFEF4444, false));
    private final IntSetting strength = add(new IntSetting("Strength", "How strong the edge glow is", 45, 10, 100, 5, "%"));
    private final BoolSetting sound = add(new BoolSetting("Heartbeat", "A soft heartbeat while your health is low", false));
    private int beatTicks;

    public LowHealthModule() {
        super("Low Health Warning", "A pulsing red edge when your health is low.", ModuleCategory.VISUALS);
    }

    @Override
    public String icon() {
        return "heart";
    }

    @Override
    public String about() {
        return "Pulses a coloured glow around the screen edge when your own health (with absorption) falls to the chosen number of hearts, "
                + "and can play a soft heartbeat. Only reads your own health.";
    }

    private boolean low() {
        LocalPlayer p = Minecraft.getInstance().player;
        return p != null && !p.isDeadOrDying() && p.getHealth() + p.getAbsorptionAmount() <= hearts.get() * 2;
    }

    @Override
    public void onTick() {
        if (!sound.get() || !low()) {
            beatTicks = 0;
            return;
        }
        if (beatTicks-- <= 0) {
            beatTicks = 18;
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BASEDRUM.value(), 0.6f, 0.5f));
        }
    }

    /** Called from the HUD layer every frame. */
    public void render(GuiGraphics g) {
        if (!isEnabled() || !low()) return;
        float pulse = (float) (0.65 + 0.35 * Math.sin(System.currentTimeMillis() / 220.0));
        int edge = Colors.fade(color.get(), strength.get() / 100f * pulse);
        int clear = Colors.withAlpha(color.get(), 0);
        int w = g.guiWidth();
        int h = g.guiHeight();
        int d = Math.max(16, Math.min(w, h) / 6);
        g.fillGradient(0, 0, w, d, edge, clear);
        g.fillGradient(0, h - d, w, h, clear, edge);
        // Sides: vertical gradients only, so draw thin columns fading inwards.
        for (int i = 0; i < d; i += 2) {
            int c = Colors.fade(edge, 1f - i / (float) d);
            g.fill(i, 0, i + 2, h, c);
            g.fill(w - i - 2, 0, w - i, h, c);
        }
    }
}
