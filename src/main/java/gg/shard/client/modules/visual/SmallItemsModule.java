package gg.shard.client.modules.visual;

import gg.shard.client.module.Module;
import gg.shard.client.module.ModuleCategory;
import gg.shard.client.module.setting.BoolSetting;
import gg.shard.client.module.setting.DoubleSetting;
import gg.shard.client.module.setting.IntSetting;
import gg.shard.client.render.ItemTints;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Small Items: shrinks (never below 50 %) and nudges the items in your first-person hands so
 * they cover less of the screen. Applied just before vanilla draws the held item
 * (ItemInHandRendererMixin); the arm, swing, timing and what others see are untouched.
 */
public final class SmallItemsModule extends Module {
    private final IntSetting mainSize = add(new IntSetting("Main hand size", "Size of the item in your main hand", 75, 50, 100, 5, "%").group("Size"));
    private final IntSetting offSize = add(new IntSetting("Offhand size", "Size of the item in your offhand", 75, 50, 100, 5, "%").group("Size"));
    private final DoubleSetting offsetX = add(new DoubleSetting("X offset", "Move the items towards the screen edge (+) or the centre (-)", 0.0, -0.3, 0.3, 0.02).group("Position"));
    private final DoubleSetting offsetY = add(new DoubleSetting("Y offset", "Move the items up (+) or down (-)", 0.0, -0.3, 0.3, 0.02).group("Position"));
    private final DoubleSetting offsetZ = add(new DoubleSetting("Z offset", "Move the items away from (-) or towards (+) you", 0.0, -0.3, 0.3, 0.02).group("Position"));
    private final BoolSetting skipShields = add(new BoolSetting("Leave shields to Shield module", "Don't resize or move shields; the Shield module sizes them", true));

    public SmallItemsModule() {
        super("Small Items", "Smaller, movable items in your first-person hands.", ModuleCategory.VISUALS);
    }

    @Override
    public String icon() {
        return "hand";
    }

    @Override
    public String about() {
        return "Shrinks the items you hold in first person (never below half size) and lets you nudge them sideways, up or forward, with separate sizes for each hand, "
                + "so swords, crystals and totems block less of the fight. Only your own first-person view changes; swings, timing and what others see are vanilla.";
    }

    private boolean applies(ItemStack stack) {
        if (!isEnabled() || stack == null || stack.isEmpty()) return false;
        return !(skipShields.get() && stack.is(Items.SHIELD));
    }

    /**
     * {scale, x, y, z} for the item in {@code hand}, or null to leave it. x points towards that
     * hand's screen edge, so one offset mirrors correctly for both hands.
     */
    static final float LIFT_UP = 0.45f;
    static final float LIFT_IN = 0.15f;

    public float[] transformFor(InteractionHand hand, HumanoidArm arm, ItemStack stack) {
        if (!applies(stack)) return null;
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
