package gg.shard.client.mixin;

import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
//? if <1.21.2 {
/*import org.spongepowered.asm.mixin.gen.Accessor;
*///?}

/** Entity Optimizer: reads whether an arrow is stuck in a block (synced entity data, read only). */
@Mixin(AbstractArrow.class)
public interface AbstractArrowInvoker {
    //? if >=1.21.2 {
    @Invoker("isInGround")
    boolean shard$isInGround();
    //?} else {
    /*// Before 1.21.2 "in ground" is a plain field.
    @Accessor("inGround")
    boolean shard$isInGround();
    *///?}
}
