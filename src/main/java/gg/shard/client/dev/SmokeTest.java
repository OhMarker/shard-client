package gg.shard.client.dev;

import gg.shard.client.ShardClient;
import gg.shard.client.gui.ClickGuiScreen;
import gg.shard.client.hud.HudEditorScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Development-only verification. With the JVM property {@code shard.smoke.dir} set in a dev
 * environment: skip first-run onboarding, connect to {@code shard.smoke.server}, wait for a
 * world, screenshot the HUD, the click GUI and the HUD editor, then quit. Never active in a
 * distributed jar (Loom only sets the properties for runClient).
 */
public final class SmokeTest {
    private SmokeTest() {}

    private static int ticksInWorld = -1;
    private static int ticksOutOfWorld;
    private static boolean connectRequested;

    public static void init() {
        String dir = System.getProperty("shard.smoke.dir");
        if (dir == null || dir.isBlank() || !FabricLoader.getInstance().isDevelopmentEnvironment()) return;
        Path out = Path.of(dir);
        try {
            Files.createDirectories(out);
        } catch (IOException e) {
            ShardClient.LOGGER.error("Smoke: cannot create {}", out, e);
            return;
        }
        String server = System.getProperty("shard.smoke.server");
        ShardClient.LOGGER.info("Smoke test armed; screenshots -> {}; server -> {}; launch args {}", out, server,
                Arrays.toString(FabricLoader.getInstance().getLaunchArguments(false)));
        ClientTickEvents.END_CLIENT_TICK.register(mc -> tick(mc, out, server));
    }

    private static void tick(Minecraft mc, Path out, String server) {
        if (mc.player == null || mc.level == null) {
            ticksInWorld = -1;
            ticksOutOfWorld++;
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
        switch (ticksInWorld) {
            case 60 -> shot(mc, out.resolve("smoke-hud.png"));
            case 80 -> mc.setScreen(new ClickGuiScreen(null));
            case 110 -> shot(mc, out.resolve("smoke-gui.png"));
            case 120 -> mc.setScreen(new HudEditorScreen(null, ShardClient.hud()));
            case 140 -> shot(mc, out.resolve("smoke-editor.png"));
            case 160 -> {
                mc.setScreen(null);
                ShardClient.config().save();
                ShardClient.LOGGER.info("Smoke test complete; stopping client");
                mc.stop();
            }
            default -> {
            }
        }
    }

    private static void shot(Minecraft mc, Path file) {
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
