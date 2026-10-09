package gg.shard.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import gg.shard.client.gui.ScaledScreen;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * GUI Scales, inventory: every mouse position the handler gives a scaled screen is divided by
 * that screen's factor, so clicks, drags, releases, scrolls and hover all match what is drawn.
 */
@Mixin(MouseHandler.class)
abstract class MouseHandlerScaleMixin {
    //? if >=1.21.9 {
    private static MouseButtonEvent shard$scale(Screen screen, MouseButtonEvent e) {
        double f = ScaledScreen.factorOf(screen);
        return f == 1.0 ? e : new MouseButtonEvent(e.x() / f, e.y() / f, e.buttonInfo());
    }

    @WrapOperation(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;Z)Z"), require = 0)
    private boolean shard$clicked(Screen screen, MouseButtonEvent e, boolean dbl, Operation<Boolean> original) {
        return original.call(screen, shard$scale(screen, e), dbl);
    }

    @WrapOperation(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseReleased(Lnet/minecraft/client/input/MouseButtonEvent;)Z"), require = 0)
    private boolean shard$released(Screen screen, MouseButtonEvent e, Operation<Boolean> original) {
        return original.call(screen, shard$scale(screen, e));
    }

    @WrapOperation(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseDragged(Lnet/minecraft/client/input/MouseButtonEvent;DD)Z"), require = 0)
    private boolean shard$dragged(Screen screen, MouseButtonEvent e, double dx, double dy, Operation<Boolean> original) {
        double f = ScaledScreen.factorOf(screen);
        return original.call(screen, shard$scale(screen, e), dx / f, dy / f);
    }
    //?} else {
    /*// Before 1.21.9 the handler passes loose coordinates.
    @WrapOperation(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseClicked(DDI)Z"), require = 0)
    private boolean shard$clicked(Screen screen, double x, double y, int button, Operation<Boolean> original) {
        double f = ScaledScreen.factorOf(screen);
        return original.call(screen, x / f, y / f, button);
    }

    @WrapOperation(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseReleased(DDI)Z"), require = 0)
    private boolean shard$released(Screen screen, double x, double y, int button, Operation<Boolean> original) {
        double f = ScaledScreen.factorOf(screen);
        return original.call(screen, x / f, y / f, button);
    }

    @WrapOperation(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseDragged(DDIDD)Z"), require = 0)
    private boolean shard$dragged(Screen screen, double x, double y, int button, double dx, double dy, Operation<Boolean> original) {
        double f = ScaledScreen.factorOf(screen);
        return original.call(screen, x / f, y / f, button, dx / f, dy / f);
    }
    *///?}

    @WrapOperation(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseMoved(DD)V"), require = 0)
    private void shard$moved(Screen screen, double x, double y, Operation<Void> original) {
        double f = ScaledScreen.factorOf(screen);
        original.call(screen, x / f, y / f);
    }

    @WrapOperation(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseScrolled(DDDD)Z"), require = 0)
    private boolean shard$scrolled(Screen screen, double x, double y, double sx, double sy, Operation<Boolean> original) {
        double f = ScaledScreen.factorOf(screen);
        return original.call(screen, x / f, y / f, sx, sy);
    }
}
