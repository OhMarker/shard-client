package gg.shard.client.modules.visual;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZoomEaseTest {
    /** Runs the easing for {@code seconds} at {@code fps} and returns where it ended. */
    private static double run(double fps, double seconds, double settle) {
        double v = 0;
        double target = -Math.log(4);
        int frames = (int) Math.round(fps * seconds);
        for (int i = 0; i < frames; i++) v = ZoomModule.ease(v, target, 1 / fps, settle);
        return v;
    }

    @Test
    void sameProgressAtAnyFrameRate() {
        double at60 = run(60, 0.05, 0.09);
        double at240 = run(240, 0.05, 0.09);
        assertEquals(at60, at240, 0.01, "frame-time based, not frame based");
    }

    @Test
    void settlesWithinTheSmoothnessTimeAndSnaps() {
        double target = -Math.log(4);
        double after = run(144, 0.09, 0.09);
        assertTrue(Math.abs(after - target) < 0.1 * Math.abs(target), "about 95% there after the settle time");
        assertEquals(target, run(144, 1.0, 0.09), "snaps exactly onto the target");
        assertEquals(target, ZoomModule.ease(0, target, 0.016, 0), "no smoothing jumps straight there");
    }
}
