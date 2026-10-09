package gg.shard.client.render;

import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/** Added to AvatarRenderState by AvatarRenderStateMixin: the bandana texture to draw, or null. */
public interface BandanaState {
    @Nullable Identifier shard$bandana();

    void shard$setBandana(@Nullable Identifier texture);
}
