package gg.shard.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.SamplerCache;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import net.minecraft.client.gui.font.FontTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Supplier;

/**
 * Sharp text: vanilla samples every glyph atlas with NEAREST filtering, which is right for the
 * pixel font but turns Inter into stair-steps whenever the glyph raster and the screen pixels
 * do not line up exactly (interface size, HUD scale, a HUD element's own scale). Shard's fonts
 * ({@code shard:ui-*}) get LINEAR filtering instead; {@code Fonts} also picks a raster density
 * close to the real one, so the filter only ever smooths small differences. The atlas packer
 * leaves a 1-texel gap between glyphs, so neighbours never bleed in.
 */
@Mixin(FontTexture.class)
abstract class FontTextureMixin {
    @WrapOperation(method = "<init>", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/SamplerCache;getRepeat(Lcom/mojang/blaze3d/textures/FilterMode;)Lcom/mojang/blaze3d/textures/GpuSampler;"),
            require = 0)
    private GpuSampler shard$linearForShardFonts(SamplerCache cache, FilterMode mode, Operation<GpuSampler> original,
                                                 @Local(argsOnly = true) Supplier<String> label) {
        String name = label == null ? null : label.get();
        if (name != null && name.startsWith("shard:ui")) return original.call(cache, FilterMode.LINEAR);
        return original.call(cache, mode);
    }
}
