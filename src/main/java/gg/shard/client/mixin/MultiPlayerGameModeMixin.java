package gg.shard.client.mixin;

import gg.shard.client.ShardClient;
import gg.shard.client.modules.combat.AnchorOptimizerModule;
import gg.shard.client.modules.combat.CrystalOptimizerModule;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Crystal and anchor prediction hooks around vanilla's attack and block-use entry points. */
@Mixin(MultiPlayerGameMode.class)
abstract class MultiPlayerGameModeMixin {
    /** A crystal this client invented must never produce an attack packet; swing and drop it. */
    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void shard$attackFake(Player player, Entity target, CallbackInfo ci) {
        if (!ShardClient.isReady()) return;
        CrystalOptimizerModule crystals = ShardClient.modules().get(CrystalOptimizerModule.class);
        if (crystals.isFake(target)) {
            crystals.onAttack(target);
            player.swing(InteractionHand.MAIN_HAND);
            ci.cancel();
        }
    }

    /** After vanilla sent the attack packet, drop the crystal locally. */
    @Inject(method = "attack", at = @At("TAIL"))
    private void shard$attackTail(Player player, Entity target, CallbackInfo ci) {
        if (ShardClient.isReady()) ShardClient.modules().get(CrystalOptimizerModule.class).onAttack(target);
    }

    /** After vanilla processed the block use (and its own charge prediction), predict the rest. */
    @Inject(method = "useItemOn", at = @At("TAIL"))
    private void shard$useItemOn(LocalPlayer player, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        if (!ShardClient.isReady()) return;
        InteractionResult result = cir.getReturnValue();
        if (result == null || !result.consumesAction()) return;
        ShardClient.modules().get(AnchorOptimizerModule.class).onUseItemOn(player, hand, hit);
        ShardClient.modules().get(CrystalOptimizerModule.class).onUseItemOn(player, hand, hit);
    }
}
