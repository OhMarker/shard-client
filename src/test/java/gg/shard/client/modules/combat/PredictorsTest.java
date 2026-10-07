package gg.shard.client.modules.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PredictorsTest {
    @Test
    void crystalRemovalIsRememberedOncePerWindow() {
        CrystalPredictor p = new CrystalPredictor(1000);
        assertTrue(p.markRemoved(7, 10_000));
        assertFalse(p.markRemoved(7, 10_200), "same crystal is not handled twice");
        assertTrue(p.wasRemoved(7, 10_900));
        assertFalse(p.wasRemoved(7, 11_500), "forgotten after the window");
        assertTrue(p.markRemoved(7, 12_000), "a reused id after the window counts again");
        assertEquals(2, p.removedTotal());
    }

    @Test
    void placementHighlightProgressesThenExpires() {
        CrystalPredictor p = new CrystalPredictor();
        long key = CrystalPredictor.posKey(10, 64, -20);
        assertEquals(key, CrystalPredictor.posKey(10, 64, -20));
        p.markPlaced(key, 5_000);
        assertTrue(p.recentlyPlaced(key, 5_100, 400));
        assertEquals(0.5, p.placedProgress(key, 5_200, 400), 1e-9);
        assertEquals(-1, p.placedProgress(key, 5_600, 400), 1e-9);
        assertFalse(p.recentlyPlaced(key, 5_600, 400));
        assertFalse(p.recentlyPlaced(CrystalPredictor.posKey(0, 0, 0), 5_100, 400));
        p.prune(20_000);
        assertEquals(-1, p.placedProgress(key, 20_000, 100_000));
    }

    @Test
    void anchorOutcomesMatchVanillaRules() {
        assertEquals(AnchorPredictor.Outcome.CHARGE, AnchorPredictor.predict(0, true, false, false));
        assertEquals(AnchorPredictor.Outcome.CHARGE, AnchorPredictor.predict(3, true, true, false));
        assertEquals(AnchorPredictor.Outcome.EXPLODE, AnchorPredictor.predict(4, true, false, false), "a full anchor explodes even with glowstone");
        assertEquals(AnchorPredictor.Outcome.EXPLODE, AnchorPredictor.predict(1, false, false, false));
        assertEquals(AnchorPredictor.Outcome.NONE, AnchorPredictor.predict(0, false, false, false), "empty anchors do nothing");
        assertEquals(AnchorPredictor.Outcome.NONE, AnchorPredictor.predict(2, false, true, false), "in the Nether it sets spawn");
        assertEquals(AnchorPredictor.Outcome.NONE, AnchorPredictor.predict(2, false, false, true), "sneaking with an item skips the block");
    }

    @Test
    void anchorPredictionsDedupeAndConsume() {
        AnchorPredictor p = new AnchorPredictor();
        long key = CrystalPredictor.posKey(1, 2, 3);
        assertTrue(p.markPredicted(key, 1000, 800));
        assertFalse(p.markPredicted(key, 1300, 800));
        assertTrue(p.consume(key, 1500, 800));
        assertFalse(p.consume(key, 1600, 800), "consumed only once");
        assertTrue(p.markPredicted(key, 5000, 800));
        p.prune(9000, 800);
        assertEquals(0, p.pendingCount());
    }
}
