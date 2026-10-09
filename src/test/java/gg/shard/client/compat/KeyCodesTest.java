package gg.shard.client.compat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KeyCodesTest {
    @Test
    void savedCodesRoundTrip() {
        for (int glfw = 0; glfw <= KeyCodes.GLFW.GLFW_KEY_LAST; glfw++) {
            int key = KeyCodes.fromGlfw(glfw);
            if (key == KeyCodes.UNMAPPED) continue;
            assertEquals(glfw, KeyCodes.toGlfw(key), "GLFW " + glfw);
            assertEquals(glfw, KeyCodes.save(KeyCodes.load(glfw)), "saved GLFW " + glfw);
        }
    }

    @Test
    void unboundStaysUnbound() {
        assertEquals(-1, KeyCodes.save(-1));
        assertEquals(-1, KeyCodes.load(-1));
    }
}
