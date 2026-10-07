package gg.shard.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import gg.shard.client.ShardClient;
import gg.shard.client.event.ShardEvents;
import gg.shard.client.modules.combat.AnchorOptimizerModule;
import gg.shard.client.modules.combat.CrystalOptimizerModule;
import gg.shard.client.modules.perf.ExplosionOptimizerModule;
import gg.shard.client.modules.visual.TotemPopModule;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Packet-level hooks. In 1.21.11 the totem event (35) is handled here, not in LivingEntity,
 * so the totem counter, pop messages, sound and animation all hang off this class. Explosion
 * packets feed the crystal/anchor prediction and the explosion optimizer. Injections sit after
 * {@code ensureRunningOnSameThread} so they never run on the network thread.
 */
@Mixin(ClientPacketListener.class)
abstract class ClientPacketListenerMixin {
    private static final String SAME_THREAD = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V";
    private static final String PLAY_LOCAL_SOUND = "Lnet/minecraft/client/multiplayer/ClientLevel;playLocalSound(DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFZ)V";
    private static final String ADD_PARTICLE = "Lnet/minecraft/client/multiplayer/ClientLevel;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V";

    @Shadow private ClientLevel level;

    private boolean shard$anchorPredicted;

    // ---- totem pops --------------------------------------------------------------------------

    @Inject(method = "handleEntityEvent", at = @At(value = "INVOKE", target = SAME_THREAD, shift = At.Shift.AFTER))
    private void shard$onEntityEvent(ClientboundEntityEventPacket packet, CallbackInfo ci) {
        if (!ShardClient.isReady() || level == null || packet.getEventId() != EntityEvent.PROTECTED_FROM_DEATH) return;
        Entity entity = packet.getEntity(level);
        if (entity instanceof LivingEntity living) ShardEvents.fireTotemPop(living);
    }

    @WrapOperation(method = "handleEntityEvent", at = @At(value = "INVOKE", target = PLAY_LOCAL_SOUND))
    private void shard$totemSound(ClientLevel instance, double x, double y, double z, SoundEvent sound, SoundSource source,
                                  float volume, float pitch, boolean delay, Operation<Void> original) {
        if (ShardClient.isReady() && sound == SoundEvents.TOTEM_USE) {
            TotemPopModule totem = ShardClient.modules().get(TotemPopModule.class);
            volume = totem.soundVolume(volume);
            pitch = totem.soundPitch(pitch);
            if (volume <= 0f) return;
        }
        original.call(instance, x, y, z, sound, source, volume, pitch, delay);
    }

    @WrapOperation(method = "handleEntityEvent", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GameRenderer;displayItemActivation(Lnet/minecraft/world/item/ItemStack;)V"))
    private void shard$totemAnimation(GameRenderer renderer, ItemStack stack, Operation<Void> original) {
        if (ShardClient.isReady() && ShardClient.modules().get(TotemPopModule.class).hideAnimation()) return;
        original.call(renderer, stack);
    }

    // ---- explosions --------------------------------------------------------------------------

    @Inject(method = "handleExplosion", at = @At(value = "INVOKE", target = SAME_THREAD, shift = At.Shift.AFTER))
    private void shard$onExplosion(ClientboundExplodePacket packet, CallbackInfo ci) {
        shard$anchorPredicted = false;
        if (!ShardClient.isReady() || level == null) return;
        shard$anchorPredicted = ShardClient.modules().get(AnchorOptimizerModule.class).consumeServerExplosion(packet.center());
        ShardClient.modules().get(CrystalOptimizerModule.class).onExplosion(level, packet.center());
    }

    @WrapOperation(method = "handleExplosion", at = @At(value = "INVOKE", target = PLAY_LOCAL_SOUND))
    private void shard$explosionSound(ClientLevel instance, double x, double y, double z, SoundEvent sound, SoundSource source,
                                      float volume, float pitch, boolean delay, Operation<Void> original) {
        if (ShardClient.isReady()) {
            if (shard$anchorPredicted && ShardClient.modules().get(AnchorOptimizerModule.class).mutesServerSound()) return;
            if (!ShardClient.modules().get(ExplosionOptimizerModule.class).allowSound(instance.getGameTime())) return;
        }
        original.call(instance, x, y, z, sound, source, volume, pitch, delay);
    }

    @WrapOperation(method = "handleExplosion", at = @At(value = "INVOKE", target = ADD_PARTICLE))
    private void shard$explosionParticle(ClientLevel instance, ParticleOptions options, double x, double y, double z,
                                         double dx, double dy, double dz, Operation<Void> original) {
        if (ShardClient.isReady()) {
            if (shard$anchorPredicted && ShardClient.modules().get(AnchorOptimizerModule.class).skipsServerParticles()) return;
            if (options.getType() == ParticleTypes.EXPLOSION_EMITTER && ShardClient.modules().get(ExplosionOptimizerModule.class).skipsEmitter()) {
                original.call(instance, ParticleTypes.EXPLOSION, x, y, z, 0.0, 0.0, 0.0);
                return;
            }
        }
        original.call(instance, options, x, y, z, dx, dy, dz);
    }

    // ---- entity spawns -----------------------------------------------------------------------

    @Inject(method = "handleAddEntity", at = @At("TAIL"))
    private void shard$onAddEntity(ClientboundAddEntityPacket packet, CallbackInfo ci) {
        if (!ShardClient.isReady() || level == null || packet.getType() != EntityType.END_CRYSTAL) return;
        Entity entity = level.getEntity(packet.getId());
        if (entity != null) ShardClient.modules().get(CrystalOptimizerModule.class).onEntityAdded(entity);
    }
}
