package gg.shard.client.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScaleTest {
    @Test
    void pageScaleKeepsTwoPixelsPerUnitAtEveryGuiScale() {
        for (int guiScale = 1; guiScale <= 4; guiScale++) {
            double page = Scale.pageScale(guiScale, 1.0);
            assertEquals(2.0, Scale.pixelsPerUnit(page, guiScale), 1e-9, "gui scale " + guiScale);
        }
        assertEquals(1.0, Scale.pageScale(2, 1.0), 1e-9, "GUI scale 2 is the design reference");
        assertEquals(2.0, Scale.pageScale(1, 1.0), 1e-9);
        assertEquals(0.5, Scale.pageScale(4, 1.0), 1e-9);
        assertEquals(1.5, Scale.pageScale(2, 1.5), 1e-9, "interface size multiplies");
        assertEquals(Scale.pageScale(2, 2.0), Scale.pageScale(2, 9.0), 1e-9, "interface size is clamped");
        assertEquals(Scale.pageScale(1, 1.0), Scale.pageScale(0, 1.0), 1e-9, "gui scale never below 1");
    }

    @Test
    void designSizeIsIdenticalForEveryGuiScaleOfTheSameWindow() {
        int[][] windows = {{1280, 720}, {1920, 1080}, {2560, 1440}};
        for (int[] w : windows) {
            for (int guiScale = 1; guiScale <= 4; guiScale++) {
                double page = Scale.pageScale(guiScale, 1.0);
                // Vanilla's guiScaledWidth is ceil(width / scale).
                int guiW = (int) Math.ceil(w[0] / (double) guiScale);
                int guiH = (int) Math.ceil(w[1] / (double) guiScale);
                assertEquals(w[0] / 2, Scale.designSize(guiW, page), 1, "width " + w[0] + " at scale " + guiScale);
                assertEquals(w[1] / 2, Scale.designSize(guiH, page), 1, "height " + w[1] + " at scale " + guiScale);
            }
        }
        // GUI scale 3 on 720p is 426x240 GUI units; floor must not lose a whole unit to float noise.
        assertEquals(639, Scale.designSize(426, Scale.pageScale(3, 1.0)));
    }

    @Test
    void mouseMapsIntoDesignUnits() {
        double page = Scale.pageScale(4, 1.0);
        assertEquals(200.0, Scale.toDesign(100.0, page), 1e-9);
        assertEquals(100.0, Scale.toGui(200.0, page), 1e-9);
        assertEquals(37.5, Scale.toDesign(75.0, Scale.pageScale(1, 1.0)), 1e-9);
    }

    @Test
    void gridColumnsFollowTheSpec() {
        // 1080p-equivalent content width: 960 - 24 - 176 - 24 - 24 = 712 units.
        assertEquals(3, Scale.gridColumns(712, 220, 16));
        // 1440p-equivalent: 1280 - 248 = 1032 units -> four columns, never five.
        assertEquals(4, Scale.gridColumns(1032, 220, 16));
        // The 1280x720 dev window: 640 - 248 = 392 units -> one wide card per row.
        assertEquals(1, Scale.gridColumns(392, 220, 16));
        assertEquals(1, Scale.gridColumns(10, 220, 16));
        assertEquals(226, Scale.cardWidth(712, 3, 16));
        assertEquals(392, Scale.cardWidth(392, 1, 16));
    }

    @Test
    void narrowOnlyForGenuinelySmallWindows() {
        assertFalse(Scale.narrow(640), "a 1280 px window is not narrow at any GUI scale");
        assertTrue(Scale.narrow(440), "an 880 px window is");
        assertFalse(Scale.narrow(Scale.NARROW_UNITS));
    }

    @Test
    void pixelsRoundToWholePixelsAndNeverZero() {
        assertEquals(24, Scale.pixels(12, 2.0));
        assertEquals(30, Scale.pixels(12, 2.5));
        assertEquals(1, Scale.pixels(0.1, 2.0));
    }

    @Test
    void hudScaleMatchesPageScaleAtOneHundredPercent() {
        for (int guiScale = 1; guiScale <= 4; guiScale++) {
            assertEquals(Scale.pageScale(guiScale, 1.0), Scale.hudScale(guiScale, 100), 1e-9);
        }
        assertEquals(Scale.pageScale(2, 1.5), Scale.hudScale(2, 150), 1e-9);
        assertEquals(Scale.hudScale(2, 300), Scale.hudScale(2, 900), 1e-9, "clamped");
    }

    @Test
    void textAndIconsAreNeverRasterisedBelowTheirOnScreenSize() {
        int[] densities = {2, 3, 4, 6};
        assertEquals(2, Scale.atLeast(1.0, densities, 0.15), "interface 50%: smallest raster");
        assertEquals(2, Scale.atLeast(2.0, densities, 0.15), "100% at any GUI scale");
        assertEquals(3, Scale.atLeast(2.5, densities, 0.15), "125% rounds up, never stretches");
        assertEquals(3, Scale.atLeast(3.1, densities, 0.15), "within slack");
        assertEquals(4, Scale.atLeast(3.2, densities, 0.15));
        assertEquals(6, Scale.atLeast(9.0, densities, 0.15), "caps at the largest");
        int[] atlases = {32, 64, 128};
        assertEquals(32, Scale.atLeast(16 * 2.0, atlases, 0.5), "16-unit icon at 2 px/unit");
        assertEquals(64, Scale.atLeast(16 * 3.0, atlases, 0.5));
        assertEquals(128, Scale.atLeast(20 * 6.0, atlases, 0.5));
        assertEquals(128, Scale.atLeast(400, atlases, 0.5));
    }
}
