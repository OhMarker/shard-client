package gg.shard.client.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TpsEstimatorTest {
    @Test
    void steadyServerReadsTwenty() {
        TpsEstimator t = new TpsEstimator();
        for (int i = 0; i < 5; i++) t.onTimePacket(i * 20L, i * 1_000_000_000L);
        assertEquals(20.0, t.tps(), 1e-6);
        assertTrue(t.known());
    }

    @Test
    void lagShowsAsFewerTicksAndIsSmoothed() {
        TpsEstimator t = new TpsEstimator();
        t.onTimePacket(0, 0);
        t.onTimePacket(20, 2_000_000_000L); // 20 ticks in 2 s
        assertEquals(10.0, t.tps(), 1e-6);
        t.onTimePacket(40, 3_000_000_000L); // back to 20
        assertEquals(14.0, t.tps(), 1e-6);
    }

    @Test
    void ignoresBurstsAndTimeJumps() {
        TpsEstimator t = new TpsEstimator();
        t.onTimePacket(0, 0);
        t.onTimePacket(20, 100_000_000L); // two packets 0.1 s apart
        assertFalse(t.known());
        t.onTimePacket(1_000_000, 2_000_000_000L); // /time set
        assertFalse(t.known());
    }
}
