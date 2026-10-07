package gg.shard.client.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColorsTest {
    @Test
    void parsesSixAndEightDigitHex() {
        assertEquals(0xFF22D3EE, Colors.parseHex("#22D3EE"));
        assertEquals(0x8022D3EE, Colors.parseHex("#8022D3EE"));
        assertEquals(0xFF22D3EE, Colors.parseHex("0x22d3ee"));
        assertNull(Colors.parseHex("#22D3E"));
        assertNull(Colors.parseHex("blue"));
        assertNull(Colors.parseHex(null));
    }

    @Test
    void formatsHexCompactly() {
        assertEquals("#22D3EE", Colors.toHex(0xFF22D3EE));
        assertEquals("#8022D3EE", Colors.toHex(0x8022D3EE));
    }

    @Test
    void mixesAndFades() {
        assertEquals(0xFF808080, Colors.mix(0xFF000000, 0xFFFFFFFF, 0.5) & 0xFFFFFFFF);
        assertEquals(0x80, Colors.alpha(Colors.fade(0xFFFFFFFF, 0.5)));
        assertEquals(0xFFFFFFFF, Colors.contrastText(0xFF000000));
        assertEquals(0xFF06070B, Colors.contrastText(0xFFFFFFFF));
    }

    @Test
    void hsbProducesSaturatedPrimaries() {
        assertEquals(0xFFFF0000, Colors.hsb(0, 1, 1));
        assertEquals(0xFF00FF00, Colors.hsb(1.0 / 3, 1, 1));
        assertEquals(0xFF0000FF, Colors.hsb(2.0 / 3, 1, 1));
        int full = Colors.health(1.0);
        int empty = Colors.health(0.0);
        assertTrue(Colors.green(full) > Colors.red(full), "full health is green-ish");
        assertTrue(Colors.red(empty) > Colors.green(empty), "no health is red-ish");
    }
}
