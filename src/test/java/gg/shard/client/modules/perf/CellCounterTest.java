package gg.shard.client.modules.perf;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CellCounterTest {
    @Test
    void countsPerCellAndResetsEachFrame() {
        CellCounter c = new CellCounter(64);
        assertEquals(1, c.increment(42L));
        assertEquals(2, c.increment(42L));
        assertEquals(1, c.increment(-7L));
        assertEquals(3, c.increment(42L));
        c.reset();
        assertEquals(1, c.increment(42L), "a new frame starts from zero");
    }

    @Test
    void fullTableAdmitsNewCellsInsteadOfGrowing() {
        CellCounter c = new CellCounter(16);
        for (long k = 0; k < 12; k++) assertEquals(1, c.increment(k * 1_000_003L));
        // Twelve of sixteen slots used: new cells are no longer tracked, so they always read 1.
        assertEquals(1, c.increment(999L));
        assertEquals(1, c.increment(999L));
        assertEquals(2, c.increment(0L), "cells already tracked keep counting");
    }
}
