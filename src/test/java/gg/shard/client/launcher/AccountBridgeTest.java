package gg.shard.client.launcher;

import com.google.gson.JsonParser;
import gg.shard.client.dev.FakeAccountBridge;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountBridgeTest {
    private static final String SECRET = "ab".repeat(32);

    @Test
    void parsesTheAccountList() {
        AccountBridge.Accounts a = AccountBridge.parseAccounts("""
                {"active":"m1","accounts":[
                  {"id":"m1","name":"OhMarkerr","uuid":"069A79F444E94726A5BEFCA90E38AAF5"},
                  {"id":"m2","name":"Alt","uuid":"0a1b2c3d-4e5f-4071-8293-a4b5c6d7e8f9"},
                  {"id":"","name":"NoId","uuid":"069a79f444e94726a5befca90e38aaf5"},
                  {"id":"m3","name":"BadUuid","uuid":"xyz"},
                  "garbage"
                ]}""");
        assertEquals("m1", a.active());
        assertEquals(2, a.accounts().size());
        assertEquals("069a79f444e94726a5befca90e38aaf5", a.accounts().get(0).uuid());
        assertEquals(UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5"), a.accounts().get(0).profileId());
        assertEquals("0a1b2c3d4e5f40718293a4b5c6d7e8f9", a.accounts().get(1).uuid());
    }

    @Test
    void emptyOrMissingListIsEmpty() {
        assertTrue(AccountBridge.parseAccounts("{}").accounts().isEmpty());
        assertNull(AccountBridge.parseAccounts("{\"accounts\":[]}").active());
        assertThrows(AccountBridge.BridgeException.class, () -> AccountBridge.parseAccounts("[1,2]"));
        assertThrows(AccountBridge.BridgeException.class, () -> AccountBridge.parseAccounts("not json"));
    }

    @Test
    void parsesASession() {
        AccountBridge.Session s = AccountBridge.parseSession("""
                {"name":"OhMarkerr","uuid":"069a79f444e94726a5befca90e38aaf5","accessToken":"tok","xuid":"","clientId":"cid"}""");
        assertEquals("OhMarkerr", s.name());
        assertEquals("tok", s.accessToken());
        assertNull(s.xuid());
        assertEquals("cid", s.clientId());
        assertTrue(!s.toString().contains("tok"), "the token must never be printed");
        assertThrows(AccountBridge.BridgeException.class, () -> AccountBridge.parseSession("{\"name\":\"A\",\"uuid\":\"069a79f444e94726a5befca90e38aaf5\"}"));
    }

    @Test
    void errorMessagesPreferTheLaunchersText() {
        assertEquals("sign in again in Shard Launcher", AccountBridge.errorMessage(409, "{\"error\":\"sign in again in Shard Launcher\"}"));
        assertEquals("unknown account", AccountBridge.errorMessage(404, ""));
        assertEquals("Shard Launcher answered HTTP 500", AccountBridge.errorMessage(500, "<html>"));
    }

    @Test
    void endpointMustBeLoopbackHttpWithAHexSecret() {
        assertNotNull(endpoint("http://127.0.0.1:53123", SECRET));
        assertEquals("http://localhost:53123", endpoint("http://localhost:53123/", SECRET).url());
        assertNull(endpoint("https://127.0.0.1:53123", SECRET));
        assertNull(endpoint("http://example.com:53123", SECRET));
        assertNull(endpoint("http://192.168.1.4:53123", SECRET));
        assertNull(endpoint("http://127.0.0.1", SECRET));
        assertNull(endpoint("http://127.0.0.1:53123", "short"));
        assertNull(AccountBridge.Endpoint.parse(null));
        assertTrue(!endpoint("http://127.0.0.1:53123", SECRET).toString().contains(SECRET));
    }

    @Test
    void launcherInfoReadsTheBridge() {
        LauncherInfo with = LauncherInfo.fromJson(JsonParser.parseString(
                "{\"accountBridge\":{\"url\":\"http://127.0.0.1:53123\",\"secret\":\"" + SECRET + "\"}}").getAsJsonObject());
        assertEquals("http://127.0.0.1:53123", with.accountBridge().url());
        assertEquals(SECRET, with.accountBridge().secret());
        assertNull(LauncherInfo.fromJson(JsonParser.parseString("{}").getAsJsonObject()).accountBridge());
        assertNull(LauncherInfo.DEFAULTS.accountBridge());
    }

    @Test
    void talksToTheBridge() throws Exception {
        try (FakeAccountBridge fake = new FakeAccountBridge(List.of(
                new FakeAccountBridge.FakeAccount("a1", "Main", "069a79f444e94726a5befca90e38aaf5"),
                new FakeAccountBridge.FakeAccount("a2", "Alt", "0a1b2c3d4e5f40718293a4b5c6d7e8f9"),
                new FakeAccountBridge.FakeAccount("a3", "Expired", "1a1b2c3d4e5f40718293a4b5c6d7e8f9")))) {
            AccountBridge bridge = new AccountBridge(fake.endpoint());
            try {
                AccountBridge.Accounts list = bridge.accounts().join();
                assertEquals("a1", list.active());
                assertEquals(3, list.accounts().size());

                AccountBridge.Session s = bridge.session("a2").join();
                assertEquals("Alt", s.name());
                assertEquals("fake-token-a2", s.accessToken());
                assertEquals("a2", fake.active());

                CompletionException expired = assertThrows(CompletionException.class, () -> bridge.session("a3").join());
                AccountBridge.BridgeException e = assertInstanceOf(AccountBridge.BridgeException.class, expired.getCause());
                assertEquals(409, e.status());
                assertEquals("sign in again in Shard Launcher", e.getMessage());

                CompletionException unknown = assertThrows(CompletionException.class, () -> bridge.session("nope").join());
                assertEquals(404, ((AccountBridge.BridgeException) unknown.getCause()).status());

                bridge.add().join();
                assertEquals(4, bridge.accounts().join().accounts().size());
            } finally {
                bridge.close();
            }
        }
    }

    @Test
    void wrongSecretAndBrowserOriginsAreRefused() throws Exception {
        try (FakeAccountBridge fake = new FakeAccountBridge(List.of())) {
            AccountBridge wrong = new AccountBridge(new AccountBridge.Endpoint(fake.endpoint().url(), SECRET));
            try {
                CompletionException e = assertThrows(CompletionException.class, () -> wrong.accounts().join());
                assertEquals(401, ((AccountBridge.BridgeException) e.getCause()).status());
            } finally {
                wrong.close();
            }
            HttpResponse<String> res = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(fake.endpoint().url() + "/v1/accounts"))
                    .header("Authorization", "Bearer " + fake.secret()).header("Origin", "http://evil.example").GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(403, res.statusCode());
        }
    }

    @Test
    void unreachableLauncherIsReported() {
        AccountBridge bridge = new AccountBridge(new AccountBridge.Endpoint("http://127.0.0.1:1", SECRET));
        try {
            CompletionException e = assertThrows(CompletionException.class, () -> bridge.accounts().join());
            assertEquals(0, ((AccountBridge.BridgeException) e.getCause()).status());
        } finally {
            bridge.close();
        }
    }

    private static AccountBridge.Endpoint endpoint(String url, String secret) {
        return AccountBridge.Endpoint.parse(JsonParser.parseString("{\"url\":\"" + url + "\",\"secret\":\"" + secret + "\"}"));
    }
}
