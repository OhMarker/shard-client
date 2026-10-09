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
    //? if >=1.21.9 {
    private static final String SAME_THREAD = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V";
    //?} else {
    /*// Before 1.21.9 packets are handed to the client's BlockableEventLoop (no PacketProcessor).
    private static final String SAME_THREAD = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/util/thread/BlockableEventLoop;)V";
    *///?}
    private static final String PLAY_LOCAL_SOUND = "Lnet/minecraft/client/multiplayer/ClientLevel;playLocalSound(DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFZ)V";
    private static final String ADD_PARTICLE = "Lnet/minecraft/client/multiplayer/ClientLevel;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V";

    @Shadow private ClientLevel level;

    private boolean shard$anchorPredicted;

    // ---- totem pops --------------------------------------------------------------------------

    @Inject(method = "handleEntityEvent", at = @At(value = "INVOKE", target = SAME_THREAD, shift = At.Shift.AFTER))
    private void shard$onEntityEvent(ClientboundEntityEventPacket packet, CallbackInfo ci) {
        if (!ShardClient.isReady() || level == null) return;
        byte id = packet.getEventId();
        //? if >=1.21.2 {
        if (id != EntityEvent.PROTECTED_FROM_DEATH && id != EntityEvent.DEATH) return;
        //?} else {
        /*// Before 1.21.2 the totem event (35) is TALISMAN_ACTIVATE.
        if (id != EntityEvent.TALISMAN_ACTIVATE && id != EntityEvent.DEATH) return;
        *///?}
        Entity entity = packet.getEntity(level);
        if (!(entity instanceof LivingEntity living)) return;
        if (id == EntityEvent.DEATH) ShardEvents.fireDeath(living);
        else ShardEvents.fireTotemPop(living);
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

    // 26.3: the floating totem belongs to the local player (LocalPlayer.displayItemActivation).
    //? if >=26.3 {
    /*@WrapOperation(method = "handleEntityEvent", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;displayItemActivation(Lnet/minecraft/world/item/ItemStack;)V"))
    private void shard$totemAnimation(net.minecraft.client.player.LocalPlayer player, ItemStack stack, Operation<Void> original) {
        if (ShardClient.isReady() && ShardClient.modules().get(TotemPopModule.class).hideAnimation()) return;
        original.call(player, stack);
    }
    *///?} else {
    @WrapOperation(method = "handleEntityEvent", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GameRenderer;displayItemActivation(Lnet/minecraft/world/item/ItemStack;)V"))
    private void shard$totemAnimation(GameRenderer renderer, ItemStack stack, Operation<Void> original) {
        if (ShardClient.isReady() && ShardClient.modules().get(TotemPopModule.class).hideAnimation()) return;
        original.call(renderer, stack);
    }
    //?}

    // ---- TPS estimate ---------------------------------------------------------------------------

    @Inject(method = "handleSetTime", at = @At(value = "INVOKE", target = SAME_THREAD, shift = At.Shift.AFTER), require = 0)
    private void shard$onTime(net.minecraft.network.protocol.game.ClientboundSetTimePacket packet, CallbackInfo ci) {
        //? if >=1.21.2 {
        gg.shard.client.modules.hud.TpsModule.ESTIMATOR.onTimePacket(packet.gameTime(), System.nanoTime());
        //?} else {
        /*gg.shard.client.modules.hud.TpsModule.ESTIMATOR.onTimePacket(packet.getGameTime(), System.nanoTime());
        *///?}
    }

    // ---- crystal prediction readout ------------------------------------------------------------

    @Inject(method = "handleRemoveEntities", at = @At(value = "INVOKE", target = SAME_THREAD, shift = At.Shift.AFTER), require = 0)
    private void shard$onRemoveEntities(net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket packet, CallbackInfo ci) {
        if (!ShardClient.isReady()) return;
        CrystalOptimizerModule crystals = ShardClient.modules().get(CrystalOptimizerModule.class);
        packet.getEntityIds().forEach(crystals::onServerRemoved);
    }

    // ---- explosions --------------------------------------------------------------------------

    @Inject(method = "handleExplosion", at = @At(value = "INVOKE", target = SAME_THREAD, shift = At.Shift.AFTER))
    private void shard$onExplosion(ClientboundExplodePacket packet, CallbackInfo ci) {
        shard$anchorPredicted = false;
        if (!ShardClient.isReady() || level == null) return;
        //? if >=1.21.2 {
        shard$anchorPredicted = ShardClient.modules().get(AnchorOptimizerModule.class).consumeServerExplosion(packet.center());
        ShardClient.modules().get(CrystalOptimizerModule.class).onExplosion(level, packet.center());
        //?} else {
        /*// Before 1.21.2 the packet has loose coordinates.
        net.minecraft.world.phys.Vec3 center = new net.minecraft.world.phys.Vec3(packet.getX(), packet.getY(), packet.getZ());
        shard$anchorPredicted = ShardClient.modules().get(AnchorOptimizerModule.class).consumeServerExplosion(center);
        ShardClient.modules().get(CrystalOptimizerModule.class).onExplosion(level, center);
        *///?}
    }

    //? if >=1.21.2 {
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
    //?} else {
    /*// Before 1.21.2 the client finalizes an Explosion, which plays the sound and adds the particle
    // itself (ExplosionMixin, which reads this context).
    @WrapOperation(method = "handleExplosion", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Explosion;finalizeExplosion(Z)V"))
    private void shard$serverExplosion(net.minecraft.world.level.Explosion explosion, boolean particles, Operation<Void> original) {
        gg.shard.client.compat.ServerExplosions.begin(shard$anchorPredicted);
        try {
            original.call(explosion, particles);
        } finally {
            gg.shard.client.compat.ServerExplosions.end();
        }
    }
    *///?}

    // ---- entity spawns -----------------------------------------------------------------------

    @Inject(method = "handleAddEntity", at = @At("TAIL"))
    private void shard$onAddEntity(ClientboundAddEntityPacket packet, CallbackInfo ci) {
        if (!ShardClient.isReady() || level == null || packet.getType() != EntityType.END_CRYSTAL) return;
        Entity entity = level.getEntity(packet.getId());
        if (entity != null) ShardClient.modules().get(CrystalOptimizerModule.class).onEntityAdded(entity);
    }
}
