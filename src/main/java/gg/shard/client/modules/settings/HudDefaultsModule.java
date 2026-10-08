package gg.shard.client.modules.settings;

import gg.shard.client.hud.HudStyle;
import gg.shard.client.module.Module;
import gg.shard.client.module.ModuleCategory;
import gg.shard.client.module.setting.IntSetting;

/**
 * Settings → HUD: the global HUD scale and the style defaults every HUD element inherits until
 * it switches "Custom style" on. Hidden, always-on module (see {@link AppearanceModule}).
 */
public final class HudDefaultsModule extends Module {
    public final IntSetting hudScale = add(new IntSetting("HUD scale", "Size of every HUD element; 100% looks the same at every GUI scale", 100, 50, 200, 5, "%")
            .details("Per-element scale from the HUD editor multiplies this."));
    private final HudStyle style;

    public HudDefaultsModule() {
        super("HUD Defaults", "Scale and default style for every HUD element.", ModuleCategory.HUD);
        style = HudStyle.defaults(this::add);
    }

    public HudStyle style() {
        return style;
    }

    @Override
    public String about() {
        return "Defaults for every HUD element: background style, colours, corner radius, padding, text shadow and alignment. "
                + "An element with \"Custom style\" switched on keeps its own values instead.";
    }

    @Override
    public boolean hidden() {
        return true;
    }

    @Override
    public boolean defaultEnabled() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void setEnabled(boolean value) {
        // Always on.
    }
}
