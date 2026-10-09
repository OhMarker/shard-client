package gg.shard.client.mixin;

import gg.shard.client.render.ShieldCosmetics;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.ItemOwner;
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
    @Inject(method = "appendItemLayers", at = @At("HEAD"))
    private void shard$owner(ItemStackRenderState state, ItemStack stack, ItemDisplayContext context, Level level, ItemOwner owner, int seed,
                             CallbackInfo ci) {
        ShieldCosmetics.push(owner, context);
    }

    @Inject(method = "appendItemLayers", at = @At("RETURN"))
    private void shard$ownerEnd(ItemStackRenderState state, ItemStack stack, ItemDisplayContext context, Level level, ItemOwner owner, int seed,
                                CallbackInfo ci) {
        ShieldCosmetics.pop();
    }
}
