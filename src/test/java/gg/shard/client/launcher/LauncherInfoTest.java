package gg.shard.client.launcher;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LauncherInfoTest {
    @Test
    void parsesTheLauncherContract() {
        String json = """
                {"schemaVersion":1,"launcherVersion":"0.1.0","accountId":"069a79f4","username":"Steve",
                 "minecraftVersion":"1.21.11","instanceId":"abc","instanceName":"Shard 1.21.11","shardBuild":null,
                 "accent":"#A78BFA","theme":"dark","equippedPath":"C:/data/cosmetics/equipped.json",
                 "sharedConfigPath":null,"writtenAt":"2026-10-06T00:00:00Z"}
                """;
        LauncherInfo info = LauncherInfo.fromJson(JsonParser.parseString(json).getAsJsonObject());
        assertTrue(info.present());
        assertEquals("0.1.0", info.launcherVersion());
        assertEquals("Steve", info.username());
        assertEquals(0xFFA78BFA, info.accent());
        assertEquals("dark", info.theme());
        assertNull(info.shardBuild());
        assertNull(info.sharedConfigPath());
        assertEquals(Path.of("C:/data/cosmetics/equipped.json"), info.equippedPath());
    }

    @Test
    void fallsBackToDefaultsWithoutTheFile() throws Exception {
        Path dir = Files.createTempDirectory("shard-li");
        LauncherInfo info = LauncherInfo.load(dir);
        assertFalse(info.present());
        assertEquals(LauncherInfo.DEFAULT_ACCENT, info.accent());
    }

    @Test
    void toleratesGarbage() throws Exception {
        Path dir = Files.createTempDirectory("shard-li2");
        Files.writeString(dir.resolve("launcher-info.json"), "{not json");
        assertFalse(LauncherInfo.load(dir).present());
        Files.writeString(dir.resolve("launcher-info.json"), "{\"accent\":\"purple\"}");
        assertEquals(LauncherInfo.DEFAULT_ACCENT, LauncherInfo.load(dir).accent());
    }
}
