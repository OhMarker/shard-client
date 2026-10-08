package gg.shard.client.modules.visual;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrosshairShapeTest {
    @Test
    void gapCrossLeavesTheCentreEmptyAndHasFourArms() {
        CrosshairShape.Mask m = CrosshairShape.mask(CrosshairShape.Kind.CROSS, 4, 2, 1, null);
        int c = m.center();
        assertFalse(m.at(c, c), "gap at the centre");
        assertFalse(m.at(c + 2, c), "still inside the gap");
        for (int d = 3; d <= 6; d++) {
            assertTrue(m.at(c + d, c));
            assertTrue(m.at(c - d, c));
            assertTrue(m.at(c, c + d));
            assertTrue(m.at(c, c - d));
        }
        assertFalse(m.at(c + 7, c), "arm is 4 long");
    }

    @Test
    void tShapeHasNoTopArmAndDotIsAThicknessSquare() {
        CrosshairShape.Mask t = CrosshairShape.mask(CrosshairShape.Kind.T_SHAPE, 4, 1, 1, null);
        int c = t.center();
        assertFalse(t.at(c, c - 3));
        assertTrue(t.at(c, c + 3));
        CrosshairShape.Mask dot = CrosshairShape.mask(CrosshairShape.Kind.DOT, 4, 0, 2, null);
        int count = 0;
        for (boolean b : dot.bits()) if (b) count++;
        assertEquals(4, count);
    }

    @Test
    void outlineGrowsTheMaskAndRunsMergeRows() {
        CrosshairShape.Mask dot = CrosshairShape.mask(CrosshairShape.Kind.DOT, 1, 0, 1, null);
        CrosshairShape.Mask grown = CrosshairShape.grow(dot, 1);
        List<CrosshairShape.Run> runs = CrosshairShape.runs(grown);
        assertEquals(3, runs.size(), "a 3x3 square is three rows");
        assertEquals(new CrosshairShape.Run(-1, -1, 3), runs.get(0));
    }

    @Test
    void customPixelsRoundTripAndAreCentred() {
        boolean[] px = new boolean[CrosshairShape.CUSTOM * CrosshairShape.CUSTOM];
        px[7 * 15 + 7] = true; // centre
        px[0] = true;
        px[224] = true;
        String text = CrosshairShape.encodePixels(px);
        assertArrayEquals(px, CrosshairShape.decodePixels(text));
        CrosshairShape.Mask m = CrosshairShape.mask(CrosshairShape.Kind.CUSTOM, 1, 0, 1, px);
        assertTrue(m.at(m.center(), m.center()));
    }

    @Test
    void shareCodeRoundTripsAndRejectsJunk() {
        CrosshairShape.Spec s = new CrosshairShape.Spec(CrosshairShape.Kind.CIRCLE, 5, 2, 1, 0xFF22D3EE, true, 0xB0000000, 1, "");
        String code = CrosshairShape.encode(s);
        assertTrue(code.startsWith("SHARD-X1-"));
        assertEquals(s, CrosshairShape.decode(code));
        assertEquals(s, CrosshairShape.decode("  " + code + "\n"), "whitespace from the clipboard is fine");
        assertNull(CrosshairShape.decode("hello"));
        assertNull(CrosshairShape.decode("SHARD-X1-!!!!"));
        assertNotNull(CrosshairShape.decode(CrosshairShape.encode(new CrosshairShape.Spec(CrosshairShape.Kind.CUSTOM, 1, 0, 1, -1, false, 0, 1, "8000"))));
    }
}
