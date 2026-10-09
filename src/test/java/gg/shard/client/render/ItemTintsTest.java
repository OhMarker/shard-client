package gg.shard.client.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemTintsTest {
    @Test
    void shieldColorKeepsTintAndMapsOpacityToAlpha() {
        assertEquals(0xFFFFFFFF, ItemTints.shieldColor(100, 0xFFFFFFFF));
        assertEquals(0x80123456, ItemTints.shieldColor(50, 0xFF123456));
        assertEquals(0x1A000000 | 0xABCDEF, ItemTints.shieldColor(5, 0xFFABCDEF), "clamped to 10 %");
    }

    @Test
    void shieldContextIsOnlyTranslucentWhileSeeThrough() {
        assertFalse(ItemTints.shieldTranslucent());
        ItemTints.beginShield(ItemTints.shieldColor(60, 0xFFFFFFFF));
        assertTrue(ItemTints.shieldTranslucent());
        ItemTints.endShield();
        assertEquals(ItemTints.NONE, ItemTints.shield());
    }

    @Test
    void hitTintBlendsFromWhiteByStrength() {
        assertEquals(0xFFFFFFFF, ItemTints.hitTint(0xFFFF0000, 0));
        assertEquals(0xFFFF0000, ItemTints.hitTint(0xFFFF0000, 100));
        assertEquals(0xFFFF8080, ItemTints.hitTint(0xFFFF0000, 50));
    }

    @Test
    void multiplyLeavesWhiteAlone() {
        assertEquals(0xFF123456, ItemTints.multiply(0xFF123456, ItemTints.NONE));
        assertEquals(0xFF123456, ItemTints.multiply(ItemTints.NONE, 0xFF123456));
        assertEquals(0x80800000, ItemTints.multiply(0xFFFF0000, 0x80808080));
    }

    @Test
    void itemScaleNeverBelowHalf() {
        assertEquals(0.5f, ItemTints.itemScale(10));
        assertEquals(0.75f, ItemTints.itemScale(75));
        assertEquals(1f, ItemTints.itemScale(150));
    }
}
