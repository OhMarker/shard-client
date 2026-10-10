package gg.shard.client.dev;

import gg.shard.client.ShardClient;
import gg.shard.client.gui.ClickGuiScreen;
import gg.shard.client.modules.visual.CosmeticsModule;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;

/**
 * In-game Cosmetics tab pass (-PsmokeOnly=wardrobe, with -PapiBase and tools/smoke-wardrobe-seed.mjs
 * run first): the menu opens on Mods even when Settings was the last tab, the pointer rests on a
 * segmented control on the Settings page (0.10.0 crashed there), then the Cosmetics tab equips a
 * cape with its own button, shows it worn in F5, and takes it off again.
 */
final class WardrobeSmoke {
    private WardrobeSmoke() {}

    static final int TICKS = 280;
    private static final String CAPE = "cape-halloween";

    static void block(Minecraft mc, int t) {
        CosmeticsModule cosmetics = ShardClient.modules().get(CosmeticsModule.class);
        ClickGuiScreen gui = mc.screen instanceof ClickGuiScreen g ? g : null;
        switch (t) {
            case 0 -> {
                mc.setScreen(null);
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                // As if the menu was last closed on Settings (0.10.0 reopened it there).
                ShardClient.config().gui().addProperty("category", "settings");
            }
            case 2 -> mc.setScreen(new ClickGuiScreen(null));
            case 4 -> SmokeTest.parkCursor(mc);
            case 14 -> {
                boolean mods = gui != null && gui.currentTab() == ClickGuiScreen.Tab.MODS;
                SmokeTest.SUMMARY.addProperty("wardrobeOpensOnMods", mods);
                if (!mods) ShardClient.LOGGER.error("Smoke wardrobe: the menu opened on {}", gui == null ? "nothing" : gui.currentTab());
                shot(mc, "wardrobe-1-opens-on-mods.png");
            }
            case 16 -> gui.selectTab(ClickGuiScreen.Tab.SETTINGS);
            case 24 -> {
                int[] c = gui.hitCentre("set:.*:[0-9]+");
                SmokeTest.SUMMARY.addProperty("wardrobeSegmentFound", c != null);
                if (c == null) ShardClient.LOGGER.error("Smoke wardrobe: no segmented control on the Settings page");
                else pointAt(mc, gui, c);
            }
            // Twenty frames with the pointer on a segment: 0.10.0 crashed on the first one.
            case 46 -> {
                SmokeTest.SUMMARY.addProperty("wardrobeSegmentHoverSurvived", true);
                shot(mc, "wardrobe-2-segment-hover.png");
            }
            case 48 -> {
                SmokeTest.parkCursor(mc);
                gui.selectTab(ClickGuiScreen.Tab.COSMETICS);
            }
            case 50 -> {
                if (!cosmetics.signedIn()) ShardClient.LOGGER.error("Smoke wardrobe: not signed in to the Shard API ({})", cosmetics.status());
                if (cosmetics.wearing("cape") != null) gui.press("cos-off:cape"); // start bare
            }
            case 110 -> shot(mc, "wardrobe-3-tab.png");
            case 112 -> {
                boolean pressed = gui.press("cos-item:" + CAPE);
                if (!pressed) ShardClient.LOGGER.error("Smoke wardrobe: no Equip button for {}", CAPE);
            }
            case 150 -> {
                SmokeTest.SUMMARY.addProperty("wardrobeEquipped", String.valueOf(cosmetics.wearing("cape")));
                shot(mc, "wardrobe-4-equipped.png");
            }
            case 152 -> {
                mc.setScreen(null);
                mc.options.hideGui = true;
                mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            }
            case 180 -> shot(mc, "wardrobe-5-f5-wearing.png");
            case 182 -> {
                mc.options.hideGui = false;
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                mc.setScreen(new ClickGuiScreen(null));
            }
            case 186 -> gui.selectTab(ClickGuiScreen.Tab.COSMETICS);
            case 190 -> {
                boolean pressed = gui.press("cos-off:cape");
                if (!pressed) ShardClient.LOGGER.error("Smoke wardrobe: no Take off button for the cape");
            }
            case 225 -> {
                SmokeTest.SUMMARY.addProperty("wardrobeAfterTakeOff", String.valueOf(cosmetics.wearing("cape")));
                shot(mc, "wardrobe-6-took-off.png");
            }
            case 227 -> {
                mc.setScreen(null);
                mc.options.hideGui = true;
                mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            }
            case 255 -> shot(mc, "wardrobe-7-f5-bare.png");
            case 257 -> {
                mc.options.hideGui = false;
                mc.options.setCameraType(CameraType.FIRST_PERSON);
            }
            default -> {
            }
        }
    }

    /** Puts the pointer on a design-unit point of the menu, as a real mouse move would. */
    private static void pointAt(Minecraft mc, ClickGuiScreen gui, int[] c) {
        double px = c[0] * gui.pixelsPerUnitNow();
        double py = c[1] * gui.pixelsPerUnitNow();
        org.lwjgl.glfw.GLFW.glfwSetCursorPos(mc.getWindow().handle(), px, py);
        ((gg.shard.client.mixin.MouseHandlerInvoker) mc.mouseHandler).shard$onMove(mc.getWindow().handle(), px, py);
    }

    private static void shot(Minecraft mc, String name) {
        mc.gui.getChat().clearMessages(false);
        SmokeTest.shot(mc, name, null);
    }
}
