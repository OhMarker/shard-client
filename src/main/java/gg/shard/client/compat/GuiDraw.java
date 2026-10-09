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
        //?} else if >=1.21.2 {
        /^RenderSystem.clear(org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT);
        ^///?} else {
        /^RenderSystem.clear(org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        ^///?}
    }

    /^* Screen.renderBlurredBackground: the blur runs on the main target at once, so draw what is pending first. ^/
    public static void blur(GuiGraphics graphics) {
        graphics.flush();
        //? if >=1.21.2 {
        Minecraft.getInstance().gameRenderer.processBlurEffect();
        //?} else {
        /^Minecraft.getInstance().gameRenderer.processBlurEffect(Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
        ^///?}
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

    //? if <1.21.2 {
    /^// Before 1.21.2 GuiGraphics blits take neither a render type nor a colour (stonecutter rule:
    // g.blit(GUI_TEXTURED, ...) and g.blitSprite(GUI_TEXTURED, ...) become these, with the same
    // arguments as 1.21.2's). Drawn as vanilla's coloured innerBlit does, with blending.
    public static final Object GUI_TEXTURED = new Object();

    public static void blit(GuiGraphics graphics, Object type, net.minecraft.resources.ResourceLocation texture, int x, int y, float u, float v,
                            int width, int height, int uWidth, int vHeight, int textureWidth, int textureHeight) {
        blit(graphics, type, texture, x, y, u, v, width, height, uWidth, vHeight, textureWidth, textureHeight, -1);
    }

    public static void blit(GuiGraphics graphics, Object type, net.minecraft.resources.ResourceLocation texture, int x, int y, float u, float v,
                            int width, int height, int uWidth, int vHeight, int textureWidth, int textureHeight, int color) {
        quad(graphics, texture, x, y, width, height, u / textureWidth, (u + uWidth) / textureWidth, v / textureHeight, (v + vHeight) / textureHeight, color);
    }

    public static void blitSprite(GuiGraphics graphics, Object type, net.minecraft.client.renderer.texture.TextureAtlasSprite sprite, int x, int y, int width, int height) {
        blitSprite(graphics, type, sprite, x, y, width, height, -1);
    }

    public static void blitSprite(GuiGraphics graphics, Object type, net.minecraft.client.renderer.texture.TextureAtlasSprite sprite, int x, int y, int width, int height, int color) {
        quad(graphics, sprite.atlasLocation(), x, y, width, height, sprite.getU0(), sprite.getU1(), sprite.getV0(), sprite.getV1(), color);
    }

    private static void quad(GuiGraphics graphics, net.minecraft.resources.ResourceLocation texture, int x, int y, int width, int height,
                             float u0, float u1, float v0, float v1, int color) {
        if (width == 0 || height == 0) return;
        graphics.flush();
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionTexColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        org.joml.Matrix4f m = graphics.pose().last().pose();
        com.mojang.blaze3d.vertex.BufferBuilder buffer = com.mojang.blaze3d.vertex.Tesselator.getInstance()
                .begin(com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS, com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_TEX_COLOR);
        buffer.addVertex(m, x, y, 0).setUv(u0, v0).setColor(color);
        buffer.addVertex(m, x, y + height, 0).setUv(u0, v1).setColor(color);
        buffer.addVertex(m, x + width, y + height, 0).setUv(u1, v1).setColor(color);
        buffer.addVertex(m, x + width, y, 0).setUv(u1, v0).setColor(color);
        com.mojang.blaze3d.vertex.BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.disableBlend();
    }
    ^///?}
}
*///?}
