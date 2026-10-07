package gg.shard.client.input;

import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Deque;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClickTrackerTest {
    @Test
    void countsClicksInsideAOneSecondWindow() {
        Deque<Long> clicks = new ArrayDeque<>();
        long t = 10_000;
        for (int i = 0; i < 6; i++) ClickTracker.record(clicks, t + i * 100);
        assertEquals(6, ClickTracker.count(clicks, t + 600));
        assertEquals(3, ClickTracker.count(clicks, t + 1250));
        assertEquals(0, ClickTracker.count(clicks, t + 5000));
    }
}
