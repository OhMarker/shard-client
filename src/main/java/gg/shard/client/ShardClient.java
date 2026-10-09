package gg.shard.client;

import gg.shard.client.command.CommandManager;
import gg.shard.client.config.ConfigManager;
import gg.shard.client.gui.Theme;
import gg.shard.client.hud.HudManager;
import gg.shard.client.input.Keybinds;
import gg.shard.client.launcher.LauncherInfo;
import gg.shard.client.module.ModuleManager;
import gg.shard.client.modules.combat.AnchorOptimizerModule;
import gg.shard.client.modules.combat.CrystalOptimizerModule;
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
import gg.shard.client.modules.perf.ExplosionOptimizerModule;
import gg.shard.client.modules.visual.CrosshairModule;
import gg.shard.client.modules.visual.CrystalTweaksModule;
import gg.shard.client.modules.visual.DeathAnimationModule;
import gg.shard.client.modules.visual.FullbrightModule;
import gg.shard.client.modules.visual.HitColorModule;
import gg.shard.client.modules.visual.HitboxModule;
import gg.shard.client.modules.visual.LowFireModule;
import gg.shard.client.modules.visual.LowShieldModule;
import gg.shard.client.modules.visual.NoHurtCamModule;
import gg.shard.client.modules.visual.TotemPopModule;
import gg.shard.client.modules.visual.ZoomModule;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.multiplayer.ServerData;
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

        // Modules whose feature another installed mod already provides stay off, with a notice.
        for (String notice : modules.applyModConflicts(ShardClient::modNameIfLoaded)) LOGGER.info(notice);

        // Per-server rules follow the connection.
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            ServerData data = handler.getServerData();
            modules.setCurrentServer(data == null ? null : data.ip);
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> modules.setCurrentServer(null));

        hud = new HudManager(modules);
        gg.shard.client.render.LowFireModels.init();
        // Bandana cosmetic on every player renderer (wide and slim).
        net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback.EVENT.register((type, renderer, helper, ctx) -> {
            if (renderer instanceof net.minecraft.client.renderer.entity.player.AvatarRenderer<?> avatar) {
                helper.register(new gg.shard.client.render.BandanaLayer(avatar));
            }
        });
        gg.shard.client.combat.CombatTracker.init();
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> client.execute(() -> {
            if (gg.shard.client.dev.SmokeTest.active() || !gg.shard.client.gui.WelcomeScreen.needed()) return;
            if (client.screen == null) client.setScreen(new gg.shard.client.gui.WelcomeScreen());
        }));
        //? if >=1.21.10 {
        net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents.BEFORE_BLOCK_OUTLINE.register((ctx, state) ->
                beforeBlockOutline(gg.shard.client.compat.WorldDraw.of(ctx), state));
        //?}
        gg.shard.client.modules.utility.GuiScalesModule.registerHudScaling();
        //? if >=1.21.10 {
        net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents.AFTER_ENTITIES.register(ctx -> afterEntities(gg.shard.client.compat.WorldDraw.of(ctx)));
        //?}
        hud.start();
        modules.start();
        Keybinds.init();
        new CommandManager().start();
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> config.save());

        ready = true;
        LOGGER.info("Shard ready: {} modules", modules.all().size());
        gg.shard.client.dev.SmokeTest.init();
    }

    /** World overlays drawn after the entities (Fabric's AFTER_ENTITIES; LevelRendererEventsMixin on 1.21.9). */
    public static void afterEntities(gg.shard.client.compat.WorldDraw draw) {
        if (!isReady()) return;
        modules.get(gg.shard.client.modules.visual.AnchorGlowModule.class).render(draw);
        //? if <1.21.11 {
        /*modules.get(gg.shard.client.modules.visual.HitboxModule.class).render(draw);
        *///?}
    }

    /** Returns false to cancel vanilla's block outline (Fabric's BEFORE_BLOCK_OUTLINE; LevelRendererEventsMixin on 1.21.9). */
    public static boolean beforeBlockOutline(gg.shard.client.compat.WorldDraw draw, net.minecraft.client.renderer.state.BlockOutlineRenderState state) {
        return !isReady() || modules.get(gg.shard.client.modules.visual.BlockOutlineModule.class).render(draw, state);
    }

    private static String modNameIfLoaded(String id) {
        return FabricLoader.getInstance().getModContainer(id).map(c -> c.getMetadata().getName()).orElse(null);
    }

    private static void registerModules(ModuleManager m) {
        // Hidden settings holders (Settings → Appearance / HUD); registered first so HUD modules can inherit defaults.
        m.register(new gg.shard.client.modules.settings.AppearanceModule());
        m.register(new gg.shard.client.modules.settings.HudDefaultsModule());
        m.register(new gg.shard.client.modules.settings.MenuScreensModule());
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
        m.register(new gg.shard.client.modules.hud.ComboModule());
        m.register(new gg.shard.client.modules.hud.ReachModule());
        m.register(new gg.shard.client.modules.hud.FightRecapModule());
        m.register(new gg.shard.client.modules.hud.CooldownsModule());
        m.register(new gg.shard.client.modules.hud.CompassModule());
        m.register(new gg.shard.client.modules.hud.SpeedModule());
        m.register(new gg.shard.client.modules.hud.TpsModule());
        m.register(new ClockModule());
        m.register(new gg.shard.client.modules.hud.TokensModule());
        m.register(new MemoryModule());
        m.register(new HitDelayIndicatorModule());
        // Visuals
        m.register(new TotemPopModule());
        m.register(new NoHurtCamModule());
        m.register(new CrystalTweaksModule());
        m.register(new DeathAnimationModule());
        m.register(new HitColorModule());
        m.register(new LowFireModule());
        m.register(new LowShieldModule());
        m.register(new gg.shard.client.modules.visual.SmallItemsModule());
        m.register(new gg.shard.client.modules.visual.AnchorGlowModule());
        m.register(new gg.shard.client.modules.utility.GuiScalesModule());
        m.register(new gg.shard.client.modules.utility.DisplayModule());
        m.register(new gg.shard.client.modules.utility.SoundsModule());
        m.register(new gg.shard.client.modules.visual.LowHealthModule());
        m.register(new gg.shard.client.modules.visual.BlockOutlineModule());
        m.register(new gg.shard.client.modules.visual.CleanScreenModule());
        m.register(new gg.shard.client.modules.visual.WeatherTimeModule());
        m.register(new gg.shard.client.modules.chat.ChatModule());
        m.register(new FullbrightModule());
        m.register(new HitboxModule());
        m.register(new gg.shard.client.modules.visual.SkyModule());
        m.register(new gg.shard.client.modules.visual.CosmeticsModule());
        m.register(new CrosshairModule());
        m.register(new ZoomModule());
        // Combat QoL
        m.register(new CrystalOptimizerModule());
        m.register(new AnchorOptimizerModule());
        m.register(new ToggleSprintModule());
        // Performance
        m.register(new ExplosionOptimizerModule());
        m.register(new gg.shard.client.modules.perf.EntityOptimizerModule());
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

    /** Settings → Appearance (accent, interface size, font, blur, motion). */
    public static gg.shard.client.modules.settings.AppearanceModule appearance() {
        return modules.get(gg.shard.client.modules.settings.AppearanceModule.class);
    }

    /** Settings → HUD (HUD scale and the style every HUD element inherits). */
    public static gg.shard.client.modules.settings.HudDefaultsModule hudDefaults() {
        return modules.get(gg.shard.client.modules.settings.HudDefaultsModule.class);
    }
}
