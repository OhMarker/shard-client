package gg.shard.client.modules.combat;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Bookkeeping for client-side crystal prediction. Pure Java: which crystal ids were removed
 * locally and when, and which block positions the player just placed a crystal on. The game
 * side decides what to remove; this class only remembers it so nothing is handled twice and the
 * highlight knows which crystals are "mine".
 */
public final class CrystalPredictor {
    private final Map<Integer, Long> removed = new HashMap<>();
    private final Map<Long, Long> placed = new HashMap<>();
    private int removedTotal;
    private final long removedWindowMs;

    public CrystalPredictor() {
        this(1500);
    }

    public CrystalPredictor(long removedWindowMs) {
        this.removedWindowMs = removedWindowMs;
    }

    /** Returns true when {@code id} was not already removed recently (so the caller should act). */
    public boolean markRemoved(int id, long now) {
        prune(now);
        if (removed.containsKey(id)) return false;
        removed.put(id, now);
        removedTotal++;
        return true;
    }

    public boolean wasRemoved(int id, long now) {
        Long at = removed.get(id);
        return at != null && now - at <= removedWindowMs;
    }

    /** Total crystals removed client-side this session (shown in logs and the smoke test). */
    public int removedTotal() {
        return removedTotal;
    }

    public static long posKey(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
    }

    public void markPlaced(long posKey, long now) {
        placed.put(posKey, now);
    }

    public boolean recentlyPlaced(long posKey, long now, long windowMs) {
        Long at = placed.get(posKey);
        return at != null && now - at <= windowMs;
    }

    /** 0..1 how far through the highlight window a placement is, or -1 when not highlighted. */
    public double placedProgress(long posKey, long now, long windowMs) {
        Long at = placed.get(posKey);
        if (at == null || windowMs <= 0) return -1;
        long age = now - at;
        if (age < 0 || age > windowMs) return -1;
        return age / (double) windowMs;
    }

    public void prune(long now) {
        for (Iterator<Map.Entry<Integer, Long>> it = removed.entrySet().iterator(); it.hasNext(); ) {
            if (now - it.next().getValue() > removedWindowMs) it.remove();
        }
        for (Iterator<Map.Entry<Long, Long>> it = placed.entrySet().iterator(); it.hasNext(); ) {
            if (now - it.next().getValue() > 5000) it.remove();
        }
    }

    public void clear() {
        removed.clear();
        placed.clear();
    }
}
