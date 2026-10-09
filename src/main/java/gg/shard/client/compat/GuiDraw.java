package gg.shard.client.compat;

// Before 1.21.6 GuiGraphics draws straight into the frame (in call order, depth-tested) instead of
// collecting render states in strata. nextStratum (stonecutter rule: GuiGraphics.nextStratum calls become
// GuiDraw.nextStratum there) draws what is pending and clears the depth buffer, which is
// what vanilla does between the HUD and the screen: everything after it draws over everything
// before, including items (drawn 150 units towards the camera). The blur (stonecutter rule:
// Screen.renderBlurredBackground calls become GuiDraw.blur) flushes before it post-processes.
//? if <1.21.6 {
/*import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public final class GuiDraw {
    private GuiDraw() {}

    public static void nextStratum(GuiGraphics graphics) {
        graphics.flush();
        //? if >=1.21.5 {
        RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(Minecraft.getInstance().getMainRenderTarget().getDepthTexture(), 1.0);
        //?} else {
        /^RenderSystem.clear(org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT);
        ^///?}
    }

    /^* Screen.renderBlurredBackground: the blur runs on the main target at once, so draw what is pending first. ^/
    public static void blur(GuiGraphics graphics) {
        graphics.flush();
        Minecraft.getInstance().gameRenderer.processBlurEffect();
        //? if <1.21.5 {
        /^Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
        ^///?}
    }

    //? if <1.21.4 {
    /^// GuiGraphics.enableScissor before 1.21.4 ignores the pose; transform the rectangle as 1.21.4 does.
    public static void enableScissor(GuiGraphics graphics, int x0, int y0, int x1, int y1) {
        org.joml.Matrix4f m = graphics.pose().last().pose();
        org.joml.Vector3f a = m.transformPosition(x0, y0, 0f, new org.joml.Vector3f());
        org.joml.Vector3f b = m.transformPosition(x1, y1, 0f, new org.joml.Vector3f());
        int left = net.minecraft.util.Mth.floor(a.x);
        int top = net.minecraft.util.Mth.floor(a.y);
        graphics.enableScissor(left, top, left + net.minecraft.util.Mth.floor(b.x - a.x), top + net.minecraft.util.Mth.floor(b.y - a.y));
    }
    ^///?}
}
*///?}
