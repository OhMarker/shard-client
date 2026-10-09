package gg.shard.client.compat;

// Stand-in for net.minecraft.client.input.KeyEvent before 1.21.9 (see MouseButtonEvent).
//? if <1.21.9 {
/*import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;

public record KeyEvent(int key, int scancode, int modifiers) {
    /^* The modifier bits held right now (1.21.9's mouse events carry them). ^/
    public static int currentModifiers() {
        int mods = 0;
        if (Screen.hasShiftDown()) mods |= GLFW.GLFW_MOD_SHIFT;
        if (Screen.hasControlDown()) mods |= GLFW.GLFW_MOD_CONTROL;
        if (Screen.hasAltDown()) mods |= GLFW.GLFW_MOD_ALT;
        return mods;
    }
}
*///?}
