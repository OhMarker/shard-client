package gg.shard.client.mixin;

import gg.shard.client.ShardClient;
import gg.shard.client.gui.ScaledScreen;
import gg.shard.client.gui.ScreenScale;
import gg.shard.client.modules.utility.GuiScalesModule;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * GUI Scales, inventory: when a container screen is laid out, its size becomes the virtual size
 * for its own GUI scale, and the factor is stored on the screen so drawing (GameRendererMixin)
 * and the mouse (MouseHandlerScaleMixin) use exactly the same value until the next layout.
 */
@Mixin(Screen.class)
abstract class ScreenScaleMixin implements ScaledScreen {
    @Unique
    private double shard$factor = 1.0;

    @Override
    public double shard$factor() {
        return shard$factor;
    }

    @ModifyVariable(method = "init(II)V", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private int shard$width(int width) {
        shard$factor = ShardClient.isReady() ? ShardClient.modules().get(GuiScalesModule.class).factorFor((Screen) (Object) this) : 1.0;
        return shard$factor == 1.0 ? width : ScreenScale.virtualSize(width, shard$factor);
    }

    @ModifyVariable(method = "init(II)V", at = @At("HEAD"), argsOnly = true, ordinal = 1, require = 0)
    private int shard$height(int height) {
        return shard$factor == 1.0 ? height : ScreenScale.virtualSize(height, shard$factor);
    }

    /**
     * 1.21.11 draws the deferred subtitles, and with them Fabric's mod HUD layers (Shard's HUD),
     * from inside Screen.renderBackground; they must not get the screen's scale.
     */
    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = "renderBackground", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Gui;renderDeferredSubtitles()V"), require = 0)
    private void shard$unscaledSubtitles(net.minecraft.client.gui.Gui gui, com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original,
                                         @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) net.minecraft.client.gui.GuiGraphics g) {
        if (shard$factor == 1.0) {
            original.call(gui);
            return;
        }
        g.pose().pushMatrix();
        g.pose().scale((float) (1.0 / shard$factor), (float) (1.0 / shard$factor));
        original.call(gui);
        g.pose().popMatrix();
    }
}
