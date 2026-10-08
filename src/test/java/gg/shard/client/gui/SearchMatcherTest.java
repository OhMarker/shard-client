package gg.shard.client.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchMatcherTest {
    @Test
    void everyWordMustMatchSomewhere() {
        assertTrue(SearchMatcher.score("outline colour", "Colour", "Block Outline") > 0, "setting name + module name");
        assertEquals(-1, SearchMatcher.score("outline sound", "Colour", "Block Outline"));
        assertEquals(0, SearchMatcher.score("  ", "Anything"), "empty query matches with no rank");
    }

    @Test
    void colourAndColorAreTheSameWord() {
        assertTrue(SearchMatcher.score("color", "Glow colour") > 0);
        assertTrue(SearchMatcher.score("colour", "Hit Color") > 0);
    }

    @Test
    void nameMatchesRankAboveDescriptionMatches() {
        int byName = SearchMatcher.score("crys", "Crystal Optimizer", "Crystals vanish when hit");
        int byDesc = SearchMatcher.score("crys", "Item Counter", "How much of your crystal kit you have left");
        assertTrue(byName > byDesc);
        assertTrue(byDesc > 0);
        assertTrue(SearchMatcher.score("fps", "FPS") > SearchMatcher.score("fps", "FPS Graph"), "exact beats prefix");
        assertTrue(SearchMatcher.score("totem", "Totem Counter") > SearchMatcher.score("totem", "Session", "Totems you popped"));
    }
}
