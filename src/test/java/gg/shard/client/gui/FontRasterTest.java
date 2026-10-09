package gg.shard.client.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Lazily built Inter fonts: ids, raster densities and whole-pixel text origins. */
class FontRasterTest {
    @Test
    void fontIdsRoundTrip() {
        assertEquals("ui-medium-10-d125", LazyFonts.path("medium", 10, 1.25));
        assertEquals("ui-regular-13-d200", LazyFonts.path("regular", 13, 2.0));
        LazyFonts.Spec spec = LazyFonts.parse("shard", "ui-semibold-18-d115");
        assertNotNull(spec);
        assertEquals("semibold", spec.weight());
        assertEquals(18, spec.size());
        assertEquals(1.15, spec.density(), 1e-9);
        assertNull(LazyFonts.parse("minecraft", "ui-medium-10-d125"), "only Shard's namespace");
        assertNull(LazyFonts.parse("shard", "ui-bold-10-d125"), "unknown weight");
        assertNull(LazyFonts.parse("shard", "ui-medium-10"), "0.7.1 names are gone");
        assertNull(LazyFonts.parse("shard", "ui-medium-10-d5"), "absurd density");
    }

    @Test
    void densityIsTheOnScreenDensity() {
        for (double d : new double[]{0.75, 1.0, 1.25, 1.5, 1.75, 2.0, 2.25, 2.5, 3.0}) {
            assertEquals(d, Fonts.densityFor(d), 1e-9, "menu and GUI densities are drawn 1:1");
        }
        // HUD 55% at GUI scale 2 with a 1.05x element: 2 * 0.55 * 1.05.
        assertEquals(1.16, Fonts.densityFor(2 * 0.55 * 1.05), 1e-9);
        assertEquals(1.0, Fonts.densityFor(0.999999), 1e-9, "float noise does not make a new font");
        assertEquals(0.5, Fonts.densityFor(0.1), 1e-9, "floor");
        assertEquals(12.0, Fonts.densityFor(40), 1e-9, "cap");
    }

    @Test
    void textOriginSnapsToWholePixels() {
        // Menu: 1.25 px per unit at GUI scale 2 means a pose scale of 0.625.
        float s = 0.625f;
        float[] snap = Fonts.pixelSnap(s, 0, 0, s, 3f, 7f, 2, 13, 21);
        assertNotNull(snap);
        double px = (s * (13 + snap[0]) + 3f) * 2;
        double py = (s * (21 + snap[1]) + 7f) * 2;
        assertEquals(Math.rint(px), px, 1e-3);
        assertEquals(Math.rint(py), py, 1e-3);
        assertEquals(0, Math.abs(snap[0]) * s * 2, 0.5 + 1e-6, "never moves more than half a pixel");
        assertNull(Fonts.pixelSnap(1, 0, 0, 1, 0, 0, 2, 10, 20), "already on a pixel");
        assertNull(Fonts.pixelSnap(0.7f, 0.7f, -0.7f, 0.7f, 0, 0, 2, 10, 20), "rotated poses are left alone");
        assertArrayEquals(new float[]{-0.2f, 0f}, Fonts.pixelSnap(1, 0, 0, 1, 0.2f, 0, 2, 0, 0), 1e-6f);
    }
}
