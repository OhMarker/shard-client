package gg.shard.client.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FightLogTest {
    @Test
    void comboCountsHitsUntilYouGetHit() {
        FightLog f = new FightLog();
        f.hitDealt(7, "Steve", 2.9, 0);
        f.hitDealt(7, "Steve", 3.1, 500);
        f.hitDealt(7, "Steve", 2.5, 900);
        assertEquals(3, f.combo());
        assertEquals(2.5, f.lastReach(), 1e-9);
        f.hurt(4f, 1000);
        assertEquals(0, f.combo());
    }

    @Test
    void aTargetDyingSoonAfterYourHitIsAKillWithARecap() {
        FightLog f = new FightLog();
        f.hitDealt(7, "Steve", 3.0, 0);
        f.crystalPlaced(100);
        f.pop(false, 200);
        f.pop(true, 300);
        f.hurt(6f, 400);
        f.hitDealt(7, "Steve", 3.2, 1000);
        f.entityDied(7, 2000);
        assertEquals(1, f.kills());
        assertEquals(1, f.streak());
        FightLog.Recap r = f.lastRecap();
        assertNotNull(r);
        assertEquals(FightLog.Result.KILL, r.result());
        assertEquals("Steve", r.opponent());
        assertEquals(2, r.hitsDealt());
        assertEquals(1, r.hitsTaken());
        assertEquals(6f, r.damageTaken(), 1e-6);
        assertEquals(1, r.popsYou());
        assertEquals(1, r.popsThem());
        assertEquals(1, r.crystals());
        assertEquals(3.2, r.bestReach(), 1e-9);
        assertFalse(f.inFight());
    }

    @Test
    void someoneElsesDeathOrALateDeathIsNotAKill() {
        FightLog f = new FightLog();
        f.hitDealt(7, "Steve", 3.0, 0);
        f.entityDied(8, 100);
        f.entityDied(7, FightLog.KILL_WINDOW_MS + 1);
        assertEquals(0, f.kills());
    }

    @Test
    void dyingEndsTheStreakAndCountsInKdr() {
        FightLog f = new FightLog();
        f.hitDealt(7, "A", 3, 0);
        f.entityDied(7, 10);
        f.hitDealt(9, "B", 3, 20);
        f.entityDied(9, 30);
        assertEquals(2, f.streak());
        f.died(40);
        assertEquals(0, f.streak());
        assertEquals(2, f.bestStreak());
        assertEquals(2.0, f.kdr(), 1e-9);
    }

    @Test
    void idleFightsEndAndTinyOnesLeaveNoRecap() {
        FightLog f = new FightLog();
        f.hitDealt(7, "A", 3, 0);
        f.tick(FightLog.IDLE_END_MS + 1);
        assertFalse(f.inFight());
        assertNull(f.lastRecap(), "one hit is not a fight");
        f.hitDealt(7, "A", 3, 100_000);
        f.hitDealt(7, "A", 3, 100_100);
        f.hurt(2, 100_200);
        assertTrue(f.inFight());
        f.tick(100_200 + FightLog.IDLE_END_MS + 1);
        assertEquals(FightLog.Result.ENDED, f.lastRecap().result());
    }
}
