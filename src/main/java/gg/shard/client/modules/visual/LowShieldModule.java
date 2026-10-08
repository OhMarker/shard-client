package gg.shard.client.modules.visual;

import gg.shard.client.module.Module;
import gg.shard.client.module.ModuleCategory;
import gg.shard.client.module.setting.BoolSetting;
import gg.shard.client.module.setting.DoubleSetting;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Lowers the first-person shield so it covers less of the screen while you block. */
public final class LowShieldModule extends Module {
    private final DoubleSetting offset = add(new DoubleSetting("Lower by", "How far down to move the shield", 0.3, 0.05, 0.6, 0.05));
    private final BoolSetting onlyBlocking = add(new BoolSetting("Only while blocking", "Leave the idle shield where vanilla draws it", true));

    public LowShieldModule() {
        super("Low Shield", "Keeps your raised shield out of your view.", ModuleCategory.VISUALS);
    }

    @Override
    public String icon() {
        return "item:shield";
    }

    /** Vertical offset to apply to the hand holding {@code stack}, or 0. */
    public float offsetFor(AbstractClientPlayer player, InteractionHand hand, ItemStack stack) {
        if (!isEnabled() || stack == null || !stack.is(Items.SHIELD)) return 0f;
        if (onlyBlocking.get() && !(player.isUsingItem() && player.getUsedItemHand() == hand)) return 0f;
        return (float) -offset.get();
    }

    @Override
    public String about() {
        return "Lowers the shield in your first-person view so it covers less of the screen. Blocking works exactly as in vanilla.";
    }
}
