package gg.shard.client.gui.menu;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackdropRasterTest {
    private static final int ACCENT = 0xFF22D3EE;

    @Test
    void shardMarkIsOpaqueInTheMiddleAndClearInTheCorners() {
        int size = 64;
        int[] px = ShardMarkRaster.pixels(size, ACCENT);
        assertEquals(size * size, px.length);
        assertEquals(0, px[0] >>> 24, "top-left corner is transparent");
        assertEquals(0, px[size * size - 1] >>> 24, "bottom-right corner is transparent");
        int centre = px[(size / 2) * size + size / 2];
        assertTrue((centre >>> 24) > 200, "the crystal's middle is nearly opaque");
    }

    @Test
    void shardMarkFollowsTheAccent() {
        int[] cyan = ShardMarkRaster.pixels(32, ACCENT);
        int[] pink = ShardMarkRaster.pixels(32, 0xFFF472B6);
        boolean differs = false;
        for (int i = 0; i < cyan.length; i++) differs |= cyan[i] != pink[i];
        assertTrue(differs);
    }

    @Test
    void polygonHelpers() {
        float[] xs = {-1, 1, 1, -1};
        float[] ys = {-1, -1, 1, 1};
        assertTrue(ShardBackdrop.inside(xs, ys, 0, 0));
        assertFalse(ShardBackdrop.inside(xs, ys, 2, 0));
        assertEquals(0.5f, ShardBackdrop.edgeDistance(xs, ys, 0.5f, 0), 1e-5f);
        assertEquals(1f, ShardBackdrop.edgeDistance(xs, ys, 2f, 0), 1e-5f);
    }
}
