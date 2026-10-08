package gg.shard.client.util;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlaceholdersTest {
    @Test
    void substitutesKnownPlaceholdersOnly() {
        assertEquals("Bob popped 3 totems", Placeholders.popMessage("{name} popped {count} totem{s}", "Bob", 3));
        assertEquals("You popped your 1st totem", Placeholders.popMessage("{name} popped your {ordinal} totem", "You", 1));
        assertEquals("Bob popped 1 totem", Placeholders.popMessage("{name} popped {count} totem{s}", "Bob", 1));
    }

    @Test
    void leavesUnknownTokensAndStrayBracesAlone() {
        assertEquals("{nope} x } {", Placeholders.format("{nope} x } {", Map.of("name", "Bob")));
        assertEquals("Bob {", Placeholders.format("{name} {", Map.of("name", "Bob")));
        assertEquals("", Placeholders.format("", Map.of()));
        assertEquals("", Placeholders.format(null, Map.of()));
    }

    @Test
    void keysAreCaseAndSpaceInsensitive() {
        assertEquals("Bob!", Placeholders.format("{ Name }!", Map.of("name", "Bob")));
    }

    @Test
    void ordinals() {
        assertEquals("1st", Placeholders.ordinal(1));
        assertEquals("2nd", Placeholders.ordinal(2));
        assertEquals("3rd", Placeholders.ordinal(3));
        assertEquals("4th", Placeholders.ordinal(4));
        assertEquals("11th", Placeholders.ordinal(11));
        assertEquals("12th", Placeholders.ordinal(12));
        assertEquals("13th", Placeholders.ordinal(13));
        assertEquals("21st", Placeholders.ordinal(21));
        assertEquals("111th", Placeholders.ordinal(111));
    }
}
