package gg.shard.client.mixin;

//? if <1.21.6 {
/*import gg.shard.client.compat.Lines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/^*
 * Before 1.21.6 the render type's line state sets RenderSystem's line width in its setup lambda
 * (method_23554, read into the shader's LineWidth uniform when the draw binds it), not in
 * CompositeRenderType.draw. Shard's wide-line types (compat.Lines) swap in their own width there.
 ^/
@Mixin(targets = "net.minecraft.client.renderer.RenderStateShard$LineStateShard")
abstract class LineStateShardMixin {
    @ModifyArg(method = "method_23554", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;lineWidth(F)V"))
    private static float shard$lineWidth(float vanilla) {
        return Lines.lineWidth(vanilla);
    }
}
*///?}
