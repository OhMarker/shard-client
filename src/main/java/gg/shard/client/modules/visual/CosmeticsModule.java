package gg.shard.client.modules.visual;

import com.mojang.blaze3d.platform.NativeImage;
import gg.shard.client.ShardClient;
import gg.shard.client.cosmetics.CapeTexture;
import gg.shard.client.cosmetics.EquippedCape;
import gg.shard.client.cosmetics.MipChain;
import gg.shard.client.module.Module;
import gg.shard.client.module.ModuleCategory;
import gg.shard.client.module.setting.BoolSetting;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Draws the cape you equipped in Shard Launcher on your own player (third person, and your elytra
 * if you choose). It is a client-side look: other players see your
 * normal Minecraft cape, and nothing is sent to the server.
 */
public final class CosmeticsModule extends Module {
    /** Dev runs without a launcher can point at an equipped.json with this JVM property. */
    public static final String DEV_EQUIPPED_PROPERTY = "shard.dev.equippedPath";

    private final BoolSetting showCape = add(new BoolSetting("Show my cape", "Wear the cape you equipped in Shard Launcher", true));
    private final BoolSetting onElytra = add(new BoolSetting("On elytra too", "Paint your elytra with the cape's elytra artwork", true));

    private volatile ClientAsset.Texture cape;
    private volatile String status = "Not loaded";
    private boolean loading;
    private PlayerSkin lastIn;
    private PlayerSkin lastOut;

    public CosmeticsModule() {
        super("Cosmetics", "Wear the cape you equipped in Shard Launcher.", ModuleCategory.VISUALS);
    }

    @Override
    public String icon() {
        return "cape";
    }

    @Override
    public boolean defaultEnabled() {
        return true;
    }

    @Override
    public String about() {
        return "Shows the cape you equipped on the Cosmetics page of Shard Launcher on your own player: "
                + "in third person and, if you like, on your elytra. High "
                + "resolution capes are drawn with smooth filtering so they stay sharp without flickering. "
                + "It is purely visual and only on your screen: other players see your normal Minecraft "
                + "cape, and nothing is sent to the server.";
    }

    /** One line for the smoke test and logs. */
    public String status() {
        return status;
    }

    private boolean attempted;

    /** Called from the AbstractClientPlayer hook for the local player's skin. */
    public PlayerSkin apply(PlayerSkin skin) {
        ClientAsset.Texture texture = cape;
        if (!isEnabled() || !showCape.get() || texture == null || skin == null) return skin;
        if (skin == lastIn && lastOut != null) return lastOut;
        PlayerSkin patched = new PlayerSkin(skin.body(), texture, onElytra.get() ? texture : skin.elytra(), skin.model(), skin.secure());
        lastIn = skin;
        lastOut = patched;
        return patched;
    }

    @Override
    public void onTick() {
        // Settings can change between frames; drop the cached patched skin.
        lastIn = null;
        lastOut = null;
        // Load once the game is fully up (render device ready, player in a world).
        if (!attempted && Minecraft.getInstance().player != null) {
            attempted = true;
            load();
        }
    }

    /** Reads equipped.json and loads the texture off the render thread; registers it on it. */
    public void load() {
        if (loading || cape != null) return;
        Path equipped = equippedPath();
        if (equipped == null) {
            status = "No launcher info (start the game from Shard Launcher)";
            return;
        }
        EquippedCape item = EquippedCape.read(equipped);
        if (item == null) {
            status = "Nothing equipped on the back";
            return;
        }
        if (!Files.isRegularFile(item.texture())) {
            status = "Texture missing: " + item.texture();
            ShardClient.LOGGER.warn("Cosmetics: {} is equipped but {} does not exist", item.id(), item.texture());
            return;
        }
        loading = true;
        status = "Loading " + item.id();
        CompletableFuture.supplyAsync(() -> {
            try (InputStream in = Files.newInputStream(item.texture()); NativeImage image = NativeImage.read(in)) {
                int w = image.getWidth();
                int h = image.getHeight();
                if (w != h * 2 || w % 64 != 0) throw new IllegalArgumentException("cape textures use the 64x32 layout, got " + w + "x" + h);
                return MipChain.build(image.getPixels(), w, h, 64);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).whenComplete((levels, error) -> Minecraft.getInstance().execute(() -> {
            loading = false;
            if (error != null) {
                status = "Could not load " + item.id() + ": " + error.getMessage();
                ShardClient.LOGGER.warn("Cosmetics: could not load {}", item.texture(), error);
                return;
            }
            register(item, levels);
        }));
    }

    private void register(EquippedCape item, List<MipChain.Level> levels) {
        Identifier id = Identifier.fromNamespaceAndPath(ShardClient.MOD_ID, "cosmetics/cape/" + item.resourceName());
        Minecraft.getInstance().getTextureManager().register(id, new CapeTexture("Shard cape " + item.id(), levels));
        cape = new ClientAsset.ResourceTexture(id, id);
        MipChain.Level base = levels.get(0);
        status = "Wearing " + item.id() + " (" + base.width() + "x" + base.height() + ", " + levels.size() + " mip levels)";
        ShardClient.LOGGER.info("Cosmetics: {}", status);
    }

    private static Path equippedPath() {
        Path fromLauncher = ShardClient.launcherInfo().equippedPath();
        if (fromLauncher != null) return fromLauncher;
        if (!FabricLoader.getInstance().isDevelopmentEnvironment()) return null;
        String dev = System.getProperty(DEV_EQUIPPED_PROPERTY);
        return dev == null || dev.isBlank() ? null : Path.of(dev);
    }
}
