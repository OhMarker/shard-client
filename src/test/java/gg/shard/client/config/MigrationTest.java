package gg.shard.client.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import gg.shard.client.module.Module;
import gg.shard.client.module.ModuleCategory;
import gg.shard.client.module.ModuleManager;
import gg.shard.client.module.setting.BoolSetting;
import gg.shard.client.module.setting.EnumSetting;
import gg.shard.client.module.setting.IntSetting;
import gg.shard.client.module.setting.StringSetting;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Config schema 2 (0.2.0) files must load into 0.3.0 with every value intact. */
class MigrationTest {
    enum Preset { CARD, MINIMAL }

    /** Stands in for a 0.3.0 HUD module: "show-background" became a preset, "label" became text. */
    static final class Fps extends Module {
        final EnumSetting<Preset> preset = add(new EnumSetting<>("Style", "", Preset.CARD));
        final BoolSetting custom = add(new BoolSetting("Custom style", "", false));
        final StringSetting label = add(new StringSetting("Label", "", "FPS"));
        final IntSetting size = add(new IntSetting("Size", "", 5, 0, 100));

        Fps() {
            super("FPS", "test", ModuleCategory.HUD);
        }

        @Override
        public boolean defaultEnabled() {
            return true;
        }

        @Override
        protected boolean migratesKey(String key, int version) {
            return version < 3 && key.equals("label");
        }

        @Override
        protected void migrateSetting(String key, JsonElement value, JsonObject all, int version) {
            switch (key) {
                case "show-background" -> {
                    if (!value.getAsBoolean()) {
                        custom.set(true);
                        preset.set(Preset.MINIMAL);
                    }
                }
                case "label" -> {
                    if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean()) label.set(value.getAsBoolean() ? "FPS" : "");
                    else label.fromJson(value);
                }
                default -> super.migrateSetting(key, value, all, version);
            }
        }
    }

    private static final String V2 = """
            {
              "version": 2,
              "modules": {
                "fps": {
                  "enabled": false,
                  "keybind": 70,
                  "settings": { "show-background": false, "label": false, "size": 42 }
                }
              },
              "servers": [ { "address": "play.example.net", "modules": ["fps"] } ],
              "gui": { "category": "VISUALS" }
            }
            """;

    @Test
    void version2FileLoadsWithValuesIntactAndIsRewrittenAsVersion3() throws Exception {
        Path dir = Files.createTempDirectory("shard-migrate");
        Files.createDirectories(dir.resolve("shard"));
        Files.writeString(dir.resolve("shard").resolve("config.json"), V2);
        ModuleManager modules = new ModuleManager();
        Fps fps = new Fps();
        modules.register(fps);
        ConfigManager config = new ConfigManager(dir, modules);
        config.load();

        assertEquals(2, config.loadedVersion());
        assertFalse(fps.isToggledOn(), "enabled state kept");
        assertEquals(70, fps.keybind(), "keybind kept");
        assertEquals(42, fps.size.get(), "unchanged keys load normally");
        assertTrue(fps.custom.get(), "show-background=false became a custom Minimal style");
        assertEquals(Preset.MINIMAL, fps.preset.get());
        assertEquals("", fps.label.get(), "label=false became an empty label");
        assertEquals("VISUALS", config.gui().get("category").getAsString());
        assertTrue(modules.blacklist().isDisabled("play.example.net", "fps"), "server rules kept");

        config.save();
        JsonObject root = JsonParser.parseString(Files.readString(dir.resolve("shard").resolve("config.json"))).getAsJsonObject();
        assertEquals(3, root.get("version").getAsInt());
        JsonObject settings = root.getAsJsonObject("modules").getAsJsonObject("fps").getAsJsonObject("settings");
        assertFalse(settings.has("show-background"), "old key is gone after the rewrite");
        assertEquals("MINIMAL", settings.get("style").getAsString());
    }

    @Test
    void version3LabelIsReadAsText() throws Exception {
        Path dir = Files.createTempDirectory("shard-migrate3");
        Files.createDirectories(dir.resolve("shard"));
        Files.writeString(dir.resolve("shard").resolve("config.json"),
                "{\"version\":3,\"modules\":{\"fps\":{\"enabled\":true,\"settings\":{\"label\":\"fps\"}}}}");
        ModuleManager modules = new ModuleManager();
        Fps fps = new Fps();
        modules.register(fps);
        new ConfigManager(dir, modules).load();
        assertEquals("fps", fps.label.get());
        assertFalse(fps.custom.get());
    }

    @Test
    void exportAndImportRoundTrip() throws Exception {
        Path dir = Files.createTempDirectory("shard-export");
        ModuleManager modules = new ModuleManager();
        Fps fps = new Fps();
        modules.register(fps);
        ConfigManager config = new ConfigManager(dir, modules);
        config.load();
        fps.size.set(77);
        fps.label.set("frames");
        String json = config.exportJson();
        assertTrue(json.contains("\"version\": 3"));
        fps.size.set(1);
        fps.label.set("x");
        assertEquals(null, config.importJson(json));
        assertEquals(77, fps.size.get());
        assertEquals("frames", fps.label.get());
        assertTrue(config.importJson("{nope").contains("valid JSON"));
        assertTrue(config.importJson("{\"hello\":1}").contains("modules"));
        assertEquals(77, fps.size.get(), "failed imports change nothing");

        config.resetAll();
        assertEquals(5, fps.size.get());
        assertEquals("FPS", fps.label.get());
        assertTrue(fps.isToggledOn());
    }

    @Test
    void profilesCarryDescriptions() throws Exception {
        Path dir = Files.createTempDirectory("shard-profiles");
        ModuleManager modules = new ModuleManager();
        modules.register(new Fps());
        ConfigManager config = new ConfigManager(dir, modules);
        config.load();
        assertTrue(config.saveProfile("pvp", "Crystal fights on the main server"));
        assertTrue(config.saveProfile("plain"));
        var infos = config.profileInfos();
        assertEquals(2, infos.size());
        assertEquals("plain", infos.get(0).name());
        assertEquals("", infos.get(0).description());
        assertEquals("Crystal fights on the main server", infos.get(1).description());
    }

    /** A module renamed in 0.4.0 ("Armor Status" became "Armor") keeps its config key. */
    static final class Armor extends Module {
        final IntSetting warn = add(new IntSetting("Warn below", "", 20, 5, 60));

        Armor() {
            super("Armor", "test", ModuleCategory.HUD);
        }

        @Override
        protected String legacyKey() {
            return "armor-status";
        }
    }

    @Test
    void renamedModuleKeepsItsSettingsKeybindAndServerRules() throws Exception {
        Path dir = Files.createTempDirectory("shard-rename");
        Files.createDirectories(dir.resolve("shard"));
        Files.writeString(dir.resolve("shard").resolve("config.json"),
                "{\"version\":3,\"modules\":{\"armor-status\":{\"enabled\":true,\"keybind\":71,\"settings\":{\"warn-below\":35}}},"
                        + "\"servers\":[{\"address\":\"play.example.net\",\"modules\":[\"armor-status\"]}]}");
        ModuleManager modules = new ModuleManager();
        Armor armor = new Armor();
        modules.register(armor);
        new ConfigManager(dir, modules).load();
        assertEquals("armor-status", armor.key());
        assertEquals("Armor", armor.name());
        assertTrue(armor.isToggledOn());
        assertEquals(71, armor.keybind());
        assertEquals(35, armor.warn.get());
        assertTrue(modules.blacklist().isDisabled("play.example.net", "armor-status"));
    }
}
