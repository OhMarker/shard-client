package gg.shard.client.mixin;

//? if >=1.21.2 <1.21.4 {
/*import gg.shard.client.render.ShieldCosmetics;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/^* Before 1.21.4: the entity a render state was extracted from, for the shield cosmetic (ShieldCosmetics.Holder). ^/
@Mixin(LivingEntityRenderState.class)
abstract class LivingEntityRenderStateMixin implements ShieldCosmetics.Holder {
    @Unique private LivingEntity shard$holderEntity;

    @Override
    public LivingEntity shard$holder() {
        return shard$holderEntity;
    }

    @Override
    public void shard$setHolder(LivingEntity holder) {
        shard$holderEntity = holder;
    }
}
*///?}
