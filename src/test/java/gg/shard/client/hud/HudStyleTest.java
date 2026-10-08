package gg.shard.client.hud;

import gg.shard.client.module.setting.Setting;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HudStyleTest {
    static final class Collector implements HudStyle.Registrar {
        final List<Setting<?>> settings = new ArrayList<>();

        @Override
        public <S extends Setting<?>> S add(S setting) {
            settings.add(setting);
            return setting;
        }
    }

    @Test
    void modulesInheritTheDefaultsUntilCustomStyleIsOn() {
        Collector d = new Collector();
        HudStyle defaults = HudStyle.defaults(d);
        Collector m = new Collector();
        HudStyle style = HudStyle.forModule(m, "FPS", true);

        defaults.preset.set(HudStyle.Preset.OUTLINED);
        defaults.padding.set(9);
        style.padding.set(2);
        HudStyle.Resolved r = style.resolve(defaults);
        assertEquals(HudStyle.Preset.OUTLINED, r.preset(), "inherited");
        assertEquals(9, r.padding(), "inherited");
        assertEquals("FPS", r.label(), "label is always the module's own");
        assertTrue(r.hasLabel());

        style.custom.set(true);
        r = style.resolve(defaults);
        assertEquals(HudStyle.Preset.CARD, r.preset(), "own default");
        assertEquals(2, r.padding(), "own value");

        style.label.set("");
        assertFalse(style.resolve(defaults).hasLabel());
        assertNull(HudStyle.forModule(new Collector(), null, false).resolve(defaults).label(), "elements without a label");
    }

    @Test
    void settingsAreDeclaredInTheSameOrderEverywhereAndHiddenWhileInherited() {
        Collector m = new Collector();
        HudStyle style = HudStyle.forModule(m, "FPS", true);
        List<String> keys = new ArrayList<>();
        for (Setting<?> s : m.settings) keys.add(s.key());
        assertEquals(List.of("custom-style", "style", "text-colour", "value-colour", "background", "corner-radius", "padding",
                "text-shadow", "alignment", "brackets", "label", "label-position"), keys);
        for (Setting<?> s : m.settings) {
            if (s == style.custom || s == style.label || s == style.labelSide) assertTrue(s.isVisible(), s.key());
            else assertFalse(s.isVisible(), s.key() + " hidden while inheriting");
            assertEquals(HudStyle.GROUP, s.group());
        }
        style.custom.set(true);
        assertTrue(style.preset.isVisible());
        assertTrue(style.align.isVisible());

        Collector noAlign = new Collector();
        HudStyle plain = HudStyle.forModule(noAlign, null, false);
        plain.custom.set(true);
        assertFalse(plain.align.isVisible(), "alignment hidden where it makes no sense");

        Collector d = new Collector();
        HudStyle defaults = HudStyle.defaults(d);
        assertNull(defaults.custom);
        assertNull(defaults.label);
        assertTrue(defaults.preset.isVisible());
        assertTrue(defaults.overrides(), "defaults always use their own values");
        assertEquals(HudStyle.Preset.CARD, defaults.resolve(null).preset());
    }
}
