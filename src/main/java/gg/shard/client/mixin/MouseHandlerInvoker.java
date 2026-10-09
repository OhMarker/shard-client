package gg.shard.client.mixin;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Smoke test only: feed a cursor position through the same path as GLFW's callback. */
@Mixin(MouseHandler.class)
public interface MouseHandlerInvoker {
    //? if >=26.3 {
    /*// 26.3 (SDL) also passes the relative motion; the callers' (x, y) get (0, 0) appended.
    @Invoker("onMove")
    void shard$onMove(long window, double x, double y, double xrel, double yrel);
    *///?} else {
    @Invoker("onMove")
    void shard$onMove(long window, double x, double y);
    //?}
}
