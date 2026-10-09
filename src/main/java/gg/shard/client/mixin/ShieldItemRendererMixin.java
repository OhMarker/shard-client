package gg.shard.client.mixin;

//? if <1.21.4 {
/*import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import gg.shard.client.render.ItemTints;
import gg.shard.client.render.ShieldCosmetics;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.object.equipment.ShieldModel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/^*
 * Before 1.21.4 (no ShieldSpecialRenderer) shields are drawn by BlockEntityWithoutLevelRenderer:
 * the shield cosmetic (holder from ShieldCosmetics, see ItemInHandLayerMixin and
 * ItemInHandRendererMixin) and the Shield module's first-person tint and opacity, as
 * ShieldSpecialRendererMixin does on newer versions. Patterns go through
 * BannerRenderer.renderPatterns (BannerRendererMixin tints those).
 ^/
@Mixin(BlockEntityWithoutLevelRenderer.class)
abstract class ShieldItemRendererMixin {
    @Shadow @Final private ShieldModel shieldModel;

    @Inject(method = "renderByItem", at = @At("HEAD"), cancellable = true)
    private void shard$cosmetic(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay,
                                CallbackInfo ci) {
        if (!stack.is(Items.SHIELD)) return;
        Identifier texture = ShieldCosmetics.currentTexture();
        if (texture == null) return;
        ci.cancel();
        // The cosmetic is drawn in vanilla's 64x64 shield layout on the whole texture (no atlas sprite).
        int color = ItemTints.multiply(-1, ItemTints.shield());
        RenderType type = ItemTints.shieldTranslucent() ? RenderTypes.entityTranslucent(texture) : RenderTypes.entitySolid(texture);
        //? if >=1.21.2 {
        VertexConsumer buffer = ItemRenderer.getFoilBuffer(buffers, type, context == ItemDisplayContext.GUI, stack.hasFoil());
        //?} else {
        /^VertexConsumer buffer = ItemRenderer.getFoilBufferDirect(buffers, type, true, stack.hasFoil());
        ^///?}
        pose.pushPose();
        pose.scale(1.0F, -1.0F, -1.0F);
        shieldModel.handle().render(pose, buffer, light, overlay, color);
        shieldModel.plate().render(pose, buffer, light, overlay, color);
        pose.popPose();
    }

    @WrapOperation(method = "renderByItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/model/object/equipment/ShieldModel;renderType(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;"))
    private RenderType shard$translucentShield(ShieldModel model, Identifier atlas, Operation<RenderType> original) {
        return ItemTints.shieldTranslucent() ? RenderTypes.entityTranslucent(atlas) : original.call(model, atlas);
    }

    @WrapOperation(method = "renderByItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/model/geom/ModelPart;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V"))
    private void shard$tintShield(ModelPart part, PoseStack pose, VertexConsumer buffer, int light, int overlay, Operation<Void> original) {
        int tint = ItemTints.shield();
        if (tint == ItemTints.NONE) original.call(part, pose, buffer, light, overlay);
        else part.render(pose, buffer, light, overlay, ItemTints.multiply(-1, tint));
    }
}
*///?}
