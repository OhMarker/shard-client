package gg.shard.client.mixin;

import gg.shard.client.ShardClient;
import gg.shard.client.modules.perf.EntityOptimizerModule;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
//? if >=1.21.9 {
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.state.LevelRenderState;
//?} else {
/*import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import gg.shard.client.modules.visual.CrystalTweaksModule;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
*///?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Entity Optimizer: start a fresh per-frame count before vanilla walks the entities to draw
 * (LevelRenderer up to 26.1, the new LevelExtractor from 26.2).
 */
//? if >=26.2 {
/*@Mixin(net.minecraft.client.renderer.extract.LevelExtractor.class)
*///?} else {
@Mixin(LevelRenderer.class)
//?}
abstract class LevelRendererEntitiesMixin {
    //? if >=1.21.9 {
    @Inject(method = "extractVisibleEntities", at = @At("HEAD"), require = 0)
    private void shard$beginEntityFrame(Camera camera, Frustum frustum, DeltaTracker delta, LevelRenderState state, CallbackInfo ci) {
        if (ShardClient.isReady()) ShardClient.modules().get(EntityOptimizerModule.class).beginFrame();
    }
    //?} else {
    /*@Inject(method = "collectVisibleEntities", at = @At("HEAD"), require = 0)
    private void shard$beginEntityFrame(Camera camera, Frustum frustum, java.util.List<Entity> list, CallbackInfoReturnable<Boolean> cir) {
        if (ShardClient.isReady()) ShardClient.modules().get(EntityOptimizerModule.class).beginFrame();
    }

    // Before 1.21.9 the glow outline belongs to the entity, not its render state: crystals with a
    // Crystal Visuals / Crystal Optimizer outline glow in Shard's colour (later versions set the
    // render state's outline colour instead).
    @WrapOperation(method = {"collectVisibleEntities", "renderEntities"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/Minecraft;shouldEntityAppearGlowing(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean shard$crystalGlows(Minecraft mc, Entity entity, Operation<Boolean> original) {
        return original.call(mc, entity) || shard$outline(entity) != 0;
    }

    @WrapOperation(method = "renderEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getTeamColor()I"))
    private int shard$crystalOutline(Entity entity, Operation<Integer> original) {
        int colour = shard$outline(entity);
        return colour != 0 ? colour : original.call(entity);
    }

    private static int shard$outline(Entity entity) {
        if (!(entity instanceof EndCrystal crystal) || !ShardClient.isReady()) return 0;
        return ShardClient.modules().get(CrystalTweaksModule.class).outline(crystal);
    }
    *///?}
}
