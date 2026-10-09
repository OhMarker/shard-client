package gg.shard.client.dev;

import com.mojang.blaze3d.platform.NativeImage;
import gg.shard.client.ShardClient;
import gg.shard.client.hud.HudModule;
import gg.shard.client.modules.hud.CoordinatesModule;
import gg.shard.client.modules.hud.CpsModule;
import gg.shard.client.modules.hud.FpsModule;
import gg.shard.client.modules.hud.KeystrokesModule;
import gg.shard.client.modules.hud.PingModule;
import gg.shard.client.modules.hud.ServerAddressModule;
import gg.shard.client.modules.hud.SpeedModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * HUD scale pass (-PsmokeOnly=hudscale): the left-hand HUD column at HUD scale 50/75/100/125%
 * and with individual element scales, each with a 3x crop of the column; then the HUD scale is
 * swept 50..200% one step per tick (twice) while frame times are recorded, to catch hitches.
 */
final class HudScaleSmoke {
    private HudScaleSmoke() {}

    static final int TICKS = 400;
    private static final int[] PERCENTS = {50, 75, 100, 125};
    private static final List<Class<? extends HudModule>> COLUMN = List.of(FpsModule.class, PingModule.class, CoordinatesModule.class,
            CpsModule.class, ServerAddressModule.class, KeystrokesModule.class, SpeedModule.class);

    private static List<HudModule> column() {
        List<HudModule> out = new ArrayList<>();
        for (var c : COLUMN) out.add(ShardClient.modules().get(c));
        return out;
    }

    static void block(Minecraft mc, int t) {
        var hud = ShardClient.hudDefaults();
        if (t == 0) {
            mc.setScreen(null);
            for (HudModule m : column()) {
                m.setEnabled(true);
                m.setScale(1.0);
            }
            hud.hudScale.set(100);
            return;
        }
        // 4 shots, 20 ticks apart, from tick 10.
        if (t >= 10 && t < 90) {
            int i = (t - 10) / 20;
            int local = (t - 10) % 20;
            if (local == 0) hud.hudScale.set(PERCENTS[i]);
            if (local == 15) shotWithCrop(mc, "hudscale-" + PERCENTS[i] + ".png");
            return;
        }
        switch (t) {
            case 90 -> {
                hud.hudScale.set(75);
                List<HudModule> col = column();
                col.get(0).setScale(0.75);
                col.get(1).setScale(1.35);
                col.get(2).setScale(0.6);
                col.get(3).setScale(1.15);
            }
            case 105 -> shotWithCrop(mc, "hudscale-75-elements.png");
            case 110 -> {
                hud.hudScale.set(50);
                for (HudModule m : column()) m.setScale(0.5);
            }
            case 125 -> shotWithCrop(mc, "hudscale-25-effective.png");
            case 130 -> {
                for (HudModule m : column()) m.setScale(1.0);
                hud.hudScale.set(100);
            }
            case 160 -> SmokeTest.startSampling();
            case 191 -> report("steady", SmokeTest.stopSampling());
            default -> {
            }
        }
        // Third sweep with the mod menu open on its Settings tab (where the slider lives).
        if (t == 320) {
            mc.setScreen(new gg.shard.client.gui.ClickGuiScreen(null));
            if (mc.screen instanceof gg.shard.client.gui.ClickGuiScreen g) g.selectTab(gg.shard.client.gui.ClickGuiScreen.Tab.SETTINGS);
        }
        if (t == 340) SmokeTest.startSampling();
        if (t >= 340 && t <= 370) hud.hudScale.set(200 - (t - 340) * 5);
        if (t == 371) {
            report("sweep-menu", SmokeTest.stopSampling());
            hud.hudScale.set(100);
        }
        if (t == 380) mc.setScreen(null);
        // Two sweeps 50..200% in 5% steps, one step per tick; the second revisits sizes already seen.
        for (int sweep = 0; sweep < 2; sweep++) {
            int start = 200 + sweep * 60;
            if (t == start) SmokeTest.startSampling();
            if (t >= start && t <= start + 30) hud.hudScale.set(50 + (t - start) * 5);
            if (t == start + 31) {
                report(sweep == 0 ? "sweep1" : "sweep2", SmokeTest.stopSampling());
                hud.hudScale.set(100);
            }
        }
    }

    private static void report(String name, List<Long> frames) {
        if (frames.isEmpty()) return;
        long max = 0;
        long sum = 0;
        int over = 0;
        for (long f : frames) {
            max = Math.max(max, f);
            sum += f;
            if (f > 25_000_000L) over++;
        }
        ShardClient.LOGGER.info(String.format(Locale.ROOT, "Smoke: HUD scale %s: %d frames, mean %.2f ms, max %.2f ms, %d over 25 ms",
                name, frames.size(), sum / 1e6 / frames.size(), max / 1e6, over));
    }

    /** Full screenshot plus a 3x crop around the HUD column. */
    private static void shotWithCrop(Minecraft mc, String name) {
        int gs = mc.getWindow().getGuiScale();
        int gw = mc.getWindow().getGuiScaledWidth();
        int gh = mc.getWindow().getGuiScaledHeight();
        int x0 = Integer.MAX_VALUE;
        int y0 = Integer.MAX_VALUE;
        int x1 = 0;
        int y1 = 0;
        for (HudModule m : column()) {
            x0 = Math.min(x0, m.pixelX(gw) * gs);
            y0 = Math.min(y0, m.pixelY(gh) * gs);
            x1 = Math.max(x1, (m.pixelX(gw) + m.scaledWidth()) * gs);
            y1 = Math.max(y1, (m.pixelY(gh) + m.scaledHeight()) * gs);
        }
        int cx = Math.max(0, x0 - 6);
        int cy = Math.max(0, y0 - 6);
        int cw = Math.min(420, x1 - cx + 12);
        int ch = Math.min(560, y1 - cy + 12);
        Path out = SmokeTest.outDir();
        Path file = out.resolve(name);
        Screenshot.takeScreenshot(mc.getMainRenderTarget(), image -> {
            try (image) {
                image.writeToFile(file);
                int w = Math.min(cw, image.getWidth() - cx);
                int h = Math.min(ch, image.getHeight() - cy);
                int zoom = 3;
                try (NativeImage z = new NativeImage(w * zoom, h * zoom, false)) {
                    for (int zy = 0; zy < h * zoom; zy++) {
                        for (int zx = 0; zx < w * zoom; zx++) z.setPixel(zx, zy, image.getPixel(cx + zx / zoom, cy + zy / zoom) | 0xFF000000);
                    }
                    z.writeToFile(out.resolve(name.replace(".png", "-zoom.png")));
                }
                ShardClient.LOGGER.info("Smoke: wrote {} (crop {},{} {}x{})", file, cx, cy, w, h);
            } catch (IOException e) {
                ShardClient.LOGGER.error("Smoke: could not write {}", file, e);
            }
        });
    }
}
