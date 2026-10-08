package gg.shard.client.dev;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import gg.shard.client.ShardClient;
import gg.shard.client.gui.ClickGuiScreen;
import gg.shard.client.hud.HudEditorScreen;
import gg.shard.client.module.Module;
import gg.shard.client.module.setting.ColorSetting;
import gg.shard.client.module.setting.Setting;
import gg.shard.client.modules.combat.CrystalOptimizerModule;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Development-only verification. With the JVM property {@code shard.smoke.dir} set in a dev
 * environment: skip first-run onboarding, connect to {@code shard.smoke.server}, wait for a
 * world, then at GUI scale 1, 2, 3, 4 and Auto screenshot the HUD, the mod grid, an open
 * settings panel, the colour picker, the Settings page and the HUD editor; write 4x zoomed crops
 * of text regions so font smoothness is visible; record the page layout per scale (it must be
 * identical); and finally place and hit an end crystal in creative to exercise the Crystal
 * Optimizer. {@code shard.smoke.bench} runs {@link Benchmark} instead. Never active in a
 * distributed jar (Loom only sets the properties for runClient).
 */
public final class SmokeTest {
    private SmokeTest() {}

    /** 0 is vanilla's "Auto". */
    private static final int[] SCALES = {1, 2, 3, 4, 0};
    private static final int STEP_TICKS = 150;

    private static int ticksInWorld = -1;
    private static int ticksOutOfWorld;
    private static boolean connectRequested;
    private static boolean active;
    private static boolean bench;
    private static Path out;

    private static boolean sampling;
    private static long lastFrameNs;
    private static final List<Long> FRAMES = new ArrayList<>();

    private static BlockPos ground;
    private static int crystalsBeforeAttack;
    private static final JsonObject SUMMARY = new JsonObject();
    private static final JsonArray LAYOUTS = new JsonArray();

    public static void init() {
        String dir = System.getProperty("shard.smoke.dir");
        if (dir == null || dir.isBlank() || !FabricLoader.getInstance().isDevelopmentEnvironment()) return;
        out = Path.of(dir);
        try {
            Files.createDirectories(out);
        } catch (IOException e) {
            ShardClient.LOGGER.error("Smoke: cannot create {}", out, e);
            return;
        }
        bench = "1".equals(System.getProperty("shard.smoke.bench"));
        String server = System.getProperty("shard.smoke.server");
        active = true;
        ShardClient.LOGGER.info("Smoke test armed ({}); output -> {}; server -> {}; launch args {}", bench ? "benchmark" : "screenshots",
                out, server, Arrays.toString(FabricLoader.getInstance().getLaunchArguments(false)));
        ClientTickEvents.END_CLIENT_TICK.register(mc -> tick(mc, server));
    }

    /** Called from the HUD layer every frame; records frame times while sampling. */
    public static void onFrame() {
        if (!sampling) return;
        long now = System.nanoTime();
        if (lastFrameNs != 0) FRAMES.add(now - lastFrameNs);
        lastFrameNs = now;
    }

    static void startSampling() {
        FRAMES.clear();
        lastFrameNs = 0;
        sampling = true;
    }

    static List<Long> stopSampling() {
        sampling = false;
        return new ArrayList<>(FRAMES);
    }

    static Path outDir() {
        return out;
    }

