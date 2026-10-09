package gg.shard.client.compat;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
//? if >=1.21.9 {
import net.minecraft.client.Minecraft;
//?}

/** Atlas sprites by Material: the AtlasManager from 1.21.9, the Material's own atlas lookup before. */
public final class Atlases {
    private Atlases() {}

    public static TextureAtlasSprite sprite(Material material) {
        //? if >=1.21.9 {
        return Minecraft.getInstance().getAtlasManager().get(material);
        //?} else {
        /*return material.sprite();
        *///?}
    }
}
