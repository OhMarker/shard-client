package gg.shard.client.gui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import gg.shard.client.module.Module;
import gg.shard.client.module.ModuleCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Shard's icon set: Lucide (ISC licence, credited in README) rasterised by
 * {@code tools/icons/build_icons.py} into white-on-transparent atlases at 32, 64 and 128 px per
 * cell. The atlases are linear-filtered ({@code .mcmeta} "blur") and tinted at draw time, and
 * each draw picks the smallest atlas whose cells are at least as large as the icon is on screen,
 * so icons stay crisp at every GUI scale, interface size and on 4K screens.
 *
 * <p>Modules name their icon with {@link Module#icon()}; the names are listed in
 * {@code tools/icons/icons.txt}. Unknown names fall back to the module's category icon.
 */
public final class Icons {
    private Icons() {}

    private static final String INDEX = "/assets/shard/textures/gui/icons.json";
    /** Old 0.3.0 glyph names that are still passed around by older call sites. */
    private static final Map<String, String> ALIASES = Map.of(
            "keys", "input", "back", "chevron-left", "folder", "profile", "globe", "server-rule",
            "generic", "copy", "box", "hitbox");

    private static Map<String, Integer> cells;
    private static int columns = 16;
    private static int rows = 1;
    private static int[] sizes = {32, 64, 128};

    public static String categoryIcon(ModuleCategory category) {
        return switch (category) {
            case HUD -> "hud";
            case VISUALS -> "visuals";
            case COMBAT -> "combat";
            case INPUT -> "input";
            case PERFORMANCE -> "performance";
            case UTILITY -> "utility";
        };
    }

    /** Kept for call sites written against 0.3.0. */
    public static String categoryGlyph(ModuleCategory category) {
        return categoryIcon(category);
    }

    public static boolean has(String name) {
        return name != null && index().containsKey(resolve(name));
    }

    /** Draws the module's icon {@code size} units square, tinted {@code color}. */
    public static void draw(GuiGraphics g, Module module, int x, int y, int size, int color) {
        String name = module.icon();
        if (!has(name)) name = categoryIcon(module.category());
        draw(g, name, x, y, size, color);
    }

    public static void draw(GuiGraphics g, Module module, int x, int y, int color) {
        draw(g, module, x, y, 16, color);
    }

    /** Draws icon {@code name} at 16 units. */
    public static void draw(GuiGraphics g, String name, int x, int y, int color) {
        draw(g, name, x, y, 16, color);
    }

    /** Draws icon {@code name} {@code size} units square at (x, y), tinted {@code color}. */
    public static void draw(GuiGraphics g, String name, int x, int y, int size, int color) {
        Integer cell = index().get(resolve(name));
        if (cell == null || size <= 0 || (color >>> 24) == 0) return;
        int cellPx = Scale.atLeast(size * Render2D.pixelsPerUnit(), sizes, 0.5);
        Identifier texture = Identifier.fromNamespaceAndPath("shard", "textures/gui/icons_" + cellPx + ".png");
        int u = (cell % columns) * cellPx;
        int v = (cell / columns) * cellPx;
        g.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, (float) u, (float) v, size, size, cellPx, cellPx,
                columns * cellPx, rows * cellPx, color);
    }

    private static String resolve(String name) {
        if (name == null) return "";
        if (name.startsWith("icon:")) name = name.substring(5);
        return ALIASES.getOrDefault(name, name);
    }

    private static Map<String, Integer> index() {
        if (cells != null) return cells;
        Map<String, Integer> map = new HashMap<>();
        try (InputStream in = Icons.class.getResourceAsStream(INDEX)) {
            if (in != null) {
                JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                columns = root.get("columns").getAsInt();
                rows = root.get("rows").getAsInt();
                var list = root.getAsJsonArray("sizes");
                sizes = new int[list.size()];
                for (int i = 0; i < sizes.length; i++) sizes[i] = list.get(i).getAsInt();
                for (var e : root.getAsJsonObject("icons").entrySet()) map.put(e.getKey(), e.getValue().getAsInt());
            }
        } catch (Exception ignored) {
            // A broken index leaves the map empty; callers simply draw nothing.
        }
        cells = map;
        return cells;
    }
}
