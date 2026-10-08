package gg.shard.client.hud;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class UndoStackTest {
    @Test
    void undoRedoWalkTheHistory() {
        UndoStack<String> s = new UndoStack<>(10);
        s.record("a"); // a -> b
        s.record("b"); // b -> c
        assertEquals("b", s.undo("c"));
        assertEquals("a", s.undo("b"));
        assertNull(s.undo("a"));
        assertEquals("b", s.redo("a"));
        assertEquals("c", s.redo("b"));
        assertNull(s.redo("c"));
    }

    @Test
    void aNewChangeClearsRedo() {
        UndoStack<String> s = new UndoStack<>(10);
        s.record("a");
        s.undo("b");
        s.record("a");
        assertFalse(s.canRedo());
    }

    @Test
    void nudgesInARowAreOneStep() {
        UndoStack<String> s = new UndoStack<>(10);
        s.record("p0", "nudge", 1000, 600);
        s.record("p1", "nudge", 1200, 600);
        s.record("p2", "nudge", 1700, 600);
        assertEquals("p0", s.undo("p3"));
        assertFalse(s.canUndo());
        s.record("q0", "nudge", 5000, 600);
        s.record("q1", "nudge", 6000, 600);
        assertEquals("q1", s.undo("q2"), "too far apart to merge");
    }

    @Test
    void historyIsCapped() {
        UndoStack<Integer> s = new UndoStack<>(3);
        for (int i = 0; i < 10; i++) s.record(i);
        assertEquals(9, s.undo(10));
        assertEquals(8, s.undo(9));
        assertEquals(7, s.undo(8));
        assertNull(s.undo(7));
    }
}
