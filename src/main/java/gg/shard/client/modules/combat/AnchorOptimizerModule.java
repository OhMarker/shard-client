package gg.shard.client.modules.combat;

import gg.shard.client.ShardClient;
import gg.shard.client.module.Module;
import gg.shard.client.module.ModuleCategory;
import gg.shard.client.module.setting.BoolSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Client-side respawn-anchor prediction in the spirit of the Anchor Optimizer mods: the anchor
 * you detonate disappears at once and the glowstone you add shows at once, instead of after a
 * round trip. The server still decides what really happened and its block updates win.
 */
public final class AnchorOptimizerModule extends Module {
    /** Vanilla's block-update flags for client-side changes (update + re-render, no neighbours). */
    private static final int CLIENT_UPDATE_FLAGS = 19;
    private static final long PREDICTION_WINDOW_MS = 1200;

    private final BoolSetting instantRemoval = add(new BoolSetting("Instant removal", "Remove the anchor the moment you detonate it", true).group("Prediction"));
    private final BoolSetting localSound = add(new BoolSetting("Explosion sound", "Play the explosion sound immediately (the server's copy is skipped)", true).group("Feedback"));
    private final BoolSetting reduceParticles = add(new BoolSetting("Reduce particles", "Skip the server's explosion emitter for anchors you predicted", true).group("Feedback"));

    private final AnchorPredictor predictor = new AnchorPredictor();
    private int predictedExplosions;

    public AnchorOptimizerModule() {
        super("Anchor Optimizer", "Anchors vanish when you blow them and charge when you feed them, without waiting for the server.", ModuleCategory.COMBAT);
    }

    @Override
    public boolean defaultEnabled() {
        return true;
    }

    @Override
    public String icon() {
        return "item:respawn_anchor";
    }

    @Override
    public List<String> conflictingMods() {
        return List.of("client_side_anchors", "herosanchoroptimizer", "anchoroptimizer");
    }

    public int predictedExplosions() {
        return predictedExplosions;
    }

    static boolean anchorWorksIn(Level level) {
        return level.dimension() == Level.NETHER;
    }

    /**
     * Called after vanilla has processed a block interaction and sent its packet. Charging is
     * already predicted by vanilla itself (RespawnAnchorBlock.charge runs on the client), so
     * only the explosion needs help here.
     */
    public void onUseItemOn(LocalPlayer player, InteractionHand hand, BlockHitResult hit) {
        if (!isEnabled()) return;
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        BlockPos pos = hit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (!state.is(Blocks.RESPAWN_ANCHOR)) return;
        int charge = state.getValue(RespawnAnchorBlock.CHARGE);
        boolean glowstone = player.getItemInHand(hand).is(Items.GLOWSTONE);
        AnchorPredictor.Outcome outcome = AnchorPredictor.predict(charge, glowstone, anchorWorksIn(level), player.isSecondaryUseActive());
        long now = System.currentTimeMillis();
        switch (outcome) {
            case EXPLODE -> {
                long key = CrystalPredictor.posKey(pos.getX(), pos.getY(), pos.getZ());
                if (!predictor.markPredicted(key, now, PREDICTION_WINDOW_MS)) return;
                predictedExplosions++;
                double x = pos.getX() + 0.5;
                double y = pos.getY() + 0.5;
                double z = pos.getZ() + 0.5;
                if (instantRemoval.get()) level.setBlock(pos, Blocks.AIR.defaultBlockState(), CLIENT_UPDATE_FLAGS);
                if (localSound.get()) {
                    float pitch = (1.0f + (level.random.nextFloat() - level.random.nextFloat()) * 0.2f) * 0.7f;
                    level.playLocalSound(x, y, z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 4.0f, pitch, false);
                }
                if (!reduceParticles.get()) level.addParticle(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 1.0, 0.0, 0.0);
                ShardClient.LOGGER.debug("Anchor at {} predicted to explode", pos);
            }
            default -> {
            }
        }
    }

    /** True when the server's explosion is one we already showed; the caller then mutes it. */
    public boolean consumeServerExplosion(Vec3 center) {
        if (!isEnabled()) return false;
        BlockPos pos = BlockPos.containing(center);
        long now = System.currentTimeMillis();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    long key = CrystalPredictor.posKey(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
                    if (predictor.consume(key, now, PREDICTION_WINDOW_MS)) return true;
                }
            }
        }
        return false;
    }

    public boolean mutesServerSound() {
        return localSound.get();
    }

    public boolean skipsServerParticles() {
        return reduceParticles.get();
    }

    @Override
    public void onTick() {
        predictor.prune(System.currentTimeMillis(), PREDICTION_WINDOW_MS);
    }

    @Override
    protected void onDisable() {
        predictor.clear();
    }
}
