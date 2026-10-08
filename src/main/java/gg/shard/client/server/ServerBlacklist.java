package gg.shard.client.server;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Per-server module rules: "on this address, keep these modules off". Pure Java so it is
 * unit-testable; the game side feeds it the address of the current multiplayer server.
 *
 * <p>Patterns are host names, optionally with a port, and may start with {@code *.} to match a
 * host and every subdomain ({@code *.hypixel.net} matches {@code mc.hypixel.net} and
 * {@code hypixel.net}). Comparison ignores case and the default port 25565.
 */
public final class ServerBlacklist {
    public static final int DEFAULT_PORT = 25565;

    /** One rule: the address pattern and the module keys that are forced off there. */
    public static final class Entry {
        private final String pattern;
        private final Set<String> modules = new LinkedHashSet<>();

        public Entry(String pattern) {
            this.pattern = normalize(pattern);
        }

        public String pattern() {
            return pattern;
        }

        public Set<String> modules() {
            return Collections.unmodifiableSet(modules);
        }

        /** Adds or removes one module from this rule only (the editor's per-rule switches). */
        public void setDisabled(String moduleKey, boolean disabled) {
            if (disabled) modules.add(moduleKey);
            else modules.remove(moduleKey);
        }
    }

    /** A new empty rule for {@code pattern} (kept even while empty so the editor can fill it); null when invalid or present. */
    public Entry add(String pattern) {
        String p = normalize(pattern);
        if (!validPattern(p)) return null;
        for (Entry e : entries) if (e.pattern.equals(p)) return null;
        Entry e = new Entry(p);
        entries.add(e);
        return e;
    }

    /** Replaces a rule's pattern, keeping its modules; false when the new pattern is invalid or taken. */
    public boolean rename(Entry entry, String pattern) {
        String p = normalize(pattern);
        if (!validPattern(p) || !entries.contains(entry)) return false;
        for (Entry e : entries) if (e != entry && e.pattern.equals(p)) return false;
        Entry replacement = new Entry(p);
        replacement.modules.addAll(entry.modules);
        entries.set(entries.indexOf(entry), replacement);
        return true;
    }

    /** Host names, IPs, optional port and a leading {@code *.} wildcard; no spaces or stray characters. */
    public static boolean validPattern(String pattern) {
        String p = normalize(pattern);
        if (p.isEmpty() || p.length() > 253) return false;
        String body = p.startsWith("*.") ? p.substring(2) : p;
        if (body.isEmpty() || body.startsWith("*")) return false;
        return body.matches("[a-z0-9._\\-\\[\\]:]+") && !body.contains("..");
    }

    private final List<Entry> entries = new ArrayList<>();

    public List<Entry> entries() {
        return Collections.unmodifiableList(entries);
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /** Lower-cases, trims and drops an explicit default port. */
    public static String normalize(String address) {
        if (address == null) return "";
        String a = address.trim().toLowerCase(Locale.ROOT);
        String defaultSuffix = ":" + DEFAULT_PORT;
        if (a.endsWith(defaultSuffix)) a = a.substring(0, a.length() - defaultSuffix.length());
        return a;
    }

    static String host(String normalized) {
        int colon = normalized.lastIndexOf(':');
        // IPv6 literals contain colons; only treat the suffix as a port when it is numeric.
        if (colon > 0 && normalized.substring(colon + 1).matches("\\d{1,5}")) return normalized.substring(0, colon);
        return normalized;
    }

    public static boolean matches(String pattern, String address) {
        String p = normalize(pattern);
        String a = normalize(address);
        if (p.isEmpty() || a.isEmpty()) return false;
        boolean patternHasPort = !host(p).equals(p);
        String target = patternHasPort ? a : host(a);
        if (p.startsWith("*.")) {
            String suffix = p.substring(1);
            return target.endsWith(suffix) || target.equals(p.substring(2));
        }
        return target.equals(p);
    }

    /** Module keys that must stay off on {@code address}. */
    public Set<String> modulesFor(String address) {
        Set<String> out = new LinkedHashSet<>();
        for (Entry e : entries) if (matches(e.pattern, address)) out.addAll(e.modules);
        return out;
    }

    public boolean isDisabled(String address, String moduleKey) {
        return modulesFor(address).contains(moduleKey);
    }

    /** Finds the exact-host entry for an address (not wildcard matches), or null. */
    public Entry entryFor(String address) {
        String wanted = host(normalize(address));
        for (Entry e : entries) if (e.pattern.equals(wanted)) return e;
        return null;
    }

    public Entry getOrCreate(String address) {
        Entry e = entryFor(address);
        if (e == null) {
            e = new Entry(host(normalize(address)));
            entries.add(e);
        }
        return e;
    }

    /** Adds or removes a module from every rule matching {@code address}; empty rules are dropped. */
    public void setDisabled(String address, String moduleKey, boolean disabled) {
        if (disabled) {
            getOrCreate(address).modules.add(moduleKey);
        } else {
            for (Entry e : new ArrayList<>(entries)) {
                if (matches(e.pattern, address)) {
                    e.modules.remove(moduleKey);
                    if (e.modules.isEmpty()) entries.remove(e);
                }
            }
        }
    }

    public void remove(Entry entry) {
        entries.remove(entry);
    }

    public Entry add(String pattern, Set<String> modules) {
        Entry e = new Entry(pattern);
        e.modules.addAll(modules);
        entries.add(e);
        return e;
    }

    public JsonArray toJson() {
        JsonArray out = new JsonArray();
        for (Entry e : entries) {
            JsonObject o = new JsonObject();
            o.addProperty("address", e.pattern);
            JsonArray mods = new JsonArray();
            for (String m : e.modules) mods.add(m);
            o.add("modules", mods);
            out.add(o);
        }
        return out;
    }

    public static ServerBlacklist fromJson(JsonElement element) {
        ServerBlacklist out = new ServerBlacklist();
        if (element == null || !element.isJsonArray()) return out;
        for (JsonElement item : element.getAsJsonArray()) {
            if (!item.isJsonObject()) continue;
            JsonObject o = item.getAsJsonObject();
            if (!o.has("address") || !o.get("address").isJsonPrimitive()) continue;
            Entry e = new Entry(o.get("address").getAsString());
            if (e.pattern.isEmpty()) continue;
            if (o.has("modules") && o.get("modules").isJsonArray()) {
                for (JsonElement m : o.getAsJsonArray("modules")) if (m.isJsonPrimitive()) e.modules.add(m.getAsString());
            }
            out.entries.add(e);
        }
        return out;
    }
}
