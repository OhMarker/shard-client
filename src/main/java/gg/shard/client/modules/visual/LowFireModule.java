package gg.shard.client.modules.visual;

import gg.shard.client.gui.Fonts;
import gg.shard.client.gui.PanelPreview;
import gg.shard.client.gui.Render2D;
import gg.shard.client.gui.Theme;
import gg.shard.client.module.Module;
import gg.shard.client.module.ModuleCategory;
import gg.shard.client.module.setting.BoolSetting;
import gg.shard.client.module.setting.ColorSetting;
import gg.shard.client.module.setting.DoubleSetting;
import gg.shard.client.module.setting.IntSetting;
import gg.shard.client.module.setting.Setting;
import gg.shard.client.render.LowFireModels;
import gg.shard.client.util.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.resources.Identifier;

/**
 * Low Fire: the first-person fire overlay, fire and soul fire blocks on the ground, and flames on
 * burning players and mobs, each with its own height, opacity and colour. Purely how things look:
 * burning, fire blocks, hitboxes and damage are untouched, and nothing hidden is revealed.
 */
public final class LowFireModule extends Module implements PanelPreview {
    private static final Material SOUL_FIRE = new Material(TextureAtlas.LOCATION_BLOCKS, Identifier.withDefaultNamespace("block/soul_fire_0"));

    // Your screen (keys kept from 0.3.0: "lower-by", "opacity").
    private final DoubleSetting height = add(new DoubleSetting("Lower by", "How far down to move the flames on your screen", 0.5, 0.0, 1.0, 0.05).group("Your screen"));
    private final IntSetting opacity = add(new IntSetting("Opacity", "Opacity of the flames on your screen (0 hides them)", 80, 0, 100, 5, "%").group("Your screen"));
    private final ColorSetting color = add(new ColorSetting("Colour", "Tint for the flames on your screen (white keeps vanilla's colour)", 0xFFFFFFFF).group("Your screen"));
    // On the ground.
    private final BoolSetting ground = add(new BoolSetting("Change ground fire", "Make fire blocks on the ground lower, see-through and tinted", true).group("Fire on the ground")
            .details("Fire in crystal fights covers the floor you need to see. The block and its hitbox stay exactly where they are."));
    private final IntSetting groundHeight = add(new IntSetting("Ground fire height", "Height of fire blocks", 45, 15, 100, 5, "%").group("Fire on the ground"));
    private final IntSetting groundOpacity = add(new IntSetting("Ground fire opacity", "Opacity of fire blocks", 60, 15, 100, 5, "%").group("Fire on the ground"));
    private final ColorSetting fireColor = add(new ColorSetting("Fire colour", "Tint for normal fire blocks", 0xFFFFFFFF).group("Fire on the ground"));
    private final ColorSetting soulColor = add(new ColorSetting("Soul fire colour", "Tint for soul fire blocks", 0xFFFFFFFF).group("Fire on the ground"));
    // Burning players and mobs.
    private final IntSetting entityHeight = add(new IntSetting("Entity fire height", "Height of the flames on burning players and mobs", 60, 20, 100, 5, "%").group("Burning players and mobs"));
    private final IntSetting entityOpacity = add(new IntSetting("Entity fire opacity", "Opacity of those flames", 70, 15, 100, 5, "%").group("Burning players and mobs"));
    private final ColorSetting entityColor = add(new ColorSetting("Entity fire colour", "Tint for those flames", 0xFFFFFFFF).group("Burning players and mobs"));

    public LowFireModule() {
        super("Low Fire", "Lower, see-through, recoloured fire on your screen, on the ground and on players.", ModuleCategory.VISUALS);
        for (Setting<?> s : new Setting<?>[]{groundHeight, groundOpacity, fireColor, soulColor}) s.visibleWhen(ground::get);
    }

    @Override
    public boolean defaultEnabled() {
        return true;
    }

    @Override
    public String icon() {
        return "fire";
    }

    @Override
    public String about() {
        return "Three kinds of fire, each with its own height, opacity and colour: the flames on your screen while you burn, fire and soul fire blocks on the ground, "
                + "and the flames on burning players and mobs. Only how fire looks changes; burning, the fire blocks, their hitboxes and damage are untouched.";
    }

    // ---- your screen (ScreenEffectRendererMixin) -------------------------------------------------

    /** Vanilla translates the flame quad by -0.3; this pushes it further down. */
    public float fireY(float original) {
        return isEnabled() ? original - (float) (height.get() * 0.6) : original;
    }

