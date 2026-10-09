package gg.shard.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.state.pip.GuiEntityRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.joml.Matrix3x2f;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * GUI Scales, inventory: 1.21.11 draws the player model in the inventory as a picture-in-picture
 * at the box it is given in plain GUI units, ignoring the pose. A scaled inventory draws under a
 * scaled pose, so the model landed outside its black box. Run the box (and the model's size)
 * through the current pose first; with vanilla's identity pose nothing changes.
 */
@Mixin(GuiGraphics.class)
abstract class GuiGraphicsEntityMixin {
    @WrapOperation(method = "submitEntityRenderState", at = @At(value = "NEW",
            target = "(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lorg/joml/Vector3f;Lorg/joml/Quaternionf;Lorg/joml/Quaternionf;IIIIFLnet/minecraft/client/gui/navigation/ScreenRectangle;)Lnet/minecraft/client/gui/render/state/pip/GuiEntityRenderState;"),
            require = 0)
    private GuiEntityRenderState shard$followPose(EntityRenderState state, Vector3f translation, Quaternionf rotation, Quaternionf camera,
                                                  int x0, int y0, int x1, int y1, float scale, ScreenRectangle scissor,
                                                  Operation<GuiEntityRenderState> original) {
        Matrix3x2f pose = ((GuiGraphics) (Object) this).pose();
        if (pose.m00 == 1f && pose.m11 == 1f && pose.m20 == 0f && pose.m21 == 0f) {
            return original.call(state, translation, rotation, camera, x0, y0, x1, y1, scale, scissor);
        }
        Vector2f a = pose.transformPosition(x0, y0, new Vector2f());
        Vector2f b = pose.transformPosition(x1, y1, new Vector2f());
        return original.call(state, translation, rotation, camera, Math.round(a.x), Math.round(a.y), Math.round(b.x), Math.round(b.y),
                scale * pose.m00, scissor);
    }
}
