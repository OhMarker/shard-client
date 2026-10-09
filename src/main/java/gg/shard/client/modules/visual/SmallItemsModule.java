package gg.shard.client.modules.visual;

import gg.shard.client.module.Module;
import gg.shard.client.module.ModuleCategory;
import gg.shard.client.module.setting.BoolSetting;
import gg.shard.client.module.setting.DoubleSetting;
import gg.shard.client.module.setting.EnumSetting;
import gg.shard.client.module.setting.IntSetting;
import gg.shard.client.render.ItemTints;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Small Items: by default exactly the Small Items mod's look (everything in your first-person
 * hands at 60 %, see {@link #PACK_SCALE}); Custom shrinks (never below 50 %) and nudges the items
 * so they cover less of the screen. Applied just before vanilla draws the held item
 * (ItemInHandRendererMixin); the arm, swing, timing and what others see are untouched.
 */
public final class SmallItemsModule extends Module {
    /**
     * The look: the Small Items mod (Smallhands 1.0.0 by Sallylabs, {@code Small-Items-1.21.11.jar})
     * with its default settings, or Shard's own sliders.
     */
    public enum Look implements gg.shard.client.module.setting.Labeled {
        SMALL_ITEMS("Small Items mod"), CUSTOM("Custom");

        private final String label;

        Look(String label) {
            this.label = label;
        }

        @Override
        public String label() {
            return label;
        }
    }

    /**
     * What the Small Items mod does (read from its classes with javap; its config defaults are
     * scale 0.6, offsets 0, affectItem and affectArm on): at the start of
     * {@code ItemInHandRenderer.renderItem} and {@code renderPlayerArm} it scales the pose by 0.6 on
     * every axis, around the hand's pivot, for every item (shields included) and for the empty
     * arm. No per-item model transforms, no resource pack.
     */
    public static final float PACK_SCALE = 0.6f;

    private final EnumSetting<Look> look = add(new EnumSetting<>("Look", "Small Items mod: exactly that mod's look (60%, items and empty hand). Custom: your own sizes and offsets", Look.SMALL_ITEMS)
            .details("The Small Items mod shrinks everything in your first-person hands to 60% around the hand, including shields and your empty arm."));
    private final IntSetting mainSize = add(new IntSetting("Main hand size", "Size of the item in your main hand", 75, 50, 100, 5, "%").group("Size"));
    private final IntSetting offSize = add(new IntSetting("Offhand size", "Size of the item in your offhand", 75, 50, 100, 5, "%").group("Size"));
    private final DoubleSetting offsetX = add(new DoubleSetting("X offset", "Move the items towards the screen edge (+) or the centre (-)", 0.0, -0.3, 0.3, 0.02).group("Position"));
    private final DoubleSetting offsetY = add(new DoubleSetting("Y offset", "Move the items up (+) or down (-)", 0.0, -0.3, 0.3, 0.02).group("Position"));
    private final DoubleSetting offsetZ = add(new DoubleSetting("Z offset", "Move the items away from (-) or towards (+) you", 0.0, -0.3, 0.3, 0.02).group("Position"));
    private final BoolSetting skipShields = add(new BoolSetting("Leave shields to Shield module", "Don't resize or move shields; the Shield module sizes them", true));

    public SmallItemsModule() {
        super("Small Items", "Smaller, movable items in your first-person hands.", ModuleCategory.VISUALS);
        for (gg.shard.client.module.setting.Setting<?> st : new gg.shard.client.module.setting.Setting<?>[]{mainSize, offSize, offsetX, offsetY, offsetZ, skipShields}) {
            st.visibleWhen(() -> look.get() == Look.CUSTOM);
        }
    }

    @Override
    public String icon() {
        return "hand";
    }

    @Override
    public String about() {
        return "By default it looks exactly like the Small Items mod: everything in your first-person hands, and your empty hand, at 60%. "
                + "Custom shrinks the items you hold (never below half size) and lets you nudge them sideways, up or forward, with separate sizes for each hand, "
                + "so swords, crystals and totems block less of the fight. Only your own first-person view changes; swings, timing and what others see are vanilla.";
    }

    private boolean applies(ItemStack stack) {
        if (!isEnabled() || stack == null || stack.isEmpty()) return false;
        if (look.get() == Look.SMALL_ITEMS) return true;
        return !(skipShields.get() && stack.is(Items.SHIELD));
    }

    /** Scale for the empty first-person arm (the Small Items mod shrinks it too), or 1. */
    public float armScale() {
        return isEnabled() && look.get() == Look.SMALL_ITEMS ? PACK_SCALE : 1f;
    }

    /**
     * {scale, x, y, z} for the item in {@code hand}, or null to leave it. x points towards that
     * hand's screen edge, so one offset mirrors correctly for both hands.
     */
    static final float LIFT_UP = 0.45f;
    static final float LIFT_IN = 0.15f;

    public float[] transformFor(InteractionHand hand, HumanoidArm arm, ItemStack stack) {
        if (!applies(stack)) return null;
        if (look.get() == Look.SMALL_ITEMS) return new float[]{PACK_SCALE, 0f, 0f, 0f};
        float scale = ItemTints.itemScale(hand == InteractionHand.MAIN_HAND ? mainSize.get() : offSize.get());
        float side = arm == HumanoidArm.RIGHT ? 1f : -1f;
        // Shrinking pivots on the grip at the bottom corner, which would sink the item off-screen;
        // lift it and pull it in by what it lost so a smaller item stays where you can see it.
        float shrink = 1f - scale;
        float x = (offsetX.get().floatValue() - LIFT_IN * shrink) * side;
        float y = offsetY.get().floatValue() + LIFT_UP * shrink;
        float z = offsetZ.get().floatValue();
        if (scale == 1f && x == 0f && y == 0f && z == 0f) return null;
        return new float[]{scale, x, y, z};
    }
}
