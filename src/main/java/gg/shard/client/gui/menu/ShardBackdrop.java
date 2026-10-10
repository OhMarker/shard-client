package gg.shard.client.gui.menu;

import com.mojang.blaze3d.platform.NativeImage;
import gg.shard.client.gui.Theme;
import gg.shard.client.util.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.util.Random;

/**
 * Shard Launcher's backdrop (launcher src/renderer/src/components/layout/Backdrop.tsx) behind the
 * title screen and server list: a blue-black gradient, soft accent glows, and slowly drifting
 * translucent crystal facets in the accent colour. The facet shapes, the glow and the launcher's
 * shard mark are rasterised once into white alpha textures and tinted per draw, so a frame is a
 * gradient plus about twenty textured quads and allocates nothing. Facets live in fractions of the
 * screen, so they keep their place across screens and window sizes.
 */
final class ShardBackdrop {
    private ShardBackdrop() {}

    private static final int TOP = 0xFF0A0E17;
    private static final int MID = 0xFF07090F;
    private static final int BOTTOM = 0xFF05070B;
    /** Facet texture: the shape spans x -1..1 and y -1.6..1.6. */
    private static final int FACET_W = 256;
    private static final int FACET_H = 410;
    private static final int GLOW = 128;
    private static final int LOGO = 256;
    /** Launcher sizes are CSS pixels in a window about this wide. */
    private static final float REF = 1300f;

    private static final Identifier[] FACETS = new Identifier[3];
    private static Identifier glow;
    private static Identifier logo;
    private static int logoAccent;

    /** Position and size in fractions of max(width, height); speed per second. */
    private static float[] fx, fy, fr, frot, fvx, fvy, fvr, falpha;
    private static int[] fsides;
    private static long lastNanos;

    static void render(GuiGraphics g, int w, int h) {
        ensureFacets();
        long now = System.nanoTime();
        float dt = lastNanos == 0 ? 0f : Math.min(0.1f, (now - lastNanos) / 1e9f);
        lastNanos = now;
        int accent = Theme.accent();
        float unit = Math.max(w, h) / REF;

        // Base gradient (#0a0e17 → #07090f at 60% → #05070b).
        int split = Math.round(h * 0.6f);
        g.fillGradient(0, 0, w, split, TOP, MID);
        g.fillGradient(0, split, w, h, MID, BOTTOM);

        // Glows: wide ellipse top-left (CSS), top-right and bottom-left (canvas).
        drawGlow(g, w * 0.2f, h * -0.1f, 1200 * unit, 600 * unit, accent, 0.08f);
        float big = Math.max(w, h);
        drawGlow(g, w * 0.82f, h * 0.1f, big * 0.6f, big * 0.6f, accent, 0.10f);
        drawGlow(g, w * 0.1f, h * 0.95f, big * 0.5f, big * 0.5f, accent, 0.06f);

        boolean move = !Theme.reduceMotion();
        float span = Math.max(w, h);
        for (int i = 0; i < fx.length; i++) {
            float r = fr[i] * span;
            if (move) {
                fx[i] += fvx[i] * dt;
                fy[i] += fvy[i] * dt;
                frot[i] += fvr[i] * dt;
                float px = fx[i] * span;
                float py = fy[i] * span;
                if (px < -r) fx[i] = (w + r) / span;
                if (px > w + r) fx[i] = -r / span;
                if (py < -r * 1.6f) fy[i] = (h + r * 1.6f) / span;
                if (py > h + r * 1.6f) fy[i] = -r * 1.6f / span;
            }
            var pose = g.pose();
            pose.pushMatrix();
            pose.translate(fx[i] * span, fy[i] * span);
            pose.rotate(frot[i]);
            int rw = Math.max(1, Math.round(r * 2));
            int rh = Math.max(1, Math.round(r * 3.2f));
            int color = Colors.withAlpha(accent, Math.round(255 * Math.min(1f, falpha[i] * 1.6f)));
            g.blit(RenderPipelines.GUI_TEXTURED, FACETS[fsides[i] - 3], -rw / 2, -rh / 2, 0f, 0f, rw, rh, FACET_W, FACET_H, FACET_W, FACET_H, color);
            pose.popMatrix();
        }
    }

    private static void drawGlow(GuiGraphics g, float cx, float cy, float rx, float ry, int accent, float alpha) {
        int x = Math.round(cx - rx);
        int y = Math.round(cy - ry);
        int w = Math.round(rx * 2);
        int h = Math.round(ry * 2);
        g.blit(RenderPipelines.GUI_TEXTURED, glowTexture(), x, y, 0f, 0f, w, h, GLOW, GLOW, GLOW, GLOW,
                Colors.withAlpha(accent, Math.round(255 * alpha)));
    }

    /** A soft accent glow centred on ({@code cx}, {@code cy}) with radius {@code r}. */
    static void glow(GuiGraphics g, int cx, int cy, int r, float alpha) {
        drawGlow(g, cx, cy, r, r, Theme.accent(), alpha);
    }

    /** The launcher's shard mark in the current accent, {@code size} units square. */
    static void logo(GuiGraphics g, int x, int y, int size, float alpha) {
        int accent = Theme.accent() | 0xFF000000;
        if (logo == null || logoAccent != accent) {
            logo = register("shard", "backdrop/logo", ShardMarkRaster.render(LOGO, accent));
            logoAccent = accent;
        }
        g.blit(RenderPipelines.GUI_TEXTURED, logo, x, y, 0f, 0f, size, size, LOGO, LOGO, LOGO, LOGO,
                Colors.withAlpha(0xFFFFFFFF, Math.round(255 * alpha)));
    }

