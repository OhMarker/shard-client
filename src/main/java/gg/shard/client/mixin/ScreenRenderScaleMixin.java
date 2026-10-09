package gg.shard.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import gg.shard.client.gui.ScaledScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * GUI Scales, inventory: draw a scaled screen under its pose factor with the mouse divided to
 * match. The screen is drawn by GameRenderer up to 26.1 and by the new Gui (Minecraft.gui) from 26.2.
 */
//? if >=26.2 {
/*@Mixin(targets = "net.minecraft.client.gui.Gui")
*///?} else {
@Mixin(GameRenderer.class)
//?}
abstract class ScreenRenderScaleMixin {
    //? if >=26.2 {
    /*@WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE",
    *///?} else if >=26.1 {
    /*@WrapOperation(method = "extractGui", at = @At(value = "INVOKE",
    *///?} else {
    @WrapOperation(method = "render", at = @At(value = "INVOKE",
    //?}
            target = "Lnet/minecraft/client/gui/screens/Screen;renderWithTooltipAndSubtitles(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"), require = 0)
    private void shard$scaledScreen(Screen screen, GuiGraphics g, int mx, int my, float pt, Operation<Void> original) {
        double f = ScaledScreen.factorOf(screen);
        if (f == 1.0) {
            original.call(screen, g, mx, my, pt);
            return;
        }
        g.pose().pushMatrix();
        g.pose().scale((float) f, (float) f);
        original.call(screen, g, (int) Math.floor(mx / f), (int) Math.floor(my / f), pt);
        g.pose().popMatrix();
    }
}
