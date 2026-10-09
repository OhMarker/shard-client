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

    @Test
    void ringHasNoCentreButCircleAndDotDoes() {
        CrosshairShape.Mask ring = CrosshairShape.mask(CrosshairShape.Kind.RING, 6, 2, 1, null);
        CrosshairShape.Mask circle = CrosshairShape.mask(CrosshairShape.Kind.CIRCLE, 6, 2, 1, null);
        assertFalse(ring.at(ring.center(), ring.center()));
        assertTrue(circle.at(circle.center(), circle.center()));
        int r = 2 + 3 + 1;
        assertTrue(ring.at(ring.center() + r, ring.center()), "ring passes through its radius");
    }

    @Test
    void squareIsAHollowBoxOfTheGivenThickness() {
        CrosshairShape.Mask sq = CrosshairShape.mask(CrosshairShape.Kind.SQUARE, 4, 1, 1, null);
        int c = sq.center();
        int r = 1 + 2 + 1;
        assertTrue(sq.at(c + r, c + r), "corner");
        assertTrue(sq.at(c - r, c));
        assertFalse(sq.at(c, c), "hollow");
        assertFalse(sq.at(c + r - 1, c), "one pixel thick");
    }

    @Test
    void chevronHasOnlyTheLowerDiagonalsAndXDotHasACentre() {
        CrosshairShape.Mask ch = CrosshairShape.mask(CrosshairShape.Kind.CHEVRON, 3, 0, 1, null);
        int c = ch.center();
        assertTrue(ch.at(c + 2, c + 2));
        assertTrue(ch.at(c - 2, c + 2));
        assertFalse(ch.at(c + 2, c - 2));
        CrosshairShape.Mask xd = CrosshairShape.mask(CrosshairShape.Kind.X_DOT, 3, 1, 1, null);
        assertTrue(xd.at(xd.center(), xd.center()));
        assertFalse(xd.at(xd.center() + 1, xd.center() + 1), "gap between dot and arms");
    }

    @Test
    void circleAndPlusHasArmsCrossingTheRing() {
        CrosshairShape.Mask m = CrosshairShape.mask(CrosshairShape.Kind.CIRCLE_CROSS, 8, 1, 1, null);
        int c = m.center();
        assertFalse(m.at(c, c));
        for (int d = 2; d <= 9; d++) assertTrue(m.at(c + d, c), "arm pixel " + d);
    }

    @Test
    void largeSizesStayCentredAndThinLinesStayOnePixel() {
        CrosshairShape.Mask m = CrosshairShape.mask(CrosshairShape.Kind.PLUS, 40, 0, 1, null);
        int c = m.center();
        assertTrue(m.at(c, c - 39), "a plus arm is 40 pixels counting the centre");
        assertFalse(m.at(c, c - 40));
        assertFalse(m.at(c + 1, c - 39), "1 pixel wide");
        assertEquals(m.size() / 2, c);
    }

    @Test
    void pixelPerfectRoundTripsAndOldCodesMeanGuiPixels() {
        CrosshairShape.Spec pp = new CrosshairShape.Spec(CrosshairShape.Kind.RING, 9, 4, 2, -1, true, 0xB0000000, 1, "", true);
        assertEquals(pp, CrosshairShape.decode(CrosshairShape.encode(pp)));
        String old = "SHARD-X1-" + java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("CROSS;5;2;1;ffffffff;1;b0000000;1;".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        CrosshairShape.Spec s = CrosshairShape.decode(old);
        assertNotNull(s);
        assertFalse(s.pixelPerfect());
    }
}
