package gg.shard.client.modules.hud;

import gg.shard.client.event.ShardEvents;
import gg.shard.client.gui.Render2D;
import gg.shard.client.gui.Theme;
import gg.shard.client.hud.HudModule;
import gg.shard.client.module.setting.BoolSetting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class TotemCounterModule extends HudModule {
    private final BoolSetting background = add(new BoolSetting("Show background", "Dark backing behind the counter", true));
    private final BoolSetting pops = add(new BoolSetting("Pop count", "Show how many totems you have popped this session", true));
    private final BoolSetting flash = add(new BoolSetting("Flash on pop", "Briefly highlight the counter when you pop", true));

    private int sessionPops;
    private long lastPopMs;

    public TotemCounterModule() {
        super("Totem Counter", "Totems left in your inventory and offhand, plus pops this session.", 0.14, 0.78);
        ShardEvents.onTotemPop(entity -> {
            LocalPlayer p = mc().player;
            if (p != null && entity == p) {
                sessionPops++;
                lastPopMs = System.currentTimeMillis();
            }
        });
    }

    @Override
    public boolean defaultEnabled() {
        return true;
    }

    public int sessionPops() {
        return sessionPops;
    }

    public void resetSession() {
        sessionPops = 0;
    }

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        LocalPlayer p = mc().player;
        if (p == null) return;
        int count = countTotems(p.getInventory());
        String main = String.valueOf(count);
        String sub = pops.get() ? sessionPops + " popped" : "";
        int w = Math.max(44, font().width(sub) + 8);
        int h = pops.get() ? 30 : 20;
        boolean flashing = flash.get() && System.currentTimeMillis() - lastPopMs < 700;
        if (background.get()) Render2D.rounded(g, 0, 0, w, h, flashing ? Theme.accentAlpha(0x99) : 0x66000000);
        g.renderItem(new ItemStack(Items.TOTEM_OF_UNDYING), 3, 2);
        int color = count == 0 ? Theme.danger() : count <= 2 ? Theme.warning() : Theme.text();
        Render2D.text(g, font(), main, 23, 6, color, true);
        if (pops.get()) Render2D.text(g, font(), sub, 4, 20, Theme.muted(), true);
        size(w, h);
    }

    static int countTotems(Inventory inv) {
        int n = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(Items.TOTEM_OF_UNDYING)) n += s.getCount();
        }
        return n;
    }

    @Override
    public String icon() {
        return "item:totem_of_undying";
    }

    @Override
    public java.util.List<String> conflictingMods() {
        return java.util.List.of("totemcounter");
    }
}
