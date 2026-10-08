package gg.shard.client.mixin;

import gg.shard.client.ShardClient;
import gg.shard.client.modules.visual.CosmeticsModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Puts the cape equipped in Shard Launcher on the local player's skin (Cosmetics module). */
@Mixin(AbstractClientPlayer.class)
abstract class AbstractClientPlayerCapeMixin {
    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void shard$launcherCape(CallbackInfoReturnable<PlayerSkin> cir) {
        if ((Object) this != Minecraft.getInstance().player || ShardClient.modules() == null) return;
        CosmeticsModule cosmetics = ShardClient.modules().get(CosmeticsModule.class);
        if (cosmetics != null) cir.setReturnValue(cosmetics.apply(cir.getReturnValue()));
    }
}
