package gg.shard.client.launcher;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 * The game's side of Shard Launcher's account bridge (docs/ACCOUNT-SWITCH-API.md): a loopback
 * HTTP API the launcher serves while the game runs. Lists the launcher's accounts, hands out a
 * fresh session for one of them and asks the launcher to add an account. Pure Java (no Minecraft
 * classes) so the parsing and the HTTP exchange are unit-tested; every call runs on one daemon
 * thread, never the render thread. The access token it returns is only ever kept in memory.
 */
public final class AccountBridge {
    private static final Pattern SECRET = Pattern.compile("[0-9a-fA-F]{64}");
    private static final Pattern UUID_HEX = Pattern.compile("[0-9a-fA-F]{32}");

    /** Where the bridge listens and the per-launch secret, from launcher-info.json. */
    public record Endpoint(String url, String secret) {
        /**
         * The endpoint described by {@code accountBridge}, or null when it is missing or unsafe: the
         * URL must be plain http on the loopback interface (the secret must never leave this machine)
         * and the secret 64 hex characters.
         */
        public static Endpoint parse(JsonElement e) {
            if (e == null || !e.isJsonObject()) return null;
            JsonObject o = e.getAsJsonObject();
            String url = str(o, "url");
            String secret = str(o, "secret");
            if (url == null || secret == null || !SECRET.matcher(secret).matches()) return null;
            try {
                URI uri = URI.create(url);
                String host = uri.getHost();
                if (!"http".equals(uri.getScheme()) || host == null || uri.getPort() <= 0) return null;
                if (!host.equals("127.0.0.1") && !host.equals("localhost") && !host.equals("[::1]")) return null;
            } catch (IllegalArgumentException ex) {
                return null;
            }
            return new Endpoint(url.endsWith("/") ? url.substring(0, url.length() - 1) : url, secret);
        }

        @Override
        public String toString() {
            return "Endpoint[" + url + "]"; // never log the secret
        }
    }

    /** One launcher account. {@code uuid} is 32 hex characters without dashes. */
    public record Account(String id, String name, String uuid) {
        public UUID profileId() {
            return AccountBridge.uuid(uuid);
        }
    }

    /** The launcher's accounts and which one is selected. */
    public record Accounts(String active, List<Account> accounts) {}

    /** A ready-to-use Minecraft session for one account. */
    public record Session(String name, String uuid, String accessToken, String xuid, String clientId) {
        public UUID profileId() {
            return AccountBridge.uuid(uuid);
        }

        @Override
        public String toString() {
            return "Session[" + name + ", " + uuid + "]"; // never log the token
        }
    }

    /** A refused or failed request, with the launcher's message when it sent one. */
    public static final class BridgeException extends RuntimeException {
        private final int status;

        public BridgeException(int status, String message) {
            super(message);
            this.status = status;
        }

        /** HTTP status, or 0 when the launcher could not be reached. */
        public int status() {
            return status;
        }
    }

