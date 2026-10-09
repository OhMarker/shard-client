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
import gg.shard.client.util.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
//? if >=1.21.2 {
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.crystal.EndCrystalModel;
import net.minecraft.client.renderer.entity.state.EndCrystalRenderState;
//?}
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;

/**
 * Crystal Visuals: size, spin speed, bounce, the bedrock base, separate core and frame colours,
 * opacity, and an outline on the crystal under your crosshair. All purely visual; hitboxes,
 * placement and explosions are unchanged. The outline is only drawn on the crystal you are
 * aiming at, which vanilla already picked by line of sight, so nothing hidden is revealed.
 */
public final class CrystalTweaksModule extends Module implements PanelPreview {
    private final DoubleSetting scale = add(new DoubleSetting("Scale", "Crystal model size", 0.65, 0.25, 1.0, 0.05, "x").group("Shape"));
    private final IntSetting spin = add(new IntSetting("Spin speed", "How fast crystals turn (0 stops them)", 100, 0, 300, 10, "%").group("Shape"));
    private final BoolSetting bounce = add(new BoolSetting("Bounce", "Bob up and down like vanilla", true).group("Shape"));
    private final BoolSetting base = add(new BoolSetting("Show base", "Draw the bedrock base under crystals that have one", true).group("Shape"));
    private final ColorSetting core = add(new ColorSetting("Core colour", "Tint for the cube in the middle (white keeps vanilla)", 0xFFFFFFFF, false).group("Colour"));
    private final ColorSetting frame = add(new ColorSetting("Frame colour", "Tint for the glass frames (white keeps vanilla)", 0xFFFFFFFF, false).group("Colour"));
    private final IntSetting opacity = add(new IntSetting("Opacity", "How solid crystals look", 100, 20, 100, 5, "%").group("Colour"));
    private final BoolSetting aimOutline = add(new BoolSetting("Outline when aimed", "Outline the crystal under your crosshair", true).group("Colour")
            .details("Only the crystal vanilla already picked under your crosshair, which needs line of sight."));
    private final ColorSetting aimColor = add(new ColorSetting("Outline colour", "Colour of that outline", 0xFFC084FC, false).group("Colour"));

    //? if >=1.21.2 {
    private EndCrystalModel coreModel;
    private EndCrystalModel frameModel;
    //?}

    public CrystalTweaksModule() {
        super("Crystal Visuals", "Smaller crystals with your colours, spin speed, bounce and base.", ModuleCategory.VISUALS);
        aimColor.visibleWhen(aimOutline::get);
    }

    @Override
    protected String legacyKey() {
        return "crystal-size";
    }

    @Override
    public boolean defaultEnabled() {
        return true;
    }

    @Override
    public String icon() {
        return "visuals";
    }

    @Override
    public String about() {
        return "How end crystals look: smaller so the player behind stays visible, faster or slower spin, no bounce, no base, your own core and frame colours, see-through, "
                + "and an outline on the crystal under your crosshair. Hitboxes, placement and explosions are unchanged; this is purely visual.";
    }

    public float renderScale() {
        return isEnabled() ? scale.getFloat() : 1f;
    }

    public float spinFactor() {
        return isEnabled() ? spin.get() / 100f : 1f;
    }

    public boolean noBounce() {
        return isEnabled() && !bounce.get();
    }

    /** True when the base is hidden (EndCrystalRendererMixin before 1.21.2, which has no render states). */
    public boolean hidesBase() {
        return isEnabled() && !base.get();
    }

    //? if >=1.21.2 {
    /** Applied to the render state after vanilla extracted it. */
    public void adjustState(EndCrystal crystal, EndCrystalRenderState state) {
        if (!isEnabled()) return;
        if (!base.get()) state.showsBottom = false;
        //? if >=1.21.9 {
        if (aimOutline.get() && Minecraft.getInstance().crosshairPickEntity == crystal) state.outlineColor = aimColor.get();
        //?}
    }
    //?}

    //? if <1.21.9 {
    /*/^*
     * Before 1.21.9 the glow outline belongs to the entity (LevelRenderer asks Minecraft whether it
     * glows and takes its team colour): the outline colour Shard gives this crystal, or 0. The
     * Crystal Optimizer's placed highlight wins over the aim outline, as on later versions.
     ^/
    public int outline(EndCrystal crystal) {
        int placed = gg.shard.client.ShardClient.modules().get(gg.shard.client.modules.combat.CrystalOptimizerModule.class)
                .highlightOutline(crystal.getX(), crystal.getY(), crystal.getZ());
        if (placed != 0) return placed;
        return isEnabled() && aimOutline.get() && Minecraft.getInstance().crosshairPickEntity == crystal ? aimColor.get() : 0;
    }
    *///?}

    /** True when colours or opacity differ from vanilla, so the crystal is drawn as two tinted models. */
    public boolean recolours() {
        return isEnabled() && (core.get() != 0xFFFFFFFF || frame.get() != 0xFFFFFFFF || opacity.get() < 100);
    }

    public int coreTint() {
        return Colors.withAlpha(core.get(), Math.round(255 * opacity.get() / 100f));
    }

    public int frameTint() {
        return Colors.withAlpha(frame.get(), Math.round(255 * opacity.get() / 100f));
    }

    public boolean translucent() {
        return opacity.get() < 100;
    }

    //? if >=1.21.2 {
    /** The cube only (frames skip drawing themselves but still carry the cube's pose). */
    public EndCrystalModel coreModel() {
        if (coreModel == null) {
            coreModel = new EndCrystalModel(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.END_CRYSTAL)) {
                @Override
                public void setupAnim(EndCrystalRenderState state) {
                    super.setupAnim(state);
                    base.visible = false;
                    outerGlass.skipDraw = true;
                    innerGlass.skipDraw = true;
                }
            };
        }
        return coreModel;
    }

    /** Frames and base, without the cube. */
    public EndCrystalModel frameModel() {
        if (frameModel == null) {
            frameModel = new EndCrystalModel(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.END_CRYSTAL)) {
                @Override
                public void setupAnim(EndCrystalRenderState state) {
                    super.setupAnim(state);
                    cube.visible = false;
                }
            };
        }
        return frameModel;
    }
    //?}

    /** Swatches showing the two colours at the chosen opacity, over a checkerboard. */
    @Override
    public int renderPreview(GuiGraphics g, int x, int y, int width) {
        if (!recolours()) return 0;
        int h = 44;
        Render2D.roundedRect(g, x, y, width, h, Theme.radius(), 0xFF0B0F18);
        Render2D.roundedOutline(g, x, y, width, h, Theme.radius(), Theme.line());
        int sw = 28;
        int sx = x + 12;
        int sy = y + (h - sw) / 2;
        Render2D.checker(g, sx, sy, sw, sw, 4);
        g.fill(sx, sy, sx + sw, sy + sw, Colors.withAlpha(frameTint(), (frameTint() >>> 24) * 3 / 5));
        g.fill(sx + 8, sy + 8, sx + sw - 8, sy + sw - 8, coreTint());
        Fonts.draw(g, "Core and frame at " + opacity.get() + "%", Fonts.Weight.MEDIUM, 11, sx + sw + 12, y + (h - Fonts.lineHeight(11)) / 2, Theme.muted());
        return h;
    }
}
