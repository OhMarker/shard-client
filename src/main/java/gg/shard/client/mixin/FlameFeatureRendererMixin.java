package gg.shard.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.VertexConsumer;
import gg.shard.client.ShardClient;
import gg.shard.client.modules.visual.LowFireModule;
import net.minecraft.client.renderer.Sheets;
//? if >=1.21.9 {
import net.minecraft.client.renderer.feature.FlameFeatureRenderer;
//?}
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Low Fire, burning players and mobs: the flame column is squashed vertically, tinted, and drawn
 * with the translucent sheet when it should be see-through. Optional hooks.
 */
//? if >=1.21.9 {
@Mixin(FlameFeatureRenderer.class)
//?} else {
/*// Before 1.21.9 the flames are drawn by EntityRenderDispatcher.renderFlame (same hooks).
@Mixin(net.minecraft.client.renderer.entity.EntityRenderDispatcher.class)
*///?}
abstract class FlameFeatureRendererMixin {
    // 26.2 batches the flames: buildGroup gets the sprites and the buffer once, prepare draws one.
    //? if >=26.2 {
    /*@ModifyArg(method = "prepare", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack$Pose;scale(FFF)V"), index = 1, require = 0)
    *///?} else if >=1.21.9 {
    @ModifyArg(method = "renderFlame", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack$Pose;scale(FFF)V"), index = 1, require = 0)
    //?} else {
    /*@ModifyArg(method = "renderFlame", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V"), index = 1, require = 0)
    *///?}
    private float shard$flameHeight(float y) {
        return ShardClient.isReady() ? y * ShardClient.modules().get(LowFireModule.class).entityHeightFactor() : y;
    }

    /** Custom fire texture: Shard's sprites instead of vanilla's FIRE_0 / FIRE_1. */
    //? if <1.21.9 {
    /*@WrapOperation(method = "renderFlame", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/resources/model/Material;sprite()Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;"), require = 0)
    private net.minecraft.client.renderer.texture.TextureAtlasSprite shard$flameSprite(net.minecraft.client.resources.model.Material material,
            Operation<net.minecraft.client.renderer.texture.TextureAtlasSprite> original) {
        if (!ShardClient.isReady()) return original.call(material);
        return original.call(ShardClient.modules().get(LowFireModule.class).fireMaterial(material));
    }
    *///?} else {
    //? if >=26.2 {
    /*@WrapOperation(method = "buildGroup", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/resources/model/AtlasManager;get(Lnet/minecraft/client/resources/model/Material;)Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;"), require = 0)
    *///?} else {
    @WrapOperation(method = "renderFlame", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/resources/model/AtlasManager;get(Lnet/minecraft/client/resources/model/Material;)Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;"), require = 0)
    //?}
    private net.minecraft.client.renderer.texture.TextureAtlasSprite shard$flameSprite(net.minecraft.client.resources.model.AtlasManager atlas,
            net.minecraft.client.resources.model.Material material, Operation<net.minecraft.client.renderer.texture.TextureAtlasSprite> original) {
        if (!ShardClient.isReady()) return original.call(atlas, material);
        return original.call(atlas, ShardClient.modules().get(LowFireModule.class).fireMaterial(material));
    }
    //?}

    //? if >=26.2 {
    /*@WrapOperation(method = "buildGroup", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;entityCutoutCull(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;"), require = 0)
    private RenderType shard$flameSheet(net.minecraft.resources.Identifier atlas, Operation<RenderType> original) {
        if (ShardClient.isReady() && ShardClient.modules().get(LowFireModule.class).entityTranslucent()) return Sheets.translucentBlockItemSheet();
        return original.call(atlas);
    }
    *///?} else {
    @WrapOperation(method = "renderFlame", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/Sheets;cutoutBlockSheet()Lnet/minecraft/client/renderer/rendertype/RenderType;"), require = 0)
    private RenderType shard$flameSheet(Operation<RenderType> original) {
        if (ShardClient.isReady() && ShardClient.modules().get(LowFireModule.class).entityTranslucent()) {
            // The flame sprites are in the block atlas; from 1.21.11 the item sheet uses the separate item atlas.
            //? if >=1.21.11 {
            return Sheets.translucentBlockItemSheet();
            //?} else {
            /*return Sheets.translucentItemSheet();
            *///?}
        }

        return original.call();
    }
    //?}

    @WrapOperation(method = "fireVertex", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;setColor(I)Lcom/mojang/blaze3d/vertex/VertexConsumer;"), require = 0)
    private static VertexConsumer shard$flameColor(VertexConsumer consumer, int color, Operation<VertexConsumer> original) {
        return original.call(consumer, ShardClient.isReady() ? ShardClient.modules().get(LowFireModule.class).entityColor(color) : color);
    }
}
