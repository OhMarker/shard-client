package gg.shard.client.gui;

import gg.shard.client.ShardClient;
import gg.shard.client.hud.HudPresets;
import gg.shard.client.module.Module;

import java.util.List;
import java.util.Set;

/**
 * One-click setups: Crystal PvP Pro (everything a crystal player wants, tuned), Minimal (a
 * small HUD and the essentials) and Recording (clean for videos and streams). Each switches
 * modules on or off and applies a HUD layout; nothing else is touched, and everything can be
 * changed afterwards.
 */
public final class QuickSetup {
    private QuickSetup() {}

    public static final String PRO = "Crystal PvP Pro";
    public static final String MINIMAL = "Minimal";
    public static final String RECORDING = "Recording";

    public record Choice(String name, String description, String icon) {}

    public static List<Choice> choices() {
        return List.of(
                new Choice(PRO, "FPS with 1% low, ping, totems, armour, kit counter, target HUD, cooldowns; crystal and anchor prediction, low fire, small shield, clean screen, crystal spot outline.", "crystal"),
                new Choice(MINIMAL, "FPS, ping and totems. Crystal and anchor prediction and low fire; everything else stays vanilla.", "hud"),
                new Choice(RECORDING, "A clean HUD without coordinates or server, no overlays, the Shard window title.", "screenshot"));
    }

    private static final Set<String> PRO_ON = Set.of("crystal-optimizer", "anchor-optimizer", "totem-pop-tweaks", "crystal-size", "low-fire", "low-shield",
            "clean-screen", "block-outline", "no-hurt-cam", "nametags", "display");
    private static final Set<String> MINIMAL_ON = Set.of("crystal-optimizer", "anchor-optimizer", "low-fire");
    private static final Set<String> RECORDING_ON = Set.of("clean-screen", "display", "crystal-size", "low-fire");

    /** Applies a setup by name; returns false for an unknown name. */
    public static boolean apply(String name) {
        Set<String> on;
        String hud;
        switch (name) {
            case PRO -> {
                on = PRO_ON;
                hud = HudPresets.MINIMAL;
            }
            case MINIMAL -> {
                on = MINIMAL_ON;
                hud = HudPresets.BARE;
            }
            case RECORDING -> {
                on = RECORDING_ON;
                hud = HudPresets.STREAMER;
            }
            default -> {
                return false;
            }
        }
        for (Module m : ShardClient.modules().all()) {
            if (on.contains(m.key()) && !m.isToggledOn()) m.toggle();
        }
        HudPresets.apply(ShardClient.hud(), hud);
        ShardClient.config().markDirty();
        return true;
    }
}
