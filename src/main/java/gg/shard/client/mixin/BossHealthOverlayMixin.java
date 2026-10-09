package gg.shard.client.mixin;

//? if <1.21.4 {
/*import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import gg.shard.client.modules.utility.GuiScalesModule;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.BossHealthOverlay;
import org.spongepowered.asm.mixin.Mixin;

/^* GUI Scales' boss bar size before 1.21.4 (no Fabric HUD layer API; see GuiLayersMixin). ^/
@Mixin(BossHealthOverlay.class)
abstract class BossHealthOverlayMixin {
    @WrapMethod(method = "render")
    private void shard$scale(GuiGraphics g, Operation<Void> original) {
        GuiScalesModule.drawPart(GuiScalesModule.Part.BOSS_BAR, g, () -> original.call(g));
    }
}
*///?}
