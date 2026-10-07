package gg.shard.client.dev;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import gg.shard.client.ShardClient;
import gg.shard.client.modules.perf.ExplosionOptimizerModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;


import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * The BENCHMARKS.md scenario, scripted: 100 end crystals on a 10x10 obsidian grid, detonated in
 * one hit, frame times sampled for the following 3 seconds, five runs with the Explosion
 * Optimizer on and five with it off, alternating. Needs the smoke player to be an operator on
 * the offline test server (commands: attribute, fill, tp, kill). Results go to bench.json.
 */
final class Benchmark {
    private Benchmark() {}

    private static final int RUNS_PER_CONFIG = 6;
    /** The first run of each session is a cold warm-up (chunk meshes, shaders) and is not recorded. */
    private static final int WARMUP_RUNS = 1;
    /** Crystals per side; they sit two blocks apart because a crystal's box is two blocks wide. */
    private static final int GRID = 10;
    private static final int SPACING = 2;
    private static final int FLOOR = GRID * SPACING;
    private static final int SAMPLE_TICKS = 60;
    private static final int PLACE_PER_TICK = 10;

    private enum Phase { SETUP, BUILD, PLACE, SETTLE, DETONATE, SAMPLE, CLEANUP, DONE }

    private static Phase phase = Phase.SETUP;
    private static int phaseTick;
    private static int run;
    private static boolean optimizerOn = true;
    private static BlockPos center;
    private static int placed;
    private static int crystalsSeen;
    private static final JsonArray RUNS = new JsonArray();

