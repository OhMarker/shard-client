package gg.shard.client.mixin;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Smoke test only: feed a cursor position through the same path as GLFW's callback. */
@Mixin(MouseHandler.class)
public interface MouseHandlerInvoker {
    @Invoker("onMove")
    void shard$onMove(long window, double x, double y);
}