    private static void tick(Minecraft mc, String server) {
        if (!active) return;
        if (mc.player == null || mc.level == null) {
            ticksInWorld = -1;
            ticksOutOfWorld++;
            // The dev window rarely has focus; never let the pause menu cover the screenshots.
            if (ticksOutOfWorld == 1) mc.options.pauseOnLostFocus = false;
            if (mc.screen instanceof AccessibilityOnboardingScreen) {
                ShardClient.LOGGER.info("Smoke: skipping accessibility onboarding");
                mc.options.onboardAccessibility = false;
                mc.options.save();
                mc.setScreen(new TitleScreen());
                return;
            }
            if (server != null && !connectRequested && ticksOutOfWorld > 40 && !(mc.screen instanceof ConnectScreen)) {
                connectRequested = true;
                ShardClient.LOGGER.info("Smoke: connecting to {} (screen {})", server,
                        mc.screen == null ? "none" : mc.screen.getClass().getSimpleName());
                ServerData data = new ServerData("Shard smoke", server, ServerData.Type.OTHER);
                ConnectScreen.startConnecting(mc.screen, mc, ServerAddress.parseString(server), data, false, null);
            }
            if (ticksOutOfWorld > 20 * 90) {
                ShardClient.LOGGER.error("Smoke: never reached a world; giving up (screen {})",
                        mc.screen == null ? "none" : mc.screen.getClass().getSimpleName());
                mc.stop();
            }
            return;
        }
        ticksInWorld++;
        if (bench) {
            Benchmark.tick(mc, ticksInWorld);
            return;
        }
        if (ticksInWorld == 5) quietHud(mc);
        if (ticksInWorld < 10) return;
        int t = ticksInWorld - 10;
        int block = t / STEP_TICKS;
        int local = t % STEP_TICKS;
        if (block < SCALES.length) {
            scaleBlock(mc, SCALES[block], local);
            return;
        }
        int after = t - SCALES.length * STEP_TICKS;
        if (after < MENU_TICKS) {
            menuBlock(mc, after);
            return;
        }
        after -= MENU_TICKS;
        if (after < EDITOR_TICKS) {
            editorBlock(mc, after);
            return;
        }
        after -= EDITOR_TICKS;
        if (after < DENSITY_TICKS) {
            densityBlock(mc, after);
            return;
        }
        switch (after - DENSITY_TICKS) {
            case 0 -> {
                mc.setScreen(null);
                setScale(mc, 2);
                prepareCrystalTest(mc);
            }
            case 30 -> placeObsidian(mc);
            case 40 -> placeCrystal(mc);
            case 43 -> logPlacement(mc);
            case 60 -> shot(mc, "smoke-crystal.png", null);
            case 70 -> attackCrystal(mc);
            case 85 -> {
                logRemoval(mc);
                finish(mc);
            }
            default -> {
            }
        }
    }

    /** One pass of screenshots at a GUI scale (0 = Auto); {@code local} is the tick within the pass. */
    private static void scaleBlock(Minecraft mc, int scale, int local) {
        String tag = scale == 0 ? "auto" : String.valueOf(scale);
        switch (local) {
            case 0 -> {
                mc.setScreen(null);
                setScale(mc, scale);
                // Park the pointer in the page margin so no hover state leaks into the screenshots.
                org.lwjgl.glfw.GLFW.glfwSetCursorPos(mc.getWindow().handle(), 4, 4);
            }
            case 15 -> shot(mc, "smoke-hud-scale" + tag + ".png", null);
            case 20 -> openGui(mc);
            case 22, 47, 72, 97, 122 -> parkCursor(mc);
            case 40 -> {
                recordLayout(mc, tag);
                shot(mc, "smoke-gui-scale" + tag + ".png", scale == 2 || scale == 4 ? "smoke-zoom-card-scale" + tag + ".png" : null);
            }
            case 45 -> openPanel(mc, "crystal-optimizer");
            case 65 -> shot(mc, "smoke-panel-scale" + tag + ".png", scale == 2 || scale == 1 ? "smoke-zoom-panel-scale" + tag + ".png" : null);
            case 70 -> {
                openPanel(mc, "keystrokes");
                openColor(mc, "keystrokes", "pressed");
            }
            case 90 -> shot(mc, "smoke-color-scale" + tag + ".png", null);
            case 95 -> openSettings(mc);
            case 115 -> shot(mc, "smoke-settings-scale" + tag + ".png", null);
            case 120 -> mc.setScreen(new HudEditorScreen(null, ShardClient.hud()));
            case 135 -> shot(mc, "smoke-editor-scale" + tag + ".png", null);
            case 140 -> mc.setScreen(null);
            default -> {
            }
        }
    }

    private static final int MENU_TICKS = 100;

    /** The 0.4.0 menu: list view, grid view, search over settings, and jumping to a setting. */
    private static void menuBlock(Minecraft mc, int local) {
        switch (local) {
            case 0 -> {
                setScale(mc, 2);
                openGui(mc);
                gui(mc).setGridView(false);
                openPanel(mc, "crystal-optimizer");
            }
            case 2, 27, 47, 67 -> parkCursor(mc);
            case 20 -> shot(mc, "smoke-menu-list.png", null);
            case 25 -> gui(mc).setGridView(true);
            case 40 -> shot(mc, "smoke-menu-grid.png", null);
            case 45 -> {
                gui(mc).setGridView(false);
                gui(mc).setSearch("hit sound");
            }
            case 60 -> shot(mc, "smoke-menu-search.png", null);
            case 65 -> gui(mc).openSearchResult(0);
            case 80 -> shot(mc, "smoke-menu-jump.png", null);
            case 85 -> {
                gui(mc).setSearch("");
                mc.setScreen(null);
            }
            default -> {
            }
        }
    }

