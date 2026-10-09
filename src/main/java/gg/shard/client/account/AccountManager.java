package gg.shard.client.account;

import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.gui.RealmsDataFetcher;
import gg.shard.client.ShardClient;
import gg.shard.client.launcher.AccountBridge;
import gg.shard.client.mixin.MinecraftSessionAccessor;
import gg.shard.client.mixin.RealmsClientAccessor;
import gg.shard.client.modules.visual.CosmeticsModule;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.gui.screens.social.PlayerSocialManager;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import net.minecraft.client.multiplayer.chat.report.ReportEnvironment;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.minecraft.client.telemetry.ClientTelemetryManager;
import net.minecraft.util.Util;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * The in-game account switcher's state (docs/ACCOUNT-SWITCH-API.md, client side). Without a bridge
 * in launcher-info.json (other launchers, older Shard Launchers) it only reports the current
 * account. With one it lists the launcher's accounts, switches to one (outside a world only) and
 * asks the launcher to add an account, polling until the new one shows up. All network calls run
 * on the bridge's own thread; results are applied on the game thread.
 */
public final class AccountManager {
    /** Dev runs: {@code -PfakeBridge} starts {@link gg.shard.client.dev.FakeAccountBridge}. */
    public static final String FAKE_BRIDGE_PROPERTY = "shard.dev.fakeBridge";
    private static final long POLL_MS = 2000;
    private static final long ADD_TIMEOUT_MS = 5 * 60_000;
    private static final long REFRESH_AFTER_MS = 30_000;

    private static AccountManager instance;

    private final AccountBridge bridge;
    private volatile List<AccountBridge.Account> accounts = List.of();
    private volatile String activeId;
    private volatile boolean loading;
    private volatile String switchingTo;
    private volatile String error;
    private volatile long errorAt;
    private volatile long lastRefresh;
    private volatile long addUntil;
    private volatile int addBaseline;
    private long nextPoll;

    private AccountManager(AccountBridge bridge) {
        this.bridge = bridge;
    }

    /** The switcher, created on first use from launcher-info.json (or the dev fake bridge). */
    public static AccountManager get() {
        if (instance == null) instance = new AccountManager(createBridge());
        return instance;
    }

    private static AccountBridge createBridge() {
        AccountBridge.Endpoint endpoint = ShardClient.launcherInfo().accountBridge();
        if (endpoint == null && FabricLoader.getInstance().isDevelopmentEnvironment() && System.getProperty(FAKE_BRIDGE_PROPERTY) != null) {
            try {
                User user = Minecraft.getInstance().getUser();
                endpoint = gg.shard.client.dev.FakeAccountBridge.withDevAccounts(user.getName(), user.getProfileId()).endpoint();
                ShardClient.LOGGER.info("Accounts: dev fake bridge on {}", endpoint.url());
            } catch (Exception e) {
                ShardClient.LOGGER.warn("Accounts: could not start the fake bridge", e);
            }
        }
        if (endpoint != null) ShardClient.LOGGER.info("Accounts: launcher bridge at {}", endpoint.url());
        return endpoint == null ? null : new AccountBridge(endpoint);
    }

    /** False without a launcher bridge: the screens show the name only. */
    public boolean available() {
        return bridge != null;
    }

    public List<AccountBridge.Account> accounts() {
        return accounts;
    }

    /** The account the game is signed in as, matched by UUID against the launcher's list. */
    public AccountBridge.Account current() {
        UUID uuid = Minecraft.getInstance().getUser().getProfileId();
        for (AccountBridge.Account a : accounts) if (uuid.equals(a.profileId())) return a;
        return null;
    }

    public String currentName() {
        return Minecraft.getInstance().getUser().getName();
    }

    public UUID currentUuid() {
        return Minecraft.getInstance().getUser().getProfileId();
    }

    public boolean loading() {
        return loading;
    }

    /** Account id being switched to, or null. */
    public String switchingTo() {
        return switchingTo;
    }

    public boolean adding() {
        return System.currentTimeMillis() < addUntil;
    }

    /** The last error, shown for ten seconds. */
    public String error() {
        return error != null && System.currentTimeMillis() - errorAt < 10_000 ? error : null;
    }

    /** Switching needs no world: a server connection is tied to the account it joined with. */
    public boolean canSwitch() {
        Minecraft mc = Minecraft.getInstance();
        return bridge != null && mc.level == null && mc.getConnection() == null && switchingTo == null;
    }

    /** Called every frame by screens that show the switcher: first load, staleness and "add" polling. */
    public void tick() {
        if (bridge == null) return;
        long now = System.currentTimeMillis();
        if (adding()) {
            if (now >= nextPoll) {
                nextPoll = now + POLL_MS;
                refresh();
            }
        } else if (!loading && now - lastRefresh > REFRESH_AFTER_MS) {
            refresh();
        }
    }

