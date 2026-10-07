package gg.shard.client.modules.visual;

import gg.shard.client.module.Module;
import gg.shard.client.module.ModuleCategory;

/** Removes the camera tilt when you take damage (the mixin in GameRendererMixin checks this). */
public final class NoHurtCamModule extends Module {
    public NoHurtCamModule() {
        super("No Hurt Cam", "Stops the screen from tilting when you take damage.", ModuleCategory.VISUALS);
    }

    @Override
    public boolean defaultEnabled() {
        return true;
    }
}
