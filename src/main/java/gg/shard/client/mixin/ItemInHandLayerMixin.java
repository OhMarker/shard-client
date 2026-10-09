package gg.shard.client.mixin;

//? if <1.21.4 {
/*import com.mojang.blaze3d.vertex.PoseStack;
import gg.shard.client.render.ShieldCosmetics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/^*
 * The shield cosmetic on third-person shields before 1.21.4: the item is drawn from the render
 * state, so the holder the state was extracted from (LivingEntityRenderStateMixin) is handed to
 * ShieldCosmetics while the arm's item draws (ShieldItemRendererMixin reads it).
 ^/
@Mixin(ItemInHandLayer.class)
abstract class ItemInHandLayerMixin {
    @Inject(method = "renderArmWithItem", at = @At("HEAD"))
    private void shard$holder(LivingEntityRenderState state, BakedModel model, ItemStack stack, ItemDisplayContext context, HumanoidArm arm,
                              PoseStack pose, MultiBufferSource buffers, int light, CallbackInfo ci) {
        ShieldCosmetics.push(((ShieldCosmetics.Holder) state).shard$holder(), context);
    }

    @Inject(method = "renderArmWithItem", at = @At("RETURN"))
    private void shard$holderEnd(LivingEntityRenderState state, BakedModel model, ItemStack stack, ItemDisplayContext context, HumanoidArm arm,
                                 PoseStack pose, MultiBufferSource buffers, int light, CallbackInfo ci) {
        ShieldCosmetics.pop();
    }
}
*///?}
