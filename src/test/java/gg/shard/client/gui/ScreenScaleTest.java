package gg.shard.client.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScreenScaleTest {
    @Test
    void factorFollowsTheWantedScaleAndClampsToWhatFits() {
        assertEquals(1.0, ScreenScale.factor(0, 2, 4), 1e-9, "0 = same as the game");
        assertEquals(1.5, ScreenScale.factor(3, 2, 4), 1e-9);
        assertEquals(0.5, ScreenScale.factor(1, 2, 4), 1e-9);
        assertEquals(2.0, ScreenScale.factor(6, 2, 4), 1e-9, "clamped to the window's max of 4");
    }

    @Test
    void everySlotCentreRoundTripsToTheSameScreenCoordinate() {
        int[] guiScales = {1, 2, 3, 4};
        int[] wanted = {1, 2, 3, 4, 5};
        for (int gs : guiScales) {
            for (int w : wanted) {
                double f = ScreenScale.factor(w, gs, 4);
                // A 176x166 inventory's slot centres, at a typical left/top.
                for (int sx = 8; sx < 176; sx += 18) {
                    for (int sy = 8; sy < 166; sy += 18) {
                        double physX = ScreenScale.toPhysical(100 + sx + 8, f, gs);
                        double physY = ScreenScale.toPhysical(40 + sy + 8, f, gs);
                        // The mouse handler divides physical pixels by the GUI scale, then we divide by the factor.
                        double backX = ScreenScale.toScreen(physX / gs, f);
                        double backY = ScreenScale.toScreen(physY / gs, f);
                        assertEquals(100 + sx + 8, backX, 1e-6);
                        assertEquals(40 + sy + 8, backY, 1e-6);
                    }
                }
            }
        }
    }

    @Test
    void anchorShiftKeepsTheAnchorStill() {
        double s = 0.75;
        double anchor = 540;
        double shifted = ScreenScale.anchorShift(anchor, s) + anchor * s;
        assertEquals(anchor, shifted, 1e-9);
        assertEquals(240, ScreenScale.virtualSize(480, 2.0));
    }
}
