package gg.shard.client.mixin;

import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Entity Optimizer: reads whether an arrow is stuck in a block (synced entity data, read only). */
@Mixin(AbstractArrow.class)
public interface AbstractArrowInvoker {
    @Invoker("isInGround")
    boolean shard$isInGround();
}