    private static final int EDITOR_TICKS = 150;
    private static java.util.Map<String, gg.shard.client.hud.HudPresets.Entry> layoutBefore;

    private static gg.shard.client.hud.HudModule hudModule(String key) {
        for (var m : ShardClient.hud().hudModules()) if (m.key().equals(key)) return m;
        throw new IllegalStateException("no HUD module " + key);
    }

    private static HudEditorScreen editor(Minecraft mc) {
        return mc.screen instanceof HudEditorScreen e ? e : null;
    }

    /** The 0.4.0 HUD editor: presets, the presets menu, snap guides, the side panel, undo, then the live HUD. */
    private static void editorBlock(Minecraft mc, int local) {
        switch (local) {
            case 0 -> {
                setScale(mc, 2);
                layoutBefore = gg.shard.client.hud.HudPresets.snapshot(ShardClient.hud());
                mc.setScreen(new HudEditorScreen(null, ShardClient.hud()));
            }
            case 2, 27, 47, 67, 87 -> parkCursor(mc);
            case 5 -> editor(mc).applyPreset(gg.shard.client.hud.HudPresets.MINIMAL);
            case 20 -> shot(mc, "smoke-hudeditor-preset.png", null);
            case 25 -> editor(mc).setPresetsOpen(true);
            case 40 -> {
                shot(mc, "smoke-hudeditor-presets-menu.png", null);
            }
            case 45 -> {
                editor(mc).setPresetsOpen(false);
                editor(mc).select(hudModule("item-counter"));
                editor(mc).previewDrag(1.5, -0.5);
            }
            case 60 -> shot(mc, "smoke-hudeditor-drag.png", null);
            case 65 -> {
                editor(mc).endPreviewDrag();
                editor(mc).select(hudModule("fps"), hudModule("ping"));
                editor(mc).openPanel(hudModule("fps"));
            }
            case 85 -> shot(mc, "smoke-hudeditor-panel.png", null);
            case 90 -> {
                var counter = hudModule("item-counter");
                double before = counter.posX(mc.getWindow().getGuiScaledWidth());
                editor(mc).undo(); // the preview drag
                editor(mc).undo(); // the preset
                double after = counter.posX(mc.getWindow().getGuiScaledWidth());
                ShardClient.LOGGER.info("Smoke: HUD editor undo moved Item Counter from {} to {}", before, after);
                SUMMARY.addProperty("hudEditorUndoWorks", Math.abs(before - after) > 0.01);
                mc.setScreen(null);
                gg.shard.client.hud.HudPresets.apply(ShardClient.hud(), gg.shard.client.hud.HudPresets.MINIMAL);
            }
            case 110 -> shot(mc, "smoke-hud-preset-minimal.png", null);
            case 115 -> gg.shard.client.hud.HudPresets.apply(ShardClient.hud(), gg.shard.client.hud.HudPresets.FULL);
            case 135 -> shot(mc, "smoke-hud-preset-full.png", null);
            case 140 -> gg.shard.client.hud.HudPresets.restore(ShardClient.hud(), layoutBefore);
            default -> {
            }
        }
    }

    private static final int DENSITY_TICKS = 80;

    /**
     * Sharp-text pass: the settings page at 125% and 150% interface size and a HUD element at a
     * non-integer scale, the cases that used to stair-step. Crops are made from these offline.
     */
    private static void densityBlock(Minecraft mc, int local) {
        var appearance = ShardClient.appearance();
        var fps = ShardClient.modules().get(gg.shard.client.modules.hud.FpsModule.class);
        switch (local) {
            case 0 -> {
                setScale(mc, 2);
                appearance.interfaceSize.set(125);
                openGui(mc);
            }
            case 2, 27 -> parkCursor(mc);
            case 20 -> shot(mc, "smoke-density-gui-125.png", null);
            case 25 -> {
                appearance.interfaceSize.set(150);
                openGui(mc);
            }
            case 45 -> shot(mc, "smoke-density-gui-150.png", null);
            case 50 -> {
                appearance.interfaceSize.set(100);
                mc.setScreen(null);
                fps.setScale(1.65);
            }
            case 65 -> shot(mc, "smoke-density-hud-fps165.png", null);
            case 70 -> fps.setScale(1.0);
            default -> {
            }
        }
    }

