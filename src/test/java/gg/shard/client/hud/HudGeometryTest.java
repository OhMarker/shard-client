package gg.shard.client.hud;

import org.junit.jupiter.api.Test;

import java.util.List;

import static gg.shard.client.hud.HudGeometry.CENTER;
import static gg.shard.client.hud.HudGeometry.END;
import static gg.shard.client.hud.HudGeometry.START;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HudGeometryTest {
    private static HudGeometry.Rect r(double x, double y, double w, double h) {
        return new HudGeometry.Rect(x, y, w, h);
    }

    @Test
    void anchorIsTheThirdTheCentreIsIn() {
        assertEquals(START, HudGeometry.anchorFor(4, 40, 960));
        assertEquals(CENTER, HudGeometry.anchorFor(460, 40, 960));
        assertEquals(END, HudGeometry.anchorFor(900, 56, 960));
    }

    @Test
    void anchoredElementsKeepTheirDistanceFromTheirCorner() {
        // 10 HUD units from the right edge at GUI scale 2 (unit 1.0) on 1920x1080...
        double unit2 = 1.0;
        double size2 = 50;
        double x2 = HudGeometry.positionFor(END, 10, size2, 960, unit2);
        assertEquals(900, x2, 1e-9);
        double off = HudGeometry.offsetFor(END, x2, size2, 960, unit2);
        assertEquals(10, off, 1e-9);
        // ...is still 10 units (20 physical px) from the right edge at GUI scale 3 (unit 2/3).
        double unit3 = 2.0 / 3.0;
        double size3 = size2 * unit3;
        double x3 = HudGeometry.positionFor(END, off, size3, 640, unit3);
        assertEquals(640 - size3 - 10 * unit3, x3, 1e-9);
        assertEquals(20, (640 - x3 - size3) * 3, 1e-9, "physical pixels from the edge");
        // Centre anchor round-trips too.
        double c = HudGeometry.offsetFor(CENTER, 455, 50, 960, 1.0);
        assertEquals(455, HudGeometry.positionFor(CENTER, c, 50, 960, 1.0), 1e-9);
    }

    @Test
    void snapsToScreenEdgesCentreAndOtherElements() {
        HudGeometry.Snap s = HudGeometry.snap(r(3, 100, 40, 10), 960, 540, List.of(), 4);
        assertEquals(0, s.x(), 1e-9, "left edge");
        assertEquals(List.of(0.0), s.verticalGuides());
        assertTrue(s.horizontalGuides().isEmpty(), "y was not near anything");

        s = HudGeometry.snap(r(458, 300, 40, 10), 960, 540, List.of(), 4);
        assertEquals(460, s.x(), 1e-9, "centre line");

        // Left edge to another element's right edge, top to its top.
        s = HudGeometry.snap(r(103, 52, 30, 10), 960, 540, List.of(r(50, 50, 50, 20)), 4);
        assertEquals(100, s.x(), 1e-9);
        assertEquals(50, s.y(), 1e-9);
        assertEquals(List.of(100.0), s.verticalGuides());
        assertEquals(List.of(50.0), s.horizontalGuides());

        s = HudGeometry.snap(r(200, 200, 30, 10), 960, 540, List.of(r(50, 50, 50, 20)), 4);
        assertEquals(200, s.x(), 1e-9, "nothing within the threshold");
    }

    @Test
    void alignAndDistribute() {
        List<HudGeometry.Rect> rects = List.of(r(10, 0, 20, 10), r(40, 30, 10, 10), r(100, 60, 30, 10));
        List<double[]> left = HudGeometry.align(rects, HudGeometry.Align.LEFT);
        for (double[] p : left) assertEquals(10, p[0], 1e-9);
        List<double[]> right = HudGeometry.align(rects, HudGeometry.Align.RIGHT);
        assertEquals(110, right.get(0)[0], 1e-9);
        assertEquals(120, right.get(1)[0], 1e-9);
        List<double[]> dist = HudGeometry.distribute(rects, true);
        // Span 10..130 = 120, widths 60, two gaps of 30.
        assertEquals(10, dist.get(0)[0], 1e-9);
        assertEquals(60, dist.get(1)[0], 1e-9);
        assertEquals(100, dist.get(2)[0], 1e-9);
    }
}
