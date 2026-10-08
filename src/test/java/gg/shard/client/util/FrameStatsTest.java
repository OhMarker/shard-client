package gg.shard.client.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FrameStatsTest {
    @Test
    void steadyFramesHaveEqualAverageAndLow() {
        FrameStats s = new FrameStats(500);
        for (int i = 0; i < 400; i++) s.add(4f); // 250 fps
        assertEquals(250, s.averageFps());
        assertEquals(250, s.lowFps(1));
    }

    @Test
    void oneStutterInAHundredShowsInTheLowNotTheAverage() {
        FrameStats s = new FrameStats(1000);
        for (int i = 0; i < 1000; i++) s.add(i % 100 == 0 ? 50f : 4f);
        // 10 frames of 50 ms (20 fps) among 990 of 4 ms.
        assertEquals(20, s.lowFps(1));
        int avg = s.averageFps();
        assertEquals(Math.round(1000.0 * 1000 / (990 * 4 + 10 * 50)), avg);
    }

    @Test
    void ignoresPausesAndKeepsTheNewestFrames() {
        FrameStats s = new FrameStats(3);
        s.add(5000f);
        s.add(0f);
        assertEquals(0, s.count());
        s.add(1f);
        s.add(2f);
        s.add(3f);
        s.add(4f);
        assertEquals(3, s.count());
        assertEquals(4f, s.recent(0));
        assertEquals(2f, s.recent(2));
        assertEquals(0, s.lowFps(1), "not enough data yet");
    }
}
