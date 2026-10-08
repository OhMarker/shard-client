package gg.shard.client.combat;

/**
 * Pure bookkeeping for a crystal fight (tested): your hits and their reach, the combo, damage you
 * take, pops on both sides, crystals you place, kills, deaths and streaks, and a recap when a
 * fight ends. Everything comes from what the client already sees: its own attacks, its own
 * health, and the totem and death events the server sends to every player. Times are
 * milliseconds supplied by the caller so tests control the clock.
 */
public final class FightLog {
    /** A hit only counts towards a kill if the target dies this soon after. */
    public static final long KILL_WINDOW_MS = 5_000;
    /** A fight ends after this long without hits, hurts, pops or crystals. */
    public static final long IDLE_END_MS = 15_000;

    public enum Result { KILL, DEATH, ENDED }

    /** What a finished fight looked like. */
    public record Recap(Result result, String opponent, long durationMs, int hitsDealt, int hitsTaken, float damageTaken,
                        int popsYou, int popsThem, int crystals, double bestReach, int bestCombo) {}

    // Session.
    private int kills;
    private int deaths;
    private int streak;
    private int bestStreak;
    // Combo and reach.
    private int combo;
    private int bestComboThisFight;
    private double lastReach = -1;
    private long lastReachAt;
    // Current target.
    private int targetId = Integer.MIN_VALUE;
    private String targetName = "";
    private long targetHitAt;
    // Current fight.
    private boolean inFight;
    private long fightStart;
    private long lastAction;
    private int hitsDealt;
    private int hitsTaken;
    private float damageTaken;
    private int popsYou;
    private int popsThem;
    private int crystals;
    private double bestReach;
    private Recap lastRecap;
    private long lastRecapAt;

    private void touch(long now) {
        if (!inFight) {
            inFight = true;
            fightStart = now;
            hitsDealt = 0;
            hitsTaken = 0;
            damageTaken = 0;
            popsYou = 0;
            popsThem = 0;
            crystals = 0;
            bestReach = 0;
            bestComboThisFight = 0;
        }
        lastAction = now;
    }

    /** You hit a living target {@code distance} blocks away (eye to its hitbox). */
    public void hitDealt(int id, String name, double distance, long now) {
        touch(now);
        hitsDealt++;
        combo++;
        bestComboThisFight = Math.max(bestComboThisFight, combo);
        lastReach = distance;
        lastReachAt = now;
        bestReach = Math.max(bestReach, distance);
        targetId = id;
        targetName = name == null ? "" : name;
        targetHitAt = now;
    }

    /** Your health went down by {@code amount}; the combo resets. */
    public void hurt(float amount, long now) {
        touch(now);
        hitsTaken++;
        damageTaken += Math.max(0, amount);
        combo = 0;
    }

    public void pop(boolean you, long now) {
        touch(now);
        if (you) popsYou++;
        else popsThem++;
    }

    public void crystalPlaced(long now) {
        touch(now);
        crystals++;
    }

    /** An entity died; a kill if it is the target you hit within {@link #KILL_WINDOW_MS}. */
    public void entityDied(int id, long now) {
        if (id != targetId || now - targetHitAt > KILL_WINDOW_MS) return;
        kills++;
        streak++;
        bestStreak = Math.max(bestStreak, streak);
        endFight(Result.KILL, now);
        targetId = Integer.MIN_VALUE;
    }

    public void died(long now) {
        deaths++;
        streak = 0;
        combo = 0;
        endFight(Result.DEATH, now);
    }

    /** Ends an idle fight; call regularly. */
    public void tick(long now) {
        if (inFight && now - lastAction > IDLE_END_MS) endFight(Result.ENDED, now);
    }

    private void endFight(Result result, long now) {
        if (!inFight) return;
        inFight = false;
        // Short scuffles without a result are not worth a recap.
        if (result == Result.ENDED && hitsDealt + hitsTaken < 3) return;
        lastRecap = new Recap(result, targetName, Math.max(0, lastAction - fightStart), hitsDealt, hitsTaken, damageTaken,
                popsYou, popsThem, crystals, bestReach, bestComboThisFight);
        lastRecapAt = now;
    }

    public void resetSession() {
        kills = 0;
        deaths = 0;
        streak = 0;
        bestStreak = 0;
        combo = 0;
        inFight = false;
        lastRecap = null;
    }

    public int kills() {
        return kills;
    }

    public int deaths() {
        return deaths;
    }

    public int streak() {
        return streak;
    }

    public int bestStreak() {
        return bestStreak;
    }

    /** Kills per death, with deaths counted as at least one. */
    public double kdr() {
        return kills / (double) Math.max(1, deaths);
    }

    public int combo() {
        return combo;
    }

    public double lastReach() {
        return lastReach;
    }

    public long lastReachAt() {
        return lastReachAt;
    }

    public int targetId() {
        return targetId;
    }

    public long targetHitAt() {
        return targetHitAt;
    }

    public boolean inFight() {
        return inFight;
    }

    public Recap lastRecap() {
        return lastRecap;
    }

    public long lastRecapAt() {
        return lastRecapAt;
    }
}
