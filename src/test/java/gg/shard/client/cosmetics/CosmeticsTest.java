package gg.shard.client.cosmetics;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CosmeticsTest {
    @Test
    void readsTheCapeOrCloakAndPointsAtTheLaunchersCachedTexture(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("equipped.json");
        Files.writeString(file, "{\"schemaVersion\":1,\"equipped\":{\"hat\":\"x\",\"cape\":\"cape-ohmarker\"},\"emotes\":[]}");
        EquippedCape cape = EquippedCape.read(file);
        assertEquals("cape-ohmarker", cape.id());
        assertEquals(dir.resolve("textures").resolve("cape-ohmarker.png").toAbsolutePath(), cape.texture());
        assertEquals("cloak-1", EquippedCape.backItemId(JsonParser.parseString("{\"equipped\":{\"cloak\":\"cloak-1\"}}")));
    }

    @Test
    void nothingEquippedOrBrokenFilesMeanNoCape(@TempDir Path dir) throws Exception {
        assertNull(EquippedCape.read(dir.resolve("missing.json")));
        Path file = dir.resolve("equipped.json");
        Files.writeString(file, "{\"equipped\":{\"hat\":\"crown\",\"cape\":\"\"}}");
        assertNull(EquippedCape.read(file));
        Files.writeString(file, "not json {");
        assertNull(EquippedCape.read(file));
        assertNull(EquippedCape.backItemId(JsonParser.parseString("[1,2]")));
    }

    @Test
    void unsafeIdsAreSanitisedLikeTheLauncher() {
        assertEquals("a_b_c.png", EquippedCape.texturePath(Path.of("x", "equipped.json"), "a/b c").getFileName().toString());
        assertEquals("cape_x", new EquippedCape("Cape X", Path.of("t.png")).resourceName());
    }

    @Test
    void mipChainGoesDownTo64PixelsWide() {
        assertEquals(7, MipChain.levelCount(4096, 2048, 64));
        assertEquals(1, MipChain.levelCount(64, 32, 64));
        List<MipChain.Level> levels = MipChain.build(new int[256 * 128], 256, 128, 64);
        assertEquals(3, levels.size());
        assertEquals(64, levels.get(2).width());
        assertEquals(32, levels.get(2).height());
    }

    @Test
    void transparentPixelsDoNotDarkenTheAverage() {
        int red = 0xFFFF0000;
        // Two opaque red pixels next to two transparent black ones stay pure red, half covered.
        int avg = MipChain.average(red, red, 0, 0);
        assertEquals(0xFF, (avg >> 16) & 0xFF);
        assertEquals(0, (avg >> 8) & 0xFF);
        assertEquals(0x80, avg >>> 24);
        assertEquals(0, MipChain.average(0, 0, 0, 0));
        // Opaque pixels average normally.
        assertEquals(0xFF808080, MipChain.average(0xFFFFFFFF, 0xFF000000, 0xFFFFFFFF, 0xFF000000));
    }
}
