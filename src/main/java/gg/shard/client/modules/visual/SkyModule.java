package gg.shard.client.modules.visual;

import gg.shard.client.module.Module;
import gg.shard.client.module.ModuleCategory;
import gg.shard.client.module.setting.BoolSetting;
import gg.shard.client.module.setting.ColorSetting;
import gg.shard.client.module.setting.EnumSetting;
import gg.shard.client.module.setting.IntSetting;
import gg.shard.client.module.setting.Labeled;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.dimension.DimensionType;

/**
 * Sky colours for the Overworld: a preset palette or your own sky and horizon colours, still
 * following day and night and dimming in rain. SkyRendererMixin swaps the colour of the sky dome
 * in the extracted render state; AtmosphericFogEnvironmentMixin tints the fog and horizon the
 * same way. The Nether, the End, water and lava fog are left alone. Nothing else changes: light
 * levels, time and weather are vanilla's.
 */
public final class SkyModule extends Module {
    public enum Preset implements Labeled {
        VANILLA("Vanilla", 0, 0, 0, 0),
        SUNSET("Sunset", 0xFF7B5EA7, 0xFFFFA071, 0xFF1D1530, 0xFF3A2238),
        NIGHT("Night", 0xFF1C2A4D, 0xFF34466E, 0xFF05070F, 0xFF0B1122),
        PASTEL("Pastel", 0xFFA9C8F5, 0xFFF7D9EA, 0xFF27284A, 0xFF3A3352),
        OCEAN("Ocean", 0xFF2D86C9, 0xFF94D8EC, 0xFF061A2E, 0xFF0C2B42),
        MINT("Mint", 0xFF6FD3B6, 0xFFD6F6E6, 0xFF0E2925, 0xFF173A33),
        LAVENDER("Lavender", 0xFF9C8CF0, 0xFFE3D7FB, 0xFF1B1533, 0xFF2A2146),
        CHERRY("Cherry blossom", 0xFFF4A3C3, 0xFFFFE3EE, 0xFF2A1424, 0xFF3D1F33),
        GOLDEN("Golden hour", 0xFFF2B155, 0xFFFDE3AA, 0xFF231A10, 0xFF3A2A18),
        CUSTOM("Custom", 0, 0, 0, 0);

        private final String label;
        final int sky;
        final int fog;
        final int nightSky;
        final int nightFog;

        Preset(String label, int sky, int fog, int nightSky, int nightFog) {
            this.label = label;
            this.sky = sky;
            this.fog = fog;
            this.nightSky = nightSky;
            this.nightFog = nightFog;
        }

        @Override
        public String label() {
            return label;
        }
    }

    private final EnumSetting<Preset> preset = add(new EnumSetting<>("Preset", "Colour palette for the sky", Preset.PASTEL).group("Colours"));
    private final ColorSetting skyColor = add(new ColorSetting("Sky colour", "Colour of the sky overhead at midday", 0xFF7AA7FF, false).group("Colours"));
    private final ColorSetting fogColor = add(new ColorSetting("Horizon colour", "Colour of the fog and the sky near the horizon", 0xFFC9DCFF, false).group("Colours"));
    private final BoolSetting dayNight = add(new BoolSetting("Day and night", "Darken the colours at night like vanilla's sky", true).group("Colours")
            .details("Uses the sun's position, so it follows the server's time or the one Weather and Time pins."));
    private final IntSetting strength = add(new IntSetting("Strength", "How strongly the colours replace vanilla's", 100, 10, 100, 5, "%").group("Colours"));
    private final BoolSetting tintFog = add(new BoolSetting("Tint horizon and fog", "Also colour the fog and the horizon, not just the sky overhead", true).group("Colours"));
    private final BoolSetting sunriseGlow = add(new BoolSetting("Sunrise glow", "Keep vanilla's orange glow around the sun at dawn and dusk", true).group("Colours"));

    public SkyModule() {
        super("Sky", "Sky and horizon colours: presets like Sunset, Ocean and Lavender, or your own.", ModuleCategory.VISUALS);
        skyColor.visibleWhen(() -> preset.get() == Preset.CUSTOM);
        fogColor.visibleWhen(() -> preset.get() == Preset.CUSTOM);
    }

    @Override
    public String icon() {
        return "sky";
    }

    @Override
    public String about() {
        return "Recolours the Overworld sky and horizon with a preset palette or your own colours. The colours still darken at night and dim in rain, "
                + "and Strength mixes them with vanilla's. Only the colour of the sky and distant fog changes: light levels, time, weather and what you "
                + "can see are untouched, and the Nether, the End and underwater fog stay vanilla.";
    }

    /** True when the sky of this level should be recoloured. */
    public boolean active(ClientLevel level) {
        return isEnabled() && preset.get() != Preset.VANILLA && level != null && level.dimensionType().skybox() == DimensionType.Skybox.OVERWORLD;
    }

    public boolean tintsFog() {
        return tintFog.get();
    }

    public boolean keepsSunriseGlow() {
        return sunriseGlow.get();
    }

    private float light(float sunAngle) {
        return dayNight.get() ? SkyPalette.daylight(sunAngle) : 1f;
    }

    /** The sky dome colour replacing {@code vanilla}; {@code sunAngle} in radians. */
    public int sky(int vanilla, float sunAngle, float rain, float thunder) {
        return SkyPalette.lerp(strength.get() / 100f, vanilla, ownSky(sunAngle, rain, thunder));
    }

    /** The module's own sky colour, before Strength mixes in vanilla's. */
    public int ownSky(float sunAngle, float rain, float thunder) {
        Preset p = preset.get();
        int day = p == Preset.CUSTOM ? skyColor.get() : p.sky;
        int night = p == Preset.CUSTOM ? SkyPalette.nightOf(day) : p.nightSky;
        return SkyPalette.at(day, night, light(sunAngle), rain, thunder);
    }

    /** The horizon / fog colour before vanilla mixes it with the sky; {@code sunAngle} in radians. */
    public int horizon(float sunAngle, float rain, float thunder) {
        Preset p = preset.get();
        int day = p == Preset.CUSTOM ? fogColor.get() : p.fog;
        int night = p == Preset.CUSTOM ? SkyPalette.nightOf(day) : p.nightFog;
        return SkyPalette.at(day, night, light(sunAngle), rain, thunder);
    }

    /** Mixes our final fog colour with vanilla's by Strength. */
    public int blendFog(int vanilla, int mine) {
        return SkyPalette.lerp(strength.get() / 100f, vanilla, mine);
    }
}
