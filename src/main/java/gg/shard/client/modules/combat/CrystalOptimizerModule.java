package gg.shard.client.modules.combat;

import gg.shard.client.ShardClient;
import gg.shard.client.module.Module;
import gg.shard.client.module.ModuleCategory;
import gg.shard.client.module.setting.BoolSetting;
import gg.shard.client.module.setting.DoubleSetting;
import gg.shard.client.module.setting.IntSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Client-side crystal prediction, mirroring Marlow's Crystal Optimizer (remove the crystal you
 * hit at once instead of waiting for the server) and Client Side Crystals (show the crystal you
 * place at once, swapped for the real one when the server confirms). The server stays
 * authoritative: no packet is added, changed or dropped; only what this client displays while
 * it waits changes.
 */
public final class CrystalOptimizerModule extends Module {
    private static final long FAKE_LIFETIME_MS = 1500;

    private final BoolSetting removeOnHit = add(new BoolSetting("Remove when hit", "Drop the crystal you attack right away instead of waiting for the server", true).group("Breaking"));
    private final BoolSetting removeOnExplosion = add(new BoolSetting("Remove on explosions", "Also drop crystals caught in a nearby explosion before the server confirms", false).group("Breaking"));
    private final DoubleSetting explosionRange = add(new DoubleSetting("Explosion range", "How close to an explosion a crystal must be", 4.0, 1.5, 6.0, 0.5, " blocks").group("Breaking"));
    private final BoolSetting showPlaced = add(new BoolSetting("Show placed instantly", "Draw the crystal you place before the server confirms it", true).group("Placing"));
    private final BoolSetting highlight = add(new BoolSetting("Highlight my crystals", "Pulse crystals you just placed so you can tell them apart", false).group("Placing"));
    private final IntSetting highlightMs = add(new IntSetting("Highlight time", "How long the pulse lasts", 400, 100, 1000, 50, " ms").group("Placing"));
    private final BoolSetting hitSound = add(new BoolSetting("Hit sound", "Play a short glass sound when a crystal is dropped", true).group("Feedback"));
    private final BoolSetting hitParticles = add(new BoolSetting("Hit particles", "Show a small burst where the crystal was", true).group("Feedback"));

    private final CrystalPredictor predictor = new CrystalPredictor();
    private final Map<Integer, Fake> fakes = new HashMap<>();
    private int nextFakeId = -2_000_000;
    private int fakesShown;

    private record Fake(BlockPos pos, long createdAt) {}

    public CrystalOptimizerModule() {
        super("Crystal Optimizer", "Crystals vanish when you hit them and appear when you place them, without waiting for the server.", ModuleCategory.COMBAT);
        removeOnExplosion.onChange(v -> {});
        explosionRange.visibleWhen(removeOnExplosion::get);
        highlightMs.visibleWhen(highlight::get);
    }

    @Override
    public boolean defaultEnabled() {
        return true;
    }

    @Override
    public String icon() {
        return "item:end_crystal";
    }

    @Override
    public List<String> conflictingMods() {
        return List.of("marlowcrystal", "clientsidecrystals", "clientsidecrystals_bundle");
    }

    public int removedCount() {
        return predictor.removedTotal();
    }

    public int fakesShown() {
        return fakesShown;
    }

    // ---- hooks called from mixins -----------------------------------------------------------

    /** True for a crystal this client invented; the attack must not reach the server. */
    public boolean isFake(Entity entity) {
        return entity != null && fakes.containsKey(entity.getId());
    }

    /** Runs after vanilla has sent the attack packet and swung. */
    public void onAttack(Entity target) {
        if (!isEnabled() || !(target instanceof EndCrystal crystal) || !removeOnHit.get()) return;
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        long now = System.currentTimeMillis();
        if (fakes.remove(crystal.getId()) != null) {
            level.removeEntity(crystal.getId(), Entity.RemovalReason.DISCARDED);
            return;
        }
        if (!predictor.markRemoved(crystal.getId(), now)) return;
        level.removeEntity(crystal.getId(), Entity.RemovalReason.DISCARDED);
        feedback(level, crystal.getX(), crystal.getY() + 1.0, crystal.getZ());
        ShardClient.LOGGER.debug("Crystal {} removed client-side on hit", crystal.getId());
    }

    /** An explosion packet arrived; optionally drop crystals it will have destroyed. */
    public void onExplosion(ClientLevel level, Vec3 center) {
        if (!isEnabled()) return;
        long now = System.currentTimeMillis();
        expireFakesNear(level, center, 3.0);
        if (!removeOnExplosion.get()) return;
        double range = explosionRange.get();
        for (Entity e : level.entitiesForRendering()) {
            if (!(e instanceof EndCrystal) || e.distanceToSqr(center) > range * range) continue;
            if (predictor.markRemoved(e.getId(), now)) level.removeEntity(e.getId(), Entity.RemovalReason.DISCARDED);
        }
    }

