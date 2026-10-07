package gg.shard.client.modules.combat;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Decides what a right-click on a respawn anchor will do, mirroring vanilla's server rules, so
 * the client can show the result immediately. Pure Java; the game side supplies the facts.
 */
public final class AnchorPredictor {
    public enum Outcome { NONE, CHARGE, EXPLODE }

    public static final int MAX_CHARGES = 4;

    private final Map<Long, Long> predicted = new HashMap<>();

    /**
     * @param charge             current CHARGE property (0..4)
     * @param holdingGlowstone   the used hand holds glowstone
     * @param anchorWorksHere    the dimension lets anchors set a spawn (the Nether)
     * @param secondaryUseActive sneaking with an item in either hand: vanilla skips block use
     */
    public static Outcome predict(int charge, boolean holdingGlowstone, boolean anchorWorksHere, boolean secondaryUseActive) {
        if (secondaryUseActive) return Outcome.NONE;
        if (holdingGlowstone && charge < MAX_CHARGES) return Outcome.CHARGE;
        if (charge <= 0) return Outcome.NONE;
        return anchorWorksHere ? Outcome.NONE : Outcome.EXPLODE;
    }

    /** Returns true when this position was not already predicted within {@code windowMs}. */
    public boolean markPredicted(long posKey, long now, long windowMs) {
        prune(now, windowMs);
        Long at = predicted.get(posKey);
        if (at != null && now - at <= windowMs) return false;
        predicted.put(posKey, now);
        return true;
    }

    /** True (and forgets the prediction) when an explosion near {@code posKey} was expected. */
    public boolean consume(long posKey, long now, long windowMs) {
        Long at = predicted.remove(posKey);
        return at != null && now - at <= windowMs;
    }

    public int pendingCount() {
        return predicted.size();
    }

    public void prune(long now, long windowMs) {
        for (Iterator<Map.Entry<Long, Long>> it = predicted.entrySet().iterator(); it.hasNext(); ) {
            if (now - it.next().getValue() > windowMs) it.remove();
        }
    }

    public void clear() {
        predicted.clear();
    }
}
