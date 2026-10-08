package gg.shard.client.modules.hud;

import gg.shard.client.event.ShardEvents;
import gg.shard.client.gui.Render2D;
import gg.shard.client.gui.Theme;
import gg.shard.client.hud.HudModule;
import gg.shard.client.hud.HudStyle;
import gg.shard.client.module.setting.BoolSetting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class TotemCounterModule extends HudModule {
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
    public String about() {
        return "Counts the totems in your own inventory and how many times you have popped since joining. Pops are detected from the totem event the server sends to every client.";
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
        HudStyle.Resolved st = style();
        int pad = st.padding();
        int icon = 16;
        int w = pad * 2 + Math.max(icon + 6 + textW(main), textW(sub));
        int h = pad * 2 + Math.max(icon, lineH()) + (pops.get() ? lineH() + 2 : 0);
        boolean flashing = flash.get() && System.currentTimeMillis() - lastPopMs < 700;
        if (flashing) Render2D.roundedRect(g, 0, 0, w, h, st.radius(), Theme.accentAlpha(0x99));
        else box(g, st, w, h);
        g.renderItem(new ItemStack(Items.TOTEM_OF_UNDYING), pad, pad);
        int color = count == 0 ? Theme.danger() : count <= 2 ? Theme.warning() : st.value();
        text(g, st, main, pad + icon + 6, pad + (icon - lineH()) / 2, color);
        if (pops.get()) text(g, st, sub, pad, pad + Math.max(icon, lineH()) + 2, st.text());
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
        return "totem";
    }

    @Override
    public java.util.List<String> conflictingMods() {
        return java.util.List.of("totemcounter");
    }
}
