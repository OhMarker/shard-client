package gg.shard.client.hud;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Snapshot undo/redo for the HUD editor (pure, tested). Call {@link #record} with the state
 * <em>before</em> a change; {@link #undo} and {@link #redo} take the current state and return
 * the one to restore. A new change clears the redo history. Changes recorded under the same
 * {@code mergeKey} within {@code mergeMs} (a run of arrow-key nudges) collapse into one step.
 */
public final class UndoStack<T> {
    private final int limit;
    private final Deque<T> undo = new ArrayDeque<>();
    private final Deque<T> redo = new ArrayDeque<>();
    private String lastKey;
    private long lastAt;

    public UndoStack(int limit) {
        this.limit = Math.max(1, limit);
    }

    public void record(T before) {
        record(before, null, 0, 0);
    }

    public void record(T before, String mergeKey, long nowMs, long mergeMs) {
        boolean merge = mergeKey != null && mergeKey.equals(lastKey) && nowMs - lastAt <= mergeMs && !undo.isEmpty();
        lastKey = mergeKey;
        lastAt = nowMs;
        redo.clear();
        if (merge) return;
        undo.push(before);
        while (undo.size() > limit) undo.removeLast();
    }

    public boolean canUndo() {
        return !undo.isEmpty();
    }

    public boolean canRedo() {
        return !redo.isEmpty();
    }

    /** Returns the state to restore, or null when there is nothing to undo. */
    public T undo(T current) {
        if (undo.isEmpty()) return null;
        redo.push(current);
        lastKey = null;
        return undo.pop();
    }

    public T redo(T current) {
        if (redo.isEmpty()) return null;
        undo.push(current);
        lastKey = null;
        return redo.pop();
    }
}
