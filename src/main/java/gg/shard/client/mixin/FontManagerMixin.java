package gg.shard.client.mixin;

import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.blaze3d.systems.RenderSystem;
import gg.shard.client.ShardClient;
import gg.shard.client.gui.LazyFonts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.font.FontManager;
import net.minecraft.client.gui.font.FontOption;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Builds Shard's Inter fonts on first use ({@link LazyFonts}) and logs how long the font stage of
 * each resource reload takes.
 */
@Mixin(FontManager.class)
abstract class FontManagerMixin {
    @Shadow @Final private Map<Identifier, FontSet> fontSets;
    @Shadow @Final private List<GlyphProvider> providersToClose;

    @Shadow
    protected abstract FontSet createFontSet(Identifier id, List<GlyphProvider.Conditional> providers, Set<FontOption> options);

    @Shadow
    private static Set<FontOption> getFontOptions(Options options) {
        throw new AssertionError();
    }

    @Inject(method = "getFontSetRaw", at = @At("HEAD"), cancellable = true)
    private void shard$lazyFont(Identifier id, CallbackInfoReturnable<FontSet> cir) {
        LazyFonts.Spec spec = LazyFonts.parse(id);
        if (spec == null) return;
        FontSet existing = fontSets.get(id);
        if (existing != null) {
            cir.setReturnValue(existing);
            return;
        }
        // Glyph atlases are GPU textures: only the render thread may create them.
        if (!RenderSystem.isOnRenderThread()) return;
        long start = System.nanoTime();
        GlyphProvider provider = LazyFonts.load(spec);
        if (provider == null) return;
        providersToClose.add(provider);
        List<GlyphProvider.Conditional> providers = new ArrayList<>();
        providers.add(new GlyphProvider.Conditional(provider, FontOption.Filter.ALWAYS_PASS));
        // Hearts, shields and arrows come from Minecraft's own font, as the old reference did.
        FontSet fallback = fontSets.get(Identifier.withDefaultNamespace("default"));
        if (fallback instanceof LazyFonts.Hook hook && hook.shard$providers() != null) providers.addAll(hook.shard$providers());
        FontSet set;
        LazyFonts.setCreating(true);
        try {
            set = createFontSet(id, providers, getFontOptions(Minecraft.getInstance().options));
        } finally {
            LazyFonts.setCreating(false);
        }
        ((LazyFonts.Hook) set).shard$markLazy();
        fontSets.put(id, set);
        long us = (System.nanoTime() - start) / 1000;
        if (us > 4000) ShardClient.LOGGER.info("Shard: built font {} in {} us", id, us);
        else ShardClient.LOGGER.debug("Shard: built font {} in {} us", id, us);
        cir.setReturnValue(set);
    }

    @Inject(method = "prepare", at = @At("RETURN"), require = 0)
    private void shard$timeFonts(ResourceManager manager, Executor executor, CallbackInfoReturnable<CompletableFuture<?>> cir) {
        long start = System.nanoTime();
        CompletableFuture<?> future = cir.getReturnValue();
        if (future == null) return;
        future.whenComplete((v, e) ->
                ShardClient.LOGGER.info("Shard: fonts loaded in {} ms", (System.nanoTime() - start) / 1_000_000));
    }
}
