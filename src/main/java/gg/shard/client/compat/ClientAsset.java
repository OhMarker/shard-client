package gg.shard.client.compat;

// Before 1.21.9 player skins name their cape and elytra textures by id; this stand-in for
// net.minecraft.core.ClientAsset keeps CapeLibrary written against 1.21.9's texture assets (the
// stonecutter replacement points imports of the vanilla class here on older versions).
//? if <1.21.9 {
/*import net.minecraft.resources.Identifier;

public final class ClientAsset {
    private ClientAsset() {}

    public interface Texture {
        Identifier texturePath();
    }

    public record ResourceTexture(Identifier id, Identifier texturePath) implements Texture {}
}
*///?}
