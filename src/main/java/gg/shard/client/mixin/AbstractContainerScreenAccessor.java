package gg.shard.client.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Smoke test only: where the container is laid out and which slot is under the pointer. */
@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
    @Accessor("leftPos")
    int shard$leftPos();

    @Accessor("topPos")
    int shard$topPos();

    @Accessor("hoveredSlot")
    Slot shard$hoveredSlot();
}
