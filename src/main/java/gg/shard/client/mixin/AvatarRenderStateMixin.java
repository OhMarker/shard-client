package gg.shard.client.mixin;

import gg.shard.client.render.BandanaState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Carries the bandana texture from extraction to BandanaLayer. */
@Mixin(AvatarRenderState.class)
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
