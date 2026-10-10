package gg.shard.client.modules.visual;

import gg.shard.client.ShardClient;
import gg.shard.client.cosmetics.CapeLibrary;
import gg.shard.client.cosmetics.EquippedCape;
import gg.shard.client.cosmetics.PlayerCosmetics;
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
import net.minecraft.resources.Identifier;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
    private final BoolSetting showShield = add(new BoolSetting("Show my shield", "Your shields use the shield cosmetic you equipped", true));
    private final BoolSetting showBandana = add(new BoolSetting("Show my bandana", "Wear the bandana you equipped (hidden under a helmet)", true));
    private final BoolSetting showOthers = add(new BoolSetting("Show other players' cosmetics", "Show the capes, shields and bandanas other Shard players equipped", true));
    private final BoolSetting onElytra = add(new BoolSetting("On elytra too", "Paint elytras with the cape's elytra artwork", true));
    private final BoolSetting earnTokens = add(new BoolSetting("Earn tokens", "Earn 10 Shard tokens for every 10 minutes you play", true));
    private final BoolSetting tokenToasts = add(new BoolSetting("Token pop-ups", "A small pop-up each time you earn tokens", true));

    private final CapeLibrary library = new CapeLibrary();
    private final ShardApi api = new ShardApi(library.cacheDir());
    /** Who wears what, from the API (players currently in the world). */
    private final Map<UUID, PlayerCosmetics.Equipped> worn = new ConcurrentHashMap<>();
    private final Set<UUID> looked = new HashSet<>();
    private final Map<PlayerSkin, PlayerSkin> patched = new WeakHashMap<>();

    private String localCapeId;
    /** Shield and bandana from the launcher's equipped.json, once it writes them. */
    private PlayerCosmetics.Equipped localSlots = PlayerCosmetics.Equipped.NONE;
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
        super("Cosmetics", "Capes, shields and bandanas for every Shard player, and tokens for playing.", ModuleCategory.VISUALS);
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
        return "Shows the cape, shield and bandana every Shard player equipped in Shard Launcher, including yours, and earns you "
                + "10 Shard tokens for every 10 minutes you play (only while you are moving; standing idle for "
                + "five minutes pauses it). Spend tokens on cosmetics in the launcher. You sign in with Mojang's own "
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
                String id = wearing("cape");
                texture = library.ready(id);
                if (texture == null) texture = library.capeTexture(id);
            }
        } else if (showOthers.get()) {
            texture = library.capeTexture(wornSlot(uuid, "cape"));
        }
        if (texture == null) return skin;
        boolean elytra = onElytra.get();
        PlayerSkin cached = patched.get(skin);
        //? if >=1.21.9 {
        if (cached != null && cached.cape() == texture && (cached.elytra() == texture) == elytra) return cached;
        PlayerSkin out = new PlayerSkin(skin.body(), texture, elytra ? texture : skin.elytra(), skin.model(), skin.secure());
        //?} else {
        /*// Before 1.21.9 a skin names its cape and elytra textures by id.
        Identifier cape = texture.texturePath();
        if (cached != null && cached.capeTexture() == cape && (cached.elytraTexture() == cape) == elytra) return cached;
        PlayerSkin out = new PlayerSkin(skin.texture(), skin.textureUrl(), cape, elytra ? cape : skin.elytraTexture(), skin.model(), skin.secure());
        *///?}
        patched.put(skin, out);
        return out;
    }

    private String wornSlot(UUID uuid, String slot) {
        PlayerCosmetics.Equipped e = worn.get(uuid);
        return e == null ? null : e.slot(slot);
    }

    /** The shield texture this player's held shields use, or null for vanilla's. Render thread. */
    public Identifier shieldTexture(UUID uuid, boolean local) {
        return slotTexture(uuid, local, "shield", showShield);
    }

    /** The bandana texture this player wears, or null. Render thread. */
    public Identifier bandanaTexture(UUID uuid, boolean local) {
        return slotTexture(uuid, local, "bandana", showBandana);
    }

    private Identifier slotTexture(UUID uuid, boolean local, String slot, BoolSetting mine) {
        if (!isEnabled() || uuid == null) return null;
        if (local) return mine.get() ? library.textureId(wearing(slot), slot) : null;
        return showOthers.get() ? library.textureId(wornSlot(uuid, slot), slot) : null;
    }

    /**
     * What you wear in a slot ("cape", "shield", "bandana"). Before signing in it is the launcher's
     * pick from equipped.json, so it shows straight away; once signed in the account is the truth
     * (only owned items can be equipped there), so a change made in the in-game Cosmetics tab or
     * in the launcher shows at once without a restart.
     */
    public String wearing(String slot) {
        ShardApi.Me me = api.me();
        if (me != null) return me.equipped().slot(slot);
        return "cape".equals(slot) ? localCapeId : localSlots.slot(slot);
    }

    // 0.8.x called "Show other players' cosmetics" "Show Shard capes".
    @Override
    protected void migrateSetting(String key, JsonElement value, JsonObject all, int version) {
        if (key.equals("show-shard-capes")) {
            if (!all.has(showOthers.key())) showOthers.fromJson(value);
            return;
        }
        super.migrateSetting(key, value, all, version);
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
                        Component.literal("You have " + me.tokens() + ". Spend them on cosmetics in Shard Launcher."));
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
        api.equipped(present).whenComplete((slots, error) -> Minecraft.getInstance().execute(() -> {
            if (error != null) return; // keep what we had; try again next minute
            for (UUID uuid : present) {
                PlayerCosmetics.Equipped e = slots.get(uuid);
                if (e == null) worn.remove(uuid);
                else worn.put(uuid, e);
            }
        }));
    }

    private void loadLocal() {
        Path equipped = equippedPath();
        if (equipped == null) {
            localStatus = "No launcher info";
            return;
        }
        localSlots = EquippedCape.readSlots(equipped);
        for (String slot : new String[]{"shield", "bandana"}) {
            String id = localSlots.slot(slot);
            if (id == null) continue;
            Path texture = EquippedCape.textureFor(equipped, id);
            if (Files.isRegularFile(texture)) library.loadLocal(id, slot, texture);
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

    /**
     * Equips an owned item in a slot (null takes it off) on your Shard account: the in-game
     * Cosmetics tab and the smoke test. You wear it at once, other Shard players see it on their next
     * lookup (within a minute), and the launcher shows it the next time it loads your account.
     * Completes on the render thread.
     */
    public java.util.concurrent.CompletableFuture<ShardApi.Me> equip(String slot, String id) {
        java.util.concurrent.CompletableFuture<ShardApi.Me> done = new java.util.concurrent.CompletableFuture<>();
        if (!api.signedIn()) {
            done.completeExceptionally(new IllegalStateException("not signed in to Shard yet"));
            return done;
        }
        api.equip(slot, id).whenComplete((me, error) -> Minecraft.getInstance().execute(() -> {
            if (error != null) {
                done.completeExceptionally(error.getCause() != null ? error.getCause() : error);
                return;
            }
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) worn.put(player.getUUID(), me.equipped());
            accountStatus = "Signed in as " + me.name() + ", " + me.tokens() + " tokens";
            nextLookup = 0;
            done.complete(me);
        }));
        return done;
    }

    /** Whether the Shard account is signed in (equipping needs it). */
    public boolean signedIn() {
        return api.signedIn() && api.me() != null;
    }

    /** The account line for the Cosmetics tab: who is signed in, or why not. */
    public String accountStatus() {
        return accountStatus;
    }

    /** Every cape, shield and bandana in the catalogue. */
    public Map<String, PlayerCosmetics.Item> catalogue() {
        return library.catalogue();
    }

    /** The preview picture of a catalogue item once loaded (256x256), else null. Render thread. */
    public Identifier preview(String id) {
        return library.preview(id);
    }

    /** For the smoke test: what the API says this player wears. */
    public Map<UUID, PlayerCosmetics.Equipped> wornSnapshot() {
        return new HashMap<>(worn);
    }
}
