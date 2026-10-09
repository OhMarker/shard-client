package gg.shard.client.cosmetics;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Catalogue v2, equipped slots and the bandana mesh (0.9.0, the OhMarker set). */
class OhMarkerSetTest {
    private static final String META = "https://raw.githubusercontent.com/OhMarker/meta/main/";

    @Test
    void catalogueV2KeepsCapesShieldsAndBandanasButNotBundles() {
        Map<String, PlayerCosmetics.Item> c = PlayerCosmetics.parseCatalogueV2("""
                {"schemaVersion":2,"cosmetics":[
                  {"id":"cape-ohmarker","type":"cape","textureUrl":"https://x/cape.png"},
                  {"id":"shield-ohmarker","type":"shield","textureUrl":"https://x/shield.png"},
                  {"id":"bandana-ohmarker","type":"bandana","textureUrl":"https://x/bandana.png"},
                  {"id":"bundle-ohmarker","type":"bundle","textureUrl":null,"items":["cape-ohmarker"]},
                  {"id":"hat-a","type":"hat","textureUrl":"https://x/h.png"},
                  {"id":"shield-local","type":"shield","textureUrl":"file:///C:/meta/shield.png"},
                  {"id":"bad id!","type":"shield","textureUrl":"https://x/s.png"}
                ]}""", false);
        assertEquals(3, c.size());
        assertEquals("shield", c.get("shield-ohmarker").slot());
        assertEquals("bandana", c.get("bandana-ohmarker").slot());
        assertEquals("cape", c.get("cape-ohmarker").slot());
        assertFalse(c.containsKey("bundle-ohmarker"));
    }

    @Test
    void devCataloguesMayUseLocalFilesAndLoopback() {
        Map<String, PlayerCosmetics.Item> c = PlayerCosmetics.parseCatalogueV2("""
                {"cosmetics":[
                  {"id":"a","type":"shield","textureUrl":"file:///C:/meta/a.png"},
                  {"id":"b","type":"bandana","textureUrl":"http://127.0.0.1:8767/b.png"},
                  {"id":"c","type":"cloak","textureUrl":"http://evil/c.png"}
                ]}""", true);
        assertEquals(2, c.size());
        assertEquals("cape", new PlayerCosmetics.Item("c", "cloak", "https://x").slot());
    }

    @Test
    void v1CatalogueStillParses() {
        Map<String, PlayerCosmetics.Item> c = PlayerCosmetics.parseCatalogueV2(
                "{\"schemaVersion\":1,\"cosmetics\":[{\"id\":\"cape-ohmarker\",\"type\":\"cape\",\"textureUrl\":\"https://x/c.png\"}]}", false);
        assertEquals(Map.of("cape-ohmarker", new PlayerCosmetics.Item("cape-ohmarker", "cape", "https://x/c.png")), c);
    }

    @Test
    void localTextureResolvesNextToTheDevCatalogue() {
        String file = "file:///C:/Users/me/meta/cosmetics-v2.json";
        assertEquals("file:///C:/Users/me/meta/cosmetics/textures/shield-ohmarker.png",
                PlayerCosmetics.localTexture(META + "cosmetics/textures/shield-ohmarker.png", META, file, u -> true));
        // Missing locally, or not on the meta repository: unchanged.
        assertEquals(META + "x.png", PlayerCosmetics.localTexture(META + "x.png", META, file, u -> false));
        assertEquals("https://elsewhere/x.png", PlayerCosmetics.localTexture("https://elsewhere/x.png", META, file, u -> true));
        assertEquals(META + "x.png", PlayerCosmetics.localTexture(META + "x.png", META, "https://cdn/cosmetics-v2.json", u -> true));
    }

