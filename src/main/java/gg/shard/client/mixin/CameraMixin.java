package gg.shard.client.mixin;

//? if >=26.1 {
/*import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import gg.shard.client.ShardClient;
import gg.shard.client.modules.visual.ZoomModule;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/^* Zoom on 26.1+: the world FOV moved from GameRenderer.getFov to Camera.calculateFov (the hand keeps its own). ^/
@Mixin(Camera.class)
abstract class CameraMixin {
    @ModifyReturnValue(method = "calculateFov", at = @At("RETURN"))
    private float shard$zoomFov(float original) {
        if (!ShardClient.isReady()) return original;
        return ShardClient.modules().get(ZoomModule.class).applyFov(original);
    }
}
*///?}
