package gg.shard.client.dev;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
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
 * world, then screenshot the HUD, the settings grid, an open settings panel and the colour
 * picker at GUI scale 3 and 2, the HUD editor, and finally place and hit an end crystal in
 * creative to exercise the Crystal Optimizer. {@code shard.smoke.bench} runs {@link Benchmark}
 * instead. Never active in a distributed jar (Loom only sets the properties for runClient).
 */
public final class SmokeTest {
    private SmokeTest() {}

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
        switch (ticksInWorld) {
            case 5 -> quietHud(mc);
            case 10 -> setScale(mc, 3);
            case 40 -> shot(mc, "smoke-hud.png");
            case 60 -> openGui(mc);
            case 90 -> shot(mc, "smoke-gui-scale3.png");
            case 100 -> openPanel(mc, "crystal-optimizer");
            case 130 -> shot(mc, "smoke-panel-scale3.png");
            case 140 -> openPanel(mc, "keystrokes");
            case 150 -> openColor(mc, "keystrokes", "pressed");
            case 180 -> shot(mc, "smoke-color-scale3.png");
            case 190 -> {
                mc.setScreen(null);
                setScale(mc, 2);
            }
            case 210 -> openGui(mc);
            case 240 -> shot(mc, "smoke-gui-scale2.png");
            case 250 -> openPanel(mc, "totem-pop-tweaks");
            case 280 -> shot(mc, "smoke-panel-scale2.png");
            case 290 -> {
                mc.setScreen(null);
                setScale(mc, 3);
            }
            case 310 -> mc.setScreen(new HudEditorScreen(null, ShardClient.hud()));
            case 330 -> shot(mc, "smoke-editor.png");
            case 340 -> {
                mc.setScreen(null);
                prepareCrystalTest(mc);
            }
            case 350 -> placeObsidian(mc);
            case 360 -> placeCrystal(mc);
            case 363 -> logPlacement(mc);
            case 380 -> shot(mc, "smoke-crystal.png");
            case 390 -> attackCrystal(mc);
            case 405 -> {
                logRemoval(mc);
                finish(mc);
            }
            default -> {
            }
        }
    }

    // ---- GUI steps ---------------------------------------------------------------------------

    private static void setScale(Minecraft mc, int scale) {
        mc.options.guiScale().set(scale);
        mc.resizeDisplay();
        ShardClient.LOGGER.info("Smoke: GUI scale {} ({}x{})", scale, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
    }

    private static void openGui(Minecraft mc) {
        mc.setScreen(new ClickGuiScreen(null));
    }

    private static void openPanel(Minecraft mc, String moduleKey) {
        if (!(mc.screen instanceof ClickGuiScreen gui)) {
            ShardClient.LOGGER.error("Smoke: settings screen is not open");
            return;
        }
        Module m = ShardClient.modules().find(moduleKey);
        if (m == null) {
            ShardClient.LOGGER.error("Smoke: no module {}", moduleKey);
            return;
        }
        gui.openModule(m);
    }

    private static void openColor(Minecraft mc, String moduleKey, String settingKey) {
        if (!(mc.screen instanceof ClickGuiScreen gui)) return;
        Module m = ShardClient.modules().find(moduleKey);
        Setting<?> s = m == null ? null : m.setting(settingKey);
        if (s instanceof ColorSetting c) gui.openColorPicker(c);
        else ShardClient.LOGGER.error("Smoke: {}.{} is not a colour setting", moduleKey, settingKey);
    }

    // ---- crystal exercise --------------------------------------------------------------------

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
        ShardClient.LOGGER.info("Smoke: crystal test at {} (facing {}), game mode {}", ground, dir, mc.gameMode.getPlayerMode());
    }

    private static void placeObsidian(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null || ground == null) return;
        player.getInventory().setSelectedSlot(0);
        ShardClient.LOGGER.info("Smoke: hotbar 0 = {}, hotbar 1 = {}", player.getInventory().getItem(0), player.getInventory().getItem(1));
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
        try {
            Files.writeString(out.resolve("smoke-summary.json"), new GsonBuilder().setPrettyPrinting().create().toJson(SUMMARY), StandardCharsets.UTF_8);
        } catch (IOException e) {
            ShardClient.LOGGER.error("Smoke: could not write summary", e);
        }
        ShardClient.LOGGER.info("Smoke test complete; stopping client");
        mc.stop();
    }

    static void shot(Minecraft mc, String name) {
        quietHud(mc);
        Path file = out.resolve(name);
        Screenshot.takeScreenshot(mc.getMainRenderTarget(), image -> {
            try (image) {
                image.writeToFile(file);
                ShardClient.LOGGER.info("Smoke: wrote {}", file);
            } catch (IOException e) {
                ShardClient.LOGGER.error("Smoke: could not write {}", file, e);
            }
        });
    }
}
