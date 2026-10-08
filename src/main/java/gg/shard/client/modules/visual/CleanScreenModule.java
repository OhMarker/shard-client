package gg.shard.client.modules.visual;

import gg.shard.client.module.Module;
import gg.shard.client.module.ModuleCategory;
import gg.shard.client.module.setting.BoolSetting;

/** Clean Screen: hides overlays that cover the fight. Nausea and portal wobble are vanilla's Distortion Effects option. */
public final class CleanScreenModule extends Module {
    private final BoolSetting pumpkin = add(new BoolSetting("Pumpkin overlay", "Hide the carved pumpkin (and other helmet) overlay", true));
    private final BoolSetting powderSnow = add(new BoolSetting("Powder snow", "Hide the frost overlay in powder snow", true));
    private final BoolSetting vignette = add(new BoolSetting("Vignette", "Hide the dark corners vanilla draws", false));
    private final BoolSetting portal = add(new BoolSetting("Portal overlay", "Hide the purple nether portal overlay", true));

    public CleanScreenModule() {
        super("Clean Screen", "Hide the pumpkin, powder snow, portal and vignette overlays.", ModuleCategory.VISUALS);
    }

    @Override
    public String icon() {
        return "eye-off";
    }

    @Override
    public String about() {
        return "Hides full-screen overlays that block your view: the carved pumpkin, powder snow frost, the portal swirl and the vignette. "
                + "For nausea and portal wobble use vanilla's Distortion Effects slider in Accessibility.";
    }

    public boolean hidePumpkin() {
        return isEnabled() && pumpkin.get();
    }

    public boolean hidePowderSnow() {
        return isEnabled() && powderSnow.get();
    }

    public boolean hideVignette() {
        return isEnabled() && vignette.get();
    }

    public boolean hidePortal() {
        return isEnabled() && portal.get();
    }
}