    /** Reloads the account list from the launcher. */
    public void refresh() {
        if (bridge == null || loading) return;
        loading = true;
        lastRefresh = System.currentTimeMillis();
        bridge.accounts().whenComplete((list, err) -> Minecraft.getInstance().execute(() -> {
            loading = false;
            if (err != null) {
                fail(err);
                return;
            }
            accounts = list.accounts();
            activeId = list.active();
            if (adding() && accounts.size() > addBaseline) addUntil = 0;
        }));
    }

    /** Asks the launcher to sign in another account (its window comes to the front). */
    public void addAccount() {
        if (bridge == null) return;
        addBaseline = accounts.size();
        bridge.add().whenComplete((v, err) -> Minecraft.getInstance().execute(() -> {
            if (err != null) {
                fail(err);
                return;
            }
            addUntil = System.currentTimeMillis() + ADD_TIMEOUT_MS;
            nextPoll = System.currentTimeMillis() + POLL_MS;
        }));
    }

    /** Switches the game to {@code account}: a fresh session from the launcher, then {@link #apply}. */
    public void switchTo(AccountBridge.Account account) {
        if (account == null || !canSwitch()) return;
        if (account.profileId() != null && account.profileId().equals(currentUuid())) return;
        switchingTo = account.id();
        error = null;
        bridge.session(account.id()).whenComplete((session, err) -> Minecraft.getInstance().execute(() -> {
            switchingTo = null;
            if (err != null) {
                fail(err);
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null || mc.getConnection() != null) {
                error = "Leave the world to switch accounts";
                errorAt = System.currentTimeMillis();
                return;
            }
            apply(mc, session);
            activeId = account.id();
        }));
    }

    private void fail(Throwable err) {
        Throwable cause = err.getCause() != null ? err.getCause() : err;
        error = cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
        errorAt = System.currentTimeMillis();
        ShardClient.LOGGER.info("Accounts: {}", error);
    }

    /**
     * Makes {@code session} the game's account. 1.21.11 binds these to the account in Minecraft's
     * constructor, so each is rebuilt the way vanilla builds it: the User (name, UUID, token), the
     * profile lookup behind getGameProfile, the UserApiService (token-bound: chat reports, block list,
     * properties such as multiplayer bans), the user-properties fetch, the social manager (block
     * list), the profile key pair manager (chat signing keys; also Shard's sign-in proof), the
     * reporting context, the telemetry manager, and Realms (its client caches the session id). The
     * session service, skin manager and profile resolver are not account-bound. Then Shard's API
     * signs in again so tokens and capes follow the new account.
     */
    static void apply(Minecraft mc, AccountBridge.Session session) {
        MinecraftSessionAccessor access = (MinecraftSessionAccessor) mc;
        User user = new User(session.name(), session.profileId(), session.accessToken(),
                Optional.ofNullable(session.xuid()), Optional.ofNullable(session.clientId()));
        access.shard$setUser(user);

        UserApiService api;
        if (access.shard$offlineDeveloperMode()) {
            api = UserApiService.OFFLINE;
        } else {
            try {
                api = new YggdrasilAuthenticationService(access.shard$proxy()).createUserApiService(user.getAccessToken());
            } catch (RuntimeException e) {
                ShardClient.LOGGER.warn("Accounts: no user API for {} ({})", user.getName(), e.getMessage());
                api = UserApiService.OFFLINE;
            }
        }
        UserApiService service = api;
        access.shard$setUserApiService(service);
        access.shard$setUserPropertiesFuture(CompletableFuture.supplyAsync(() -> {
            try {
                return service.fetchProperties();
            } catch (Exception e) {
                ShardClient.LOGGER.warn("Accounts: could not fetch user properties ({})", e.getMessage());
                return UserApiService.OFFLINE_PROPERTIES;
            }
        }, Util.nonCriticalIoPool()));
        if (access.shard$offlineDeveloperMode()) {
            access.shard$setProfileFuture(CompletableFuture.completedFuture(null));
        } else {
            UUID id = user.getProfileId();
            access.shard$setProfileFuture(CompletableFuture.supplyAsync(() -> mc.services().sessionService().fetchProfile(id, true), Util.nonCriticalIoPool()));
        }
        access.shard$setPlayerSocialManager(new PlayerSocialManager(mc, service));
        access.shard$setProfileKeyPairManager(ProfileKeyPairManager.create(service, user, mc.gameDirectory.toPath()));
        access.shard$setReportingContext(ReportingContext.create(ReportEnvironment.local(), service));
        try {
            mc.getTelemetryManager().close();
        } catch (RuntimeException ignored) {
            // the old manager only flushes its log
        }
        access.shard$setTelemetryManager(new ClientTelemetryManager(mc, service, user));
        RealmsClientAccessor.shard$setInstance(null);
        access.shard$setRealmsDataFetcher(new RealmsDataFetcher(RealmsClient.getOrCreate(mc)));
        ShardClient.LOGGER.info("Accounts: switched to {}", user.getName());
        if (ShardClient.modules() != null) ShardClient.modules().get(CosmeticsModule.class).onAccountChanged();
    }
}
