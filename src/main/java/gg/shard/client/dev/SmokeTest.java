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

    public static boolean active() {
        return active;
    }
    private static boolean bench;
    private static Path out;

    private static boolean sampling;
    private static long lastFrameNs;
    private static final List<Long> FRAMES = new ArrayList<>();

    private static BlockPos ground;
    private static int crystalsBeforeAttack;
    static final JsonObject SUMMARY = new JsonObject();
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
        if ("screens".equals(System.getProperty("shard.smoke.only"))) {
            ScreensSmoke.tick(mc); // title screen, server list and account switcher (-PsmokeOnly=screens)
            return;
        }
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
        // A resource reload (Mojang screen) pauses the script so no shot captures it.
        if (mc.getOverlay() != null) return;
        ticksInWorld++;
        if (bench) {
            Benchmark.tick(mc, ticksInWorld);
            return;
        }
        if (ticksInWorld == 2) {
            // A previous run may have left the player dead or in another game mode; start from a known spot.
            if (mc.player.isDeadOrDying()) mc.player.respawn();
            mc.player.connection.sendCommand("gamemode creative");
            mc.player.connection.sendCommand("tp @s -5 -22 2 180 30");
            mc.player.connection.sendCommand("time set noon");
            mc.player.connection.sendCommand("gamerule doDaylightCycle false");
        }
        if (ticksInWorld == 5) quietHud(mc);
        if (ticksInWorld < 10) return;
        int t = ticksInWorld - 10;
        String only = System.getProperty("shard.smoke.only");
        if ("cosmetics".equals(only)) {
            if (t < COSMETICS_TICKS) cosmeticsBlock(mc, t);
            else finish(mc);
            return;
        }
        if ("set".equals(only)) {
            if (t < SetSmoke.TICKS) SetSmoke.block(mc, t);
            else finish(mc);
            return;
        }
        if ("wardrobe".equals(only)) {
            if (t < WardrobeSmoke.TICKS) WardrobeSmoke.block(mc, t);
            else finish(mc);
            return;
        }
        if ("drop2".equals(only)) {
            if (t < DropSmoke.TICKS) DropSmoke.block(mc, t);
            else finish(mc);
            return;
        }
        if ("scales".equals(only)) {
            if (t < SCALES_TICKS) scalesBlock(mc, t);
            else finish(mc);
            return;
        }
        if ("features".equals(only)) {
            if (t < FEATURES_TICKS) featuresBlock(mc, t);
            else finish(mc);
            return;
        }
        if ("audit".equals(only)) {
            if (!auditBlock(mc, t)) finish(mc);
            return;
        }
        if ("fire".equals(only)) {
            if (t < FIRE_TICKS) fireBlock(mc, t);
            else finish(mc);
            return;
        }
        if ("hudscale".equals(only)) {
            if (t < HudScaleSmoke.TICKS) HudScaleSmoke.block(mc, t);
            else finish(mc);
            return;
        }
        if ("menu".equals(only)) {
            if (t < MENU07_TICKS) menu07Block(mc, t);
            else finish(mc);
            return;
        }
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
        if (after < FIRE_TICKS) {
            fireBlock(mc, after);
            return;
        }
        after -= FIRE_TICKS;
        if (after < CROSSHAIR_TICKS) {
            crosshairBlock(mc, after);
            return;
        }
        after -= CROSSHAIR_TICKS;
        if (after < SHIELD_TICKS) {
            shieldBlock(mc, after);
            return;
        }
        after -= SHIELD_TICKS;
        if (after < ANCHOR_TICKS) {
            anchorCrystalBlock(mc, after);
            return;
        }
        after -= ANCHOR_TICKS;
        if (after < SCALES_TICKS) {
            scalesBlock(mc, after);
            return;
        }
        after -= SCALES_TICKS;
        if (after < DISPLAY_TICKS) {
            displayBlock(mc, after);
            return;
        }
        after -= DISPLAY_TICKS;
        if (after < STYLE_TICKS) {
            styleBlock(mc, after);
            return;
        }
        after -= STYLE_TICKS;
        if (after < STEP7_TICKS) {
            step7Block(mc, after);
            return;
        }
        after -= STEP7_TICKS;
        if (after < DENSITY_TICKS) {
            densityBlock(mc, after);
            return;
        }
        after -= DENSITY_TICKS;
        if (after < COSMETICS_TICKS) {
            cosmeticsBlock(mc, after);
            return;
        }
        switch (after - COSMETICS_TICKS) {
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

    private static final int MENU_TICKS = 125;

    /** The 0.4.0 menu: list view, grid view, search over settings, and jumping to a setting. */
    private static void menuBlock(Minecraft mc, int local) {
        switch (local) {
            case 0 -> {
                setScale(mc, 2);
                openGui(mc);
                gui(mc).setGridView(false);
                openPanel(mc, "crystal-optimizer");
            }
            case 2 -> {
                // On windows under 1640 px the detail column covers the list; show the list itself.
                gui(mc).showList();
                parkCursor(mc);
            }
            case 27, 47, 67 -> parkCursor(mc);
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
                // The view of the owner's 0.3.0 feedback screenshot: the HUD category.
                gui(mc).setSearch("");
                gui(mc).showCategory(gg.shard.client.module.ModuleCategory.HUD);
                gui(mc).setGridView(true);
            }
            case 87 -> {
                gui(mc).showList();
                parkCursor(mc);
            }
            case 100 -> shot(mc, "smoke-menu-hud-grid.png", null);
            case 102 -> gui(mc).setGridView(false);
            case 115 -> shot(mc, "smoke-menu-hud-list.png", null);
            case 120 -> mc.setScreen(null);
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

    private static void setSetting(String moduleKey, String settingKey, String value) {
        for (var m : ShardClient.modules().all()) {
            if (!m.key().equals(moduleKey)) continue;
            for (var st : m.settings()) if (st.key().equals(settingKey)) st.parse(value);
        }
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
            case 115 -> {
                gg.shard.client.hud.HudPresets.apply(ShardClient.hud(), gg.shard.client.hud.HudPresets.FULL);
                setSetting("fps", "frame-time-graph", "true");
                setSetting("ping", "graph", "true");
                // Room for the graphs.
                hudModule("ping").applyLayout(new double[]{0, 0, 4, 40, 1});
                hudModule("cps").applyLayout(new double[]{0, 0, 4, 76, 1});
                hudModule("coordinates").applyLayout(new double[]{0, 0, 4, 92, 1});
            }
            case 135 -> shot(mc, "smoke-hud-preset-full.png", null);
            case 140 -> {
                setSetting("fps", "frame-time-graph", "false");
                setSetting("ping", "graph", "false");
                gg.shard.client.hud.HudPresets.restore(ShardClient.hud(), layoutBefore);
            }
            default -> {
            }
        }
    }

    private static final int FIRE_TICKS = 200;

    private static void cmd(Minecraft mc, String format, Object... args) {
        if (mc.player != null) mc.player.connection.sendCommand(String.format(java.util.Locale.ROOT, format, args));
    }

    private static Module module(String key) {
        for (var m : ShardClient.modules().all()) if (m.key().equals(key)) return m;
        throw new IllegalStateException("no module " + key);
    }

    /** Low Fire: fire and soul fire on the ground, a burning zombie and a burning player; off, on, tinted, and the preview. */
    private static void fireBlock(Minecraft mc, int local) {
        LocalPlayer p = mc.player;
        if (p == null) return;
        Direction dir = p.getDirection();
        BlockPos base = p.blockPosition();
        BlockPos fire = base.relative(dir, 3).below(base.getY() - groundY(mc, base));
        BlockPos soul = fire.relative(dir.getClockWise());
        BlockPos fire2 = fire.relative(dir.getCounterClockWise());
        BlockPos mob = fire.relative(dir, 2).relative(dir.getCounterClockWise(), 2);
        switch (local) {
            case 0 -> {
                mc.setScreen(null);
                setScale(mc, 2);
                cmd(mc, "setblock %d %d %d minecraft:soul_sand", soul.getX(), soul.getY() - 1, soul.getZ());
                cmd(mc, "setblock %d %d %d minecraft:fire", fire.getX(), fire.getY(), fire.getZ());
                cmd(mc, "setblock %d %d %d minecraft:fire", fire2.getX(), fire2.getY(), fire2.getZ());
                cmd(mc, "setblock %d %d %d minecraft:soul_fire", soul.getX(), soul.getY(), soul.getZ());
                cmd(mc, "difficulty easy");
                cmd(mc, "gamerule doFireTick false");
                cmd(mc, "summon minecraft:zombie %d %d %d {Fire:2000s,NoAI:1b,Silent:1b,PersistenceRequired:1b}", mob.getX(), mob.getY(), mob.getZ());
                // Look at the fire.
                p.setXRot(32f);
                module("low-fire").setEnabled(false);
            }
            // Custom fire texture off then on (a sprite swap since 0.7.2: no resource reload).
            case 15 -> setSetting("low-fire", "custom-fire-texture", "false");
            case 35 -> shot(mc, "smoke-fire-texture-off.png", null);
            case 37 -> setSetting("low-fire", "custom-fire-texture", "true");
            case 52 -> shot(mc, "smoke-fire-vanilla.png", null);
            case 55 -> module("low-fire").setEnabled(true);
            case 100 -> shot(mc, "smoke-fire-low.png", null);
            case 105 -> {
                setSetting("low-fire", "fire-colour", "#7DD3FC");
                setSetting("low-fire", "soul-fire-colour", "#F472B6");
                setSetting("low-fire", "entity-fire-colour", "#86EFAC");
                setSetting("low-fire", "colour", "#C4B5FD");
            }
            case 150 -> shot(mc, "smoke-fire-tinted.png", null);
            case 155 -> {
                openGui(mc);
                openPanel(mc, "low-fire");
            }
            case 157 -> parkCursor(mc);
            case 175 -> shot(mc, "smoke-fire-panel.png", null);
            case 180 -> {
                mc.setScreen(null);
                for (String k : new String[]{"fire-colour", "soul-fire-colour", "entity-fire-colour", "colour"}) setSetting("low-fire", k, "#FFFFFF");
                cmd(mc, "kill @e[type=minecraft:zombie]");
                cmd(mc, "difficulty peaceful");
                for (BlockPos b : new BlockPos[]{fire, fire2, soul}) cmd(mc, "setblock %d %d %d minecraft:air", b.getX(), b.getY(), b.getZ());
                cmd(mc, "setblock %d %d %d minecraft:obsidian", soul.getX(), soul.getY() - 1, soul.getZ());
            }
            default -> {
            }
        }
    }

    /** Y of the first air block above the floor under {@code from} (the player hovers in the smoke world). */
    private static int groundY(Minecraft mc, BlockPos from) {
        BlockPos.MutableBlockPos m = from.mutable();
        for (int i = 0; i < 6 && mc.level.getBlockState(m.below()).isAir(); i++) m.move(Direction.DOWN);
        return m.getY();
    }

    private static final int CROSSHAIR_TICKS = 90;

    /** Crosshair: the preview, the pixel editor, and a styled crosshair in the world. */
    private static void crosshairBlock(Minecraft mc, int local) {
        switch (local) {
            case 0 -> {
                setScale(mc, 2);
                module("crosshair").setEnabled(true);
                openGui(mc);
                openPanel(mc, "crosshair");
            }
            case 2, 27 -> parkCursor(mc);
            case 20 -> shot(mc, "smoke-crosshair-panel.png", null);
            case 25 -> setSetting("crosshair", "style", "CUSTOM");
            case 40 -> shot(mc, "smoke-crosshair-editor.png", null);
            case 45 -> {
                setSetting("crosshair", "style", "CIRCLE");
                setSetting("crosshair", "colour", "#22D3EE");
                mc.setScreen(null);
            }
            case 60 -> shot(mc, "smoke-crosshair-world.png", null);
            case 65 -> {
                setSetting("crosshair", "style", "CROSS");
                setSetting("crosshair", "colour", "#FFFFFF");
                module("crosshair").setEnabled(false);
            }
            default -> {
            }
        }
    }

    private static final int SHIELD_TICKS = 130;

    /** Shield: holding and blocking with the module off and on, and the panel preview. */
    private static void shieldBlock(Minecraft mc, int local) {
        LocalPlayer p = mc.player;
        if (p == null) return;
        Module shield = module("low-shield");
        switch (local) {
            case 0 -> {
                setScale(mc, 2);
                mc.setScreen(null);
                cmd(mc, "item replace entity @s weapon.offhand with minecraft:shield");
                p.getInventory().setSelectedSlot(8);
                p.setXRot(10f);
                shield.setEnabled(false);
            }
            case 20 -> shot(mc, "smoke-shield-vanilla-hold.png", null);
            case 22 -> mc.options.keyUse.setDown(true);
            case 40 -> shot(mc, "smoke-shield-vanilla-block.png", null);
            case 42 -> {
                mc.options.keyUse.setDown(false);
                shield.setEnabled(true);
            }
            case 60 -> shot(mc, "smoke-shield-hold.png", null);
            case 62 -> mc.options.keyUse.setDown(true);
            case 80 -> shot(mc, "smoke-shield-block.png", null);
            case 82 -> {
                mc.options.keyUse.setDown(false);
                openGui(mc);
                openPanel(mc, "low-shield");
            }
            case 84 -> parkCursor(mc);
            case 100 -> shot(mc, "smoke-shield-panel.png", null);
            case 105 -> {
                mc.setScreen(null);
                cmd(mc, "item replace entity @s weapon.offhand with minecraft:air");
                p.getInventory().setSelectedSlot(0);
            }
            default -> {
            }
        }
    }

    private static final int ANCHOR_TICKS = 140;

    /** Anchor Glow on anchors with 0-4 charges, then crystals vanilla and with Crystal Visuals colours. */
    private static void anchorCrystalBlock(Minecraft mc, int local) {
        LocalPlayer p = mc.player;
        if (p == null) return;
        Direction dir = p.getDirection();
        BlockPos base = p.blockPosition();
        int gy = groundY(mc, base);
        BlockPos row = new BlockPos(base.getX(), gy, base.getZ()).relative(dir, 4);
        Direction side = dir.getClockWise();
        switch (local) {
            case 0 -> {
                mc.setScreen(null);
                setScale(mc, 2);
                p.setXRot(28f);
                for (int i = 0; i <= 4; i++) {
                    BlockPos a = row.relative(side, (i - 2) * 2);
                    cmd(mc, "setblock %d %d %d minecraft:respawn_anchor[charges=%d]", a.getX(), a.getY(), a.getZ(), i);
                }
                module("anchor-glow").setEnabled(true);
                setSetting("anchor-glow", "outline-empty-anchors", "true");
            }
            case 30 -> shot(mc, "smoke-anchor-glow.png", null);
            case 32 -> {
                for (int i = 0; i <= 4; i++) {
                    BlockPos a = row.relative(side, (i - 2) * 2);
                    cmd(mc, "setblock %d %d %d minecraft:air", a.getX(), a.getY(), a.getZ());
                }
                for (int i = -1; i <= 1; i++) {
                    BlockPos c = row.relative(side, i * 2);
                    cmd(mc, "summon minecraft:end_crystal %d.5 %d %d.5 {ShowBottom:1b}", c.getX(), c.getY(), c.getZ());
                }
                module("crystal-size").setEnabled(false);
            }
            case 60 -> shot(mc, "smoke-crystals-vanilla.png", null);
            case 62 -> {
                module("crystal-size").setEnabled(true);
                setSetting("crystal-size", "core-colour", "#22D3EE");
                setSetting("crystal-size", "frame-colour", "#F472B6");
                setSetting("crystal-size", "opacity", "70");
                setSetting("crystal-size", "spin-speed", "0");
                setSetting("crystal-size", "bounce", "false");
                setSetting("crystal-size", "show-base", "false");
            }
            case 90 -> shot(mc, "smoke-crystals-styled.png", null);
            case 92 -> {
                openGui(mc);
                openPanel(mc, "crystal-size");
            }
            case 94 -> parkCursor(mc);
            case 110 -> shot(mc, "smoke-crystals-panel.png", null);
            case 115 -> {
                mc.setScreen(null);
                cmd(mc, "kill @e[type=minecraft:end_crystal]");
                module("anchor-glow").setEnabled(false);
                for (String[] kv : new String[][]{{"core-colour", "#FFFFFF"}, {"frame-colour", "#FFFFFF"}, {"opacity", "100"}, {"spin-speed", "100"}, {"bounce", "true"}, {"show-base", "true"}}) {
                    setSetting("crystal-size", kv[0], kv[1]);
                }
            }
            default -> {
            }
        }
    }

    private static final int SCALES_TICKS = 210;
    /** Ticks per slot: move, then read the hover after a frame has surely been drawn. */
    private static final int SLOT_TICKS = 3;
    private static int slotIndex;
    private static int slotMisses;
    private static int slotChecks;

    /** GUI Scales: the inventory at scale 3 while the game is at 2, every slot hovered by the real pointer; a smaller hotbar. */
    private static void scalesBlock(Minecraft mc, int local) {
        LocalPlayer p = mc.player;
        if (p == null) return;
        Module scales = module("gui-scales");
        if (local == 0) {
            setScale(mc, 2);
            scales.setEnabled(true);
            setSetting("gui-scales", "inventory-scale", "GAME");
            mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(p));
        } else if (local == 2) {
            parkCursor(mc);
        } else if (local == 15) {
            shot(mc, "smoke-inventory-game-scale.png", null);
            mc.setScreen(null);
            setSetting("gui-scales", "inventory-scale", "S3");
            mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(p));
            slotIndex = 0;
            slotMisses = 0;
            slotChecks = 0;
        } else if (local >= 20 && local < 20 + 47 * SLOT_TICKS && mc.screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> screen) {
            var acc = (gg.shard.client.mixin.AbstractContainerScreenAccessor) screen;
            var slots = screen.getMenu().slots;
            int step = local - 20;
            if (step % SLOT_TICKS == 0 && slotIndex < slots.size()) {
                // Move the real pointer to the centre of the next slot, in physical pixels.
                var slot = slots.get(slotIndex);
                double f = gg.shard.client.gui.ScaledScreen.factorOf(screen);
                int gs = mc.getWindow().getGuiScale();
                // Mouse events arrive in window coordinates, which equal the framebuffer's only when
                // the window fits the monitor; convert the way vanilla's mouse handler does.
                var win = mc.getWindow();
                double sx = win.getScreenWidth() / (double) win.getWidth();
                double sy = win.getScreenHeight() / (double) win.getHeight();
                if (slotIndex == 0) ShardClient.LOGGER.info("Smoke: window {}x{}, framebuffer {}x{}", win.getScreenWidth(), win.getScreenHeight(), win.getWidth(), win.getHeight());
                double px = gg.shard.client.gui.ScreenScale.toPhysical(acc.shard$leftPos() + slot.x + 8, f, gs) * sx;
                double py = gg.shard.client.gui.ScreenScale.toPhysical(acc.shard$topPos() + slot.y + 8, f, gs) * sy;
                ((gg.shard.client.mixin.MouseHandlerInvoker) mc.mouseHandler).shard$onMove(mc.getWindow().handle(), px, py);
            } else if (step % SLOT_TICKS == SLOT_TICKS - 1 && slotIndex < slots.size()) {
                var hovered = acc.shard$hoveredSlot();
                slotChecks++;
                if (hovered != slots.get(slotIndex)) slotMisses++;
                slotIndex++;
            }
        } else if (local == 20 + 47 * SLOT_TICKS + 1) {
            ShardClient.LOGGER.info("Smoke: inventory at GUI scale 3 over game scale 2: {} slots checked, {} wrong", slotChecks, slotMisses);
            SUMMARY.addProperty("inventoryScaleSlotsChecked", slotChecks);
            SUMMARY.addProperty("inventoryScaleSlotMisses", slotMisses);
            shot(mc, "smoke-inventory-scale3.png", null);
        } else if (local == 20 + 47 * SLOT_TICKS + 4) {
            mc.setScreen(null);
            setSetting("gui-scales", "inventory-scale", "GAME");
            setSetting("gui-scales", "hotbar-scale", "75");
        } else if (local == 20 + 47 * SLOT_TICKS + 20) {
            shot(mc, "smoke-hotbar-75.png", null);
        } else if (local == 20 + 47 * SLOT_TICKS + 22) {
            setSetting("gui-scales", "hotbar-scale", "100");
            // The survival inventory draws the player model; it must sit in its box at scale 3.
            cmd(mc, "gamemode survival");
        } else if (local == 20 + 47 * SLOT_TICKS + 26) {
            setSetting("gui-scales", "inventory-scale", "S3");
            mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(p));
        } else if (local == 20 + 47 * SLOT_TICKS + 28) {
            parkCursor(mc);
        } else if (local == 20 + 47 * SLOT_TICKS + 40) {
            shot(mc, "smoke-inventory-model-scale3.png", null);
        } else if (local == 20 + 47 * SLOT_TICKS + 42) {
            mc.setScreen(null);
            setSetting("gui-scales", "inventory-scale", "GAME");
            cmd(mc, "gamemode creative");
            scales.setEnabled(false);
        }
    }

    private static final int DISPLAY_TICKS = 70;
    private static int[] windowBefore;

    /** Display: borderless on (window = monitor), then off (window restored); the title. */
    private static void displayBlock(Minecraft mc, int local) {
        var display = ShardClient.modules().get(gg.shard.client.modules.utility.DisplayModule.class);
        var w = mc.getWindow();
        switch (local) {
            case 0 -> {
                windowBefore = new int[]{w.getX(), w.getY(), w.getScreenWidth(), w.getScreenHeight()};
                display.setBorderless(true);
            }
            case 20 -> {
                var mode = org.lwjgl.glfw.GLFW.glfwGetVideoMode(org.lwjgl.glfw.GLFW.glfwGetPrimaryMonitor());
                // The window is one pixel taller than the monitor on purpose (see DisplayModule.setBorderless).
                boolean covers = mode != null && w.getX() <= 0 && w.getY() <= 0 && w.getScreenWidth() >= mode.width() && w.getScreenHeight() >= mode.height() && w.getScreenHeight() <= mode.height() + 1;
                ShardClient.LOGGER.info("Smoke: borderless window {}x{} at {},{}; monitor {}x{}; covers monitor: {}; title \"{}\"",
                        w.getScreenWidth(), w.getScreenHeight(), w.getX(), w.getY(), mode == null ? 0 : mode.width(), mode == null ? 0 : mode.height(),
                        covers, display.titleOverride());
                SUMMARY.addProperty("borderlessCoversMonitor", covers);
                shot(mc, "smoke-borderless.png", null);
            }
            case 25 -> display.setBorderless(false);
            case 50 -> {
                boolean restored = w.getScreenWidth() == windowBefore[2] && w.getScreenHeight() == windowBefore[3];
                ShardClient.LOGGER.info("Smoke: after borderless off the window is {}x{} (was {}x{}); restored: {}",
                        w.getScreenWidth(), w.getScreenHeight(), windowBefore[2], windowBefore[3], restored);
                SUMMARY.addProperty("borderlessRestoresWindow", restored);
            }
            default -> {
            }
        }
    }

    private static final int STYLE_TICKS = 70;

    /** HUD styles: the FPS element's style preview, then Pill with the label after the value and brackets. */
    private static void styleBlock(Minecraft mc, int local) {
        switch (local) {
            case 0 -> {
                setScale(mc, 2);
                openGui(mc);
                openPanel(mc, "fps");
            }
            case 2 -> parkCursor(mc);
            case 20 -> shot(mc, "smoke-hudstyle-panel.png", null);
            case 25 -> {
                mc.setScreen(null);
                setSetting("fps", "custom-style", "true");
                setSetting("fps", "style", "PILL");
                setSetting("fps", "label-position", "AFTER");
                setSetting("fps", "brackets", "true");
            }
            case 45 -> shot(mc, "smoke-hudstyle-pill.png", null);
            case 50 -> {
                setSetting("fps", "custom-style", "false");
                setSetting("fps", "label-position", "BEFORE");
                setSetting("fps", "brackets", "false");
            }
            default -> {
            }
        }
    }

    private static final int STEP7_TICKS = 220;
    private static java.util.Map<String, gg.shard.client.hud.HudPresets.Entry> layoutBefore7;
    private static int zombieId = -1;

    /** Step 7: welcome, Crystal PvP Pro, a fight against a zombie (target, combo, reach, recap), cooldowns, compass, TPS, chat. */
    private static void step7Block(Minecraft mc, int local) {
        LocalPlayer p = mc.player;
        if (p == null) return;
        Direction dir = p.getDirection();
        BlockPos base = p.blockPosition();
        BlockPos zpos = new BlockPos(base.getX(), groundY(mc, base), base.getZ()).relative(dir, 2);
        switch (local) {
            case 0 -> {
                setScale(mc, 2);
                layoutBefore7 = gg.shard.client.hud.HudPresets.snapshot(ShardClient.hud());
                var welcome = new gg.shard.client.gui.WelcomeScreen();
                mc.setScreen(welcome);
                welcome.showStep(0, gg.shard.client.gui.QuickSetup.PRO);
            }
            case 2 -> parkCursor(mc);
            case 15 -> shot(mc, "smoke-welcome.png", null);
            case 18 -> {
                mc.setScreen(null);
                gg.shard.client.gui.QuickSetup.apply(gg.shard.client.gui.QuickSetup.PRO);
                for (String k : new String[]{"combo", "reach", "fight-recap", "compass", "speed", "tps", "chat"}) module(k).setEnabled(true);
                cmd(mc, "difficulty easy");
                cmd(mc, "summon minecraft:zombie %d %d %d {NoAI:1b,Silent:1b,PersistenceRequired:1b,Tags:[\"shardsmoke\"],ArmorItems:[{id:\"minecraft:diamond_boots\",count:1},{id:\"minecraft:diamond_leggings\",count:1},{id:\"minecraft:diamond_chestplate\",count:1},{id:\"minecraft:diamond_helmet\",count:1}]}",
                        zpos.getX(), zpos.getY(), zpos.getZ());
                p.setXRot(15f);
                p.getInventory().setSelectedSlot(8);
            }
            case 40, 50, 60 -> {
                var z = mc.level.getEntitiesOfClass(net.minecraft.world.entity.monster.zombie.Zombie.class, p.getBoundingBox().inflate(6)).stream().findFirst().orElse(null);
                if (z != null) {
                    zombieId = z.getId();
                    mc.gameMode.attack(p, z);
                    p.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                }
            }
            case 62 -> {
                //? if >=1.21.2 {
                p.getCooldowns().addCooldown(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ENDER_PEARL), 200);
                //?} else {
                /*// Before 1.21.2 cooldowns are per item, not per stack cooldown group.
                p.getCooldowns().addCooldown(net.minecraft.world.item.Items.ENDER_PEARL, 200);
                *///?}
                mc.gui.getChat().addMessage(net.minecraft.network.chat.Component.literal("Shard smoke: chat line"));
                mc.gui.getChat().addMessage(net.minecraft.network.chat.Component.literal("Shard smoke: repeated line"));
                mc.gui.getChat().addMessage(net.minecraft.network.chat.Component.literal("Shard smoke: repeated line"));
                mc.gui.getChat().addMessage(net.minecraft.network.chat.Component.literal("Shard smoke: repeated line"));
            }
            case 75 -> {
                ShardClient.LOGGER.info("Smoke: fight log combo {} reach {} target {}", gg.shard.client.combat.CombatTracker.LOG.combo(),
                        gg.shard.client.combat.CombatTracker.LOG.lastReach(), gg.shard.client.combat.CombatTracker.LOG.targetId());
                shot(mc, "smoke-step7-fight.png", null);
            }
            case 80 -> cmd(mc, "kill @e[type=minecraft:zombie,tag=shardsmoke]");
            case 100 -> {
                var log = gg.shard.client.combat.CombatTracker.LOG;
                ShardClient.LOGGER.info("Smoke: after the kill: kills {} streak {} recap {}", log.kills(), log.streak(), log.lastRecap());
                SUMMARY.addProperty("fightLogKills", log.kills());
                shot(mc, "smoke-step7-recap.png", null);
            }
            case 105 -> {
                openGui(mc);
                openSettingsPageSmoke(mc);
            }
            case 107 -> parkCursor(mc);
            case 125 -> shot(mc, "smoke-step7-settings.png", null);
            case 130 -> {
                mc.setScreen(null);
                for (String k : new String[]{"combo", "reach", "fight-recap", "compass", "speed", "tps", "chat"}) module(k).setEnabled(false);
                gg.shard.client.hud.HudPresets.restore(ShardClient.hud(), layoutBefore7);
                cmd(mc, "difficulty peaceful");
                p.getInventory().setSelectedSlot(0);
            }
            default -> {
            }
        }
    }

    private static void openSettingsPageSmoke(Minecraft mc) {
        if (mc.screen instanceof ClickGuiScreen gui) gui.openSettingsPage();
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
    private static final int COSMETICS_TICKS = 320;

    /**
     * The cape equipped in Shard Launcher (dev runs: -PequippedPath=<equipped.json>): from behind,
     * from the front, from above, zoomed in, on an elytra, and with the module off for comparison.
     */
    private static void cosmeticsBlock(Minecraft mc, int local) {
        var cosmetics = ShardClient.modules().get(gg.shard.client.modules.visual.CosmeticsModule.class);
        // Five seconds after setup for sign-in, the cape lookup and the catalogue download.
        if (local >= 2) {
            if (local < 102) return;
            local -= 100;
        }
        switch (local) {
            case 0 -> {
                mc.setScreen(null);
                setScale(mc, 2);
                cmd(mc, "item replace entity @s armor.chest with air");
                cmd(mc, "tp @s -5 60 2 180 8");
                mc.options.hideGui = true;
                mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
            }
            case 1 -> {
                spawnOtherPlayer(mc);
                if (mc.player != null) {
                    mc.player.getAbilities().flying = true;
                    mc.player.onUpdateAbilities();
                }
            }
            // Out in the open, above the test arena's roof, so the cape is lit by the sky.
            case 2, 47, 67, 92, 117, 147 -> {
                if (mc.player != null) {
                    mc.player.getAbilities().flying = true;
                    mc.player.onUpdateAbilities();
                }
            }
            case 40 -> {
                SUMMARY.addProperty("capeStatus", cosmetics.status());
                SUMMARY.addProperty("apiSaysOtherWears", String.valueOf(cosmetics.wornSnapshot().get(OTHER_PLAYER)));
                var account = cosmetics.account();
                SUMMARY.addProperty("shardAccount", account == null ? "not signed in" : account.name() + " " + account.tokens() + " tokens");
                //? if >=1.21.9 {
                SUMMARY.addProperty("otherPlayerHasShardCape", otherPlayer != null
                        && otherPlayer.getSkin().cape() != null
                        && otherPlayer.getSkin().cape().texturePath().getNamespace().equals(ShardClient.MOD_ID));
                //?} else {
                /*SUMMARY.addProperty("otherPlayerHasShardCape", otherPlayer != null
                        && otherPlayer.getSkin().capeTexture() != null
                        && otherPlayer.getSkin().capeTexture().getNamespace().equals(ShardClient.MOD_ID));
                *///?}
                ShardClient.LOGGER.info("Smoke: cosmetics {}", cosmetics.status());
                shot(mc, "smoke-cape-back.png", null);
            }
            case 45 -> cmd(mc, "tp @s -5 60 2 180 45");
            case 60 -> shot(mc, "smoke-cape-above.png", null);
            case 65 -> {
                cmd(mc, "tp @s -5 60 2 180 8");
                mc.options.fov().set(30);
            }
            case 85 -> shot(mc, "smoke-cape-close.png", null);
            case 90 -> {
                mc.options.fov().set(70);
                mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);
                cmd(mc, "tp @s -5 60 2 150 8");
            }
            case 110 -> shot(mc, "smoke-cape-front.png", null);
            case 115 -> {
                mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
                cmd(mc, "tp @s -5 60 2 180 8");
                cmd(mc, "item replace entity @s armor.chest with elytra");
            }
            case 140 -> shot(mc, "smoke-cape-elytra.png", null);
            case 145 -> {
                cmd(mc, "item replace entity @s armor.chest with air");
                cosmetics.setEnabled(false);
            }
            case 165 -> shot(mc, "smoke-cape-off.png", null);
            case 170 -> {
                cosmetics.setEnabled(true);
                setSetting("cosmetics", "show-other-players'-cosmetics", "false");
            }
            case 180 -> shot(mc, "smoke-cape-others-off.png", null);
            case 185 -> setSetting("cosmetics", "show-other-players'-cosmetics", "true");
            case 190 -> {
                if (otherPlayer != null) {
                    otherPlayer.discard();
                    otherPlayer = null;
                }
                cmd(mc, "tp @s -5 -22 2 180 30");
                mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
                mc.options.hideGui = false;
                mc.options.fov().set(70);
            }
            default -> {
            }
        }
    }

    /** A UUID that is in the dev cape list (run/dev-meta/player-cosmetics.json) but is not ShardSmoke. */
    private static final java.util.UUID OTHER_PLAYER = java.util.UUID.fromString("5f3c1a2e-0000-4000-8000-0000000000aa");
    private static net.minecraft.client.player.RemotePlayer otherPlayer;

    /**
     * A second player that exists only on this client, standing three blocks ahead with its back to
     * the camera, so the shots show what another Shard player sees on someone in the cape list.
     */
    private static void spawnOtherPlayer(Minecraft mc) {
        if (mc.level == null || mc.player == null) return;
        var player = new net.minecraft.client.player.RemotePlayer(mc.level, new com.mojang.authlib.GameProfile(OTHER_PLAYER, "ShardFriend"));
        player.setPos(-3.5, 60, -1.0);
        player.setYRot(180);
        player.setYHeadRot(180);
        player.yBodyRot = 180;
        player.yBodyRotO = 180;
        try {
            // Real clients send their skin customisation; this one has none, so turn every part on (dev mappings).
            var field = net.minecraft.world.entity.Avatar.class.getDeclaredField("DATA_PLAYER_MODE_CUSTOMISATION");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            var accessor = (net.minecraft.network.syncher.EntityDataAccessor<Byte>) field.get(null);
            player.getEntityData().set(accessor, (byte) 0x7F);
        } catch (ReflectiveOperationException e) {
            ShardClient.LOGGER.error("Smoke: could not show the other player's cape layer", e);
        }
        // Client-side stand-in: 26.2 no longer numbers entities on construction.
        player.setId(-3_000_001);
        mc.level.addEntity(player);
        otherPlayer = player;
    }

    static void parkCursor(Minecraft mc) {
        org.lwjgl.glfw.GLFW.glfwSetCursorPos(mc.getWindow().handle(), 4, 4);
        // The unfocused dev window gets no move event for that, so tell the mouse handler directly;
        // otherwise the pointer stays where opening a screen centred it and hovers leak into shots.
        ((gg.shard.client.mixin.MouseHandlerInvoker) mc.mouseHandler).shard$onMove(mc.getWindow().handle(), 4, 4);
    }

    // ---- GUI steps -----------------------------------------------------------------------------

    private static void setScale(Minecraft mc, int scale) {
        mc.options.guiScale().set(scale);
        mc.resizeDisplay();
        ShardClient.LOGGER.info("Smoke: GUI scale {} -> effective {} ({}x{})", scale == 0 ? "auto" : scale, mc.getWindow().getGuiScale(),
                mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
    }


    private static final int FEATURES_TICKS = 545;
    private static final String[] FEATURE_MODULES = {"sky", "hitboxes", "small-items", "low-shield", "hit-color", "entity-optimizer", "zoom"};
    private static final java.util.Map<String, Boolean> featuresEnabledBefore = new java.util.HashMap<>();

    /** A feature screenshot without the command feedback lines in chat. */
    private static void fshot(Minecraft mc, String name, String zoomName) {
        mc.gui.getChat().clearMessages(false);
        shot(mc, name, zoomName);
    }

    private static void fly(LocalPlayer p) {
        p.getAbilities().flying = true;
        p.onUpdateAbilities();
    }

    private static net.minecraft.world.entity.monster.zombie.Zombie nearestZombie(Minecraft mc, LocalPlayer p) {
        return mc.level.getEntitiesOfClass(net.minecraft.world.entity.monster.zombie.Zombie.class, p.getBoundingBox().inflate(8)).stream()
                .min(java.util.Comparator.comparingDouble(p::distanceToSqr)).orElse(null);
    }

    /**
     * Visual feature pass (-PsmokeOnly=features): Sky presets, styled hitboxes, Small Items, the
     * Shield tint and opacity, Hit Color on armour, Entity Optimizer with 200 XP orbs, and Zoom.
     */
    private static void featuresBlock(Minecraft mc, int local) {
        LocalPlayer p = mc.player;
        if (p == null) return;
        switch (local) {
            case 0 -> {
                mc.setScreen(null);
                setScale(mc, 2);
                for (String k : FEATURE_MODULES) featuresEnabledBefore.put(k, module(k).isEnabled());
                cmd(mc, "difficulty easy");
                cmd(mc, "time set noon");
                cmd(mc, "weather clear");
                cmd(mc, "kill @e[type=minecraft:item]");
                cmd(mc, "tp @s -5 -22 2 180 12");
            }
            case 95 -> {
                // Clear the earlier passes' obsidian in front of the player and any dropped items.
                int gy = groundY(mc, p.blockPosition());
                cmd(mc, "fill -9 %d -14 -1 %d 1 minecraft:air", gy, gy + 3);
                cmd(mc, "kill @e[type=minecraft:item]");
            }
            // ---- Hitboxes on a NoAI zombie three blocks ahead.
            case 100 -> {
                int gy = groundY(mc, p.blockPosition());
                cmd(mc, "summon minecraft:zombie -3.2 %d -2.5 {NoAI:1b,Silent:1b,PersistenceRequired:1b,Rotation:[60f,0f],Tags:[\"shardfeat\"]}", gy);
                module("hitboxes").setEnabled(true);
                setSetting("hitboxes", "fill", "false");
                setSetting("hitboxes", "look-direction", "false");
            }
            case 101 -> p.setXRot(12f);
            case 125 -> fshot(mc, "features-hitbox-default.png", null);
            case 127 -> {
                setSetting("hitboxes", "fill", "true");
                setSetting("hitboxes", "fill-opacity", "30");
            }
            case 140 -> fshot(mc, "features-hitbox-fill.png", null);
            case 142 -> {
                setSetting("hitboxes", "fill", "false");
                setSetting("hitboxes", "fill-opacity", "12");
                setSetting("hitboxes", "look-direction", "true");
            }
            case 155 -> fshot(mc, "features-hitbox-look.png", null);
            case 157 -> {
                setSetting("hitboxes", "look-direction", "false");
                module("hitboxes").setEnabled(featuresEnabledBefore.get("hitboxes"));
                cmd(mc, "tp @e[type=minecraft:zombie,tag=shardfeat] -5 -200 2");
            }
            // ---- Small Items: a sword and a totem in vanilla, in the Small Items mod look (60%) and Custom at 50%.
            case 165 -> {
                p.getInventory().setSelectedSlot(0);
                cmd(mc, "item replace entity @s hotbar.0 with minecraft:diamond_sword");
                cmd(mc, "item replace entity @s weapon.offhand with minecraft:totem_of_undying");
                p.setXRot(10f);
                module("small-items").setEnabled(false);
            }
            case 182 -> fshot(mc, "features-smallitems-vanilla.png", null);
            case 184 -> {
                module("small-items").setEnabled(true);
                setSetting("small-items", "look", "small_items");
            }
            case 192 -> fshot(mc, "features-smallitems-mod.png", null);
            case 194 -> {
                setSetting("small-items", "look", "custom");
                setSetting("small-items", "main-hand-size", "50");
                setSetting("small-items", "offhand-size", "50");
            }
            case 200 -> fshot(mc, "features-smallitems-custom50.png", null);
            // ---- Shield: vanilla, then 50% opacity with the Ice tint, holding and blocking.
            case 202 -> {
                setSetting("small-items", "main-hand-size", "75");
                setSetting("small-items", "offhand-size", "75");
                setSetting("small-items", "look", "small_items");
                module("small-items").setEnabled(featuresEnabledBefore.get("small-items"));
                cmd(mc, "item replace entity @s hotbar.0 with minecraft:air");
                cmd(mc, "item replace entity @s weapon.offhand with minecraft:shield");
                module("low-shield").setEnabled(false);
            }
            case 220 -> fshot(mc, "features-shield-vanilla.png", null);
            case 222 -> {
                module("low-shield").setEnabled(true);
                setSetting("low-shield", "opacity", "50");
                setSetting("low-shield", "tint-preset", "ICE");
                // Vanilla size and place, so only the opacity and tint differ from the vanilla shot.
                setSetting("low-shield", "holding-width", "100");
                setSetting("low-shield", "holding-size", "100");
                setSetting("low-shield", "holding-lower", "0");
            }
            case 237 -> fshot(mc, "features-shield-ice50.png", null);
            case 239 -> mc.options.keyUse.setDown(true);
            case 252 -> fshot(mc, "features-shield-ice50-block.png", null);
            case 254 -> {
                mc.options.keyUse.setDown(false);
                setSetting("low-shield", "opacity", "100");
                setSetting("low-shield", "tint-preset", "CUSTOM");
                setSetting("low-shield", "tint", "#FFFFFF");
                setSetting("low-shield", "holding-width", "65");
                setSetting("low-shield", "holding-size", "75");
                setSetting("low-shield", "holding-lower", "0.15");
                module("low-shield").setEnabled(featuresEnabledBefore.get("low-shield"));
                cmd(mc, "item replace entity @s weapon.offhand with minecraft:air");
                p.getInventory().setSelectedSlot(8);
                int gy = groundY(mc, p.blockPosition());
                //? if >=1.21.5 {
                cmd(mc, "summon minecraft:zombie -4.5 %d -1.5 {NoAI:1b,Silent:1b,PersistenceRequired:1b,Rotation:[0f,0f],Tags:[\"shardfeat\"],"
                        + "equipment:{head:{id:\"minecraft:iron_helmet\",count:1},chest:{id:\"minecraft:iron_chestplate\",count:1},"
                        + "feet:{id:\"minecraft:iron_boots\",count:1}}}", gy);
                //?} else {
                /*// Before 1.21.5 mob equipment is ArmorItems (feet, legs, chest, head).
                cmd(mc, "summon minecraft:zombie -4.5 %d -1.5 {NoAI:1b,Silent:1b,PersistenceRequired:1b,Rotation:[0f,0f],Tags:[\"shardfeat\"],"
                        + "ArmorItems:[{id:\"minecraft:iron_boots\",count:1},{},{id:\"minecraft:iron_chestplate\",count:1},{id:\"minecraft:iron_helmet\",count:1}]}", gy);
                *///?}
                module("hit-color").setEnabled(true);
                setSetting("hit-color", "tint-armor", "true");
            }
            // ---- Hit Color: an armoured zombie just after a hit, armour tint on and off.
            case 255 -> p.setXRot(8f);
            case 272 -> fshot(mc, "features-hitcolor-before.png", null);
            case 275, 300 -> {
                var z = nearestZombie(mc, p);
                if (z != null) {
                    mc.gameMode.attack(p, z);
                    p.swing(InteractionHand.MAIN_HAND);
                } else {
                    ShardClient.LOGGER.error("Smoke: no zombie to hit for Hit Color");
                }
            }
            case 278 -> {
                var z = nearestZombie(mc, p);
                ShardClient.LOGGER.info("Smoke: hit colour (armour tint on): zombie hurtTime {} health {}", z == null ? -1 : z.hurtTime, z == null ? -1 : z.getHealth());
                fshot(mc, "features-hitcolor-armor-on.png", null);
            }
            case 285 -> setSetting("hit-color", "tint-armor", "false");
            case 303 -> {
                var z = nearestZombie(mc, p);
                ShardClient.LOGGER.info("Smoke: hit colour (armour tint off): zombie hurtTime {} health {}", z == null ? -1 : z.hurtTime, z == null ? -1 : z.getHealth());
                fshot(mc, "features-hitcolor-armor-off.png", null);
            }
            case 310 -> {
                setSetting("hit-color", "tint-armor", "true");
                module("hit-color").setEnabled(featuresEnabledBefore.get("hit-color"));
                cmd(mc, "tp @e[type=minecraft:zombie,tag=shardfeat] -5 -200 2");
                // Spectators do not pull orbs in, so they stay where they were summoned.
                cmd(mc, "gamemode spectator");
                module("entity-optimizer").setEnabled(false);
            }
            // ---- Entity Optimizer: 200 XP orbs (distinct values so vanilla does not merge them) on a 3x3 patch.
            case 312, 313, 314, 315, 316, 317, 318, 319, 320, 321 -> {
                int gy = groundY(mc, p.blockPosition());
                for (int i = 0; i < 20; i++) {
                    int n = (local - 312) * 20 + i;
                    double x = -6 + (n % 3) + 0.25 + 0.5 * ((n / 3) % 2);
                    double z = -2 - ((n / 6) % 3) + 0.25 + 0.5 * ((n / 18) % 2);
                    cmd(mc, "summon minecraft:experience_orb %.2f %d %.2f {Value:%ds}", x, gy, z, n + 1);
                }
            }
            case 322 -> p.setXRot(30f);
            case 345 -> {
                int orbs = 0;
                for (Entity e : mc.level.entitiesForRendering()) if (e instanceof net.minecraft.world.entity.ExperienceOrb) orbs++;
                ShardClient.LOGGER.info("Smoke: {} experience orbs in the client world", orbs);
                SUMMARY.addProperty("featuresOrbs", orbs);
                fshot(mc, "features-orbs-off.png", null);
            }
            case 347 -> {
                module("entity-optimizer").setEnabled(true);
                setSetting("entity-optimizer", "orbs-per-block", "2");
            }
            case 370 -> fshot(mc, "features-orbs-on.png", null);
            case 372 -> {
                module("entity-optimizer").setEnabled(featuresEnabledBefore.get("entity-optimizer"));
                cmd(mc, "kill @e[type=minecraft:experience_orb]");
                cmd(mc, "gamemode creative");
            }
            // ---- Zoom: the same view unzoomed and held.
            case 380 -> {
                p.setXRot(2f);
                int gy = groundY(mc, p.blockPosition());
                //? if >=1.21.5 {
                cmd(mc, "summon minecraft:zombie -4.5 %d -14.5 {NoAI:1b,Silent:1b,PersistenceRequired:1b,Tags:[\"shardfeat\"],"
                        + "equipment:{head:{id:\"minecraft:diamond_helmet\",count:1},chest:{id:\"minecraft:diamond_chestplate\",count:1}}}", gy);
                //?} else {
                /*cmd(mc, "summon minecraft:zombie -4.5 %d -14.5 {NoAI:1b,Silent:1b,PersistenceRequired:1b,Tags:[\"shardfeat\"],"
                        + "ArmorItems:[{},{},{id:\"minecraft:diamond_chestplate\",count:1},{id:\"minecraft:diamond_helmet\",count:1}]}", gy);
                *///?}
                module("zoom").setEnabled(true);
            }
            case 400 -> fshot(mc, "features-zoom-off.png", null);
            case 402 -> ShardClient.modules().get(gg.shard.client.modules.visual.ZoomModule.class).forceHeldForSmoke(true);
            case 430 -> fshot(mc, "features-zoom-on.png", null);
            case 432 -> {
                ShardClient.modules().get(gg.shard.client.modules.visual.ZoomModule.class).forceHeldForSmoke(false);
                module("zoom").setEnabled(featuresEnabledBefore.get("zoom"));
                cmd(mc, "tp @e[type=minecraft:zombie,tag=shardfeat] -5 -200 2");
                cmd(mc, "difficulty peaceful");
                p.getInventory().setSelectedSlot(0);
            }
            // ---- Sky, last so the rain has faded: out above the arena roof, looking up past the horizon.
            case 440 -> {
                cmd(mc, "tp @s -5 60 2 180 -25");
                module("sky").setEnabled(false);
            }
            case 441, 450, 470, 490, 510 -> fly(p);
            case 480 -> fshot(mc, "features-sky-off.png", null);
            case 482 -> {
                module("sky").setEnabled(true);
                setSetting("sky", "preset", "SUNSET");
            }
            case 495 -> fshot(mc, "features-sky-sunset.png", null);
            case 497 -> setSetting("sky", "preset", "NIGHT");
            case 512 -> fshot(mc, "features-sky-night.png", null);
            case 514 -> setSetting("sky", "preset", "PASTEL");
            case 529 -> fshot(mc, "features-sky-pastel.png", null);
            case 531 -> {
                module("sky").setEnabled(featuresEnabledBefore.get("sky"));
                cmd(mc, "tp @s -5 -22 2 180 30");
            }
            default -> {
            }
        }
    }

    private static final java.util.List<String> AUDIT_DEAD = new java.util.ArrayList<>();
    private static java.util.List<Module> auditModules;

    /** Clicks every switch and segment of every mod's settings, then the Settings and Cosmetics tabs (-PsmokeOnly=audit). */
    private static boolean auditBlock(Minecraft mc, int t) {
        if (auditModules == null) {
            auditModules = new java.util.ArrayList<>();
            for (Module m : ShardClient.modules().all()) if (!m.hidden()) auditModules.add(m);
        }
        int step = t / 4;
        int local = t % 4;
        int total = auditModules.size() + 2;
        if (step >= total) {
            ShardClient.LOGGER.info("Smoke: click audit found {} dead controls: {}", AUDIT_DEAD.size(), AUDIT_DEAD);
            com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
            AUDIT_DEAD.forEach(arr::add);
            SUMMARY.add("deadControls", arr);
            return false;
        }
        if (local == 0) {
            if (!(mc.screen instanceof ClickGuiScreen)) openGui(mc);
            ClickGuiScreen gui = gui(mc);
            if (step < auditModules.size()) {
                gui.selectTab(ClickGuiScreen.Tab.MODS);
                gui.openModule(auditModules.get(step));
            } else gui.selectTab(step == auditModules.size() ? ClickGuiScreen.Tab.SETTINGS : ClickGuiScreen.Tab.COSMETICS);
        } else if (local == 3) {
            AUDIT_DEAD.addAll(gui(mc).clickAudit());
        }
        return true;
    }

    private static final int MENU07_TICKS = 150;

    /** The 0.7.0 mod menu: tabs, tiles, hover, settings view, search and an eased scroll (-PsmokeOnly=menu). */
    private static void menu07Block(Minecraft mc, int t) {
        ClickGuiScreen gui = mc.screen instanceof ClickGuiScreen g ? g : null;
        switch (t) {
            case 0 -> {
                openGui(mc);
                gui(mc).selectTab(ClickGuiScreen.Tab.MODS);
                gui(mc).showAll();
            }
            case 2, 24, 49, 64, 79, 94, 109 -> parkCursor(mc);
            case 20 -> shot(mc, "menu-all.png", null, 3);
            case 22 -> gui.showCategory(gg.shard.client.module.ModuleCategory.HUD);
            case 35 -> shot(mc, "menu-hud.png", null, 0);
            case 37 -> moveCursor(mc, gui, 1);
            case 45 -> shot(mc, "menu-hover.png", null, 0);
            case 47 -> gui.openModule(module("fps"));
            case 60 -> shot(mc, "menu-settings-view.png", null, 3);
            case 62 -> gui.selectTab(ClickGuiScreen.Tab.SETTINGS);
            case 75 -> shot(mc, "menu-settings-tab.png", null, 0);
            case 77 -> gui.selectTab(ClickGuiScreen.Tab.COSMETICS);
            case 90 -> shot(mc, "menu-cosmetics-tab.png", null, 0);
            case 92 -> gui.selectTab(ClickGuiScreen.Tab.PROFILES);
            case 105 -> shot(mc, "menu-profiles-tab.png", null, 0);
            case 107 -> {
                gui.selectTab(ClickGuiScreen.Tab.MODS);
                gui.setSearch("fire");
            }
            case 120 -> shot(mc, "menu-search.png", null, 0);
            case 122 -> {
                gui.setSearch("");
                gui.showAll();
            }
            case 125 -> {
                double s = mc.getWindow().getGuiScaledWidth() / (double) mc.getWindow().getWidth();
                mc.screen.mouseScrolled(mc.getWindow().getWidth() / 2.0 * s, mc.getWindow().getHeight() / 2.0 * s, 0, -2);
            }
            case 126 -> shot(mc, "menu-scroll-mid.png", null, 0);
            case 140 -> shot(mc, "menu-scroll-end.png", null, 0);
            default -> {
            }
        }
    }

    /** Puts the pointer on the n-th tile of the first row. */
    private static void moveCursor(Minecraft mc, ClickGuiScreen gui, int tile) {
        int[] c = gui.tileCentre(tile);
        double px = c[0] * gui.pixelsPerUnitNow();
        double py = c[1] * gui.pixelsPerUnitNow();
        org.lwjgl.glfw.GLFW.glfwSetCursorPos(mc.getWindow().handle(), px, py);
        ((gg.shard.client.mixin.MouseHandlerInvoker) mc.mouseHandler).shard$onMove(mc.getWindow().handle(), px, py);
    }

    /** A screenshot plus, when {@code zoom} is above 0, a zoomed crop of the menu's top-left quarter. */
    private static void shot(Minecraft mc, String name, String unused, int zoom) {
        quietHud(mc);
        Path file = out.resolve(name);
        Screenshot.takeScreenshot(mc.getMainRenderTarget(), image -> {
            try (image) {
                image.writeToFile(file);
                ShardClient.LOGGER.info("Smoke: wrote {}", file);
                if (zoom > 0) {
                    int cw = Math.min(image.getWidth(), image.getWidth() / 3);
                    int ch = Math.min(image.getHeight(), image.getHeight() / 3);
                    int x0 = (image.getWidth() - Math.min(image.getWidth(), 960 * image.getHeight() / 720)) / 2;
                    int y0 = image.getHeight() / 10;
                    x0 = Math.max(0, Math.min(image.getWidth() - cw, x0));
                    try (NativeImage z = new NativeImage(cw * zoom, ch * zoom, false)) {
                        for (int zy = 0; zy < ch * zoom; zy++) {
                            for (int zx = 0; zx < cw * zoom; zx++) z.setPixel(zx, zy, image.getPixel(x0 + zx / zoom, y0 + zy / zoom) | 0xFF000000);
                        }
                        z.writeToFile(out.resolve(name.replace(".png", "-zoom.png")));
                    }
                }
            } catch (IOException e) {
                ShardClient.LOGGER.error("Smoke: could not write {}", file, e);
            }
        });
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

    static void finish(Minecraft mc) {
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
        var clipped = new com.google.gson.JsonArray();
        gg.shard.client.gui.Fonts.CLIPPED.stream().sorted().forEach(clipped::add);
        SUMMARY.add("clippedTexts", clipped);
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
