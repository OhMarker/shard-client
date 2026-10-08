package gg.shard.client.gui;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * Anti-aliased corner textures for rounded rectangles, generated at runtime: a white disc (or
 * ring, for outlines) of the requested pixel radius, with 4x4 supersampled coverage in the
 * alpha channel ({@link CornerMask}), cached per radius and registered as a
 * {@link DynamicTexture}. The four quadrants of one disc are the four corners of a rectangle;
 * {@link Render2D} blits them and fills the edges and the centre with plain quads, so only the
 * curves need a texture.
 */
final class RoundedTextures {
    private RoundedTextures() {}

    private static final Map<Long, Identifier> CACHE = new HashMap<>();

    /** A filled disc of {@code radiusPx} pixels; the texture is {@code 2 * radiusPx} square. */
    static Identifier disc(int radiusPx) {
        return get(radiusPx, 0);
    }

    /** A ring of {@code ringPx} pixels on the outside of a disc of {@code radiusPx} pixels. */
    static Identifier ring(int radiusPx, int ringPx) {
        return get(radiusPx, Math.max(1, ringPx));
    }

    static int cached() {
        return CACHE.size();
    }

    private static Identifier get(int radiusPx, int ringPx) {
        int r = Math.max(1, radiusPx);
        long key = ((long) r << 32) | (ringPx & 0xFFFFFFFFL);
        Identifier id = CACHE.get(key);
        if (id != null) return id;
        id = Identifier.fromNamespaceAndPath("shard", "rounded/" + r + "_" + ringPx);
        int size = r * 2;
        int[] mask = CornerMask.mask(r, ringPx);
        NativeImage image = new NativeImage(size, size, false);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) image.setPixel(x, y, mask[y * size + x]);
        }
        DynamicTexture texture = new DynamicTexture(() -> "shard rounded " + r + "/" + ringPx, image);
        Minecraft.getInstance().getTextureManager().register(id, texture);
        CACHE.put(key, id);
        return id;
    }
}