    @Test
    void equippedV2HasEverySlotAndOldServersGiveCapes() {
        Map<UUID, PlayerCosmetics.Equipped> m = PlayerCosmetics.parseEquipped(JsonParser.parseString("""
                {"4a5e875e479a43f1bfc16c6bd326643d":{"cape":"cape-ohmarker","shield":"shield-ohmarker","bandana":null},
                 "cc971d24-1e2e-3c7e-9140-8043ba1bd54a":"cape-ohmarker",
                 "5f3c1a2e0000400080000000000000aa":{"cape":null,"shield":null,"bandana":null},
                 "6f3c1a2e0000400080000000000000aa":{"shield":"../../etc"},
                 "nope":{"cape":"cape-ohmarker"}}""").getAsJsonObject());
        assertEquals(2, m.size());
        PlayerCosmetics.Equipped owner = m.get(UUID.fromString("4a5e875e-479a-43f1-bfc1-6c6bd326643d"));
        assertEquals("shield-ohmarker", owner.shield());
        assertEquals("shield-ohmarker", owner.slot("shield"));
        assertNull(owner.bandana());
        PlayerCosmetics.Equipped old = m.get(UUID.fromString("cc971d24-1e2e-3c7e-9140-8043ba1bd54a"));
        assertEquals(new PlayerCosmetics.Equipped("cape-ohmarker", null, null), old);
        assertTrue(PlayerCosmetics.parseEquipped(null).isEmpty());
    }

    @Test
    void accountReadsEquippedSlots() {
        ShardApi.Me me = ShardApi.parseMe(JsonParser.parseString("""
                {"uuid":"cc971d241e2e3c7e91408043ba1bd54a","name":"ShardSmoke","tokens":0,"owned":["shield-ohmarker"],
                 "cape":"cape-ohmarker","equipped":{"cape":"cape-ohmarker","shield":"shield-ohmarker","bandana":"bandana-ohmarker"}}""").getAsJsonObject());
        assertEquals("bandana-ohmarker", me.equipped().bandana());
        ShardApi.Me older = ShardApi.parseMe(JsonParser.parseString("""
                {"uuid":"cc971d241e2e3c7e91408043ba1bd54a","name":"ShardSmoke","tokens":0,"owned":[],"cape":"cape-ohmarker"}""").getAsJsonObject());
        assertEquals(new PlayerCosmetics.Equipped("cape-ohmarker", null, null), older.equipped());
    }

    @Test
    void launcherEquippedJsonMayGainShieldAndBandana(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("equipped.json");
        Files.writeString(file, "{\"schemaVersion\":1,\"equipped\":{\"cape\":\"cape-ohmarker\",\"shield\":\"shield-ohmarker\",\"bandana\":\"bandana-ohmarker\"}}");
        PlayerCosmetics.Equipped e = EquippedCape.readSlots(file);
        assertEquals(new PlayerCosmetics.Equipped("cape-ohmarker", "shield-ohmarker", "bandana-ohmarker"), e);
        assertEquals(dir.resolve("textures").resolve("shield-ohmarker.png"), EquippedCape.textureFor(file, "shield-ohmarker"));
        Files.writeString(file, "{\"equipped\":{\"cape\":\"cape-ohmarker\"}}");
        assertEquals(new PlayerCosmetics.Equipped("cape-ohmarker", null, null), EquippedCape.readSlots(file));
        assertEquals(PlayerCosmetics.Equipped.NONE, EquippedCape.readSlots(dir.resolve("missing.json")));
    }

    @Test
    void textureLayoutsPerSlot() {
        assertNull(PlayerCosmetics.layoutProblem("cape", 4096, 2048, 8192));
        assertNull(PlayerCosmetics.layoutProblem("shield", 2048, 2048, 8192));
        assertNull(PlayerCosmetics.layoutProblem("bandana", 2048, 2048, 8192));
        assertNotNull(PlayerCosmetics.layoutProblem("shield", 2048, 1024, 8192));
        assertNotNull(PlayerCosmetics.layoutProblem("cape", 2048, 2048, 8192));
        assertNotNull(PlayerCosmetics.layoutProblem("bandana", 2000, 2000, 8192));
        assertNotNull(PlayerCosmetics.layoutProblem("bandana", 16384, 16384, 8192));
    }

    // ---- bandana mesh ---------------------------------------------------------------------------

    private static final List<BandanaMesh.Quad> MESH = BandanaMesh.build();

    @Test
    void everyTextureCoordinateIsInsideTheArt() {
        for (BandanaMesh.Quad q : MESH) {
            for (int i = 0; i < 4; i++) {
                assertTrue(q.u(i) >= 0 && q.u(i) <= 1 && q.w(i) >= 0 && q.w(i) <= 1, "uv out of range");
            }
        }
    }

