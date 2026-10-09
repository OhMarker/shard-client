package gg.shard.client.gui;

import com.mojang.blaze3d.font.GlyphProvider;
import gg.shard.client.ShardClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.font.providers.TrueTypeGlyphProviderDefinition;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shard's Inter fonts are created on first use instead of being listed as font definitions.
 *
 * <p>0.7.1 shipped 330 definitions (weight x size x raster density). Minecraft opens every TTF
 * provider while resources load and then builds each font's width table on the render thread by
 * loading the metrics of every Inter glyph and every fallback glyph: seconds of font loading in
 * the start-up reload (the "Not Responding" Mojang screen) and the same wait on every resource
 * pack change. Now no definition exists; {@code FontManagerMixin} builds the font
 * {@code shard:ui-<weight>-<size>-d<density x 100>} the first time something draws or measures it
 * (one TrueType face, about a millisecond), adds it to the font manager like any loaded font, and
 * the next resource reload closes it with the others. Because fonts cost nothing until used, the
 * raster density can be exactly the on-screen density (any HUD scale), so glyphs are drawn 1:1.
 */
public final class LazyFonts {
    private LazyFonts() {}

    /** One requested font: weight id ("regular", "medium", "semibold"), size in design units, pixels per unit. */
    public record Spec(String weight, int size, double density) {}

    private static final Pattern ID = Pattern.compile("ui-(regular|medium|semibold)-(\\d{1,3})-d(\\d{1,5})");
    private static boolean creating;

    /** Implemented by {@code FontSetMixin}. */
    public interface Hook {
        List<GlyphProvider.Conditional> shard$providers();

        void shard$markLazy();
    }

    /** The font a path of {@code shard:ui-<weight>-<size>-d<density x 100>} asks for, or null (pure, tested). */
    public static Spec parse(String namespace, String path) {
        if (!"shard".equals(namespace) || path == null) return null;
        Matcher m = ID.matcher(path);
        if (!m.matches()) return null;
        int size = Integer.parseInt(m.group(2));
        int density = Integer.parseInt(m.group(3));
        if (size < 4 || size > 96 || density < 25 || density > 2400) return null;
        return new Spec(m.group(1), size, density / 100.0);
    }

    public static Spec parse(Identifier id) {
        return id == null ? null : parse(id.getNamespace(), id.getPath());
    }

    /** Path for a font; density is rounded to hundredths (pure, tested). */
    public static String path(String weight, int size, double density) {
        return "ui-" + weight + "-" + size + "-d" + Math.round(density * 100);
    }

    /** Opens Inter in this weight at this size and density, or null if it cannot be read. */
    public static GlyphProvider load(Spec spec) {
        Identifier file = Identifier.fromNamespaceAndPath("shard", "inter-" + spec.weight() + ".ttf");
        TrueTypeGlyphProviderDefinition definition = new TrueTypeGlyphProviderDefinition(file, spec.size(), (float) spec.density(),
                TrueTypeGlyphProviderDefinition.Shift.NONE, "");
        try {
            return definition.unpack().left().orElseThrow().load(Minecraft.getInstance().getResourceManager());
        } catch (Exception e) {
            ShardClient.LOGGER.error("Could not open Inter {} at {} x{}", spec.weight(), spec.size(), spec.density(), e);
            return null;
        }
    }

    /** True while {@code FontManagerMixin} builds a font; the font set then skips its width table. */
    public static boolean creating() {
        return creating;
    }

    public static void setCreating(boolean value) {
        creating = value;
    }
}