    /** Opening a screen recentres the cursor; move it into the page margin so no hover state shows. */
    private static void parkCursor(Minecraft mc) {
        org.lwjgl.glfw.GLFW.glfwSetCursorPos(mc.getWindow().handle(), 4, 4);
    }

    // ---- GUI steps -----------------------------------------------------------------------------

    private static void setScale(Minecraft mc, int scale) {
        mc.options.guiScale().set(scale);
        mc.resizeDisplay();
        ShardClient.LOGGER.info("Smoke: GUI scale {} -> effective {} ({}x{})", scale == 0 ? "auto" : scale, mc.getWindow().getGuiScale(),
                mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
    }

    private static void openGui(Minecraft mc) {
        mc.setScreen(new ClickGuiScreen(null));
    }

    private static ClickGuiScreen gui(Minecraft mc) {
        if (mc.screen instanceof ClickGuiScreen g) return g;
        ShardClient.LOGGER.error("Smoke: settings screen is not open (screen {})", mc.screen == null ? "none" : mc.screen.getClass().getSimpleName());
        return null;
    }

    private static void openPanel(Minecraft mc, String moduleKey) {
        ClickGuiScreen gui = gui(mc);
        if (gui == null) return;
        Module m = ShardClient.modules().find(moduleKey);
        if (m == null) {
            ShardClient.LOGGER.error("Smoke: no module {}", moduleKey);
            return;
        }
        gui.openModule(m);
    }

    private static void openColor(Minecraft mc, String moduleKey, String settingKey) {
        ClickGuiScreen gui = gui(mc);
        if (gui == null) return;
        Module m = ShardClient.modules().find(moduleKey);
        Setting<?> s = m == null ? null : m.setting(settingKey);
        if (s instanceof ColorSetting c) gui.openColorPicker(c);
        else ShardClient.LOGGER.error("Smoke: {}.{} is not a colour setting", moduleKey, settingKey);
    }

    private static void openSettings(Minecraft mc) {
        ClickGuiScreen gui = gui(mc);
        if (gui != null) gui.openSettingsPage();
    }

    private static void recordLayout(Minecraft mc, String tag) {
        ClickGuiScreen gui = gui(mc);
        if (gui == null) return;
        ClickGuiScreen.LayoutInfo info = gui.layoutInfo();
        JsonObject o = new JsonObject();
        o.addProperty("guiScaleSetting", tag);
        o.addProperty("guiScale", mc.getWindow().getGuiScale());
        o.addProperty("guiWidth", mc.getWindow().getGuiScaledWidth());
        o.addProperty("guiHeight", mc.getWindow().getGuiScaledHeight());
        o.addProperty("pageScale", info.pageScale());
        o.addProperty("designWidth", info.designWidth());
        o.addProperty("designHeight", info.designHeight());
        o.addProperty("narrow", info.narrow());
        o.addProperty("gridColumns", info.gridColumns());
        o.addProperty("cardWidth", info.cardWidth());
        o.addProperty("cardHeight", info.cardHeight());
        o.addProperty("sidebarWidth", info.sidebarWidth());
        o.addProperty("panelWidth", info.panelWidth());
        LAYOUTS.add(o);
        ShardClient.LOGGER.info("Smoke: layout at GUI scale {}: {}", tag, o);
    }

    // ---- crystal exercise ----------------------------------------------------------------------

    /** Items come from server commands (the smoke player is an operator) so both sides agree. */
    static void giveKit(LocalPlayer player) {
        player.connection.sendCommand("clear @s");
        player.connection.sendCommand("give @s minecraft:obsidian 64");
        player.connection.sendCommand("give @s minecraft:end_crystal 64");
    }

    private static void prepareCrystalTest(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null || mc.gameMode == null) return;
        giveKit(player);
        player.getInventory().setSelectedSlot(0);
        Direction dir = player.getDirection();
        ground = player.blockPosition().relative(dir, 2).below();
        // Do not depend on the terrain (the benchmark leaves the player hovering): build the base block and clear above it.
        player.connection.sendCommand(String.format(java.util.Locale.ROOT, "setblock %d %d %d minecraft:obsidian", ground.getX(), ground.getY(), ground.getZ()));
        player.connection.sendCommand(String.format(java.util.Locale.ROOT, "fill %d %d %d %d %d %d minecraft:air", ground.getX(), ground.getY() + 1, ground.getZ(),
                ground.getX(), ground.getY() + 3, ground.getZ()));
        player.connection.sendCommand("kill @e[type=minecraft:end_crystal]");
        ShardClient.LOGGER.info("Smoke: crystal test at {} (facing {}), game mode {}", ground, dir, mc.gameMode.getPlayerMode());
    }

