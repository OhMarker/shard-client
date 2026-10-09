package gg.shard.client.mixin;

//? if <1.21.11 {
/*import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import gg.shard.client.compat.Lines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/^*
 * Before 1.21.11 the line width is one value per draw (RenderSystem's shader line width, set by
 * the render type's line state). Shard's wide-line types (compat.Lines) swap in their own width.
 ^/
@Mixin(targets = "net.minecraft.client.renderer.RenderType$CompositeRenderType")
abstract class CompositeRenderTypeMixin {
    @ModifyExpressionValue(method = "draw", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/RenderSystem;getShaderLineWidth()F"))
    private float shard$lineWidth(float vanilla) {
        return Lines.lineWidth(vanilla);
    }
}
*///?}
