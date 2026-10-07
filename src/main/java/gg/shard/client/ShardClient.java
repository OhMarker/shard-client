package gg.shard.client;

import gg.shard.client.command.CommandManager;
import gg.shard.client.config.ConfigManager;
import gg.shard.client.gui.Theme;
import gg.shard.client.hud.HudManager;
import gg.shard.client.input.Keybinds;
import gg.shard.client.launcher.LauncherInfo;
import gg.shard.client.module.ModuleManager;
import gg.shard.client.modules.combat.ToggleSprintModule;
import gg.shard.client.modules.hud.ArmorStatusModule;
import gg.shard.client.modules.hud.ClockModule;
import gg.shard.client.modules.hud.CoordinatesModule;
import gg.shard.client.modules.hud.CpsModule;
import gg.shard.client.modules.hud.FpsModule;
import gg.shard.client.modules.hud.HitDelayIndicatorModule;
import gg.shard.client.modules.hud.ItemCounterModule;
import gg.shard.client.modules.hud.KeystrokesModule;
import gg.shard.client.modules.hud.MemoryModule;
import gg.shard.client.modules.hud.PingModule;
import gg.shard.client.modules.hud.PotionEffectsModule;
import gg.shard.client.modules.hud.ServerAddressModule;
import gg.shard.client.modules.hud.SessionStatsModule;
import gg.shard.client.modules.hud.TotemCounterModule;
import gg.shard.client.modules.perf.TotemAnimationModule;
import gg.shard.client.modules.visual.CrystalTweaksModule;
import gg.shard.client.modules.visual.NoHurtCamModule;
import gg.shard.client.modules.visual.ParticleMultiplierModule;
import gg.shard.client.modules.visual.PopMessagesModule;
import gg.shard.client.modules.visual.ZoomModule;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ShardClient implements ClientModInitializer {
    public static final String MOD_ID = "shard";
    public static final Logger LOGGER = LoggerFactory.getLogger("Shard");

    private static ModuleManager modules;
    private static ConfigManager config;
    private static HudManager hud;
    private static LauncherInfo launcherInfo = LauncherInfo.DEFAULTS;
    private static boolean ready;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Shard client initialising");
        launcherInfo = LauncherInfo.load(FabricLoader.getInstance().getGameDir());
        Theme.setAccent(launcherInfo.accent());
        if (launcherInfo.present()) {
            LOGGER.info("Launched by Shard Launcher {} (instance \"{}\", build {})",
                    launcherInfo.launcherVersion(), launcherInfo.instanceName(), launcherInfo.shardBuild());
        }

        modules = new ModuleManager();
        registerModules(modules);

        config = new ConfigManager(FabricLoader.getInstance().getConfigDir(), modules);
        modules.onChanged(m -> config.markDirty());
        modules.onTick(config::flushIfDirty);
        config.load();

        hud = new HudManager(modules);
        hud.start();
        modules.start();
        Keybinds.init();
        new CommandManager().start();
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> config.save());

        ready = true;
        LOGGER.info("Shard ready: {} modules", modules.all().size());
        gg.shard.client.dev.SmokeTest.init();
    }

    private static void registerModules(ModuleManager m) {
        // HUD
        m.register(new FpsModule());
        m.register(new PingModule());
        m.register(new CoordinatesModule());
        m.register(new CpsModule());
        m.register(new KeystrokesModule());
        m.register(new ArmorStatusModule());
        m.register(new TotemCounterModule());
        m.register(new PotionEffectsModule());
        m.register(new ItemCounterModule());
        m.register(new ServerAddressModule());
        m.register(new SessionStatsModule());
        m.register(new ClockModule());
        m.register(new MemoryModule());
        m.register(new HitDelayIndicatorModule());
        // Visuals
        m.register(new NoHurtCamModule());
        m.register(new ParticleMultiplierModule());
        m.register(new CrystalTweaksModule());
        m.register(new ZoomModule());
        m.register(new PopMessagesModule());
        // Combat QoL
        m.register(new ToggleSprintModule());
        // Performance
        m.register(new TotemAnimationModule());
    }

    /** True once modules and config exist; mixins check this because they can run very early. */
    public static boolean isReady() {
        return ready;
    }

    public static ModuleManager modules() {
        return modules;
    }

    public static ConfigManager config() {
        return config;
    }

    public static HudManager hud() {
        return hud;
    }

    public static LauncherInfo launcherInfo() {
        return launcherInfo;
    }
}
