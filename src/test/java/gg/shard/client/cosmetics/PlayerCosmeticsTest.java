package gg.shard.client.cosmetics;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerCosmeticsTest {
    @Test
    void readsUuidsWithOrWithoutDashes() {
        assertEquals(UUID.fromString("4a5e875e-479a-43f1-bfc1-6c6bd326643d"), PlayerCosmetics.parseUuid("4a5e875e479a43f1bfc16c6bd326643d"));
        assertEquals(UUID.fromString("cc971d24-1e2e-3c7e-9140-8043ba1bd54a"), PlayerCosmetics.parseUuid("CC971D24-1E2E-3C7E-9140-8043BA1BD54A"));
    }

    @Test
    void apiAddressMustBeHttps() {
        assertEquals("https://shard-api.x.workers.dev", ShardApi.apiFromServices("{\"api\":\"https://shard-api.x.workers.dev/\"}"));
        assertNull(ShardApi.apiFromServices("{\"api\":\"http://evil\"}"));
        assertNull(ShardApi.apiFromServices("{}"));
        assertNull(ShardApi.apiFromServices("not json"));
    }

    @Test
    void readsTheAccount() {
        ShardApi.Me me = ShardApi.parseMe(com.google.gson.JsonParser.parseString("""
                {"uuid":"4a5e875e479a43f1bfc16c6bd326643d","name":"OhMarkerr","tokens":1210,"owned":["cape-ohmarker"],
                 "cape":null,"admin":true,"inGame":true,"secondsToNextTokens":420}""").getAsJsonObject());
        assertEquals(1210, me.tokens());
        assertEquals(List.of("cape-ohmarker"), me.owned());
        assertNull(me.cape());
        assertTrue(me.admin());
        assertEquals(420, me.secondsToNextTokens());
    }

    @Test
    void catalogueKeepsOnlyHttpsBackItems() {
        Map<String, String> c = PlayerCosmetics.parseCatalogue("""
                {"cosmetics":[
                  {"id":"cape-ohmarker","type":"cape","textureUrl":"https://raw.githubusercontent.com/OhMarker/meta/main/cosmetics/textures/cape-ohmarker.png"},
                  {"id":"cloak-a","type":"cloak","textureUrl":"https://example.org/a.png"},
                  {"id":"hat-a","type":"hat","textureUrl":"https://example.org/h.png"},
                  {"id":"cape-b","type":"cape","textureUrl":"bundled://cosmetics/textures/b.png"},
                  {"id":"cape-c","type":"cape","textureUrl":"http://insecure/c.png"}
                ]}""");
        assertEquals(2, c.size());
        assertTrue(c.containsKey("cape-ohmarker"));
        assertTrue(c.containsKey("cloak-a"));
    }

    @Test
    void junkIsEmptyNotAnError() {
        assertTrue(PlayerCosmetics.parseCatalogue("{}").isEmpty());
        assertTrue(PlayerCosmetics.parseCatalogue("{\"cosmetics\":3}").isEmpty());
        assertNull(PlayerCosmetics.parseUuid("xyz"));
    }
}
