package gg.shard.client.util;

import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KeysTest {
    @Test
    void namesLettersDigitsAndFunctionKeys() {
        assertEquals("R", Keys.name(GLFW.GLFW_KEY_R));
        assertEquals("7", Keys.name(GLFW.GLFW_KEY_7));
        assertEquals("F5", Keys.name(GLFW.GLFW_KEY_F5));
        assertEquals("RShift", Keys.name(GLFW.GLFW_KEY_RIGHT_SHIFT));
        assertEquals("None", Keys.name(Keys.NONE));
    }

    @Test
    void parsesNamesBackToCodes() {
        assertEquals(GLFW.GLFW_KEY_R, Keys.fromName("r"));
        assertEquals(GLFW.GLFW_KEY_F5, Keys.fromName("F5"));
        assertEquals(GLFW.GLFW_KEY_RIGHT_SHIFT, Keys.fromName("rshift"));
        assertEquals(GLFW.GLFW_KEY_SPACE, Keys.fromName("Space"));
        assertEquals(Keys.NONE, Keys.fromName("none"));
        assertEquals(Integer.MIN_VALUE, Keys.fromName("definitely-not-a-key"));
    }

    @Test
    void roundTripsEveryNamedKey() {
        for (int key = 32; key <= GLFW.GLFW_KEY_LAST; key++) {
            String name = Keys.name(key);
            if (name.startsWith("Key ")) continue;
            assertEquals(key, Keys.fromName(name), "round trip for " + name);
        }
    }
}
