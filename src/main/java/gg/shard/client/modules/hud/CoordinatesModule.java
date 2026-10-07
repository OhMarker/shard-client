package gg.shard.client.modules.hud;

import gg.shard.client.gui.Render2D;
import gg.shard.client.gui.Theme;
import gg.shard.client.hud.HudModule;
import gg.shard.client.module.setting.BoolSetting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;

import java.util.Locale;

public final class CoordinatesModule extends HudModule {
    private final BoolSetting background = add(new BoolSetting("Background", "Dark backing behind the text", true));
    private final BoolSetting direction = add(new BoolSetting("Direction", "Show the facing direction", true));
    private final BoolSetting decimals = add(new BoolSetting("Decimals", "One decimal place instead of block coordinates", false));

    public CoordinatesModule() {
        super("Coordinates", "Your position and facing direction.", 0.01, 0.10);
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
        String text = pos + dir;
        int w = font().width(text) + 6;
        if (background.get()) Render2D.rounded(g, 0, 0, w, 12, 0x66000000);
        Render2D.text(g, font(), pos, 3, 2, Theme.text(), true);
        if (direction.get()) Render2D.text(g, font(), dir, 3 + font().width(pos), 2, Theme.accent(), true);
        size(w, 12);
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
}
