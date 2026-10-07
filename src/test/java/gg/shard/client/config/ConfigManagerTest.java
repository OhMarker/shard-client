package gg.shard.client.config;

import com.google.gson.JsonObject;
import gg.shard.client.module.Module;
import gg.shard.client.module.ModuleCategory;
import gg.shard.client.module.ModuleManager;
import gg.shard.client.module.setting.BoolSetting;
import gg.shard.client.module.setting.IntSetting;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigManagerTest {
    static final class Demo extends Module {
        final BoolSetting flag = add(new BoolSetting("Flag", "", false));
        final IntSetting size = add(new IntSetting("Size", "", 5, 0, 100));

        Demo() {
            super("Demo Module", "test", ModuleCategory.VISUALS);
        }

        @Override
        public boolean defaultEnabled() {
            return true;
        }
    }

    @Test
    void roundTripsModulesAndGuiState() throws Exception {
        Path dir = Files.createTempDirectory("shard-cfg");
        ModuleManager modules = new ModuleManager();
        Demo demo = new Demo();
        modules.register(demo);
        ConfigManager config = new ConfigManager(dir, modules);
        config.load();
        assertTrue(demo.isEnabled(), "defaultEnabled applies when no file exists");

        demo.setEnabled(false);
        demo.setKeybind(82);
        demo.flag.set(true);
        demo.size.set(42);
        JsonObject panels = new JsonObject();
        panels.addProperty("x", 10);
        config.gui().add("panels", panels);
        config.save();
        assertTrue(Files.isRegularFile(dir.resolve("shard").resolve("config.json")));

        ModuleManager modules2 = new ModuleManager();
        Demo demo2 = new Demo();
        modules2.register(demo2);
        ConfigManager config2 = new ConfigManager(dir, modules2);
        config2.load();
        assertFalse(demo2.isEnabled());
        assertEquals(82, demo2.keybind());
        assertTrue(demo2.flag.get());
        assertEquals(42, demo2.size.get());
        assertEquals(10, config2.gui().getAsJsonObject("panels").get("x").getAsInt());
    }

    @Test
    void quarantinesCorruptFilesAndKeepsDefaults() throws Exception {
        Path dir = Files.createTempDirectory("shard-cfg2");
        Files.createDirectories(dir.resolve("shard"));
        Files.writeString(dir.resolve("shard").resolve("config.json"), "{broken");
        ModuleManager modules = new ModuleManager();
        Demo demo = new Demo();
        modules.register(demo);
        new ConfigManager(dir, modules).load();
        assertTrue(demo.isEnabled());
        assertEquals(5, demo.size.get());
        try (var files = Files.list(dir.resolve("shard"))) {
            assertTrue(files.anyMatch(p -> p.getFileName().toString().contains("corrupt")));
        }
    }

    @Test
    void profilesSaveAndLoad() throws Exception {
        Path dir = Files.createTempDirectory("shard-cfg3");
        ModuleManager modules = new ModuleManager();
        Demo demo = new Demo();
        modules.register(demo);
        ConfigManager config = new ConfigManager(dir, modules);
        config.load();
        demo.size.set(77);
        assertTrue(config.saveProfile("pvp"));
        demo.size.set(1);
        assertTrue(config.loadProfile("pvp"));
        assertEquals(77, demo.size.get());
        assertFalse(config.saveProfile("../evil"));
        assertEquals(1, config.profiles().size());
    }
}
