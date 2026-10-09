package gg.shard.client.compat;

import com.mojang.blaze3d.platform.NativeImage;

/**
 * NativeImage pixels in ARGB on every version (stonecutter rule: {@code image.setPixel(...)} and
 * {@code getPixel} become these before 1.21.2, where NativeImage only has the ABGR
 * {@code setPixelRGBA}/{@code getPixelRGBA}).
 */
public final class NativeImages {
    private NativeImages() {}

    public static void setPixel(NativeImage image, int x, int y, int argb) {
        //? if >=1.21.2 {
        image.setPixel(x, y, argb);
        //?} else {
        /*image.setPixelRGBA(x, y, swapRedBlue(argb));
        *///?}
    }

    public static int getPixel(NativeImage image, int x, int y) {
        //? if >=1.21.2 {
        return image.getPixel(x, y);
        //?} else {
        /*return swapRedBlue(image.getPixelRGBA(x, y));
        *///?}
    }

    /** ARGB <-> ABGR (the same swap both ways). */
    public static int swapRedBlue(int c) {
        return (c & 0xFF00FF00) | ((c >> 16) & 0xFF) | ((c & 0xFF) << 16);
    }
}
