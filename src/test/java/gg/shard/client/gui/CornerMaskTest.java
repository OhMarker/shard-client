package gg.shard.client.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CornerMaskTest {
    @Test
    void discIsOpaqueInsideTransparentOutsideAndPartialOnTheEdge() {
        int r = 12;
        assertEquals(255, CornerMask.coverage(r, r, r, 0), "centre");
        assertEquals(255, CornerMask.coverage(r, 0, r, 0), "top edge midpoint is inside the disc");
        assertEquals(0, CornerMask.coverage(0, 0, r, 0), "corner of the square is outside");
        int edge = CornerMask.coverage(3, 3, r, 0);
        assertTrue(edge > 0 && edge < 255, "the curve is anti-aliased, got " + edge);
        int[] mask = CornerMask.mask(r, 0);
        assertEquals(24 * 24, mask.length);
        for (int argb : mask) assertEquals(0xFFFFFF, argb & 0xFFFFFF, "white with alpha only");
    }

    @Test
    void ringLeavesTheInsideEmpty() {
        int r = 12;
        assertEquals(0, CornerMask.coverage(r, r, r, 2), "centre is hollow");
        assertEquals(255, CornerMask.coverage(r, 0, r, 2), "outer edge midpoint is on the ring");
        assertEquals(0, CornerMask.coverage(r, 4, r, 2), "inside the ring is hollow");
        int[] mask = CornerMask.mask(r, 2);
        int opaque = 0;
        for (int argb : mask) if ((argb >>> 24) == 255) opaque++;
        assertTrue(opaque > 0 && opaque < mask.length / 4, "a thin ring, got " + opaque + " opaque pixels");
    }

    @Test
    void coverageIsSymmetric() {
        int r = 9;
        for (int y = 0; y < 2 * r; y++) {
            for (int x = 0; x < 2 * r; x++) {
                int c = CornerMask.coverage(x, y, r, 0);
                assertEquals(c, CornerMask.coverage(2 * r - 1 - x, y, r, 0), "mirror x at " + x + "," + y);
                assertEquals(c, CornerMask.coverage(x, 2 * r - 1 - y, r, 0), "mirror y at " + x + "," + y);
            }
        }
    }
}