    /** The player is about to use an item on a block; predict a crystal placement. */
    public void onUseItemOn(LocalPlayer player, InteractionHand hand, BlockHitResult hit) {
        if (!isEnabled()) return;
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(Items.END_CRYSTAL)) return;
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        BlockPos base = hit.getBlockPos();
        BlockState state = level.getBlockState(base);
        if (!state.is(Blocks.OBSIDIAN) && !state.is(Blocks.BEDROCK)) return;
        BlockPos above = base.above();
        if (!level.getBlockState(above).isAir() || !level.getBlockState(above.above()).isAir()) return;
        AABB box = new AABB(above.getX(), above.getY(), above.getZ(), above.getX() + 1.0, above.getY() + 2.0, above.getZ() + 1.0);
        if (!level.getEntities((Entity) null, box).isEmpty()) return;
        long now = System.currentTimeMillis();
        predictor.markPlaced(CrystalPredictor.posKey(above.getX(), above.getY(), above.getZ()), now);
        if (showPlaced.get()) spawnFake(level, above, now);
    }

    private void spawnFake(ClientLevel level, BlockPos pos, long now) {
        EndCrystal fake = new EndCrystal(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        fake.setId(nextFakeId--);
        fake.setShowBottom(false);
        level.addEntity(fake);
        fakes.put(fake.getId(), new Fake(pos.immutable(), now));
        fakesShown++;
        ShardClient.LOGGER.debug("Fake crystal {} shown at {}", fake.getId(), pos);
    }

    /** The server spawned an entity; a real crystal replaces the fake at the same block. */
    public void onEntityAdded(Entity entity) {
        if (fakes.isEmpty() || !(entity instanceof EndCrystal) || isFake(entity)) return;
        BlockPos pos = entity.blockPosition();
        ClientLevel level = Minecraft.getInstance().level;
        for (Iterator<Map.Entry<Integer, Fake>> it = fakes.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, Fake> e = it.next();
            if (e.getValue().pos.equals(pos)) {
                if (level != null) level.removeEntity(e.getKey(), Entity.RemovalReason.DISCARDED);
                it.remove();
            }
        }
    }

    private void expireFakesNear(ClientLevel level, Vec3 center, double radius) {
        for (Iterator<Map.Entry<Integer, Fake>> it = fakes.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, Fake> e = it.next();
            BlockPos p = e.getValue().pos;
            if (center.distanceToSqr(p.getX() + 0.5, p.getY() + 1.0, p.getZ() + 0.5) <= radius * radius) {
                level.removeEntity(e.getKey(), Entity.RemovalReason.DISCARDED);
                it.remove();
            }
        }
    }

    @Override
    public void onTick() {
        long now = System.currentTimeMillis();
        predictor.prune(now);
        if (fakes.isEmpty()) return;
        ClientLevel level = Minecraft.getInstance().level;
        for (Iterator<Map.Entry<Integer, Fake>> it = fakes.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, Fake> e = it.next();
            if (now - e.getValue().createdAt > FAKE_LIFETIME_MS) {
                if (level != null) level.removeEntity(e.getKey(), Entity.RemovalReason.DISCARDED);
                it.remove();
            }
        }
    }

    @Override
    protected void onDisable() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) for (int id : fakes.keySet()) level.removeEntity(id, Entity.RemovalReason.DISCARDED);
        fakes.clear();
        predictor.clear();
    }

    /** Scale multiplier for a crystal at (x, y, z): a short pulse for crystals you just placed. */
    public float highlightScale(double x, double y, double z) {
        if (!isEnabled() || !highlight.get()) return 1f;
        long key = CrystalPredictor.posKey((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
        double progress = predictor.placedProgress(key, System.currentTimeMillis(), highlightMs.get());
        if (progress < 0) return 1f;
        return 1f + 0.18f * (float) Math.sin(progress * Math.PI);
    }

    private void feedback(ClientLevel level, double x, double y, double z) {
        if (hitSound.get()) level.playLocalSound(x, y, z, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 0.5f, 1.3f, false);
        if (hitParticles.get()) {
            for (int i = 0; i < 4; i++) {
                level.addParticle(ParticleTypes.CRIT, x + (Math.random() - 0.5) * 0.6, y + Math.random() * 0.6, z + (Math.random() - 0.5) * 0.6, 0, 0.05, 0);
            }
        }
    }
}
