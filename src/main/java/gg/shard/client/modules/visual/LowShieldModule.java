package gg.shard.client.modules.visual;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import gg.shard.client.gui.Fonts;
import gg.shard.client.gui.PanelPreview;
import gg.shard.client.gui.Render2D;
import gg.shard.client.gui.Theme;
import gg.shard.client.module.Module;
import gg.shard.client.module.ModuleCategory;
import gg.shard.client.module.setting.BoolSetting;
import gg.shard.client.module.setting.ColorSetting;
import gg.shard.client.module.setting.DoubleSetting;
import gg.shard.client.module.setting.EnumSetting;
import gg.shard.client.module.setting.IntSetting;
import gg.shard.client.module.setting.Labeled;
import gg.shard.client.render.ItemTints;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Shield: a thinner, smaller first-person shield you can move, with separate settings for when
 * you block and when you just hold it, so it covers less of the fight either way. Only your own
 * first-person view changes; blocking, its timing and what others see are vanilla.
 */
public final class LowShieldModule extends Module implements PanelPreview {
    /** Quick tints; picking one sets the colour, editing the colour switches back to Custom. */
    public enum TintPreset implements Labeled {
        CUSTOM("Custom", -1), VANILLA("Vanilla", 0xFFFFFFFF), ICE("Ice", 0xFF9FD8FF), GHOST("Ghost", 0xFFB8B8C8),
        EMBER("Ember", 0xFFFFA070), ROSE("Rose", 0xFFFF9FC8), MINT("Mint", 0xFFA8F0C8), GOLD("Gold", 0xFFFFE08A);

        private final String label;
        final int argb;

        TintPreset(String label, int argb) {
            this.label = label;
            this.argb = argb;
        }

        @Override
        public String label() {
            return label;
        }
    }

    private final IntSetting blockWidth = add(new IntSetting("Blocking width", "How wide the shield is while you block", 65, 30, 100, 5, "%").group("While blocking"));
    private final IntSetting blockSize = add(new IntSetting("Blocking size", "Overall size while you block", 80, 40, 100, 5, "%").group("While blocking"));
    private final DoubleSetting blockLower = add(new DoubleSetting("Blocking lower", "Move it down while you block", 0.2, 0.0, 0.6, 0.05).group("While blocking"));
    private final DoubleSetting blockSide = add(new DoubleSetting("Blocking sideways", "Move it towards the edge (+) or the centre (-) while you block", 0.0, -0.3, 0.3, 0.05).group("While blocking"));
    private final IntSetting holdWidth = add(new IntSetting("Holding width", "How wide the shield is while you just hold it", 65, 30, 100, 5, "%").group("While holding"));
    private final IntSetting holdSize = add(new IntSetting("Holding size", "Overall size while you just hold it", 75, 40, 100, 5, "%").group("While holding"));
    private final DoubleSetting holdLower = add(new DoubleSetting("Holding lower", "Move it down while you just hold it", 0.15, 0.0, 0.6, 0.05).group("While holding"));
    private final DoubleSetting holdSide = add(new DoubleSetting("Holding sideways", "Move it towards the edge (+) or the centre (-) while you just hold it", 0.0, -0.3, 0.3, 0.05).group("While holding"));
    private final BoolSetting mainHand = add(new BoolSetting("Main hand too", "Also change a shield in your main hand, not only the offhand", true));
    private final IntSetting opacity = add(new IntSetting("Opacity", "How solid the shield looks (100 is vanilla)", 100, 10, 100, 5, "%").group("Look")
            .details("Below 100 % the shield is drawn see-through so you can watch the fight behind it."));
    private final ColorSetting tint = add(new ColorSetting("Tint", "Colour multiplied into the shield (white keeps vanilla's)", 0xFFFFFFFF, false).group("Look"));
    private final EnumSetting<TintPreset> preset = add(new EnumSetting<>("Tint preset", "Pick a ready-made tint", TintPreset.CUSTOM).group("Look"));

    public LowShieldModule() {
        super("Shield", "A thinner, smaller, lower, see-through or tinted shield, whether you block or just hold it.", ModuleCategory.VISUALS);
        preset.onChange(p -> {
            if (p != TintPreset.CUSTOM && tint.get() != p.argb) tint.set(p.argb);
        });
        tint.onChange(c -> {
            if (preset.get() != TintPreset.CUSTOM && preset.get().argb != c) preset.set(TintPreset.CUSTOM);
        });
    }

    @Override
    protected String legacyKey() {
        return "low-shield";
    }

    @Override
    public String icon() {
        return "shield";
    }

    @Override
    public String about() {
        return "Makes your first-person shield thinner and smaller and moves it out of the way, with one set of values for blocking and one for just holding it, "
                + "so it covers less of the screen even when you are not blocking, and can make it see-through or tint it. Blocking works exactly as in vanilla; nobody else sees a difference.";
    }