    static void tick(Minecraft mc, int ticksInWorld) {
        LocalPlayer player = mc.player;
        if (player == null || mc.gameMode == null) return;
        phaseTick++;
        switch (phase) {
            case SETUP -> {
                if (phaseTick == 20) {
                    center = player.blockPosition();
                    // Measure real frame times: no vsync, no idle limiter, every particle (vanilla 120 fps cap stays).
                    mc.options.enableVsync().set(false);
                    mc.options.inactivityFpsLimit().set(net.minecraft.client.InactivityFpsLimit.MINIMIZED);
                    mc.options.particles().set(net.minecraft.server.level.ParticleStatus.ALL);
                    // The Gradle-launched window starts behind everything; a minimised or inactive
                    // window makes vanilla throttle to 10 fps, which would swamp the measurement.
                    long handle = mc.getWindow().handle();
                    org.lwjgl.glfw.GLFW.glfwRestoreWindow(handle);
                    org.lwjgl.glfw.GLFW.glfwShowWindow(handle);
                    org.lwjgl.glfw.GLFW.glfwFocusWindow(handle);
                    ShardClient.LOGGER.info("Bench: window minimized={} iconified={} focused={} visible={} fpsLimit={} vsync={} inactivity={}",
                            mc.getWindow().isMinimized(),
                            org.lwjgl.glfw.GLFW.glfwGetWindowAttrib(handle, org.lwjgl.glfw.GLFW.GLFW_ICONIFIED),
                            org.lwjgl.glfw.GLFW.glfwGetWindowAttrib(handle, org.lwjgl.glfw.GLFW.GLFW_FOCUSED),
                            org.lwjgl.glfw.GLFW.glfwGetWindowAttrib(handle, org.lwjgl.glfw.GLFW.GLFW_VISIBLE),
                            mc.options.framerateLimit().get(), mc.options.enableVsync().get(), mc.options.inactivityFpsLimit().get());
                    send(player, "attribute @s minecraft:block_interaction_range base set 64");
                    send(player, "attribute @s minecraft:entity_interaction_range base set 64");
                    send(player, "gamemode creative");
                    SmokeTest.giveKit(player);
                    player.getInventory().setSelectedSlot(1);
                    ShardClient.LOGGER.info("Bench: centre {}", center);
                }
                if (phaseTick == 40) {
                    send(player, String.format(Locale.ROOT, "tp @s %d %d %d", center.getX(), center.getY() + 6, center.getZ()));
                    player.getAbilities().flying = true;
                    player.onUpdateAbilities();
                }
                if (phaseTick == 60) next(Phase.BUILD);
            }
            case BUILD -> {
                if (phaseTick == 1) {
                    int y = center.getY() - 1;
                    int x0 = center.getX() - FLOOR / 2;
                    int z0 = center.getZ() - FLOOR / 2;
                    send(player, String.format(Locale.ROOT, "fill %d %d %d %d %d %d minecraft:obsidian", x0, y, z0, x0 + FLOOR - 1, y, z0 + FLOOR - 1));
                    send(player, String.format(Locale.ROOT, "fill %d %d %d %d %d %d minecraft:air", x0, y + 1, z0, x0 + FLOOR - 1, y + 3, z0 + FLOOR - 1));
                    send(player, "kill @e[type=minecraft:end_crystal]");
                    player.getAbilities().flying = true;
                    player.onUpdateAbilities();
                    ShardClient.modules().get(ExplosionOptimizerModule.class).setEnabled(optimizerOn);
                }
                if (phaseTick == 30) {
                    placed = 0;
                    player.getInventory().setSelectedSlot(1);
                    ShardClient.LOGGER.info("Bench: placing with {}", player.getInventory().getSelectedItem());
                    next(Phase.PLACE);
                }
            }
            case PLACE -> {
                int y = center.getY() - 1;
                player.getInventory().setSelectedSlot(1);
                for (int i = 0; i < PLACE_PER_TICK && placed < GRID * GRID; i++, placed++) {
                    int gx = placed % GRID;
                    int gz = placed / GRID;
                    BlockPos obsidian = new BlockPos(center.getX() - FLOOR / 2 + gx * SPACING, y, center.getZ() - FLOOR / 2 + gz * SPACING);
                    BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(obsidian).add(0, 0.5, 0), Direction.UP, obsidian, false);
                    mc.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hit);
                }
                if (placed >= GRID * GRID) next(Phase.SETTLE);
            }
            case SETTLE -> {
                if (phaseTick == 30) {
                    crystalsSeen = SmokeTest.crystals(mc).size();
                    ShardClient.LOGGER.info("Bench: run {} ({}) has {} crystals", run + 1 - WARMUP_RUNS, optimizerOn ? "optimizer on" : "optimizer off", crystalsSeen);
                    next(Phase.DETONATE);
                }
            }
            case DETONATE -> {
                EndCrystal target = null;
                double best = Double.MAX_VALUE;
                BlockPos corner = new BlockPos(center.getX() - FLOOR / 2, center.getY(), center.getZ() - FLOOR / 2);
                for (EndCrystal c : SmokeTest.crystals(mc)) {
                    double d = c.distanceToSqr(Vec3.atCenterOf(corner));
                    if (d < best) {
                        best = d;
                        target = c;
                    }
                }
                SmokeTest.startSampling();
                if (target != null) {
                    mc.gameMode.attack(player, target);
                    player.swing(InteractionHand.MAIN_HAND);
                } else {
                    ShardClient.LOGGER.error("Bench: nothing to detonate");
                }
                next(Phase.SAMPLE);
            }
            case SAMPLE -> {
                if (phaseTick >= SAMPLE_TICKS) {
                    List<Long> frames = SmokeTest.stopSampling();
                    if (run < WARMUP_RUNS) ShardClient.LOGGER.info("Bench: warm-up run done ({} frames)", frames.size());
                    else record(frames);
                    next(Phase.CLEANUP);
                }
            }
            case CLEANUP -> {
                if (phaseTick == 1) send(player, "kill @e[type=minecraft:end_crystal]");
                if (phaseTick == 40) {
                    run++;
                    if (run >= WARMUP_RUNS) optimizerOn = !optimizerOn;
                    if (run >= WARMUP_RUNS + RUNS_PER_CONFIG * 2) next(Phase.DONE);
                    else next(Phase.BUILD);
                }
            }
            case DONE -> {
                if (phaseTick == 1) finish(mc);
            }
        }
    }

    private static void next(Phase p) {
        phase = p;
        phaseTick = 0;
    }

    private static void send(LocalPlayer player, String command) {
        player.connection.sendCommand(command);
    }

    private static void record(List<Long> frames) {
        JsonObject o = new JsonObject();
        o.addProperty("run", run + 1 - WARMUP_RUNS);
        o.addProperty("optimizer", optimizerOn ? "on" : "off");
        o.addProperty("crystals", crystalsSeen);
        o.addProperty("frames", frames.size());
        if (!frames.isEmpty()) {
            List<Long> sorted = new ArrayList<>(frames);
            Collections.sort(sorted);
            double total = 0;
            for (long f : sorted) total += f;
            double avgMs = total / sorted.size() / 1_000_000.0;
            long p99 = sorted.get(Math.min(sorted.size() - 1, (int) Math.floor(sorted.size() * 0.99)));
            long worst = sorted.get(sorted.size() - 1);
            o.addProperty("avgMs", round(avgMs));
            o.addProperty("avgFps", round(1000.0 / avgMs));
            o.addProperty("p99Ms", round(p99 / 1_000_000.0));
            o.addProperty("onePercentLowFps", round(1_000_000_000.0 / p99));
            o.addProperty("worstMs", round(worst / 1_000_000.0));
            ShardClient.LOGGER.info("Bench: run {} optimizer {}: {} frames, avg {} ms ({} fps), p99 {} ms (1% low {} fps), worst {} ms",
                    run + 1 - WARMUP_RUNS, optimizerOn ? "on" : "off", frames.size(), round(avgMs), round(1000.0 / avgMs),
                    round(p99 / 1_000_000.0), round(1_000_000_000.0 / p99), round(worst / 1_000_000.0));
        }
        RUNS.add(o);
    }

    private static double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static void finish(Minecraft mc) {
        JsonObject root = new JsonObject();
        root.add("runs", RUNS);
        root.add("summary", summarize());
        try {
            Files.writeString(SmokeTest.outDir().resolve("bench.json"), new GsonBuilder().setPrettyPrinting().create().toJson(root), StandardCharsets.UTF_8);
            ShardClient.LOGGER.info("Bench: wrote {}", SmokeTest.outDir().resolve("bench.json"));
        } catch (IOException e) {
            ShardClient.LOGGER.error("Bench: could not write results", e);
        }
        ShardClient.LOGGER.info("Bench summary: {}", root.get("summary"));
        mc.stop();
    }

    private static JsonObject summarize() {
        JsonObject out = new JsonObject();
        for (String config : new String[]{"on", "off"}) {
            double avg = 0;
            double low = 0;
            double worst = 0;
            int n = 0;
            for (var e : RUNS) {
                JsonObject o = e.getAsJsonObject();
                if (!o.get("optimizer").getAsString().equals(config) || !o.has("avgFps")) continue;
                avg += o.get("avgFps").getAsDouble();
                low += o.get("onePercentLowFps").getAsDouble();
                worst += o.get("worstMs").getAsDouble();
                n++;
            }
            JsonObject c = new JsonObject();
            c.addProperty("runs", n);
            if (n > 0) {
                c.addProperty("avgFps", round(avg / n));
                c.addProperty("onePercentLowFps", round(low / n));
                c.addProperty("worstMs", round(worst / n));
            }
            out.add(config, c);
        }
        return out;
    }
}
