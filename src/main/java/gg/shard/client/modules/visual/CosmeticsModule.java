package gg.shard.client.modules.visual;

import gg.shard.client.ShardClient;
import gg.shard.client.cosmetics.CapeLibrary;
import gg.shard.client.cosmetics.EquippedCape;
import gg.shard.client.cosmetics.ShardApi;
import gg.shard.client.module.Module;
import gg.shard.client.module.ModuleCategory;
import gg.shard.client.module.setting.BoolSetting;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.ClientAsset;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerSkin;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shard capes and tokens. Signs in to the Shard API as your Minecraft account (Mojang's own
 * server-join check), earns tokens while you play (a heartbeat every two minutes, only when you
 * have moved in the last five), and shows every Shard player's equipped cape, including yours.
 * Purely visual: nothing is sent to the Minecraft server.
 */
public final class CosmeticsModule extends Module {
    /** Dev runs without a launcher can point at an equipped.json with this JVM property. */
    public static final String DEV_EQUIPPED_PROPERTY = "shard.dev.equippedPath";
    private static final int HEARTBEAT_TICKS = 20 * 120;
    private static final int EQUIPPED_TICKS = 20 * 60;
    private static final int CATALOGUE_TICKS = 20 * 60 * 15;
    private static final int SIGN_IN_RETRY_TICKS = 20 * 60 * 5;
    private static final long IDLE_AFTER_MS = 5 * 60_000;

    private final BoolSetting showCape = add(new BoolSetting("Show my cape", "Wear the cape you equipped in Shard Launcher", true));
    private final BoolSetting showOthers = add(new BoolSetting("Show Shard capes", "Show the capes other Shard players equipped", true));
    private final BoolSetting onElytra = add(new BoolSetting("On elytra too", "Paint elytras with the cape's elytra artwork", true));
    private final BoolSetting earnTokens = add(new BoolSetting("Earn tokens", "Earn 10 Shard tokens for every 10 minutes you play", true));
    private final BoolSetting tokenToasts = add(new BoolSetting("Token pop-ups", "A small pop-up each time you earn tokens", true));

    private final CapeLibrary library = new CapeLibrary();
    private final ShardApi api = new ShardApi(library.cacheDir());
    /** Who wears what, from the API (players currently in the world). */
    private final Map<UUID, String> worn = new ConcurrentHashMap<>();
    private final Set<UUID> looked = new HashSet<>();
    private final Map<PlayerSkin, PlayerSkin> patched = new WeakHashMap<>();

    private String localCapeId;
    private volatile String localStatus = "Not loaded";
    private volatile String accountStatus = "Not signed in";
    private boolean started;
    private boolean signingIn;
    private boolean signInAgain;
    private int ticks;
    private int nextSignIn;
    private int nextBeat = HEARTBEAT_TICKS;
    private int nextLookup;
    private int nextCatalogue = CATALOGUE_TICKS;
    private long lastInput = System.currentTimeMillis();
    private float lastYaw = Float.NaN;
    private float lastPitch = Float.NaN;
    private double lastX = Double.NaN;
    private double lastZ = Double.NaN;

    public CosmeticsModule() {
        super("Cosmetics", "Capes for every Shard player, and tokens for playing.", ModuleCategory.VISUALS);
    }

    @Override
    public String icon() {
        return "cape";
    }

    @Override
    public boolean hidden() {
        return true;
    }

    @Override
    public boolean alwaysOn() {
        return true;
    }

    @Override
    public boolean defaultEnabled() {
        return true;
    }

    @Override
    public String about() {
        return "Shows the cape every Shard player equipped in Shard Launcher, including yours, and earns you "
                + "10 Shard tokens for every 10 minutes you play (only while you are moving; standing idle for "
                + "five minutes pauses it). Spend tokens on capes in the launcher. You sign in with Mojang's own "
                + "check, the same one servers use, so your account details never reach Shard. Purely visual: "
                + "nothing is sent to the Minecraft server, and players without Shard see normal capes.";
    }

    /** One line for the smoke test and logs. */
    public String status() {
        return localStatus + "; " + accountStatus + "; " + library.status();
    }

    /** The signed-in account (tokens, owned capes), or null. */
    public ShardApi.Me account() {
        return api.me();
    }

    /** Called from the AbstractClientPlayer hook for every player's skin. */
    public PlayerSkin apply(PlayerSkin skin, UUID uuid, boolean local) {
        if (!isEnabled() || skin == null) return skin;
        ClientAsset.Texture texture = null;
        if (local) {
            if (showCape.get()) {
                // The launcher's choice shows straight away; once signed in it must also be owned
                // (bought or given), so nobody wears a cape they did not pay for, even on their screen.
                ShardApi.Me me = api.me();
                if (localCapeId != null && (me == null || me.owned().contains(localCapeId))) texture = library.ready(localCapeId);
                if (texture == null) texture = library.texture(worn.get(uuid));
            }
        } else if (showOthers.get()) {
            texture = library.texture(worn.get(uuid));
        }
        if (texture == null) return skin;
        boolean elytra = onElytra.get();
        PlayerSkin cached = patched.get(skin);
        if (cached != null && cached.cape() == texture && (cached.elytra() == texture) == elytra) return cached;
        PlayerSkin out = new PlayerSkin(skin.body(), texture, elytra ? texture : skin.elytra(), skin.model(), skin.secure());
        patched.put(skin, out);
        return out;
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return;
        ticks++;
        trackInput(player);
        if (!started) {
            started = true;
            loadLocal();
            library.refresh();
        }
        if (ticks >= nextCatalogue) {
            nextCatalogue = ticks + CATALOGUE_TICKS;
            library.refresh();
        }
        if (!api.signedIn() && !signingIn && ticks >= nextSignIn) signIn();
        if (api.signedIn() && earnTokens.get() && ticks >= nextBeat) {
            nextBeat = ticks + HEARTBEAT_TICKS;
            heartbeat();
        }
        lookUpCapes(mc);
        if (localCapeId != null && library.ready(localCapeId) != null && !localStatus.startsWith("Wearing")) {
            localStatus = "Wearing " + localCapeId;
        }
    }

