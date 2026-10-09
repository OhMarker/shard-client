package gg.shard.client.mixin;

// Render states arrived in 1.21.2 (BandanaLayer reads the player before).
//? if >=1.21.2 {
import gg.shard.client.render.BandanaState;
//? if >=1.21.9 {
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
//?} else {
/*import net.minecraft.client.renderer.entity.state.PlayerRenderState;
*///?}
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Carries the bandana texture from extraction to BandanaLayer. */
//? if >=1.21.9 {
@Mixin(AvatarRenderState.class)
//?} else {
/*@Mixin(PlayerRenderState.class)
*///?}
abstract class AvatarRenderStateMixin implements BandanaState {
    @Unique private Identifier shard$bandanaTexture;

    @Override
    public Identifier shard$bandana() {
        return shard$bandanaTexture;
    }

    @Override
    public void shard$setBandana(Identifier texture) {
        shard$bandanaTexture = texture;
    }
}
//?}
