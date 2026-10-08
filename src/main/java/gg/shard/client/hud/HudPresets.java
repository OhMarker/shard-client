package gg.shard.client.hud;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import gg.shard.client.ShardClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static gg.shard.client.hud.HudGeometry.CENTER;
import static gg.shard.client.hud.HudGeometry.END;
import static gg.shard.client.hud.HudGeometry.START;

/**
 * HUD layout presets: which HUD elements are on, where (anchor + offset in HUD units) and at what
 * scale. Three are built in for crystal PvP; players can save their own, stored in the config's
 * {@code gui.hudPresets}. A layout snapshot (the same shape) also backs the editor's undo.
 *
 * <p>Built-in positions are tuned for GUI scale 2 at 1080p: the kit row sits above the hearts,
 * totems left of the offhand slot and armour right of the hotbar, info top-left, effects top-right.
 */
public final class HudPresets {
    private HudPresets() {}

    /** One element in a layout: on or off, and {anchorX, anchorY, offX, offY, scale}. */
    public record Entry(boolean enabled, double[] layout) {}

    public static final String MINIMAL = "Crystal PvP minimal";
    public static final String FULL = "Crystal PvP full";
    public static final String STREAMER = "Streamer";
    /** FPS, ping and totems only (Quick setup: Minimal). */
    public static final String BARE = "Bare";

    private static double[] at(int ax, int ay, double ox, double oy) {
        return new double[]{ax, ay, ox, oy, 1.0};
    }

    public static List<String> builtIn() {
        return List.of(MINIMAL, FULL, STREAMER, BARE);
    }

    /** Built-in layout by name, or null. Elements not listed are switched off. */
    public static Map<String, double[]> builtIn(String name) {
        Map<String, double[]> m = new LinkedHashMap<>();
        switch (name) {
            case MINIMAL -> {
                m.put("fps", at(START, START, 4, 4));
                m.put("ping", at(START, START, 4, 20));
                m.put("totem-counter", at(CENTER, END, -150, 4));
                m.put("armor-status", at(CENTER, END, 180, 4));
                m.put("item-counter", at(CENTER, END, 0, 52));
                m.put("hit-delay", at(CENTER, CENTER, 0, 12));
                m.put("target-hud", at(CENTER, CENTER, 90, 30));
                m.put("cooldowns", at(CENTER, END, 0, 78));
            }
            case BARE -> {
                m.put("fps", at(START, START, 4, 4));
                m.put("ping", at(START, START, 4, 20));
                m.put("totem-counter", at(CENTER, END, -150, 4));
            }
            case FULL -> {
                m.putAll(builtIn(MINIMAL));
                m.put("cps", at(START, START, 4, 36));
                m.put("coordinates", at(START, START, 4, 52));
                m.put("keystrokes", at(START, END, 4, 4));
                m.put("potion-effects", at(END, START, 4, 4));
                m.put("session-stats", at(END, CENTER, 4, 0));
                m.put("clock", at(END, END, 4, 4));
            }
            case STREAMER -> {
                // No coordinates or server address on stream.
                m.putAll(builtIn(MINIMAL));
                m.put("cps", at(START, START, 4, 36));
                m.put("keystrokes", at(START, END, 4, 4));
                m.put("potion-effects", at(END, START, 4, 4));
            }
            default -> {
                return null;
            }
        }
        return m;
    }

    // ---- snapshots ----------------------------------------------------------------------------

    public static Map<String, Entry> snapshot(HudManager hud) {
        Map<String, Entry> out = new LinkedHashMap<>();
        for (HudModule m : hud.hudModules()) out.put(m.key(), new Entry(m.isToggledOn(), m.layout()));
        return out;
    }

    public static void restore(HudManager hud, Map<String, Entry> snapshot) {
        for (HudModule m : hud.hudModules()) {
            Entry e = snapshot.get(m.key());
            if (e == null) continue;
            if (m.isToggledOn() != e.enabled()) m.toggle();
            m.applyLayout(e.layout());
        }
        ShardClient.config().markDirty();
    }

    /** Applies a built-in or saved preset; returns false when no preset has that name. */
    public static boolean apply(HudManager hud, String name) {
        Map<String, double[]> built = builtIn(name);
        if (built != null) {
            for (HudModule m : hud.hudModules()) {
                double[] l = built.get(m.key());
                if (l == null) {
                    // Toggle Sprint draws on screen but is not a HUD element; leave it alone.
                    if (m.category() == gg.shard.client.module.ModuleCategory.HUD && m.isToggledOn()) m.toggle();
                    continue;
                }
                if (!m.isToggledOn()) m.toggle();
                m.applyLayout(l);
            }
            ShardClient.config().markDirty();
            return true;
        }
        Map<String, Entry> saved = saved().get(name);
        if (saved == null) return false;
        restore(hud, saved);
        return true;
    }

    // ---- saved presets (config gui.hudPresets) -------------------------------------------------

    public static Map<String, Map<String, Entry>> saved() {
        Map<String, Map<String, Entry>> out = new LinkedHashMap<>();
        JsonObject gui = ShardClient.config().gui();
        if (!gui.has("hudPresets") || !gui.get("hudPresets").isJsonObject()) return out;
        for (var preset : gui.getAsJsonObject("hudPresets").entrySet()) {
            if (!preset.getValue().isJsonObject()) continue;
            Map<String, Entry> entries = new LinkedHashMap<>();
            for (var e : preset.getValue().getAsJsonObject().entrySet()) {
                if (!e.getValue().isJsonObject()) continue;
                JsonObject o = e.getValue().getAsJsonObject();
                JsonArray arr = o.getAsJsonArray("layout");
                if (arr == null || arr.size() < 5) continue;
                double[] l = new double[5];
                for (int i = 0; i < 5; i++) l[i] = arr.get(i).getAsDouble();
                entries.put(e.getKey(), new Entry(o.has("enabled") && o.get("enabled").getAsBoolean(), l));
            }
            out.put(preset.getKey(), entries);
        }
        return out;
    }

    public static List<String> savedNames() {
        return new ArrayList<>(saved().keySet());
    }

    /** Saves the current layout under {@code name}; false for an empty or built-in name. */
    public static boolean save(HudManager hud, String name) {
        String n = name == null ? "" : name.trim();
        if (n.isEmpty() || builtIn().contains(n)) return false;
        JsonObject gui = ShardClient.config().gui();
        if (!gui.has("hudPresets") || !gui.get("hudPresets").isJsonObject()) gui.add("hudPresets", new JsonObject());
        JsonObject preset = new JsonObject();
        for (var e : snapshot(hud).entrySet()) {
            JsonObject o = new JsonObject();
            o.addProperty("enabled", e.getValue().enabled());
            JsonArray arr = new JsonArray();
            for (double v : e.getValue().layout()) arr.add(v);
            o.add("layout", arr);
            preset.add(e.getKey(), o);
        }
        gui.getAsJsonObject("hudPresets").add(n, preset);
        ShardClient.config().markDirty();
        return true;
    }

    public static void delete(String name) {
        JsonObject gui = ShardClient.config().gui();
        if (gui.has("hudPresets") && gui.get("hudPresets").isJsonObject()) {
            JsonElement removed = gui.getAsJsonObject("hudPresets").remove(name);
            if (removed != null) ShardClient.config().markDirty();
        }
    }
}
