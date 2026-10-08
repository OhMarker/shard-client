package gg.shard.client.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ItemIdsTest {
    @Test
    void parsesCommaOrSpaceSeparatedIdsAndAddsTheNamespace() {
        assertEquals(List.of("minecraft:ender_pearl", "minecraft:tnt", "sodium:thing"),
                ItemIds.parse("Ender_Pearl, minecraft:tnt  sodium:thing"));
    }

    @Test
    void dropsInvalidTokensAndDuplicates() {
        assertEquals(List.of("minecraft:tnt"), ItemIds.parse("tnt, tnt, b@d, ,"));
        assertEquals(List.of(), ItemIds.parse(""));
        assertEquals(List.of(), ItemIds.parse(null));
        assertNull(ItemIds.normalize("a:b:c"));
        assertNull(ItemIds.normalize("   "));
        assertEquals("minecraft:block/fire_1", ItemIds.normalize("block/fire_1"));
    }
}
