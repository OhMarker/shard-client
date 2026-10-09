package gg.shard.client.mixin;

//? if <1.21.4 {
/*import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import gg.shard.client.hud.HudManager;
import gg.shard.client.modules.utility.GuiScalesModule;
import gg.shard.client.modules.utility.GuiScalesModule.Part;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/^*
 * Before 1.21.4 Fabric API has no HUD layer registry (only HudRenderCallback, drawn at the end of
 * Gui.render outside the layer stack). Shard's HUD becomes the last root layer of Gui's
 * LayeredDraw, as Fabric's addLayer does on 1.21.4/1.21.5, and GUI Scales wraps the same vanilla
 * parts Fabric names there (hotbar with its bars, XP level, scoreboard, tab list, title, action bar).
 ^/
@Mixin(Gui.class)
abstract class GuiLayersMixin {
    @Shadow @Final private LayeredDraw layers;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void shard$hudLayer(CallbackInfo ci) {
        layers.add(HudManager::legacyLayer);
    }

    @WrapMethod(method = "renderHotbarAndDecorations")
    private void shard$scaleHotbar(GuiGraphics g, DeltaTracker delta, Operation<Void> original) {
        GuiScalesModule.drawPart(Part.HOTBAR, g, () -> original.call(g, delta));
    }

    @WrapMethod(method = "renderExperienceLevel")
    private void shard$scaleLevel(GuiGraphics g, DeltaTracker delta, Operation<Void> original) {
        GuiScalesModule.drawPart(Part.HOTBAR, g, () -> original.call(g, delta));
    }

    @WrapMethod(method = "renderScoreboardSidebar")
    private void shard$scaleScoreboard(GuiGraphics g, DeltaTracker delta, Operation<Void> original) {
        GuiScalesModule.drawPart(Part.SCOREBOARD, g, () -> original.call(g, delta));
    }

    @WrapMethod(method = "renderTabList")
    private void shard$scaleTabList(GuiGraphics g, DeltaTracker delta, Operation<Void> original) {
        GuiScalesModule.drawPart(Part.TAB_LIST, g, () -> original.call(g, delta));
    }

    @WrapMethod(method = "renderTitle")
    private void shard$scaleTitle(GuiGraphics g, DeltaTracker delta, Operation<Void> original) {
        GuiScalesModule.drawPart(Part.TITLE, g, () -> original.call(g, delta));
    }

    @WrapMethod(method = "renderOverlayMessage")
    private void shard$scaleOverlayMessage(GuiGraphics g, DeltaTracker delta, Operation<Void> original) {
        GuiScalesModule.drawPart(Part.OVERLAY_MESSAGE, g, () -> original.call(g, delta));
    }
}
*///?}
