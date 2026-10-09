package gg.shard.client.compat;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;

import java.util.function.Consumer;

/**
 * Screenshot.takeScreenshot with a callback (stonecutter rule: calls become Screenshots.take
 * before 1.21.5, where the screenshot is read back synchronously and returned).
 */
public final class Screenshots {
    private Screenshots() {}

    public static void take(RenderTarget target, Consumer<NativeImage> out) {
        //? if >=1.21.5 {
        net.minecraft.client.Screenshot.takeScreenshot(target, out);
        //?} else {
        /*out.accept(net.minecraft.client.Screenshot.takeScreenshot(target));
        *///?}
    }
}
