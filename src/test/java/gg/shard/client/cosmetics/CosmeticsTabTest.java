package gg.shard.client.cosmetics;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** The in-game Cosmetics tab (0.11.0): catalogue details, ordering and preview fitting. */
class CosmeticsTabTest {

    @Test
    void catalogueKeepsNameRarityAndPreview() {
        Map<String, PlayerCosmetics.Item> c = PlayerCosmetics.parseCatalogueV2("""
                {"schemaVersion":2,"cosmetics":[
                  {"id":"cape-halloween","type":"cape","name":"Halloween Cape","rarity":"Special",
                   "textureUrl":"https://x/c.png","previewUrl":"https://x/p.png"},
                  {"id":"cape-plain","type":"cape","textureUrl":"https://x/d.png","previewUrl":"http://evil/p.png"}
                ]}""", false);
        PlayerCosmetics.Item h = c.get("cape-halloween");
        assertEquals("Halloween Cape", h.name());
        assertEquals("special", h.rarity());
        assertEquals("https://x/p.png", h.previewUrl());
        PlayerCosmetics.Item p = c.get("cape-plain");
        assertEquals("cape-plain", p.name());
        assertEquals("common", p.rarity());
        assertNull(p.previewUrl(), "previews follow the same URL rules as textures");
    }

    @Test
    void ownedFirstThenSlotThenRarity() {
        var bandana = new PlayerCosmetics.Item("b", "bandana", "https://x", "Bandana", "mythic", null);
        var capeCommon = new PlayerCosmetics.Item("c1", "cape", "https://x", "Plain", "common", null);
        var capeSpecial = new PlayerCosmetics.Item("c2", "cape", "https://x", "Spooky", "special", null);
        var shield = new PlayerCosmetics.Item("s", "shield", "https://x", "Shield", "epic", null);
        var all = List.of(bandana, capeCommon, capeSpecial, shield);
        assertEquals(List.of(capeCommon, bandana, capeSpecial, shield), CosmeticsTab.list(all, Set.of("c1", "b"), null));
        assertEquals(List.of(capeSpecial, capeCommon), CosmeticsTab.list(all, Set.of(), "cape"));
    }

    @Test
    void labels() {
        assertEquals("Shield", CosmeticsTab.slotLabel("shield"));
        assertEquals("", CosmeticsTab.slotLabel(null));
        assertEquals(7, CosmeticsTab.rarityRank("weird"));
    }

    @Test
    void previewIsCentredAndScaledIntoTheSquare() {
        // 4x8 opaque red, fitted into 4x4: 2x4 wide, centred with a transparent column each side.
        int[] red = new int[4 * 8];
        java.util.Arrays.fill(red, 0xFFFF0000);
        int[] out = PreviewFit.fit(red, 4, 8, 4);
        for (int y = 0; y < 4; y++) {
            assertEquals(0, out[y * 4], "left margin");
            assertEquals(0xFFFF0000, out[y * 4 + 1]);
            assertEquals(0xFFFF0000, out[y * 4 + 2]);
            assertEquals(0, out[y * 4 + 3], "right margin");
        }
    }

    @Test
    void transparentPixelsDoNotDarkenTheAverage() {
        // One white pixel and one fully transparent black one average to half-transparent white.
        int[] out = PreviewFit.fit(new int[]{0xFFFFFFFF, 0x00000000}, 2, 1, 1);
        assertEquals(0x80FFFFFF, out[0]);
    }

    @Test
    void smallPicturesAreNotScaledUp() {
        int[] out = PreviewFit.fit(new int[]{0xFF00FF00}, 1, 1, 3);
        assertEquals(0xFF00FF00, out[4]);
        assertEquals(0, out[0]);
    }
}
