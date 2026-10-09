package gg.shard.client.cosmetics;

import com.mojang.blaze3d.platform.NativeImage;
//? if >=1.21.5 {
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
//?} else {
/*import com.mojang.blaze3d.platform.TextureUtil;
*///?}
import net.minecraft.client.renderer.texture.AbstractTexture;

import java.util.List;

/**
 * A cape texture with a full mip chain and trilinear filtering. Vanilla uploads capes as one
 * level sampled with NEAREST, which is right for 64x32 pixel art; a 4096x2048 painting drawn a
 * few hundred pixels tall would shimmer and look jagged that way. Must be created on the render
 * thread.
 */
public final class CapeTexture extends AbstractTexture {

    //? if <1.21.5 {
    /*// Before 1.21.5 textures are plain GL names: allocate the mip chain, upload each level, and
    // set trilinear filtering and clamping (1.21.2/1.21.3 set both per upload).
    public CapeTexture(String label, List<MipChain.Level> levels) {
        MipChain.Level base = levels.get(0);
        TextureUtil.prepareImage(getId(), levels.size() - 1, base.width(), base.height());
        for (int level = 0; level < levels.size(); level++) {
            MipChain.Level l = levels.get(level);
            try (NativeImage image = new NativeImage(l.width(), l.height(), false)) {
                int[] px = l.argb();
                for (int y = 0; y < l.height(); y++) {
                    int row = y * l.width();
                    for (int x = 0; x < l.width(); x++) image.setPixel(x, y, px[row + x]);
                }
                //? if >=1.21.4 {
                image.upload(level, 0, 0, 0, 0, l.width(), l.height(), false);
                //?} else {
                /^image.upload(level, 0, 0, 0, 0, l.width(), l.height(), true, true, levels.size() > 1, false);
                ^///?}
            }
        }
        this.mipmapped = levels.size() > 1;
        //? if >=1.21.4 {
        setClamp(true);
        //?}
        setFilter(true, mipmapped);
    }

    private final boolean mipmapped;

    /^* Entity render types set NEAREST on every draw (TextureStateShard); this texture stays trilinear. ^/
    @Override
    public void setFilter(boolean blur, boolean mipmap) {
        super.setFilter(true, mipmapped);
    }

    //? if <1.21.4 {
    /^@Override
    public void load(net.minecraft.server.packs.resources.ResourceManager manager) {
    }
    ^///?}
    *///?} else {
    public CapeTexture(String label, List<MipChain.Level> levels) {
        GpuDevice device = RenderSystem.getDevice();
        MipChain.Level base = levels.get(0);
        //? if >=1.21.6 {
        // Usage 5 = copy destination + texture binding, the same as vanilla's DynamicTexture.
        this.texture = device.createTexture(() -> label, GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING,
                TextureFormat.RGBA8, base.width(), base.height(), 1, levels.size());
        //?} else {
        /*// Before 1.21.6 textures have no usage flags, layers or views.
        this.texture = device.createTexture(() -> label, TextureFormat.RGBA8, base.width(), base.height(), levels.size());
        *///?}
        //? if >=1.21.11 {
        this.sampler = RenderSystem.getSamplerCache().getSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE,
                FilterMode.LINEAR, FilterMode.LINEAR, true);
        //?} else {
        /*this.texture.setAddressMode(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE);
        this.texture.setTextureFilter(FilterMode.LINEAR, FilterMode.LINEAR, true);
        *///?}
        //? if >=1.21.6 {
        this.textureView = device.createTextureView(this.texture);
        //?}
        CommandEncoder encoder = device.createCommandEncoder();
        for (int level = 0; level < levels.size(); level++) {
            MipChain.Level l = levels.get(level);
            try (NativeImage image = new NativeImage(l.width(), l.height(), false)) {
                int[] px = l.argb();
                for (int y = 0; y < l.height(); y++) {
                    int row = y * l.width();
                    for (int x = 0; x < l.width(); x++) image.setPixel(x, y, px[row + x]);
                }
                //? if >=26.2 {
                /*encoder.writeToTexture(this.texture, image, level, 0, 0, 0);
                *///?} else if >=1.21.6 {
                encoder.writeToTexture(this.texture, image, level, 0, 0, 0, l.width(), l.height(), 0, 0);
                //?} else {
                /*encoder.writeToTexture(this.texture, image, level, 0, 0, l.width(), l.height(), 0, 0);
                *///?}
            }
        }
    }
    //?}
}