    private boolean applies(AbstractClientPlayer player, InteractionHand hand, ItemStack stack) {
        if (!isEnabled() || stack == null || !stack.is(Items.SHIELD)) return false;
        boolean off = hand == InteractionHand.OFF_HAND;
        return off || mainHand.get();
    }

    private static boolean blocking(AbstractClientPlayer player, InteractionHand hand) {
        return player.isUsingItem() && player.getUsedItemHand() == hand;
    }

    /** View-space offset {x, y} for the hand holding {@code stack}; x is towards that hand's edge. */
    public float[] offsetFor(AbstractClientPlayer player, InteractionHand hand, ItemStack stack) {
        if (!applies(player, hand, stack)) return null;
        boolean block = blocking(player, hand);
        float side = (block ? blockSide : holdSide).get().floatValue();
        // The offhand is on the left for a right-handed player; "towards the edge" flips with it.
        boolean left = (hand == InteractionHand.OFF_HAND) == (player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT);
        return new float[]{left ? -side : side, -(block ? blockLower : holdLower).get().floatValue()};
    }

    /** Item-space scale {x (width), y, z} for the shield, or null to leave it. */
    public float[] scaleFor(AbstractClientPlayer player, InteractionHand hand, ItemStack stack) {
        if (!applies(player, hand, stack)) return null;
        boolean block = blocking(player, hand);
        float size = (block ? blockSize.get() : holdSize.get()) / 100f;
        float width = (block ? blockWidth.get() : holdWidth.get()) / 100f;
        return new float[]{size * width, size, size};
    }

    /** ARGB multiplier (tint + opacity) for the shield in this hand, or {@link ItemTints#NONE} to leave it. */
    public int colorFor(AbstractClientPlayer player, InteractionHand hand, ItemStack stack) {
        if (!applies(player, hand, stack)) return ItemTints.NONE;
        return ItemTints.shieldColor(opacity.get(), tint.get());
    }

    // 0.3.0 had "Lower by" and "Only while blocking".

    @Override
    protected void migrateSetting(String key, JsonElement value, JsonObject all, int version) {
        switch (key) {
            case "lower-by" -> {
                blockLower.fromJson(value);
                boolean onlyBlocking = !all.has("only-while-blocking") || all.get("only-while-blocking").getAsBoolean();
                if (!onlyBlocking) holdLower.fromJson(value);
            }
            case "only-while-blocking" -> {
            }
            default -> super.migrateSetting(key, value, all, version);
        }
    }

    // ---- preview -------------------------------------------------------------------------------

    /** Two screen-shaped boxes, holding and blocking, with the shield's outline at the chosen size and place. */
    @Override
    public int renderPreview(GuiGraphics g, int x, int y, int width) {
        int h = 96;
        int half = (width - 12) / 2;
        box(g, x, y, half, h, "Holding", holdWidth.get(), holdSize.get(), holdLower.get(), holdSide.get(), 0.55f);
        box(g, x + half + 12, y, half, h, "Blocking", blockWidth.get(), blockSize.get(), blockLower.get(), blockSide.get(), 0.35f);
        return h;
    }

    private static void box(GuiGraphics g, int x, int y, int w, int h, String label, int widthPct, int sizePct, double lower, double side, float baseX) {
        Render2D.roundedRect(g, x, y, w, h, Theme.radius(), 0xFF0B0F18);
        Render2D.roundedOutline(g, x, y, w, h, Theme.radius(), Theme.line());
        g.enableScissor(x + 1, y + 1, x + w - 1, y + h - 1);
        // Vanilla's shield as a faint ghost, Shard's on top.
        int vw = Math.round(w * 0.42f);
        int vh = Math.round(h * 0.95f);
        int vx = x + Math.round(w * baseX) - vw / 2;
        int vy = y + h - Math.round(vh * 0.62f);
        Render2D.roundedOutline(g, vx, vy, vw, vh, 8, 0x40FFFFFF);
        float s = sizePct / 100f;
        int sw = Math.max(6, Math.round(vw * s * widthPct / 100f));
        int sh = Math.max(6, Math.round(vh * s));
        int sx = vx + vw / 2 - sw / 2 - (int) Math.round(side * w * 0.6);
        int sy = vy + vh - sh + (int) Math.round(lower * h * 0.9) - Math.round((vh - sh) * 0.4f);
        Render2D.roundedRect(g, sx, sy, sw, sh, Math.min(8, sw / 3), 0xFF8B6B3D);
        Render2D.roundedOutline(g, sx, sy, sw, sh, Math.min(8, sw / 3), 0xFFB0B7C3);
        g.fill(sx + sw / 2, sy + 4, sx + sw / 2 + 1, sy + sh - 4, 0x80B0B7C3);
        g.disableScissor();
        Fonts.draw(g, label, Fonts.Weight.MEDIUM, 10, x + 8, y + 6, Theme.subtle());
    }
}
