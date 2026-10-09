package gg.shard.client.mixin;

import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.realmsclient.gui.RealmsDataFetcher;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.gui.screens.social.PlayerSocialManager;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.minecraft.client.telemetry.ClientTelemetryManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.net.Proxy;
import java.util.concurrent.CompletableFuture;

/**
 * Everything 1.21.11's Minecraft binds to the signed-in account (checked with javap against the
 * constructor): the in-game account switcher replaces them together (gg.shard.client.account.SessionSwitch).
 */
@Mixin(Minecraft.class)
public interface MinecraftSessionAccessor {
    @Mutable
    @Accessor("user")
    void shard$setUser(User user);

    @Mutable
    @Accessor("profileFuture")
    void shard$setProfileFuture(CompletableFuture<ProfileResult> future);

    @Accessor("userApiService")
    UserApiService shard$userApiService();

    @Mutable
    @Accessor("userApiService")
    void shard$setUserApiService(UserApiService service);

    @Mutable
    @Accessor("userPropertiesFuture")
    void shard$setUserPropertiesFuture(CompletableFuture<UserApiService.UserProperties> future);

    @Mutable
    @Accessor("playerSocialManager")
    void shard$setPlayerSocialManager(PlayerSocialManager manager);

    //? if >=26.2 {
    /*@Accessor("remoteFriendListUpdateHandler")
    net.minecraft.client.gui.screens.social.RemoteFriendListUpdateHandler shard$remoteFriendListUpdateHandler();

    @Mutable
    @Accessor("remoteFriendListUpdateHandler")
    void shard$setRemoteFriendListUpdateHandler(net.minecraft.client.gui.screens.social.RemoteFriendListUpdateHandler handler);
    *///?}

    @Mutable
    @Accessor("profileKeyPairManager")
    void shard$setProfileKeyPairManager(ProfileKeyPairManager manager);

    @Accessor("reportingContext")
    void shard$setReportingContext(ReportingContext context);

    @Mutable
    @Accessor("realmsDataFetcher")
    void shard$setRealmsDataFetcher(RealmsDataFetcher fetcher);

    @Mutable
    @Accessor("telemetryManager")
    void shard$setTelemetryManager(ClientTelemetryManager manager);

    @Accessor("proxy")
    Proxy shard$proxy();

    // 1.21.9 added the offline developer mode (--offline-developer-mode); before, there is none.
    //? if >=1.21.9 {
    @Accessor("offlineDeveloperMode")
    boolean shard$offlineDeveloperMode();
    //?}
}
