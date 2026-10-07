package gg.shard.client.gui;

import gg.shard.client.module.Module;
import gg.shard.client.module.ModuleCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/** Resolves a module's {@link Module#icon()} spec to a vanilla item or a built-in glyph. */
public final class Icons {
    private Icons() {}

    private static final Map<String, ItemStack> ITEMS = new HashMap<>();

    public static String categoryGlyph(ModuleCategory category) {
        return switch (category) {
            case HUD -> "hud";
            case VISUALS -> "visuals";
            case COMBAT -> "combat";
            case INPUT -> "keys";
            case PERFORMANCE -> "performance";
        };
    }

    /** Draws a 16x16 icon at (x, y); {@code color} is used for glyphs only. */
    public static void draw(GuiGraphics g, Module module, int x, int y, int color) {
        String spec = module.icon();
        if (spec != null && spec.startsWith("item:")) {
            ItemStack stack = item(spec.substring(5));
            if (stack != null) {
                g.renderItem(stack, x, y);
                return;
            }
        }
        String glyph = spec != null && spec.startsWith("glyph:") ? spec.substring(6) : null;
        if (glyph == null || !Glyphs.has(glyph)) glyph = categoryGlyph(module.category());
        Glyphs.draw(g, glyph, x, y, color);
    }

    private static ItemStack item(String id) {
        return ITEMS.computeIfAbsent(id, key -> {
            Identifier location = Identifier.tryParse(key.contains(":") ? key : "minecraft:" + key);
            if (location == null) return null;
            Item item = BuiltInRegistries.ITEM.getOptional(location).orElse(null);
            return item == null ? null : new ItemStack(item);
        });
    }
}
