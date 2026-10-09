package gg.shard.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.VertexConsumer;
import gg.shard.client.ShardClient;
import gg.shard.client.modules.visual.LowFireModule;
import gg.shard.client.modules.visual.TotemPopModule;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/**
 * Low Fire, your screen: vanilla's renderFire translates each flame quad by (±0.24, -0.3, 0) and
 * colours it white with alpha 0.9; the height, colour and opacity are adjusted here. Both hooks
 * are optional so a vanilla change degrades the feature instead of crashing.
 */
@Mixin(ScreenEffectRenderer.class)
abstract class ScreenEffectRendererMixin {
    // 26.2 submits the overlay as custom geometry: submitFire picks the sprite, its lambda places the
    // two quads (Matrix4f.translate) and buildFireQuad passes the packed colour (0.9 alpha white).
    //? if >=26.2 {
    /*/^* Custom fire texture on your screen. ^/
    @org.spongepowered.asm.mixin.injection.ModifyVariable(method = "submitFire", at = @At("HEAD"), argsOnly = true, require = 0)
    private static net.minecraft.client.renderer.texture.TextureAtlasSprite shard$fireSprite(net.minecraft.client.renderer.texture.TextureAtlasSprite sprite) {
        return ShardClient.isReady() ? ShardClient.modules().get(LowFireModule.class).screenSprite(sprite) : sprite;
    }

    @ModifyArg(method = "lambda$submitFire$0", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4f;translate(FFF)Lorg/joml/Matrix4f;"), index = 1, require = 0)
    private static float shard$fireHeight(float y) {
        return ShardClient.isReady() ? ShardClient.modules().get(LowFireModule.class).fireY(y) : y;
    }

    @ModifyArg(method = "buildFireQuad", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/ScreenEffectRenderer;buildSpriteQuad(Lcom/mojang/blaze3d/vertex/VertexConsumer;Lorg/joml/Matrix4f;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;FFFFFI)V"),
            index = 8, require = 0)
    private static int shard$fireColor(int argb) {
        if (!ShardClient.isReady()) return argb;
        float[] c = ShardClient.modules().get(LowFireModule.class).screenColor(
                (argb >> 16 & 0xFF) / 255f, (argb >> 8 & 0xFF) / 255f, (argb & 0xFF) / 255f, (argb >>> 24) / 255f);
        return net.minecraft.util.ARGB.colorFromFloat(c[3], c[0], c[1], c[2]);
    }
    *///?} else {
    /** Custom fire texture on your screen. */
    @org.spongepowered.asm.mixin.injection.ModifyVariable(method = "renderFire", at = @At("HEAD"), argsOnly = true, require = 0)
    private static net.minecraft.client.renderer.texture.TextureAtlasSprite shard$fireSprite(net.minecraft.client.renderer.texture.TextureAtlasSprite sprite) {
        return ShardClient.isReady() ? ShardClient.modules().get(LowFireModule.class).screenSprite(sprite) : sprite;
    }

    @ModifyArg(method = "renderFire", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"), index = 1, require = 0)
    private static float shard$fireHeight(float y) {
        return ShardClient.isReady() ? ShardClient.modules().get(LowFireModule.class).fireY(y) : y;
    }

    @WrapOperation(method = "renderFire", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;setColor(FFFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;"), require = 0)
    private static VertexConsumer shard$fireColor(VertexConsumer consumer, float r, float g, float b, float a, Operation<VertexConsumer> original) {
        if (!ShardClient.isReady()) return original.call(consumer, r, g, b, a);
        float[] c = ShardClient.modules().get(LowFireModule.class).screenColor(r, g, b, a);
        return original.call(consumer, c[0], c[1], c[2], c[3]);
    }
    //?}

    /** Totem Pops, animation size: vanilla scales the floating item by 0.8 on every axis; this multiplies that. */
    @ModifyArgs(method = "renderItemActivationAnimation", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V"), require = 0)
    private void shard$totemSize(Args args) {
        if (!ShardClient.isReady()) return;
        float f = ShardClient.modules().get(TotemPopModule.class).animationScale();
        if (f == 1f) return;
        for (int i = 0; i < 3; i++) args.set(i, args.<Float>get(i) * f);
    }
}