    /**
     * The in-game account switcher changed the Minecraft account: drop the old Shard session and
     * sign in as the new account right away (also outside a world), so tokens and capes follow it.
     */
    public void onAccountChanged() {
        api.signOut();
        worn.clear();
        looked.clear();
        accountStatus = "Not signed in";
        if (signingIn) signInAgain = true; // the sign-in in flight is for the old account
        else signIn();
    }

    /** Moving the mouse or walking counts as playing; five minutes without either is idle. */
    private void trackInput(LocalPlayer p) {
        if (p.getYRot() != lastYaw || p.getXRot() != lastPitch || p.getX() != lastX || p.getZ() != lastZ) {
            if (!Float.isNaN(lastYaw)) lastInput = System.currentTimeMillis();
            lastYaw = p.getYRot();
            lastPitch = p.getXRot();
            lastX = p.getX();
            lastZ = p.getZ();
        }
    }

    private void signIn() {
        signingIn = true;
        accountStatus = "Signing in";
        api.signIn().whenComplete((me, error) -> Minecraft.getInstance().execute(() -> {
            signingIn = false;
            if (signInAgain) {
                signInAgain = false;
                api.signOut();
                signIn();
                return;
            }
            if (error != null) {
                Throwable cause = error.getCause() != null ? error.getCause() : error;
                accountStatus = "Not signed in (" + cause.getMessage() + ")";
                nextSignIn = ticks + SIGN_IN_RETRY_TICKS;
                ShardClient.LOGGER.info("Cosmetics: Shard sign-in failed: {}", cause.getMessage());
                return;
            }
            accountStatus = "Signed in as " + me.name() + ", " + me.tokens() + " tokens";
            ShardClient.LOGGER.info("Cosmetics: {}", accountStatus);
            nextBeat = ticks + 20 * 5; // first heartbeat soon, to start the clock
            looked.clear();
            nextLookup = 0;
        }));
    }

    private void heartbeat() {
        boolean active = System.currentTimeMillis() - lastInput < IDLE_AFTER_MS;
        int before = api.me() == null ? 0 : api.me().tokens();
        api.heartbeat(active).whenComplete((me, error) -> Minecraft.getInstance().execute(() -> {
            if (error != null) {
                ShardClient.LOGGER.debug("Cosmetics: heartbeat failed", error);
                return;
            }
            accountStatus = "Signed in as " + me.name() + ", " + me.tokens() + " tokens";
            int earned = me.tokens() - before;
            if (earned > 0 && tokenToasts.get()) {
                SystemToast.add(Minecraft.getInstance().getToastManager(), SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                        Component.literal("+" + earned + " Shard tokens"),
                        Component.literal("You have " + me.tokens() + ". Spend them on capes in Shard Launcher."));
            }
        }));
    }

    /** Asks the API about players in the world: new faces right away, everyone again every minute. */
    private void lookUpCapes(Minecraft mc) {
        Set<UUID> present = new HashSet<>();
        for (Player p : mc.level.players()) present.add(p.getUUID());
        boolean newFace = !looked.containsAll(present);
        if (!newFace && ticks < nextLookup) return;
        nextLookup = ticks + EQUIPPED_TICKS;
        looked.clear();
        looked.addAll(present);
        api.equipped(present).whenComplete((capes, error) -> Minecraft.getInstance().execute(() -> {
            if (error != null) return; // keep what we had; try again next minute
            for (UUID uuid : present) {
                String cape = capes.get(uuid);
                if (cape == null) worn.remove(uuid);
                else worn.put(uuid, cape);
            }
        }));
    }

    private void loadLocal() {
        Path equipped = equippedPath();
        if (equipped == null) {
            localStatus = "No launcher info";
            return;
        }
        EquippedCape item = EquippedCape.read(equipped);
        if (item == null) {
            localStatus = "Nothing equipped on the back";
            return;
        }
        if (!Files.isRegularFile(item.texture())) {
            localStatus = "Texture missing: " + item.texture();
            ShardClient.LOGGER.warn("Cosmetics: {} is equipped but {} does not exist", item.id(), item.texture());
            return;
        }
        localCapeId = item.id();
        localStatus = "Loading " + item.id();
        library.loadLocal(item.id(), item.texture());
    }

    private static Path equippedPath() {
        Path fromLauncher = ShardClient.launcherInfo().equippedPath();
        if (fromLauncher != null) return fromLauncher;
        if (!FabricLoader.getInstance().isDevelopmentEnvironment()) return null;
        String dev = System.getProperty(DEV_EQUIPPED_PROPERTY);
        return dev == null || dev.isBlank() ? null : Path.of(dev);
    }

    /** For the smoke test: what the API says this player wears. */
    public Map<UUID, String> wornSnapshot() {
        return new HashMap<>(worn);
    }
}
