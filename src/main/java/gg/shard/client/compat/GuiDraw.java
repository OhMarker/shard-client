package gg.shard.client.compat;

// Before 1.21.6 GuiGraphics draws straight into the frame (in call order, depth-tested) instead of
// collecting render states in strata. nextStratum (stonecutter rule: `g.nextStratum()` becomes
// `GuiDraw.nextStratum(g)` there) draws what is pending and clears the depth buffer, which is
// what vanilla does between the HUD and the screen: everything after it draws over everything
// before, including items (drawn 150 units towards the camera).
//? if <1.21.6 {
/*import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public final class GuiDraw {
    private GuiDraw() {}

    public static void nextStratum(GuiGraphics graphics) {
        graphics.flush();
        RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(Minecraft.getInstance().getMainRenderTarget().getDepthTexture(), 1.0);
    }
}
*///?}
