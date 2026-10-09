package gg.shard.client.render;

import gg.shard.client.ShardClient;
import gg.shard.client.compat.Atlases;
import gg.shard.client.modules.visual.LowFireModule;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBlockStateModel;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBakery;
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
    public record Ground(boolean active, float height, int fireColor, int soulColor, boolean customTexture) {
        static final Ground OFF = new Ground(false, 1f, 0xFFFFFFFF, 0xFFFFFFFF, false);
    }

    /** Vanilla's and Shard's fire sprites, read on the render thread for the chunk mesher. */
    private record Sprites(TextureAtlasSprite fire0, TextureAtlasSprite fire1, TextureAtlasSprite custom0, TextureAtlasSprite custom1) {}

    private static volatile Sprites sprites;

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
        refreshSprites();
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

    /** Sprites change identity on every resource reload; pick the new ones up on the next tick. */
    private static void refreshSprites() {
        try {
            TextureAtlasSprite f0 = Atlases.sprite(ModelBakery.FIRE_0);
            Sprites s = sprites;
            if (s != null && s.fire0() == f0) return;
            TextureAtlasSprite c0 = Atlases.sprite(LowFireModule.CUSTOM_FIRE_0);
            TextureAtlasSprite c1 = Atlases.sprite(LowFireModule.CUSTOM_FIRE_1);
            // A missing custom sprite (atlas not stitched yet) keeps vanilla's.
            boolean ok = !c0.contents().name().getPath().contains("missingno");
            sprites = new Sprites(f0, Atlases.sprite(ModelBakery.FIRE_1), ok ? c0 : null, ok ? c1 : null);
            if (s != null && current.customTexture()) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.level != null) mc.levelRenderer.allChanged();
            }
        } catch (RuntimeException ignored) {
            // Atlas not ready yet (very early in start-up).
        }
    }

    /** Moves the quad's UVs from vanilla's fire sprite to Shard's when it uses one. */
    static void swapSprite(MutableQuadView q) {
        Sprites s = sprites;
        if (s == null || s.custom0() == null) return;
        float u = 0f;
        float v = 0f;
        for (int i = 0; i < 4; i++) {
            u += q.u(i);
            v += q.v(i);
        }
        u /= 4f;
        v /= 4f;
        TextureAtlasSprite from = contains(s.fire0(), u, v) ? s.fire0() : contains(s.fire1(), u, v) ? s.fire1() : null;
        if (from == null) return;
        TextureAtlasSprite to = from == s.fire0() ? s.custom0() : s.custom1();
        for (int i = 0; i < 4; i++) {
            float nu = (q.u(i) - from.getU0()) / (from.getU1() - from.getU0());
            float nv = (q.v(i) - from.getV0()) / (from.getV1() - from.getV0());
            q.uv(i, to.getU0() + nu * (to.getU1() - to.getU0()), to.getV0() + nv * (to.getV1() - to.getV0()));
        }
    }

    private static boolean contains(TextureAtlasSprite s, float u, float v) {
        return u >= s.getU0() && u <= s.getU1() && v >= s.getV0() && v <= s.getV1();
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
            boolean swap = g.customTexture() && !soul;
            if (!g.active() && !swap) {
                super.emitQuads(emitter, level, pos, state, random, cullTest);
                return;
            }
            if (!g.active()) {
                emitter.pushTransform(q -> {
                    swapSprite(q);
                    return true;
                });
                try {
                    super.emitQuads(emitter, level, pos, state, random, cullTest);
                } finally {
                    emitter.popTransform();
                }
                return;
            }
            int tint = soul ? g.soulColor() : g.fireColor();
            int alpha = tint >>> 24;
            emitter.pushTransform(q -> {
                if (swap) swapSprite(q);
                for (int i = 0; i < 4; i++) {
                    q.pos(i, q.x(i), q.y(i) * g.height(), q.z(i));
                    q.color(i, multiply(q.color(i), tint));
                }
                //? if >=26.1 {
                /*if (alpha < 255) q.chunkLayer(ChunkSectionLayer.TRANSLUCENT);
                *///?} else {
                if (alpha < 255) q.renderLayer(ChunkSectionLayer.TRANSLUCENT);
                //?}

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
