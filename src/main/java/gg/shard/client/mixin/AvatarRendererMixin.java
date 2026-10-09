package gg.shard.client.mixin;

import gg.shard.client.ShardClient;
import gg.shard.client.modules.visual.CosmeticsModule;
import gg.shard.client.render.BandanaState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
//? if >=1.21.9 {
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
//?} else {
/*import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
*///?}
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Looks up the player's bandana while vanilla still has the entity (BandanaLayer draws it). */
//? if >=1.21.9 {
@Mixin(AvatarRenderer.class)
abstract class AvatarRendererMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
            at = @At("TAIL"))
    private void shard$bandana(Avatar avatar, AvatarRenderState state, float partialTick, CallbackInfo ci) {
//?} else {
/*// Before 1.21.9 (no Avatar / mannequins) players are drawn by PlayerRenderer.
@Mixin(PlayerRenderer.class)
abstract class AvatarRendererMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/client/player/AbstractClientPlayer;Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;F)V",
            at = @At("TAIL"))
    private void shard$bandana(AbstractClientPlayer avatar, PlayerRenderState state, float partialTick, CallbackInfo ci) {
*///?}
        Identifier texture = null;
        if (avatar instanceof AbstractClientPlayer player && ShardClient.isReady()) {
            CosmeticsModule cosmetics = ShardClient.modules().get(CosmeticsModule.class);
            if (cosmetics != null) {
                try {
                    texture = cosmetics.bandanaTexture(player.getUUID(), player == Minecraft.getInstance().player);
                } catch (RuntimeException e) {
                    ShardClient.LOGGER.error("Cosmetics: bandana lookup failed", e);
                }
            }
        }
        ((BandanaState) state).shard$setBandana(texture);
    }
}
