package gg.shard.client.modules.visual;

import gg.shard.client.module.Module;
import gg.shard.client.module.ModuleCategory;
import gg.shard.client.module.setting.BoolSetting;
import gg.shard.client.module.setting.EnumSetting;
import gg.shard.client.module.setting.Labeled;

/** Weather and Time: no rain or snow and a fixed time of day, on your screen only. */
public final class WeatherTimeModule extends Module {
    public enum Time implements Labeled {
        SERVER("Server's time", -1), MORNING("Morning", 1000), NOON("Noon", 6000), SUNSET("Sunset", 12500), NIGHT("Night", 18000);

        private final String label;
        final long ticks;

        Time(String label, long ticks) {
            this.label = label;
            this.ticks = ticks;
        }

        @Override
        public String label() {
            return label;
        }
    }

    private final BoolSetting clearWeather = add(new BoolSetting("Clear weather", "No rain, snow or thunder darkness on your screen", true));
    private final EnumSetting<Time> time = add(new EnumSetting<>("Time of day", "Pin the sky and light to a time of day", Time.SERVER));

    public WeatherTimeModule() {
        super("Weather and Time", "Clear weather and a fixed time of day, on your screen only.", ModuleCategory.VISUALS);
    }

    @Override
    public String icon() {
        return "sky";
    }

    @Override
    public String about() {
        return "Hides rain and snow and lets you pin the time of day, for you only. The server's weather and time are untouched, so mobs, crops and other "
                + "players see the real ones; only your sky and light change.";
    }

    public boolean clearWeather() {
        return isEnabled() && clearWeather.get();
    }

    /** The day time to show, or -1 for the server's. */
    public long fixedTime() {
        return isEnabled() ? time.get().ticks : -1;
    }
}
