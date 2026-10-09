package gg.shard.client.modules.visual;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkyPaletteTest {
    @Test
    void daylightIsFullAtNoonAndZeroAtMidnight() {
        assertEquals(1f, SkyPalette.daylight(0f));
        assertEquals(0f, SkyPalette.daylight((float) Math.PI));
        float dusk = SkyPalette.daylight((float) (Math.PI / 2));
        assertTrue(dusk > 0f && dusk < 1f, "fades at sunset");
    }

    @Test
    void mixesDayAndNightAndDimsForRain() {
        int day = 0xFF8080FF;
        int night = 0xFF000010;
        assertEquals(day, SkyPalette.at(day, night, 1f, 0f, 0f));
        assertEquals(night, SkyPalette.at(day, night, 0f, 0f, 0f));
        assertEquals(0xFF4040_80, SkyPalette.at(day, night, 1f, 1f, 0f), "full rain halves the light");
        assertEquals(0xFF2020_40, SkyPalette.at(day, night, 1f, 1f, 1f), "thunder halves it again");
    }

    @Test
    void lerpClampsAndStaysOpaque() {
        assertEquals(0xFF000000, SkyPalette.lerp(-1f, 0x00000000, 0xFFFFFFFF));
        assertEquals(0xFFFFFFFF, SkyPalette.lerp(2f, 0x00000000, 0xFFFFFFFF));
        assertEquals(0xFF808080, SkyPalette.lerp(0.5f, 0xFF000000, 0xFFFFFFFF));
    }
}
