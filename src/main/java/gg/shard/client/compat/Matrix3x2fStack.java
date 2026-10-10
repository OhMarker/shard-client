package gg.shard.client.compat;

// Before 1.21.6 GuiGraphics.pose() is a 3D PoseStack, not JOML's 2D Matrix3x2fStack. The
// stonecutter rules point the org.joml import here and turn GuiGraphics.pose() calls into Matrix3x2fStack.of
// on those versions, so the GUI code keeps its 2D push/translate/scale calls. The wrapper holds
// no state of its own (every call goes to the graphics' PoseStack), so creating one per call is fine.
//? if <1.21.6 {
/*import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Matrix4f;

public final class Matrix3x2fStack {
    private final PoseStack stack;

    private Matrix3x2fStack(PoseStack stack) {
        this.stack = stack;
    }

    public static Matrix3x2fStack of(GuiGraphics graphics) {
        return new Matrix3x2fStack(graphics.pose());
    }

    public PoseStack poseStack() {
        return stack;
    }

    public Matrix3x2fStack pushMatrix() {
        stack.pushPose();
        return this;
    }

    public Matrix3x2fStack popMatrix() {
        stack.popPose();
        return this;
    }

    public Matrix3x2fStack translate(float x, float y) {
        stack.translate(x, y, 0f);
        return this;
    }

    public Matrix3x2fStack scale(float x, float y) {
        stack.scale(x, y, 1f);
        return this;
    }

    public Matrix3x2fStack scale(float s) {
        return scale(s, s);
    }

    public Matrix3x2fStack rotate(float radians) {
        stack.mulPose(com.mojang.math.Axis.ZP.rotation(radians));
        return this;
    }

    private Matrix4f last() {
        return stack.last().pose();
    }

    // The 2D affine part of the 4x4 pose (column-major: m20/m21 of a 3x2 are the translation).
    public float m00() { return last().m00(); }
    public float m01() { return last().m01(); }
    public float m10() { return last().m10(); }
    public float m11() { return last().m11(); }
    public float m20() { return last().m30(); }
    public float m21() { return last().m31(); }
}
*///?}
