package gg.shard.client.modules.hud;

import gg.shard.client.gui.Theme;
import gg.shard.client.hud.HudModule;
import gg.shard.client.hud.HudStyle;
import gg.shard.client.module.setting.BoolSetting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;

import java.util.Locale;

public final class CoordinatesModule extends HudModule {
    private final BoolSetting direction = add(new BoolSetting("Direction", "Show the facing direction after the position", true));
    private final BoolSetting decimals = add(new BoolSetting("Decimals", "One decimal place instead of whole block coordinates", false));

    public CoordinatesModule() {
        super("Coords", "Your position and facing direction.", 0.01, 0.10);
    }

    @Override
    protected String legacyKey() {
        return "coordinates";
    }

    @Override
    protected String defaultLabel() {
        return "XYZ";
    }

    @Override
    public String about() {
        return "Your own X, Y and Z and the direction you face, as vanilla's F3 shows them. Only your position is read; other players are never located.";
    }

    @Override
    public boolean defaultEnabled() {
        return true;
    }

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        LocalPlayer p = mc().player;
        if (p == null) return;
        String pos = decimals.get()
                ? String.format(Locale.ROOT, "%.1f  %.1f  %.1f", p.getX(), p.getY(), p.getZ())
                : p.getBlockX() + "  " + p.getBlockY() + "  " + p.getBlockZ();
        String dir = direction.get() ? "  " + facing(p.getDirection(), p.getYRot()) : "";
        HudStyle.Resolved st = style();
        String label = labelText(st);
        int pad = st.padding();
        int lw = textW(label);
        int pw = textW(pos);
        int w = pad * 2 + lw + pw + textW(dir);
        int h = pad * 2 + lineH();
        box(g, st, w, h);
        text(g, st, label, pad, pad, st.text());
        text(g, st, pos, pad + lw, pad, st.value());
        if (!dir.isEmpty()) text(g, st, dir, pad + lw + pw, pad, Theme.accent());
        size(w, h);
    }

    static String facing(Direction d, float yaw) {
        return switch (d) {
            case NORTH -> "N (-Z)";
            case SOUTH -> "S (+Z)";
            case EAST -> "E (+X)";
            case WEST -> "W (-X)";
            default -> String.format(Locale.ROOT, "%.0f°", yaw);
        };
    }

    @Override
    public String icon() {
        return "compass";
    }
}