    private static void placeObsidian(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null || ground == null) return;
        player.getInventory().setSelectedSlot(0);
        ShardClient.LOGGER.info("Smoke: hotbar 0 = {}, hotbar 1 = {}, target {} is {}", player.getInventory().getItem(0), player.getInventory().getItem(1),
                ground, mc.level.getBlockState(ground));
        var result = use(mc, player, ground);
        ShardClient.LOGGER.info("Smoke: placed obsidian above {} -> {} (client result {})", ground, mc.level.getBlockState(ground.above()), result);
    }

    private static void placeCrystal(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null || ground == null) return;
        player.getInventory().setSelectedSlot(1);
        var result = use(mc, player, ground.above());
        ShardClient.LOGGER.info("Smoke: placed crystal on {} (client result {})", ground.above(), result);
    }

    private static Object use(Minecraft mc, LocalPlayer player, BlockPos target) {
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(target).add(0, 0.5, 0), Direction.UP, target, false);
        var result = mc.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hit);
        player.swing(InteractionHand.MAIN_HAND);
        return result;
    }

    private static void quietHud(Minecraft mc) {
        mc.getToastManager().clear();
        mc.options.tutorialStep = net.minecraft.client.tutorial.TutorialSteps.NONE;
        mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
    }

    static List<EndCrystal> crystals(Minecraft mc) {
        List<EndCrystal> out = new ArrayList<>();
        if (mc.level == null) return out;
        for (Entity e : mc.level.entitiesForRendering()) if (e instanceof EndCrystal c) out.add(c);
        return out;
    }

    private static void logPlacement(Minecraft mc) {
        CrystalOptimizerModule module = ShardClient.modules().get(CrystalOptimizerModule.class);
        List<EndCrystal> all = crystals(mc);
        int fakes = 0;
        for (EndCrystal c : all) if (module.isFake(c)) fakes++;
        ShardClient.LOGGER.info("Smoke: Crystal Optimizer showed {} fake crystal(s); world now has {} crystal(s), {} of them fake",
                module.fakesShown(), all.size(), fakes);
        SUMMARY.addProperty("fakesShown", module.fakesShown());
        SUMMARY.addProperty("crystalsAfterPlace", all.size());
        SUMMARY.addProperty("fakesRemaining", fakes);
    }

    private static void attackCrystal(Minecraft mc) {
        LocalPlayer player = mc.player;
        CrystalOptimizerModule module = ShardClient.modules().get(CrystalOptimizerModule.class);
        EndCrystal target = null;
        for (EndCrystal c : crystals(mc)) if (!module.isFake(c)) target = c;
        crystalsBeforeAttack = crystals(mc).size();
        if (target == null || player == null) {
            ShardClient.LOGGER.error("Smoke: no real crystal to attack (crystals={})", crystalsBeforeAttack);
            SUMMARY.addProperty("attacked", false);
            return;
        }
        mc.gameMode.attack(player, target);
        player.swing(InteractionHand.MAIN_HAND);
        boolean gone = mc.level.getEntity(target.getId()) == null;
        ShardClient.LOGGER.info("Smoke: attacked crystal {}; removed client-side immediately: {}", target.getId(), gone);
        SUMMARY.addProperty("attacked", true);
        SUMMARY.addProperty("removedImmediately", gone);
    }

    private static void logRemoval(Minecraft mc) {
        CrystalOptimizerModule module = ShardClient.modules().get(CrystalOptimizerModule.class);
        int after = crystals(mc).size();
        ShardClient.LOGGER.info("Smoke: Crystal Optimizer removed {} crystal(s) client-side this session; crystals before attack {}, after {}",
                module.removedCount(), crystalsBeforeAttack, after);
        SUMMARY.addProperty("removedCount", module.removedCount());
        SUMMARY.addProperty("crystalsBeforeAttack", crystalsBeforeAttack);
        SUMMARY.addProperty("crystalsAfterAttack", after);
    }

    private static void finish(Minecraft mc) {
        ShardClient.config().save();
        SUMMARY.add("layouts", LAYOUTS);
        boolean identical = true;
        for (int i = 1; i < LAYOUTS.size(); i++) {
            JsonObject a = LAYOUTS.get(0).getAsJsonObject();
            JsonObject b = LAYOUTS.get(i).getAsJsonObject();
            for (String k : new String[]{"narrow", "gridColumns", "cardWidth", "cardHeight", "sidebarWidth", "panelWidth"}) {
                if (!a.get(k).equals(b.get(k))) identical = false;
            }
            // Vanilla rounds the GUI size up, so a window height that does not divide by the GUI
            // scale (1061 on a 1080p monitor) shifts the page by a unit or two; that is not a layout change.
            for (String k : new String[]{"designWidth", "designHeight"}) {
                if (Math.abs(a.get(k).getAsInt() - b.get(k).getAsInt()) > 2) identical = false;
            }
        }
        SUMMARY.addProperty("layoutIdenticalAcrossScales", identical);
        SUMMARY.addProperty("configSchemaLoaded", ShardClient.config().loadedVersion());
        try {
            Files.writeString(out.resolve("smoke-summary.json"), new GsonBuilder().setPrettyPrinting().create().toJson(SUMMARY), StandardCharsets.UTF_8);
        } catch (IOException e) {
            ShardClient.LOGGER.error("Smoke: could not write summary", e);
        }
        ShardClient.LOGGER.info("Smoke test complete (layouts identical across scales: {}); stopping client", identical);
        mc.stop();
    }

    /**
     * Writes the framebuffer to {@code name}; with {@code zoomName} also writes a 4x
     * nearest-neighbour crop of the region around the first card's text (or the panel header),
     * so anti-aliasing is visible pixel for pixel.
     */
    static void shot(Minecraft mc, String name, String zoomName) {
        quietHud(mc);
        Path file = out.resolve(name);
        Screenshot.takeScreenshot(mc.getMainRenderTarget(), image -> {
            try (image) {
                image.writeToFile(file);
                ShardClient.LOGGER.info("Smoke: wrote {}", file);
                if (zoomName != null) writeZoom(image, out.resolve(zoomName), zoomName.contains("panel"));
            } catch (IOException e) {
                ShardClient.LOGGER.error("Smoke: could not write {}", file, e);
            }
        });
    }

    /** Crops a text region (physical pixels) and scales it 4x with nearest sampling. */
    private static void writeZoom(NativeImage image, Path file, boolean panel) throws IOException {
        int zoom = 4;
        // The 1280x720 window at any GUI scale is 640x360 design units, 2 px per unit:
        // first card text starts at design (296, 92); the panel header text at (contentX + 60, 52).
        int cropW = 220;
        int cropH = 70;
        int x = panel ? image.getWidth() - 2 * (224 + 360 - 20) + 2 * 72 : 2 * 296;
        int y = panel ? 2 * 76 : 2 * 92;
        if (panel && image.getWidth() <= 2 * 640) x = 2 * (224 + 52);
        x = Math.max(0, Math.min(image.getWidth() - cropW, x));
        y = Math.max(0, Math.min(image.getHeight() - cropH, y));
        try (NativeImage zoomed = new NativeImage(cropW * zoom, cropH * zoom, false)) {
            for (int zy = 0; zy < cropH * zoom; zy++) {
                for (int zx = 0; zx < cropW * zoom; zx++) {
                    zoomed.setPixel(zx, zy, image.getPixel(x + zx / zoom, y + zy / zoom) | 0xFF000000);
                }
            }
            zoomed.writeToFile(file);
            ShardClient.LOGGER.info("Smoke: wrote {} (crop {},{} {}x{} at {}x)", file, x, y, cropW, cropH, zoom);
        }
    }
}
