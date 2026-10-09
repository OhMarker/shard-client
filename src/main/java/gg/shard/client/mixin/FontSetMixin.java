package gg.shard.client.mixin;

import com.mojang.blaze3d.font.GlyphProvider;
import gg.shard.client.gui.LazyFonts;
import net.minecraft.client.gui.font.FontOption;
import net.minecraft.client.gui.font.FontSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Shard's lazily built fonts skip the width table vanilla builds for every font set: it loads the
 * metrics of every glyph of every provider (Inter plus all of Minecraft's fallback fonts) and is
 * only used for obfuscated text, which Shard's UI never draws. That table is what made 330 Inter
 * definitions take seconds on the render thread.
 */
@Mixin(FontSet.class)
abstract class FontSetMixin implements LazyFonts.Hook {
    @Shadow private List<GlyphProvider.Conditional> allProviders;
    @Unique private boolean shard$lazy;

    @Override
    public List<GlyphProvider.Conditional> shard$providers() {
        return allProviders;
    }

    @Override
    public void shard$markLazy() {
        shard$lazy = true;
    }

    /**
     * Vanilla's version walks every glyph of every provider to build the width table and to drop
     * providers no glyph comes from. For Shard's fonts: keep every provider whose filter passes
     * (Inter first, then Minecraft's fallbacks) and skip the walk.
     */
    @Inject(method = "selectProviders", at = @At("HEAD"), cancellable = true, require = 0)
    private void shard$selectQuickly(List<GlyphProvider.Conditional> providers, Set<FontOption> options,
                                     CallbackInfoReturnable<List<GlyphProvider>> cir) {
        if (!shard$lazy && !LazyFonts.creating()) return;
        List<GlyphProvider> out = new ArrayList<>();
        for (GlyphProvider.Conditional c : providers) if (c.filter().apply(options)) out.add(c.provider());
        cir.setReturnValue(out);
    }

    //? if <1.21.5 {
    /*@Shadow @org.spongepowered.asm.mixin.Final private net.minecraft.resources.Identifier name;

    /^*
     * Sharp text before 1.21.5 (FontTextureMixin's job on newer versions): glyph atlases are plain
     * GL textures whose filter the text render types reset through AbstractTexture's cache, and
     * 1.21.2/1.21.3 glyph uploads set NEAREST on every upload. After a glyph lands on one of
     * Shard's atlases, set LINEAR on the GL texture directly (the cache keeps saying NEAREST, so
     * nothing resets it).
     ^/
    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = "stitch", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/font/FontTexture;add(Lcom/mojang/blaze3d/font/SheetGlyphInfo;)Lnet/minecraft/client/gui/font/glyphs/BakedGlyph;"))
    private net.minecraft.client.gui.font.glyphs.BakedGlyph shard$linearForShardFonts(net.minecraft.client.gui.font.FontTexture texture,
            com.mojang.blaze3d.font.SheetGlyphInfo glyph, com.llamalad7.mixinextras.injector.wrapoperation.Operation<net.minecraft.client.gui.font.glyphs.BakedGlyph> original) {
        net.minecraft.client.gui.font.glyphs.BakedGlyph baked = original.call(texture, glyph);
        if (baked != null && "shard".equals(name.getNamespace()) && name.getPath().startsWith("ui")) {
            com.mojang.blaze3d.platform.GlStateManager._bindTexture(texture.getId());
            com.mojang.blaze3d.platform.GlStateManager._texParameter(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER, org.lwjgl.opengl.GL11.GL_LINEAR);
            com.mojang.blaze3d.platform.GlStateManager._texParameter(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER, org.lwjgl.opengl.GL11.GL_LINEAR);
        }
        return baked;
    }
    *///?}
}
