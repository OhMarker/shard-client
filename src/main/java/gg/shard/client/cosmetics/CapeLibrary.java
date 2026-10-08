package gg.shard.client.cosmetics;

import com.mojang.blaze3d.platform.NativeImage;
import gg.shard.client.ShardClient;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cape textures: the catalogue (id to picture URL) comes from the meta repository on GitHub and is
 * cached on disk, so a restart without internet still works; each picture is downloaded once,
 * cached, and uploaded once per cape id no matter how many players wear it. Who wears what comes
 * from the Shard API (ShardApi). Downloads never talk to the Minecraft server.
 */
public final class CapeLibrary {
    public static final String META_BASE = "https://raw.githubusercontent.com/OhMarker/meta/main/";
    /** Dev runs can serve their own meta folder (smoke test). */
    public static final String DEV_META_PROPERTY = "shard.dev.metaBase";

    private static final long MAX_JSON = 1 << 20;
    private static final long MAX_PNG = 16L << 20;
    private static final int MAX_WIDTH = 8192;

    private final Path cacheDir;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private volatile Map<String, String> catalogue = Map.of();
    private final Map<String, ClientAsset.Texture> textures = new ConcurrentHashMap<>();
    /** Ids being loaded or that failed (failures are retried after the next list refresh). */
    private final Map<String, Boolean> busy = new ConcurrentHashMap<>();
    private volatile boolean refreshing;
    private volatile String status = "Not loaded";

    public CapeLibrary() {
        this.cacheDir = FabricLoader.getInstance().getConfigDir().resolve("shard").resolve("cosmetics-cache");
    }

    public String status() {
        return status;
    }

    /** The texture for a cape id when ready; starts loading it from the catalogue otherwise. */
    public ClientAsset.Texture texture(String capeId) {
        if (capeId == null) return null;
        ClientAsset.Texture ready = textures.get(capeId);
        if (ready != null) return ready;
        String url = catalogue.get(capeId);
        if (url != null && busy.putIfAbsent(capeId, Boolean.TRUE) == null) {
            CompletableFuture.supplyAsync(() -> downloadTexture(capeId, url)).whenComplete((file, error) -> {
                if (error != null) {
                    ShardClient.LOGGER.warn("Cosmetics: could not download {} from {}", capeId, url, error);
                    return; // stays busy until the next refresh, so a broken URL is not hammered
                }
                loadFile(capeId, file);
            });
        }
        return null;
    }

    /** Loads a cape from a local file (the launcher's equipped cape) once; see {@link #ready}. */
    public void loadLocal(String capeId, Path file) {
        if (busy.putIfAbsent(capeId, Boolean.TRUE) == null) loadFile(capeId, file);
    }

    /** The texture for a cape id if it is already uploaded. */
    public ClientAsset.Texture ready(String capeId) {
        return capeId == null ? null : textures.get(capeId);
    }

    private void loadFile(String capeId, Path file) {
        CompletableFuture.supplyAsync(() -> {
            try (InputStream in = Files.newInputStream(file); NativeImage image = NativeImage.read(in)) {
                int w = image.getWidth();
                int h = image.getHeight();
                if (w != h * 2 || w % 64 != 0 || w > MAX_WIDTH) {
                    throw new IllegalArgumentException("cape textures use the 64x32 layout, got " + w + "x" + h);
                }
                return MipChain.build(image.getPixels(), w, h, 64);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).whenComplete((levels, error) -> Minecraft.getInstance().execute(() -> {
            if (error != null) {
                ShardClient.LOGGER.warn("Cosmetics: could not load {} from {}", capeId, file, error);
                return;
            }
            Identifier id = Identifier.fromNamespaceAndPath(ShardClient.MOD_ID,
                    "cosmetics/cape/" + EquippedCape.safeId(capeId).toLowerCase(Locale.ROOT));
            Minecraft.getInstance().getTextureManager().register(id, new CapeTexture("Shard cape " + capeId, levels));
            textures.put(capeId, new ClientAsset.ResourceTexture(id, id));
            ShardClient.LOGGER.info("Cosmetics: {} ready ({}x{}, {} mip levels)", capeId,
                    levels.get(0).width(), levels.get(0).height(), levels.size());
        }));
    }

    public Path cacheDir() {
        return cacheDir;
    }

    /** Re-reads the catalogue (falls back to the cached copy when offline). */
    public void refresh() {
        if (refreshing) return;
        refreshing = true;
        CompletableFuture.runAsync(() -> {
            try {
                String base = metaBase();
                String catalogueJson = fetchJson(base + "cosmetics.json", "cosmetics.json");
                if (catalogueJson != null) catalogue = Map.copyOf(PlayerCosmetics.parseCatalogue(catalogueJson, !META_BASE.equals(base)));
                // Retry anything that failed last time.
                busy.keySet().removeIf(id -> !textures.containsKey(id));
                status = catalogue.size() + " cape" + (catalogue.size() == 1 ? "" : "s") + " in the catalogue";
                ShardClient.LOGGER.info("Cosmetics: {}", status);
            } catch (RuntimeException e) {
                status = "Cape catalogue unavailable: " + e.getMessage();
                ShardClient.LOGGER.warn("Cosmetics: could not refresh the cape catalogue", e);
            } finally {
                refreshing = false;
            }
        });
    }

    private static String metaBase() {
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            String dev = System.getProperty(DEV_META_PROPERTY);
            if (dev != null && !dev.isBlank()) return dev.endsWith("/") ? dev : dev + "/";
        }
        return META_BASE;
    }

    /** Downloads a JSON file and caches it; returns the cached copy when the download fails. */
    private String fetchJson(String url, String cacheName) {
        Path cached = cacheDir.resolve(cacheName);
        try {
            byte[] body = get(url, MAX_JSON);
            String text = new String(body, StandardCharsets.UTF_8);
            writeAtomic(cached, body);
            return text;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            ShardClient.LOGGER.info("Cosmetics: {} unavailable ({}); using the cached copy", url, e.getMessage());
            try {
                return Files.isRegularFile(cached) ? Files.readString(cached, StandardCharsets.UTF_8) : null;
            } catch (IOException ignored) {
                return null;
            }
        }
    }

    /** The cached PNG for a cape, downloaded again when its URL changed. */
    private Path downloadTexture(String capeId, String url) {
        String safe = EquippedCape.safeId(capeId);
        Path png = cacheDir.resolve("textures").resolve(safe + ".png");
        Path source = cacheDir.resolve("textures").resolve(safe + ".url");
        try {
            if (Files.isRegularFile(png) && Files.isRegularFile(source)
                    && Files.readString(source, StandardCharsets.UTF_8).equals(url)) {
                return png;
            }
            byte[] body = get(url, MAX_PNG);
            writeAtomic(png, body);
            writeAtomic(source, url.getBytes(StandardCharsets.UTF_8));
            return png;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private byte[] get(String url, long maxBytes) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("User-Agent", "ShardClient (+https://github.com/OhMarker/shard-client)")
                .GET()
                .build();
        HttpResponse<InputStream> response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
        try (InputStream in = response.body()) {
            if (response.statusCode() != 200) throw new IOException("HTTP " + response.statusCode() + " from " + url);
            byte[] body = in.readNBytes((int) maxBytes + 1);
            if (body.length > maxBytes) throw new IOException(url + " is larger than " + maxBytes + " bytes");
            return body;
        }
    }

    private static void writeAtomic(Path target, byte[] bytes) throws IOException {
        Files.createDirectories(target.getParent());
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        Files.copy(new ByteArrayInputStream(bytes), tmp, StandardCopyOption.REPLACE_EXISTING);
        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
}
