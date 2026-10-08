package gg.shard.client.server;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerBlacklistTest {
    @Test
    void normalizesCaseAndDefaultPort() {
        assertEquals("mc.hypixel.net", ServerBlacklist.normalize(" MC.Hypixel.NET:25565 "));
        assertEquals("play.example.org:25566", ServerBlacklist.normalize("play.example.org:25566"));
        assertEquals("", ServerBlacklist.normalize(null));
    }

    @Test
    void matchesExactHostsIgnoringPortUnlessPatternHasOne() {
        assertTrue(ServerBlacklist.matches("mc.hypixel.net", "MC.HYPIXEL.NET:25565"));
        assertTrue(ServerBlacklist.matches("mc.hypixel.net", "mc.hypixel.net:25577"));
        assertFalse(ServerBlacklist.matches("mc.hypixel.net:25577", "mc.hypixel.net:25578"));
        assertTrue(ServerBlacklist.matches("mc.hypixel.net:25577", "mc.hypixel.net:25577"));
        assertFalse(ServerBlacklist.matches("hypixel.net", "mc.hypixel.net"));
    }

    @Test
    void wildcardMatchesSubdomainsAndTheBareHost() {
        assertTrue(ServerBlacklist.matches("*.hypixel.net", "mc.hypixel.net"));
        assertTrue(ServerBlacklist.matches("*.hypixel.net", "hypixel.net"));
        assertFalse(ServerBlacklist.matches("*.hypixel.net", "nothypixel.net"));
        assertFalse(ServerBlacklist.matches("*.hypixel.net", ""));
    }

    @Test
    void setDisabledCreatesAndPrunesEntries() {
        ServerBlacklist b = new ServerBlacklist();
        b.setDisabled("Play.Server.gg:25565", "zoom", true);
        b.setDisabled("play.server.gg", "fullbright", true);
        assertEquals(Set.of("zoom", "fullbright"), b.modulesFor("play.server.gg:25565"));
        assertTrue(b.isDisabled("play.server.gg", "zoom"));
        assertFalse(b.isDisabled("other.server.gg", "zoom"));
        b.setDisabled("play.server.gg", "zoom", false);
        b.setDisabled("play.server.gg", "fullbright", false);
        assertTrue(b.isEmpty(), "empty rules are removed");
    }

    @Test
    void jsonRoundTripKeepsOrderAndWildcards() {
        ServerBlacklist b = new ServerBlacklist();
        b.add("*.hypixel.net", Set.of("zoom"));
        b.setDisabled("mc.other.net", "hitbox", true);
        ServerBlacklist again = ServerBlacklist.fromJson(b.toJson());
        assertEquals(2, again.entries().size());
        assertEquals("*.hypixel.net", again.entries().get(0).pattern());
        assertEquals(Set.of("zoom"), again.modulesFor("mini.hypixel.net"));
        assertEquals(Set.of("hitbox"), again.modulesFor("mc.other.net"));
        assertTrue(ServerBlacklist.fromJson(null).isEmpty());
    }

    @Test
    void editorAddsRenamesAndValidatesPatterns() {
        assertTrue(ServerBlacklist.validPattern("*.hypixel.net"));
        assertTrue(ServerBlacklist.validPattern("play.example.net:25566"));
        assertTrue(ServerBlacklist.validPattern("127.0.0.1"));
        assertFalse(ServerBlacklist.validPattern(""));
        assertFalse(ServerBlacklist.validPattern("*"));
        assertFalse(ServerBlacklist.validPattern("**.x.net"));
        assertFalse(ServerBlacklist.validPattern("bad host.net"));
        assertFalse(ServerBlacklist.validPattern("a..b"));

        ServerBlacklist b = new ServerBlacklist();
        ServerBlacklist.Entry e = b.add("*.Example.net");
        assertEquals("*.example.net", e.pattern());
        assertEquals(null, b.add("*.example.net"), "duplicates are refused");
        assertEquals(null, b.add("not valid"));
        e.setDisabled("zoom", true);
        assertTrue(b.isDisabled("mc.example.net", "zoom"));
        assertTrue(b.rename(e, "play.other.net"));
        assertFalse(b.isDisabled("mc.example.net", "zoom"));
        assertTrue(b.isDisabled("play.other.net", "zoom"), "modules move with the rename");
        assertFalse(b.rename(b.entries().get(0), "bad host"));
        b.add("x.net");
        assertFalse(b.rename(b.entries().get(0), "x.net"), "cannot rename onto another rule");
        b.entries().get(0).setDisabled("zoom", false);
        assertFalse(b.isDisabled("play.other.net", "zoom"));
        assertEquals(2, b.entries().size(), "empty rules stay so the editor can fill them");
    }
}
