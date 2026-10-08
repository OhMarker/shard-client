package gg.shard.client.cosmetics;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import net.minecraft.client.renderer.texture.AbstractTexture;

import java.util.List;

/**
 * A cape texture with a full mip chain and trilinear filtering. Vanilla uploads capes as one
 * level sampled with NEAREST, which is right for 64x32 pixel art; a 4096x2048 painting drawn a
 * few hundred pixels tall would shimmer and look jagged that way. Must be created on the render
 * thread.
 */
public final class CapeTexture extends AbstractTexture {

    public CapeTexture(String label, List<MipChain.Level> levels) {
        GpuDevice device = RenderSystem.getDevice();
        MipChain.Level base = levels.get(0);
        // Usage 5 = copy destination + texture binding, the same as vanilla's DynamicTexture.
        this.texture = device.createTexture(() -> label, GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING,
                TextureFormat.RGBA8, base.width(), base.height(), 1, levels.size());
        this.sampler = RenderSystem.getSamplerCache().getSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE,
                FilterMode.LINEAR, FilterMode.LINEAR, true);
        this.textureView = device.createTextureView(this.texture);
        CommandEncoder encoder = device.createCommandEncoder();
        for (int level = 0; level < levels.size(); level++) {
            MipChain.Level l = levels.get(level);
            try (NativeImage image = new NativeImage(l.width(), l.height(), false)) {
                int[] px = l.argb();
                for (int y = 0; y < l.height(); y++) {
                    int row = y * l.width();
                    for (int x = 0; x < l.width(); x++) image.setPixel(x, y, px[row + x]);
                }
                encoder.writeToTexture(this.texture, image, level, 0, 0, 0, l.width(), l.height(), 0, 0);
            }
        }
    }
}
