package gg.shard.client.hud;

import gg.shard.client.module.setting.BoolSetting;
import gg.shard.client.module.setting.ColorSetting;
import gg.shard.client.module.setting.EnumSetting;
import gg.shard.client.module.setting.IntSetting;
import gg.shard.client.module.setting.Setting;
import gg.shard.client.module.setting.StringSetting;

import java.util.function.Supplier;

/**
 * The settings every HUD element shares, declared in the same order everywhere: a style preset,
 * text and value colours, background colour and opacity, corner radius, padding, text shadow,
 * alignment and the label text. Settings → HUD holds one global copy; a module uses the global
 * values until its "Custom style" switch is on. Pure Java so the resolution rules are tested.
 */
public final class HudStyle {
    public enum Preset implements gg.shard.client.module.setting.Labeled {
        CARD("Card"), MINIMAL("Minimal"), OUTLINED("Outlined"), PILL("Pill");

        private final String label;

        Preset(String label) {
            this.label = label;
        }

        @Override
        public String label() {
            return label;
        }
    }

    /** Where the label goes relative to the value: "FPS 240" or "240 FPS". */
    public enum LabelSide implements gg.shard.client.module.setting.Labeled {
        BEFORE("Before"), AFTER("After");

        private final String label;

        LabelSide(String label) {
            this.label = label;
        }

        @Override
        public String label() {
            return label;
        }
    }

    public enum Align { LEFT, CENTER, RIGHT }

    public static final String GROUP = "Style";
    public static final int DEFAULT_TEXT = 0xFFE8ECF4;
    public static final int DEFAULT_VALUE = 0xFFFFFFFF;
    public static final int DEFAULT_BACKGROUND = 0x66000000;

    /** Lets {@code Module.add} (which is protected) register the settings. */
    public interface Registrar {
        <S extends Setting<?>> S add(S setting);
    }

    /** Null for the global defaults. */
    public final BoolSetting custom;
    public final EnumSetting<Preset> preset;
    public final ColorSetting text;
    public final ColorSetting value;
    public final ColorSetting background;
    public final IntSetting radius;
    public final IntSetting padding;
    public final BoolSetting shadow;
    public final EnumSetting<Align> align;
    public final BoolSetting brackets;
    /** Null for the global defaults and for elements that have no label. */
    public final StringSetting label;
    /** Null for the global defaults and for elements that have no label. */
    public final EnumSetting<LabelSide> labelSide;

    /** The values a module actually draws with after inheritance is resolved. */
    public record Resolved(Preset preset, int text, int value, int background, int radius, int padding,
                           boolean shadow, Align align, String label, boolean labelAfter, boolean brackets) {
        public Resolved(Preset preset, int text, int value, int background, int radius, int padding, boolean shadow, Align align, String label) {
            this(preset, text, value, background, radius, padding, shadow, align, label, false, false);
        }

        public Resolved withPreset(Preset p) {
            return new Resolved(p, text, value, background, radius, padding, shadow, align, label, labelAfter, brackets);
        }

        public boolean hasLabel() {
            return label != null && !label.isEmpty();
        }
    }

    private HudStyle(Registrar r, boolean perModule, String defaultLabel, boolean hasAlign) {
        custom = perModule ? r.add(new BoolSetting("Custom style", "Override the HUD defaults from Settings for this element only", false)
                .details("Off keeps every style row below in sync with Settings → HUD.").group(GROUP)) : null;
        Supplier<Boolean> shown = custom == null ? () -> true : custom::get;
        preset = r.add(new EnumSetting<>("Style", "Card fills the background, Minimal is text only, Outlined adds a thin border, Pill is fully rounded", Preset.CARD).group(GROUP));
        preset.visibleWhen(shown);
        text = r.add(new ColorSetting("Text colour", "Colour of labels and plain text", DEFAULT_TEXT).group(GROUP));
        text.visibleWhen(shown);
        value = r.add(new ColorSetting("Value colour", "Colour of the number or value", DEFAULT_VALUE).group(GROUP));
        value.visibleWhen(shown);
        background = r.add(new ColorSetting("Background", "Background colour; the alpha channel is its opacity", DEFAULT_BACKGROUND).group(GROUP));
        background.visibleWhen(shown);
        radius = r.add(new IntSetting("Corner radius", "Rounding of the background box", 3, 0, 16, 1, "").group(GROUP));
        radius.visibleWhen(shown);
        padding = r.add(new IntSetting("Padding", "Space between the box edge and the text", 2, 0, 16, 1, "").group(GROUP));
        padding.visibleWhen(shown);
        shadow = r.add(new BoolSetting("Text shadow", "Vanilla-style drop shadow under the text", false).group(GROUP));
        shadow.visibleWhen(shown);
        align = r.add(new EnumSetting<>("Alignment", "Where multi-line text sits inside the box", Align.LEFT).group(GROUP));
        align.visibleWhen(() -> hasAlign && shown.get());
        brackets = r.add(new BoolSetting("Brackets", "Show the value as [240] instead of 240", false).group(GROUP));
        brackets.visibleWhen(shown);
        label = defaultLabel == null ? null
                : r.add(new StringSetting("Label", "Text shown with the value; leave it empty to hide it", defaultLabel, 24).group(GROUP));
        labelSide = defaultLabel == null ? null
                : r.add(new EnumSetting<>("Label position", "\"FPS 240\" or \"240 FPS\"", LabelSide.BEFORE).group(GROUP));
    }

    /** Per-module style; {@code defaultLabel} null means the element has no label row. */
    public static HudStyle forModule(Registrar r, String defaultLabel, boolean hasAlign) {
        return new HudStyle(r, true, defaultLabel, hasAlign);
    }

    /** The global defaults held by Settings → HUD. */
    public static HudStyle defaults(Registrar r) {
        return new HudStyle(r, false, null, true);
    }

    /** True when this module draws with its own values instead of the defaults. */
    public boolean overrides() {
        return custom == null || custom.get();
    }

    /** Resolves against the global {@code defaults} (null falls back to this style's own values). */
    public Resolved resolve(HudStyle defaults) {
        HudStyle src = overrides() || defaults == null ? this : defaults;
        return new Resolved(src.preset.get(), src.text.get(), src.value.get(), src.background.get(), src.radius.get(),
                src.padding.get(), src.shadow.get(), src.align.get(), label == null ? null : label.get(),
                labelSide != null && labelSide.get() == LabelSide.AFTER, src.brackets.get());
    }

    /** Copies every look setting (not the label text) from {@code from}. */
    public void copyLook(HudStyle from) {
        preset.set(from.preset.get());
        text.set(from.text.get());
        value.set(from.value.get());
        background.set(from.background.get());
        radius.set(from.radius.get());
        padding.set(from.padding.get());
        shadow.set(from.shadow.get());
        align.set(from.align.get());
        brackets.set(from.brackets.get());
    }
}