    // ---- set-up -------------------------------------------------------------------------------

    private static void ensureFacets() {
        if (fx != null) return;
        int n = 14;
        Random r = new Random(7919);
        fx = new float[n];
        fy = new float[n];
        fr = new float[n];
        frot = new float[n];
        fvx = new float[n];
        fvy = new float[n];
        fvr = new float[n];
        falpha = new float[n];
        fsides = new int[n];
        for (int i = 0; i < n; i++) {
            // Fractions of the long side; the launcher's per-frame speeds at 60 fps, per second.
            fx[i] = r.nextFloat();
            fy[i] = r.nextFloat() * 9f / 16f;
            fr[i] = (40 + r.nextFloat() * 160) / REF;
            frot[i] = r.nextFloat() * (float) Math.PI * 2;
            fvx[i] = (r.nextFloat() - 0.5f) * 0.08f * 60 / REF;
            fvy[i] = (r.nextFloat() - 0.5f) * 0.06f * 60 / REF;
            fvr[i] = (r.nextFloat() - 0.5f) * 0.0008f * 60;
            falpha[i] = 0.025f + r.nextFloat() * 0.05f;
            fsides[i] = 3 + r.nextInt(3);
        }
        for (int s = 3; s <= 5; s++) FACETS[s - 3] = register("shard", "backdrop/facet" + s, facetImage(s));
    }

    private static Identifier glowTexture() {
        if (glow == null) {
            NativeImage image = new NativeImage(GLOW, GLOW, false);
            float c = GLOW / 2f;
            for (int y = 0; y < GLOW; y++) {
                for (int x = 0; x < GLOW; x++) {
                    float d = (float) Math.hypot(x + 0.5f - c, y + 0.5f - c) / c;
                    int a = Math.round(255 * Math.max(0f, 1f - d));
                    image.setPixel(x, y, (a << 24) | 0xFFFFFF);
                }
            }
            glow = register("shard", "backdrop/glow", image);
        }
        return glow;
    }

    /**
     * One facet as the launcher draws it: a polygon with alternating radii 1 and 0.62 (y stretched
     * 1.6x), filled with a diagonal fade (full at top-left, clear at bottom-right) and a 1px edge
     * at three quarters of the fill's peak. Alpha only; 3x3 supersampled.
     */
    static NativeImage facetImage(int sides) {
        float[] px = new float[sides];
        float[] py = new float[sides];
        for (int i = 0; i < sides; i++) {
            double a = (double) i / sides * Math.PI * 2;
            float rr = i % 2 == 0 ? 1f : 0.62f;
            px[i] = (float) Math.cos(a) * rr;
            py[i] = (float) Math.sin(a) * rr * 1.6f;
        }
        NativeImage image = new NativeImage(FACET_W, FACET_H, false);
        float sx = 2f / FACET_W;
        float sy = 3.2f / FACET_H;
        float edge = 1.0f * sx; // one texel
        for (int y = 0; y < FACET_H; y++) {
            for (int x = 0; x < FACET_W; x++) {
                float fill = 0;
                float stroke = 0;
                for (int j = 0; j < 3; j++) {
                    for (int k = 0; k < 3; k++) {
                        float ux = -1f + (x + (k + 0.5f) / 3f) * sx;
                        float uy = -1.6f + (y + (j + 0.5f) / 3f) * sy;
                        if (inside(px, py, ux, uy)) {
                            float t = Math.max(0f, Math.min(1f, (ux + uy + 2f) / 4f));
                            fill += 1f - t;
                        }
                        if (edgeDistance(px, py, ux, uy) < edge * 0.75f) stroke += 1f;
                    }
                }
                fill /= 9f;
                stroke = stroke / 9f * 0.75f;
                float a = stroke + fill * (1f - stroke);
                image.setPixel(x, y, (Math.round(255 * Math.min(1f, a)) << 24) | 0xFFFFFF);
            }
        }
        return image;
    }

    static boolean inside(float[] px, float[] py, float x, float y) {
        boolean in = false;
        for (int i = 0, j = px.length - 1; i < px.length; j = i++) {
            if ((py[i] > y) != (py[j] > y) && x < (px[j] - px[i]) * (y - py[i]) / (py[j] - py[i]) + px[i]) in = !in;
        }
        return in;
    }

    static float edgeDistance(float[] px, float[] py, float x, float y) {
        float best = Float.MAX_VALUE;
        for (int i = 0, j = px.length - 1; i < px.length; j = i++) {
            float dx = px[i] - px[j];
            float dy = py[i] - py[j];
            float len = dx * dx + dy * dy;
            float t = len == 0 ? 0 : Math.max(0, Math.min(1, ((x - px[j]) * dx + (y - py[j]) * dy) / len));
            float ex = px[j] + t * dx - x;
            float ey = py[j] + t * dy - y;
            best = Math.min(best, ex * ex + ey * ey);
        }
        return (float) Math.sqrt(best);
    }

    private static Identifier register(String ns, String path, NativeImage image) {
        Identifier id = Identifier.fromNamespaceAndPath(ns, path);
        //? if >=1.21.5 {
        DynamicTexture texture = new DynamicTexture(() -> "shard " + path, image);
        //?} else {
        /*DynamicTexture texture = new DynamicTexture(image);
        *///?}
        Minecraft.getInstance().getTextureManager().register(id, texture);
        return id;
    }
}