    private final Endpoint endpoint;
    private final HttpClient http;
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Shard accounts");
        t.setDaemon(true);
        return t;
    });

    public AccountBridge(Endpoint endpoint) {
        this.endpoint = endpoint;
        // The bridge is on this machine: never use a system proxy for it.
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).proxy(HttpClient.Builder.NO_PROXY).build();
    }

    public Endpoint endpoint() {
        return endpoint;
    }

    /** {@code GET /v1/accounts}. */
    public CompletableFuture<Accounts> accounts() {
        return CompletableFuture.supplyAsync(() -> parseAccounts(send("GET", "/v1/accounts", Duration.ofSeconds(5))), worker);
    }

    /** {@code POST /v1/accounts/<id>/session}: the launcher refreshes the token if needed (can take a while). */
    public CompletableFuture<Session> session(String accountId) {
        String path = "/v1/accounts/" + URLEncoder.encode(accountId, StandardCharsets.UTF_8).replace("+", "%20") + "/session";
        return CompletableFuture.supplyAsync(() -> parseSession(send("POST", path, Duration.ofSeconds(60))), worker);
    }

    /** {@code POST /v1/accounts/add}: the launcher starts its Microsoft sign-in; poll {@link #accounts()}. */
    public CompletableFuture<Void> add() {
        return CompletableFuture.runAsync(() -> send("POST", "/v1/accounts/add", Duration.ofSeconds(5)), worker);
    }

    public void close() {
        worker.shutdownNow();
    }

    private String send(String method, String path, Duration timeout) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint.url() + path))
                .timeout(timeout)
                .header("Authorization", "Bearer " + endpoint.secret())
                .header("User-Agent", "ShardClient")
                .method(method, HttpRequest.BodyPublishers.noBody())
                .build();
        try {
            HttpResponse<String> res = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (res.statusCode() / 100 != 2) throw new BridgeException(res.statusCode(), errorMessage(res.statusCode(), res.body()));
            return res.body();
        } catch (IOException e) {
            throw new BridgeException(0, "Shard Launcher was closed. Keep it open (it can sit in the tray) to switch accounts; reopen the game from it if you closed it.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BridgeException(0, "interrupted");
        }
    }

    // ---- parsing (package-private for tests) ------------------------------------------------

    static Accounts parseAccounts(String json) {
        JsonObject o = object(json);
        List<Account> list = new ArrayList<>();
        JsonElement arr = o.get("accounts");
        if (arr != null && arr.isJsonArray()) {
            for (JsonElement e : (JsonArray) arr) {
                if (!e.isJsonObject()) continue;
                JsonObject a = e.getAsJsonObject();
                String id = str(a, "id");
                String name = str(a, "name");
                String uuid = str(a, "uuid");
                if (id == null || id.isEmpty() || name == null || name.isEmpty() || uuid(uuid) == null) continue;
                list.add(new Account(id, name, normaliseUuid(uuid)));
            }
        }
        return new Accounts(str(o, "active"), List.copyOf(list));
    }

    static Session parseSession(String json) {
        JsonObject o = object(json);
        String name = str(o, "name");
        String uuid = str(o, "uuid");
        String token = str(o, "accessToken");
        if (name == null || name.isEmpty() || uuid(uuid) == null || token == null || token.isEmpty()) {
            throw new BridgeException(200, "Shard Launcher sent an incomplete session");
        }
        return new Session(name, normaliseUuid(uuid), token, blankToNull(str(o, "xuid")), blankToNull(str(o, "clientId")));
    }

    /** The launcher's {@code {"error": "..."}} text, or a plain description of the status. */
    static String errorMessage(int status, String body) {
        try {
            JsonElement parsed = body == null || body.isBlank() ? null : JsonParser.parseString(body);
            if (parsed != null && parsed.isJsonObject()) {
                String error = str(parsed.getAsJsonObject(), "error");
                if (error != null && !error.isBlank()) return error;
            }
        } catch (RuntimeException ignored) {
            // fall through to the status text
        }
        return switch (status) {
            case 401 -> "Shard Launcher refused the request";
            case 404 -> "unknown account";
            case 409 -> "sign in again in Shard Launcher";
            default -> "Shard Launcher answered HTTP " + status;
        };
    }

    /** A UUID from 32 hex characters (dashes tolerated), or null. */
    public static UUID uuid(String hex) {
        if (hex == null) return null;
        String plain = hex.replace("-", "");
        if (!UUID_HEX.matcher(plain).matches()) return null;
        return new UUID(Long.parseUnsignedLong(plain.substring(0, 16), 16), Long.parseUnsignedLong(plain.substring(16), 16));
    }

    private static String normaliseUuid(String hex) {
        return hex.replace("-", "").toLowerCase(Locale.ROOT);
    }

    private static JsonObject object(String json) {
        try {
            JsonElement e = JsonParser.parseString(json == null ? "" : json);
            if (e.isJsonObject()) return e.getAsJsonObject();
        } catch (RuntimeException ignored) {
            // reported below
        }
        throw new BridgeException(200, "Shard Launcher sent an unreadable answer");
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    private static String str(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isString() ? e.getAsString() : null;
    }
}