    /** Overlay colour {r, g, b, a} given vanilla's. */
    public float[] screenColor(float r, float g, float b, float a) {
        if (!isEnabled()) return new float[]{r, g, b, a};
        int c = color.get();
        return new float[]{r * ((c >> 16) & 0xFF) / 255f, g * ((c >> 8) & 0xFF) / 255f, b * (c & 0xFF) / 255f, a * opacity.get() / 100f};
    }

    // ---- on the ground (LowFireModels) -----------------------------------------------------------

    public LowFireModels.Ground groundSnapshot() {
        if (!isEnabled() || !ground.get()) return new LowFireModels.Ground(false, 1f, 0xFFFFFFFF, 0xFFFFFFFF);
        int a = Math.round(255 * groundOpacity.get() / 100f);
        return new LowFireModels.Ground(true, groundHeight.get() / 100f, Colors.withAlpha(fireColor.get(), a), Colors.withAlpha(soulColor.get(), a));
    }

    // ---- burning players and mobs (FlameFeatureRendererMixin) ------------------------------------

    public float entityHeightFactor() {
        return isEnabled() ? entityHeight.get() / 100f : 1f;
    }

    /** True when the flames need the translucent sheet. */
    public boolean entityTranslucent() {
        return isEnabled() && entityOpacity.get() < 100;
    }

    public int entityColor(int original) {
        if (!isEnabled()) return original;
        int tint = Colors.withAlpha(entityColor.get(), Math.round(255 * entityOpacity.get() / 100f));
        return LowFireModels.multiply(original, tint);
    }

    // ---- preview -------------------------------------------------------------------------------

    /** Your screen on the left; a fire block and a soul fire block on a floor on the right. */
    @Override
    public int renderPreview(GuiGraphics g, int x, int y, int width) {
        int h = 96;
        Render2D.roundedRect(g, x, y, width, h, Theme.radius(), 0xFF0B0F18);
        Render2D.roundedOutline(g, x, y, width, h, Theme.radius(), Theme.line());
        TextureAtlasSprite fire;
        TextureAtlasSprite soul;
        try {
            fire = Minecraft.getInstance().getAtlasManager().get(ModelBakery.FIRE_1);
            soul = Minecraft.getInstance().getAtlasManager().get(SOUL_FIRE);
        } catch (RuntimeException e) {
            return 0;
        }
        g.enableScissor(x + 1, y + 1, x + width - 1, y + h - 1);
        int half = width / 2;
        int size = 64;
        int drop = (int) Math.round(height.get() * 0.6 / 0.9 * size);
        int screenTint = Colors.withAlpha(color.get(), Math.round(255 * 0.9f * opacity.get() / 100f));
        int fy = y + h - size + 18 + drop;
        if (opacity.get() > 0) {
            g.blitSprite(RenderPipelines.GUI_TEXTURED, fire, x + 6, fy, size, size, screenTint);
            g.blitSprite(RenderPipelines.GUI_TEXTURED, fire, x + half - 6 - size, fy, size, size, screenTint);
        }
        Fonts.drawCentered(g, "Your screen", Fonts.Weight.MEDIUM, 10, x + half / 2, y + 6, Theme.subtle());
        g.fill(x + half, y + 8, x + half + 1, y + h - 8, Theme.line());
        int floor = y + h - 14;
        g.fill(x + half + 8, floor, x + width - 8, floor + 1, Theme.lineStrong());
        int block = 36;
        boolean change = ground.get();
        int bh = change ? Math.max(4, Math.round(block * groundHeight.get() / 100f)) : block;
        int a = change ? Math.round(255 * groundOpacity.get() / 100f) : 255;
        int gx = x + half + (half - block * 2 - 12) / 2;
        g.blitSprite(RenderPipelines.GUI_TEXTURED, fire, gx, floor - bh, block, bh, Colors.withAlpha(change ? fireColor.get() : 0xFFFFFFFF, a));
        g.blitSprite(RenderPipelines.GUI_TEXTURED, soul, gx + block + 12, floor - bh, block, bh, Colors.withAlpha(change ? soulColor.get() : 0xFFFFFFFF, a));
        Fonts.drawCentered(g, "On the ground", Fonts.Weight.MEDIUM, 10, x + half + half / 2, y + 6, Theme.subtle());
        g.disableScissor();
        return h;
    }
}
