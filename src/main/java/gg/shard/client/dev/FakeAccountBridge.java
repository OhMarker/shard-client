package gg.shard.client.dev;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import gg.shard.client.launcher.AccountBridge;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/**
 * A stand-in for Shard Launcher's account bridge (docs/ACCOUNT-SWITCH-API.md) for dev runs
 * ({@code -PfakeBridge}) and unit tests: same endpoints, same Bearer secret and Origin checks, on a
 * random loopback port. Sessions carry a fake access token, so they only work on offline-mode
 * servers. "Add account" appends a new account on the next {@code GET /v1/accounts}, like a sign-in
 * finishing in the launcher. An account named "Expired" answers 409, like a refresh that failed.
 */
public final class FakeAccountBridge implements AutoCloseable {
    public record FakeAccount(String id, String name, String uuid) {}

    private final HttpServer server;
    private final String secret;
    private final List<FakeAccount> accounts = new ArrayList<>();
    private String active;
    private boolean addPending;
    private int added;
    private volatile int sessions;

    public FakeAccountBridge(List<FakeAccount> initial) throws IOException {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        secret = HexFormat.of().formatHex(bytes);
        accounts.addAll(initial);
        active = initial.isEmpty() ? null : initial.get(0).id();
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", this::handle);
        server.start();
    }

    /** Three accounts, the first named like the dev player so the title screen starts on it. */
    public static FakeAccountBridge withDevAccounts(String currentName, UUID currentUuid) throws IOException {
        return new FakeAccountBridge(List.of(
                new FakeAccount("acc-1", currentName, hex(currentUuid)),
                new FakeAccount("acc-2", "OhMarkerr", "4f0e8d1c2b3a49f8a7c6d5e4f3a2b1c0"),
                new FakeAccount("acc-3", "Expired", "0a1b2c3d4e5f40718293a4b5c6d7e8f9")));
    }

    public AccountBridge.Endpoint endpoint() {
        return new AccountBridge.Endpoint("http://127.0.0.1:" + server.getAddress().getPort(), secret);
    }

    public String secret() {
        return secret;
    }

    public synchronized String active() {
        return active;
    }

    public int sessionsHandedOut() {
        return sessions;
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private void handle(HttpExchange ex) throws IOException {
        try (ex) {
            if (ex.getRequestHeaders().containsKey("Origin")) {
                reply(ex, 403, error("forbidden"));
                return;
            }
            if (!("Bearer " + secret).equals(ex.getRequestHeaders().getFirst("Authorization"))) {
                reply(ex, 401, error("unauthorized"));
                return;
            }
            String path = ex.getRequestURI().getPath();
            String method = ex.getRequestMethod();
            synchronized (this) {
                if ("GET".equals(method) && "/v1/accounts".equals(path)) {
                    if (addPending) {
                        addPending = false;
                        added++;
                        accounts.add(new FakeAccount("acc-new-" + added, "NewAccount" + added, hex(UUID.nameUUIDFromBytes(("new" + added).getBytes(StandardCharsets.UTF_8)))));
                    }
                    JsonObject o = new JsonObject();
                    o.addProperty("active", active);
                    JsonArray list = new JsonArray();
                    for (FakeAccount a : accounts) {
                        JsonObject j = new JsonObject();
                        j.addProperty("id", a.id());
                        j.addProperty("name", a.name());
                        j.addProperty("uuid", a.uuid());
                        list.add(j);
                    }
                    o.add("accounts", list);
                    reply(ex, 200, o.toString());
                    return;
                }
                if ("POST".equals(method) && "/v1/accounts/add".equals(path)) {
                    addPending = true;
                    reply(ex, 202, "{}");
                    return;
                }
                if ("POST".equals(method) && path.startsWith("/v1/accounts/") && path.endsWith("/session")) {
                    String id = path.substring("/v1/accounts/".length(), path.length() - "/session".length());
                    FakeAccount account = accounts.stream().filter(a -> a.id().equals(id)).findFirst().orElse(null);
                    if (account == null) {
                        reply(ex, 404, error("unknown account"));
                        return;
                    }
                    if ("Expired".equals(account.name())) {
                        reply(ex, 409, error("sign in again in Shard Launcher"));
                        return;
                    }
                    active = account.id();
                    sessions++;
                    JsonObject o = new JsonObject();
                    o.addProperty("name", account.name());
                    o.addProperty("uuid", account.uuid());
                    o.addProperty("accessToken", "fake-token-" + account.id());
                    reply(ex, 200, o.toString());
                    return;
                }
            }
            reply(ex, 404, error("not found"));
        }
    }

    private static String error(String message) {
        JsonObject o = new JsonObject();
        o.addProperty("error", message);
        return o.toString();
    }

    private static void reply(HttpExchange ex, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static String hex(UUID uuid) {
        return uuid.toString().replace("-", "");
    }
}
