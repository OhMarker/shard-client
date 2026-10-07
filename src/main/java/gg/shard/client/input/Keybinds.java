package gg.shard.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import gg.shard.client.ShardClient;
import gg.shard.client.gui.ClickGuiScreen;
import gg.shard.client.hud.HudEditorScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

/** Global key mappings shown in the vanilla Controls screen under a "Shard" category. */
public final class Keybinds {
    private Keybinds() {}

    public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(Identifier.fromNamespaceAndPath(ShardClient.MOD_ID, "main"));

    public static KeyMapping openGui;
    public static KeyMapping hudEditor;

    public static void init() {
        openGui = KeyBindingHelper.registerKeyBinding(
                new KeyMapping("key.shard.gui", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, CATEGORY));
        hudEditor = KeyBindingHelper.registerKeyBinding(
                new KeyMapping("key.shard.hud", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(Keybinds::tick);
    }

    private static void tick(Minecraft mc) {
        while (openGui.consumeClick()) {
            if (mc.screen == null) mc.setScreen(new ClickGuiScreen(null));
        }
        while (hudEditor.consumeClick()) {
            if (mc.screen == null && mc.player != null) mc.setScreen(new HudEditorScreen(null, ShardClient.hud()));
        }
    }
}
