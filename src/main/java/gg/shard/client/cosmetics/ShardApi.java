package gg.shard.client.cosmetics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.world.entity.player.ProfileKeyPair;
import net.minecraft.world.entity.player.ProfilePublicKey;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.security.GeneralSecurityException;
import java.security.Signature;
import java.util.Base64;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * The game's side of the Shard API (API.md in shard-api): signing in with the account's
 * Mojang-signed key, the play-time heartbeat, and looking up who wears which cape. The access token
 * only ever goes to Mojang. The API address is read from the meta repository (services.json), so it can move
 * without a client update. Every call runs on one background thread.
 */
public final class ShardApi {
    public static final String SERVICES_URL = CapeLibrary.META_BASE + "services.json";
    /** Dev runs point at `wrangler dev` and sign in without Mojang (offline dev account). */
    public static final String DEV_API_PROPERTY = "shard.dev.apiBase";

    /** The signed-in player's account, as the API last reported it. */
    public record Me(String uuid, String name, int tokens, List<String> owned, String cape, boolean admin, int secondsToNextTokens) {}

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Shard API");
        t.setDaemon(true);
        return t;
    });
    private final Path cacheDir;
    private volatile String base;
    private volatile String session;
    private volatile Me me;

    public ShardApi(Path cacheDir) {
        this.cacheDir = cacheDir;
    }

    public Me me() {
        return me;
    }

    public boolean signedIn() {
        return session != null;
    }

    /** Signs in as the game's current Minecraft account. */
    public CompletableFuture<Me> signIn() {
        return CompletableFuture.supplyAsync(() -> {
            User user = Minecraft.getInstance().getUser();
            JsonObject challenge = post("/v1/auth/challenge", new JsonObject(), false);
            String serverId = challenge.get("serverId").getAsString();
            JsonObject verify = new JsonObject();
            verify.addProperty("username", user.getName());
            verify.addProperty("serverId", serverId);
            if (devApi() != null) {
                verify.addProperty("devUuid", user.getProfileId().toString());
            } else {
                verify.add("proof", proof(user.getProfileId(), serverId));
            }
            JsonObject result = post("/v1/auth/verify", verify, false);
            session = result.get("session").getAsString();
            me = parseMe(result.getAsJsonObject("me"));
            return me;
        }, worker);
    }

    /** Play-time heartbeat; returns the updated account. */
    public CompletableFuture<Me> heartbeat(boolean active) {
        return CompletableFuture.supplyAsync(() -> {
            JsonObject body = new JsonObject();
            body.addProperty("active", active);
            me = parseMe(post("/v1/heartbeat", body, true));
            return me;
        }, worker);
    }

    /** Cape ids for the given players (only those wearing one), up to 100 per call. */
    public CompletableFuture<Map<UUID, String>> equipped(Collection<UUID> uuids) {
        return CompletableFuture.supplyAsync(() -> {
            Map<UUID, String> out = new HashMap<>();
            List<UUID> all = new ArrayList<>(uuids);
            for (int i = 0; i < all.size(); i += 100) {
                String list = all.subList(i, Math.min(all.size(), i + 100)).stream()
                        .map(u -> u.toString().replace("-", "")).collect(Collectors.joining(","));
                JsonObject players = get("/v1/equipped?uuids=" + list).getAsJsonObject("players");
                for (Map.Entry<String, JsonElement> e : players.entrySet()) {
                    UUID uuid = PlayerCosmetics.parseUuid(e.getKey());
                    String cape = e.getValue().isJsonPrimitive() ? e.getValue().getAsString() : null;
                    if (uuid != null && cape != null && PlayerCosmetics.SAFE_ID.matcher(cape).matches()) out.put(uuid, cape);
                }
            }
            return out;
        }, worker);
    }

    /**
     * Proof of account: the key pair Mojang signs for chat (the game keeps it anyway) signs the
     * API's one-time challenge, and the API checks Mojang's signature itself. Mojang refuses
     * requests from Cloudflare, so the API cannot simply ask Mojang (API.md "Identity").
     */
    private static JsonObject proof(UUID uuid, String serverId) {
        ProfileKeyPair keys = Minecraft.getInstance().getProfileKeyPairManager().prepareKeyPair().join()
                .orElseThrow(() -> new IllegalStateException("this account has no Mojang chat key (multiplayer may be off in its privacy settings)"));
        ProfilePublicKey.Data data = keys.publicKey().data();
        try {
            Signature signer = Signature.getInstance("SHA256withRSA");
            signer.initSign(keys.privateKey());
            signer.update(("shard-auth:" + serverId).getBytes(StandardCharsets.UTF_8));
            JsonObject proof = new JsonObject();
            proof.addProperty("uuid", uuid.toString().replace("-", ""));
            proof.addProperty("publicKey", Base64.getEncoder().encodeToString(data.key().getEncoded()));
            proof.addProperty("keySignature", Base64.getEncoder().encodeToString(data.keySignature()));
            proof.addProperty("expiresAt", data.expiresAt().toEpochMilli());
            proof.addProperty("signature", Base64.getEncoder().encodeToString(signer.sign()));
            return proof;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("could not sign the Shard challenge", e);
        }
    }

    static Me parseMe(JsonObject o) {
        List<String> owned = new ArrayList<>();
        JsonElement list = o.get("owned");
        if (list != null && list.isJsonArray()) for (JsonElement e : (JsonArray) list) owned.add(e.getAsString());
        JsonElement cape = o.get("cape");
        return new Me(o.get("uuid").getAsString(), o.get("name").getAsString(), o.get("tokens").getAsInt(), List.copyOf(owned),
                cape == null || cape.isJsonNull() ? null : cape.getAsString(), o.has("admin") && o.get("admin").getAsBoolean(),
                o.has("secondsToNextTokens") ? o.get("secondsToNextTokens").getAsInt() : 0);
    }

    // ---- HTTP ---------------------------------------------------------------------------------

    private JsonObject get(String path) {
        return send(HttpRequest.newBuilder(URI.create(base() + path)).GET(), false);
    }

    private JsonObject post(String path, JsonObject body, boolean auth) {
        return send(HttpRequest.newBuilder(URI.create(base() + path))
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString())), auth);
    }

    private JsonObject send(HttpRequest.Builder builder, boolean auth) {
        builder.timeout(Duration.ofSeconds(20)).header("User-Agent", "ShardClient");
        if (auth) {
            String token = session;
            if (token == null) throw new IllegalStateException("not signed in to Shard");
            builder.header("Authorization", "Bearer " + token);
        }
        try {
            HttpResponse<String> res = http.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JsonElement parsed = res.body().isEmpty() ? new JsonObject() : JsonParser.parseString(res.body());
            if (res.statusCode() == 401 && auth) session = null; // expired: sign in again next time
            if (res.statusCode() / 100 != 2) {
                String message = parsed.isJsonObject() && parsed.getAsJsonObject().has("error")
                        ? parsed.getAsJsonObject().get("error").getAsString() : "HTTP " + res.statusCode();
                throw new IllegalStateException(message);
            }
            return parsed.getAsJsonObject();
        } catch (IOException e) {
            throw new IllegalStateException("Shard server unreachable (" + e.getMessage() + ")", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted", e);
        }
    }

    /** The API address: dev override, else meta's services.json (cached for offline starts). */
    private String base() {
        String b = base;
        if (b != null) return b;
        String dev = devApi();
        if (dev != null) return base = trim(dev);
        Path cached = cacheDir.resolve("services.json");
        String text = null;
        try {
            HttpResponse<String> res = http.send(HttpRequest.newBuilder(URI.create(SERVICES_URL)).timeout(Duration.ofSeconds(15)).GET().build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (res.statusCode() == 200) {
                text = res.body();
                Files.createDirectories(cacheDir);
                Files.writeString(cached, text, StandardCharsets.UTF_8);
            }
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
        }
        try {
            if (text == null && Files.isRegularFile(cached)) text = Files.readString(cached, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            // fall through
        }
        String api = text == null ? null : apiFromServices(text);
        if (api == null) throw new IllegalStateException("Shard server address not available yet");
        return base = api;
    }

    static String apiFromServices(String json) {
        try {
            JsonElement root = JsonParser.parseString(json);
            if (!root.isJsonObject() || !root.getAsJsonObject().has("api")) return null;
            String api = root.getAsJsonObject().get("api").getAsString();
            return api.startsWith("https://") ? trim(api) : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String trim(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private static String devApi() {
        if (!FabricLoader.getInstance().isDevelopmentEnvironment()) return null;
        String dev = System.getProperty(DEV_API_PROPERTY);
        return dev == null || dev.isBlank() ? null : dev;
    }
}
