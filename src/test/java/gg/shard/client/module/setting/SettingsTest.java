package gg.shard.client.module.setting;

import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsTest {
    enum Mode { ROW, COLUMN, GRID }

    @Test
    void intSettingClampsAndSnapsToStep() {
        IntSetting s = new IntSetting("Width", "", 40, 16, 120, 2, "px");
        s.set(500);
        assertEquals(120, s.get());
        s.set(-3);
        assertEquals(16, s.get());
        s.setFraction(0.5);
        assertEquals(68, s.get());
        assertEquals("68px", s.display());
        assertTrue(s.parse("33"));
        assertEquals(33, s.get());
        assertFalse(s.parse("lots"));
    }

    @Test
    void doubleSettingRoundsNoise() {
        DoubleSetting d = new DoubleSetting("Scale", "", 0.65, 0.25, 1.0, 0.05, "x");
        d.setFraction(0.4);
        assertEquals(0.55, d.get(), 1e-9);
        assertEquals("0.55x", d.display());
        d.set(0.1 + 0.2);
        assertEquals(0.3, d.get(), 1e-9);
    }

    @Test
    void enumSettingCyclesAndParsesLoosely() {
        EnumSetting<Mode> e = new EnumSetting<>("Layout", "", Mode.ROW);
        e.cycle(true);
        assertEquals(Mode.COLUMN, e.get());
        e.cycle(false);
        e.cycle(false);
        assertEquals(Mode.GRID, e.get());
        assertTrue(e.parse("row"));
        assertEquals(Mode.ROW, e.get());
        assertFalse(e.parse("hexagon"));
        assertEquals("Row", e.display());
        assertTrue(e.fromJson(new JsonPrimitive("GRID")));
        assertEquals(Mode.GRID, e.get());
    }

    @Test
    void colorSettingRoundTripsJson() {
        ColorSetting c = new ColorSetting("Color", "", 0xFF22D3EE);
        c.setAlpha(0x80);
        assertEquals("#8022D3EE", c.toJson().getAsString());
        ColorSetting again = new ColorSetting("Color", "", 0xFFFFFFFF);
        assertTrue(again.fromJson(c.toJson()));
        assertEquals(c.get(), again.get());
        ColorSetting opaque = new ColorSetting("Opaque", "", 0xFF000000, false);
        opaque.set(0x10FF0000);
        assertEquals(0xFF, opaque.alpha());
    }

    @Test
    void boolSettingParsesWords() {
        BoolSetting b = new BoolSetting("Flag", "", false);
        assertTrue(b.parse("on"));
        assertTrue(b.get());
        assertTrue(b.parse("toggle"));
        assertFalse(b.get());
        assertFalse(b.parse("maybe"));
    }

    @Test
    void changeListenersFireOnlyOnRealChanges() {
        int[] fired = {0};
        IntSetting s = new IntSetting("N", "", 1, 0, 10);
        s.onChange(v -> fired[0]++);
        s.set(1);
        s.set(2);
        s.set(2);
        assertEquals(1, fired[0]);
        assertTrue(s.isDefault() == false);
        s.reset();
        assertTrue(s.isDefault());
    }
}
