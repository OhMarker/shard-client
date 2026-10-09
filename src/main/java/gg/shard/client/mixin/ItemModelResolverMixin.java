package gg.shard.client.mixin;

import gg.shard.client.render.ShieldCosmetics;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
//? if >=1.21.9 {
import net.minecraft.world.entity.ItemOwner;
//?} else {
/*import net.minecraft.world.entity.LivingEntity;
*///?}
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Remembers who holds the item being resolved, for the shield cosmetic (see ShieldCosmetics). */
@Mixin(ItemModelResolver.class)
abstract class ItemModelResolverMixin {
    //? if >=1.21.9 {
    @Inject(method = "appendItemLayers", at = @At("HEAD"))
    private void shard$owner(ItemStackRenderState state, ItemStack stack, ItemDisplayContext context, Level level, ItemOwner owner, int seed,
                             CallbackInfo ci) {
        ShieldCosmetics.push(owner == null ? null : owner.asLivingEntity(), context);
    }

    @Inject(method = "appendItemLayers", at = @At("RETURN"))
    private void shard$ownerEnd(ItemStackRenderState state, ItemStack stack, ItemDisplayContext context, Level level, ItemOwner owner, int seed,
                                CallbackInfo ci) {
        ShieldCosmetics.pop();
    }
    //?} else {
    /*// Before 1.21.9 the holder is a LivingEntity.
    @Inject(method = "appendItemLayers", at = @At("HEAD"))
    private void shard$owner(ItemStackRenderState state, ItemStack stack, ItemDisplayContext context, Level level, LivingEntity owner, int seed,
                             CallbackInfo ci) {
        ShieldCosmetics.push(owner, context);
    }

    @Inject(method = "appendItemLayers", at = @At("RETURN"))
    private void shard$ownerEnd(ItemStackRenderState state, ItemStack stack, ItemDisplayContext context, Level level, LivingEntity owner, int seed,
                                CallbackInfo ci) {
        ShieldCosmetics.pop();
    }
    *///?}
}
