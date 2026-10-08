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

/**
 * Shard capes on player skins (Cosmetics module): the launcher's equipped cape on the local player,
 * and capes from Shard's shared cape list on any player in it.
 */
@Mixin(AbstractClientPlayer.class)
abstract class AbstractClientPlayerCapeMixin {
    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void shard$cape(CallbackInfoReturnable<PlayerSkin> cir) {
        if (ShardClient.modules() == null) return;
        CosmeticsModule cosmetics = ShardClient.modules().get(CosmeticsModule.class);
        if (cosmetics == null) return;
        AbstractClientPlayer self = (AbstractClientPlayer) (Object) this;
        boolean local = self == Minecraft.getInstance().player;
        PlayerSkin skin = cir.getReturnValue();
        try {
            PlayerSkin out = cosmetics.apply(skin, self.getUUID(), local);
            if (out != skin) cir.setReturnValue(out);
        } catch (RuntimeException e) {
            // A cosmetic must never crash the game: turn the module off and keep the vanilla skin.
            ShardClient.LOGGER.error("Cosmetics failed while dressing a player; turning the module off", e);
            cosmetics.setEnabled(false);
        }
    }
}
