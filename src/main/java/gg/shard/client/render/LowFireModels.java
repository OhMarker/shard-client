package gg.shard.client.render;

import gg.shard.client.ShardClient;
import gg.shard.client.modules.visual.LowFireModule;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBlockStateModel;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Predicate;

/**
 * Low Fire's ground fire: fire and soul fire block models are wrapped (Fabric model loading
 * API) so their quads can be made lower, tinted and see-through while the world mesh is built.
 * This goes through the Fabric rendering API, which both vanilla (Indigo) and Sodium honour, so
 * it works with the launcher's bundled mods. Only how the block looks changes: the block, its
 * hitbox and the damage it does are untouched.
 *
 * <p>Chunk meshes are built on worker threads, so they read an immutable snapshot of the
 * settings; when the snapshot changes (a slider moves, the module is switched, a server rule
 * kicks in) the world is re-meshed once the value has settled for a moment.
 */
public final class LowFireModels {
    private LowFireModels() {}

    /** What the mesher needs; {@code height} 0..1, colours ARGB (alpha is opacity). */
    public record Ground(boolean active, float height, int fireColor, int soulColor) {
        static final Ground OFF = new Ground(false, 1f, 0xFFFFFFFF, 0xFFFFFFFF);
    }

    private static volatile Ground current = Ground.OFF;
    private static Ground pending = Ground.OFF;
    private static long pendingSince;

    public static void init() {
        ModelLoadingPlugin.register(ctx -> ctx.modifyBlockModelAfterBake().register((model, context) -> {
            BlockState state = context.state();
            if (state.is(Blocks.FIRE)) return new FireModel(model, false);
            if (state.is(Blocks.SOUL_FIRE)) return new FireModel(model, true);
            return model;
        }));
        ClientTickEvents.END_CLIENT_TICK.register(client -> tick());
    }

    private static void tick() {
        if (!ShardClient.isReady()) return;
        Ground wanted = ShardClient.modules().get(LowFireModule.class).groundSnapshot();
        long now = System.currentTimeMillis();
        if (!wanted.equals(pending)) {
            pending = wanted;
            pendingSince = now;
        }
        // Wait until the slider has stopped for 250 ms; re-meshing on every step would stutter.
        if (!pending.equals(current) && now - pendingSince > 250) {
            current = pending;
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) mc.levelRenderer.allChanged();
        }
    }

    private static final class FireModel extends WrapperBlockStateModel {
        private final boolean soul;

        FireModel(BlockStateModel wrapped, boolean soul) {
            super(wrapped);
            this.soul = soul;
        }

        @Override
        public void emitQuads(QuadEmitter emitter, BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random,
                              Predicate<Direction> cullTest) {
            Ground g = current;
            if (!g.active()) {
                super.emitQuads(emitter, level, pos, state, random, cullTest);
                return;
            }
            int tint = soul ? g.soulColor() : g.fireColor();
            int alpha = tint >>> 24;
            emitter.pushTransform(q -> {
                for (int i = 0; i < 4; i++) {
                    q.pos(i, q.x(i), q.y(i) * g.height(), q.z(i));
                    q.color(i, multiply(q.color(i), tint));
                }
                if (alpha < 255) q.renderLayer(ChunkSectionLayer.TRANSLUCENT);
                return true;
            });
            try {
                super.emitQuads(emitter, level, pos, state, random, cullTest);
            } finally {
                emitter.popTransform();
            }
        }
    }

    /** Component-wise ARGB multiply. */
    public static int multiply(int a, int b) {
        int al = ((a >>> 24) * (b >>> 24)) / 255;
        int r = (((a >> 16) & 0xFF) * ((b >> 16) & 0xFF)) / 255;
        int gr = (((a >> 8) & 0xFF) * ((b >> 8) & 0xFF)) / 255;
        int bl = ((a & 0xFF) * (b & 0xFF)) / 255;
        return al << 24 | r << 16 | gr << 8 | bl;
    }
}
