package gg.shard.client.modules.settings;

import com.google.gson.JsonObject;
import gg.shard.client.gui.Fonts;
import gg.shard.client.gui.Render2D;
import gg.shard.client.gui.Theme;
import gg.shard.client.module.Module;
import gg.shard.client.module.ModuleCategory;
import gg.shard.client.module.setting.BoolSetting;
import gg.shard.client.module.setting.ColorSetting;
import gg.shard.client.module.setting.EnumSetting;
import gg.shard.client.module.setting.IntSetting;

/**
 * Settings → Appearance. A hidden, always-on module so its settings get persistence, the
 * settings rows and the {@code .set appearance …} command for free. Values are pushed into
 * {@link Theme}, {@link Fonts} and {@link Render2D} whenever they change.
 */
public final class AppearanceModule extends Module {
    public enum FontMode { SMOOTH, VANILLA }

    public final BoolSetting launcherAccent = add(new BoolSetting("Launcher accent", "Use the accent colour Shard Launcher sends with the game", true)
            .details("Switch it off to pick your own accent below. Without the launcher the default is crystal cyan."));
    public final ColorSetting accent = add(new ColorSetting("Accent colour", "The one colour used for active switches, focus, the selected category and the primary button", 0xFF22D3EE, false));
    public final IntSetting interfaceSize = add(new IntSetting("Interface size", "Size of the mod menu and its settings; 100% is the compact size at 1080p", 100, 75, 150, 5, "%")
            .details("The page keeps the same layout at every Minecraft GUI scale; this is the only size knob you need."));
    public final EnumSetting<FontMode> font = add(new EnumSetting<>("Font", "Smooth is Inter, the launcher's font; Vanilla is the Minecraft bitmap font", FontMode.SMOOTH)
            .details("HUD elements follow this choice too. Symbols Inter lacks always fall back to vanilla."));
    public final IntSetting blur = add(new IntSetting("Blur strength", "How much the world behind the page is blurred (0 is off)", 5, 0, 10, 1, ""));
    public final BoolSetting reduceMotion = add(new BoolSetting("Reduce motion", "Snap hover, switches, the panel and popovers into place instead of animating them", false));
    public final BoolSetting smoothCorners = add(new BoolSetting("Smooth corners", "Anti-aliased corner textures; off draws the 0.2.0 stepped fills", true)
            .details("Only turn this off if the rounded corners ever render wrong on your GPU."));

    public AppearanceModule() {
        super("Appearance", "How the settings page and HUD look.", ModuleCategory.HUD);
        accent.visibleWhen(() -> !launcherAccent.get());
        launcherAccent.onChange(v -> apply());
        accent.onChange(v -> apply());
        font.onChange(v -> apply());
        reduceMotion.onChange(v -> apply());
        smoothCorners.onChange(v -> apply());
        apply();
    }

    @Override
    public String about() {
        return "The look of Shard's own screens: accent colour, interface size, the font the page and the HUD use, "
                + "background blur, motion and corner rendering. Nothing here changes gameplay.";
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

    @Override
    public void load(JsonObject in, int version) {
        super.load(in, version);
        apply();
    }

    /** Pushes the current values into the renderers; cheap, so callers may do it every frame. */
    public void apply() {
        Theme.setAccentOverride(launcherAccent.get() ? null : accent.get());
        Fonts.setSmooth(font.get() == FontMode.SMOOTH);
        Theme.setReduceMotion(reduceMotion.get());
        Render2D.setTexturedCorners(smoothCorners.get());
    }

    public double interfaceScale() {
        return interfaceSize.get() / 100.0;
    }
}