    @Test
    void theShellSitsOutsideTheHatLayerAndOffTheFace() {
        for (BandanaMesh.Quad q : MESH) {
            for (int i = 0; i < 4; i++) {
                float x = q.x(i), y = q.y(i), z = q.z(i);
                boolean outside = Math.abs(x) >= 4.52f || Math.abs(z) >= 4.52f || y <= -8.52f;
                assertTrue(outside, "vertex inside the hat layer: " + x + "," + y + "," + z);
                // In front of the face (z < 0 on the front plane) nothing comes below the hairline.
                if (z <= -4.55f) assertTrue(y <= -6f, "covers the face: y " + y);
            }
        }
    }

    @Test
    void topShowsTheEmblemAndJoinsTheSidesSeamlessly() {
        // Centre of the top = centre of the art (the ringed moon).
        assertEquals(0.5f, (BandanaMesh.TOP_U0 + BandanaMesh.TOP_U1) / 2, 1e-6);
        assertEquals(0.5f, BandanaMesh.topV(0), 1e-6);
        // Back edge shows the art's top, the front edge its bottom: upright for someone in front.
        assertEquals(BandanaMesh.TOP_V0, BandanaMesh.topV(BandanaMesh.E), 1e-6);
        assertEquals(BandanaMesh.TOP_V1, BandanaMesh.topV(-BandanaMesh.E), 1e-6);
        // Every vertex on the top's rim has the same uv as any side/band vertex at that point.
        for (BandanaMesh.Quad a : MESH) {
            for (int i = 0; i < 4; i++) {
                if (a.y(i) != BandanaMesh.TOP || a.ny() != -1) continue;
                for (BandanaMesh.Quad b : MESH) {
                    if (b.ny() == -1 || b.nz() == 1) continue; // other top quads; the back panel has its own crop
                    for (int j = 0; j < 4; j++) {
                        if (b.x(j) == a.x(i) && b.y(j) == a.y(i) && b.z(j) == a.z(i)) {
                            assertEquals(a.u(i), b.u(j), 1e-5, "u seam at " + a.x(i) + "," + a.z(i));
                            assertEquals(a.w(i), b.w(j), 1e-5, "v seam at " + a.x(i) + "," + a.z(i));
                        }
                    }
                }
            }
        }
    }

    @Test
    void topAndBackReadTheSameWayRound() {
        // Seen from the front, art u grows towards +x on top; seen from behind it grows towards -x.
        // Both read left to right for their viewer, so neither is mirrored.
        BandanaMesh.Quad top = MESH.stream().filter(q -> q.ny() == -1).findFirst().orElseThrow();
        BandanaMesh.Quad back = MESH.stream().filter(q -> q.nz() == 1 && q.z(0) == BandanaMesh.E).findFirst().orElseThrow();
        assertTrue(slopeUx(top) > 0);
        assertTrue(slopeUx(back) < 0);
        // The back panel is upright: lower on the head means further down the art.
        for (int i = 0; i < 4; i++) for (int j = 0; j < 4; j++) {
            if (back.y(i) > back.y(j)) assertTrue(back.w(i) > back.w(j));
        }
    }

    private static float slopeUx(BandanaMesh.Quad q) {
        for (int i = 0; i < 4; i++) for (int j = 0; j < 4; j++) {
            if (q.x(i) != q.x(j) && q.y(i) == q.y(j) && q.z(i) == q.z(j)) return (q.u(j) - q.u(i)) / (q.x(j) - q.x(i));
        }
        throw new AssertionError("no horizontal edge");
    }

    @Test
    void sidesDropTowardsTheBackAndTailsHangBehind() {
        assertEquals(BandanaMesh.FRONT_HEM, BandanaMesh.sideHem(-BandanaMesh.E), 1e-5);
        assertEquals(BandanaMesh.BACK_HEM, BandanaMesh.sideHem(BandanaMesh.E), 1e-5);
        float last = Float.NEGATIVE_INFINITY;
        for (float z = -BandanaMesh.E; z <= BandanaMesh.E; z += 0.25f) {
            float hem = BandanaMesh.sideHem(z);
            assertTrue(hem >= last - 1e-6, "hem must only go down towards the back");
            last = hem;
        }
        // Everything below the back hem (knot and tails) is behind the head.
        for (BandanaMesh.Quad q : MESH) {
            for (int i = 0; i < 4; i++) if (q.y(i) > BandanaMesh.BACK_HEM + 1e-4) assertTrue(q.z(i) > BandanaMesh.E - 0.1f);
        }
    }
}
