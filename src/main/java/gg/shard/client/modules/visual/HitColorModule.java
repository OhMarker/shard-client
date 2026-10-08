package gg.shard.client.modules.visual;

import com.mojang.blaze3d.platform.NativeImage;
import gg.shard.client.ShardClient;
import gg.shard.client.mixin.OverlayTextureAccessor;
import gg.shard.client.module.Module;
import gg.shard.client.module.ModuleCategory;
import gg.shard.client.module.setting.ColorSetting;
import gg.shard.client.module.setting.IntSetting;
import gg.shard.client.util.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/**
 * Recolours the flash entities show when they take damage. Vanilla paints the top half of the
 * 16x16 overlay texture with 0xB2FF0000 (red at 70%); this module repaints that half with the
 * chosen colour and strength, and restores vanilla's values when switched off. The flash
 * duration trims how many of vanilla's 10 hurt ticks keep the tint.
 */
public final class HitColorModule extends Module {
    private static final int VANILLA_TINT = 0xB2FF0000;
    private static final int VANILLA_HURT_TICKS = 10;

    private final ColorSetting color = add(new ColorSetting("Colour", "Tint shown on hit", 0xFFFF3B3B, false));
    private final IntSetting strength = add(new IntSetting("Strength", "How strong the tint is", 70, 10, 100, 5, "%"));
    private final IntSetting flashTicks = add(new IntSetting("Flash duration", "How long the tint stays after a hit (vanilla is 10 ticks)", 10, 1, 10, 1, " ticks")
            .details("Shorter flashes make rapid hits easier to count."));

    private boolean painted;

    public HitColorModule() {
        super("Hit Color", "Pick the colour and length of the flash entities show when hit.", ModuleCategory.VISUALS);
        color.onChange(v -> repaint());
        strength.onChange(v -> repaint());
    }

    @Override
    public String about() {
        return "Changes the colour, strength and length of the damage flash on every entity by repainting the overlay texture vanilla uses. "
                + "Hits are detected exactly as vanilla detects them; nothing about damage is predicted or revealed.";
    }

    @Override
    public String icon() {
        return "glyph:drop";
    }

    @Override
    public List<String> conflictingMods() {
        return List.of("tostraights-customization");
    }

    /** False when the hurt tint should already be gone for this entity under the chosen duration. */
    public boolean showsOverlay(LivingEntity entity) {
        if (!isEnabled() || entity.hurtTime <= 0) return true;
        return entity.hurtTime > VANILLA_HURT_TICKS - flashTicks.get();
    }

    @Override
    protected void onEnable() {
        repaint();
    }

    @Override
    protected void onDisable() {
        paint(VANILLA_TINT);
    }

    private void repaint() {
        if (isEnabled()) paint(Colors.withAlpha(color.get(), Math.round(255 * strength.get() / 100f)));
    }

    private void paint(int argb) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameRenderer == null) return;
        OverlayTexture overlay = mc.gameRenderer.overlayTexture();
        if (!(overlay instanceof OverlayTextureAccessor accessor)) return;
        DynamicTexture texture = accessor.shard$texture();
        NativeImage image = texture.getPixels();
        if (image == null) return;
        if (argb == VANILLA_TINT && !painted) return;
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 16; x++) image.setPixel(x, y, argb);
        }
        texture.upload();
        painted = argb != VANILLA_TINT;
        ShardClient.LOGGER.debug("Hit overlay painted {}", Colors.toHex(argb));
    }
}
