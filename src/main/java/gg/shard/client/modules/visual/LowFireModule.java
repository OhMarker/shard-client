package gg.shard.client.modules.visual;

import gg.shard.client.gui.Fonts;
import gg.shard.client.gui.PanelPreview;
import gg.shard.client.gui.Render2D;
import gg.shard.client.gui.Theme;
import gg.shard.client.module.Module;
import gg.shard.client.util.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBakery;
import gg.shard.client.module.ModuleCategory;
import gg.shard.client.module.setting.DoubleSetting;
import gg.shard.client.module.setting.IntSetting;

/** Pushes the first-person fire overlay down and fades it so you can still see the fight. */
public final class LowFireModule extends Module implements PanelPreview {
    private final DoubleSetting height = add(new DoubleSetting("Lower by", "How far down to move the flames", 0.5, 0.0, 1.0, 0.05));
    private final IntSetting opacity = add(new IntSetting("Opacity", "Flame opacity", 80, 20, 100, 5, "%"));

    public LowFireModule() {
        super("Low Fire", "Smaller, lower, see-through flames while you burn.", ModuleCategory.VISUALS);
    }

    @Override
    public boolean defaultEnabled() {
        return true;
    }

    @Override
    public String icon() {
        return "item:fire_charge";
    }

    @Override
    public String about() {
        return "Moves the first-person fire overlay down and makes it see-through while you burn, so you can still see the fight. "
                + "Only your own screen effect changes; burning itself and what others see are untouched.";
    }

    /** Live preview in the settings panel: a screen-shaped box with the flames at the chosen height and opacity. */
    @Override
    public int renderPreview(GuiGraphics g, int x, int y, int width) {
        int h = 88;
        Render2D.roundedRect(g, x, y, width, h, Theme.radius(), 0xFF0B0F18);
        Render2D.roundedOutline(g, x, y, width, h, Theme.radius(), Theme.line());
        TextureAtlasSprite sprite;
        try {
            sprite = Minecraft.getInstance().getAtlasManager().get(ModelBakery.FIRE_1);
        } catch (RuntimeException e) {
            return 0;
        }
        g.enableScissor(x + 1, y + 1, x + width - 1, y + h - 1);
        int size = 72;
        int drop = (int) Math.round(height.get() * 0.6 / 0.9 * size);
        int alpha = Math.round(255 * 0.9f * opacity.get() / 100f);
        int color = Colors.withAlpha(0xFFFFFF, alpha);
        int fy = y + h - size + 20 + drop;
        g.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x + 12, fy, size, size, color);
        g.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x + width - 12 - size, fy, size, size, color);
        g.disableScissor();
        Fonts.drawCentered(g, "Preview", Fonts.Weight.MEDIUM, 11, x + width / 2, y + 8, Theme.subtle());
        return h;
    }

    /** Vanilla translates the flame quad by -0.3; this pushes it further down. */
    public float fireY(float original) {
        return isEnabled() ? original - (float) (height.get() * 0.6) : original;
    }

    public float fireAlpha(float original) {
        return isEnabled() ? original * opacity.get() / 100f : original;
    }
}
