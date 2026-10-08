package gg.shard.client.modules.hud;

import gg.shard.client.ShardClient;
import gg.shard.client.combat.CombatTracker;
import gg.shard.client.gui.Render2D;
import gg.shard.client.gui.Theme;
import gg.shard.client.hud.HudModule;
import gg.shard.client.hud.HudStyle;
import gg.shard.client.module.setting.BoolSetting;
import gg.shard.client.module.setting.IntSetting;
import gg.shard.client.modules.visual.TotemPopModule;
import gg.shard.client.util.Colors;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

/**
 * Target HUD: the player you are fighting (the one you hit last, or the one under your
 * crosshair): head, name, health and absorption, armour durability, totem pops this fight and
 * distance. Only what the client already has: vanilla's synced health, the equipment the server
 * sends, pops from the totem event. Hidden a few seconds after the fight moves on.
 */
public final class TargetHudModule extends HudModule {
    private final IntSetting timeout = add(new IntSetting("Hide after", "Seconds after your last hit before it hides", 6, 2, 20, 1, " s"));
    private final BoolSetting lookAt = add(new BoolSetting("Show who you aim at", "Also show the player under your crosshair", true));
    private final BoolSetting armor = add(new BoolSetting("Armour", "Show their armour with durability", true));
    private final BoolSetting players = add(new BoolSetting("Players only", "Ignore mobs", true));

    public TargetHudModule() {
        super("Target HUD", "The player you are fighting: health, armour, pops and distance.", 0.53, 0.53);
    }

    @Override
    public String icon() {
        return "target";
    }

    @Override
    public String about() {
        return "Shows the player you are fighting: the one you hit last, or the one under your crosshair. Head, name, health with absorption, armour durability, "
                + "totem pops and distance, all from what the client already receives. It hides a few seconds after you stop hitting them.";
    }

    private LivingEntity target() {
        var mc = mc();
        if (mc.level == null) return null;
        Entity picked = mc.crosshairPickEntity;
        if (lookAt.get() && picked instanceof LivingEntity l && (!players.get() || l instanceof Player)) return l;
        int id = CombatTracker.LOG.targetId();
        if (System.currentTimeMillis() - CombatTracker.LOG.targetHitAt() > timeout.get() * 1000L) return null;
        Entity e = mc.level.getEntity(id);
        if (!(e instanceof LivingEntity l) || l.isRemoved()) return null;
        if (players.get() && !(l instanceof Player)) return null;
        return l;
    }

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        LivingEntity t = target();
        if (t == null) {
            size(1, 1);
            return;
        }
        HudStyle.Resolved st = style();
        int pad = Math.max(3, st.padding());
        int head = 24;
        int w = 128;
        int h = pad * 2 + head + (armor.get() ? 14 : 0);
        box(g, st, w, h);
        int tx = pad;
        if (t instanceof AbstractClientPlayer p) {
            PlayerFaceRenderer.draw(g, p.getSkin(), pad, pad, head);
            tx += head + 5;
        }
        String name = t.getName().getString();
        text(g, st, gg.shard.client.gui.Fonts.clip(name, WEIGHT, TEXT_SIZE, w - tx - pad), tx, pad, st.text());
        float hp = t.getHealth();
        float abs = t.getAbsorptionAmount();
        float max = Math.max(1f, t.getMaxHealth());
        int barW = w - tx - pad;
        int barY = pad + lineH() + 2;
        Render2D.roundedRect(g, tx, barY, barW, 4, 2, 0x55000000);
        int hw = Math.round(barW * Math.min(1f, hp / max));
        if (hw > 0) Render2D.roundedRect(g, tx, barY, hw, 4, 2, Colors.health(hp / max));
        if (abs > 0) {
            int aw = Math.round(barW * Math.min(1f, abs / max));
            Render2D.roundedRect(g, tx, barY + 5, aw, 2, 1, 0xFFFACC15);
        }
        String info = String.format(Locale.ROOT, "%.1f", hp) + (abs > 0 ? "+" + Math.round(abs) : "");
        int pops = t instanceof Player pl ? ShardClient.modules().get(TotemPopModule.class).popsFor(pl.getUUID()) : 0;
        if (pops > 0) info += "  " + pops + (pops == 1 ? " pop" : " pops");
        var me = mc().player;
        if (me != null) info += "  " + String.format(Locale.ROOT, "%.1fm", me.distanceTo(t));
        text(g, st, info, tx, barY + 7, st.value());
        if (armor.get()) {
            int ax = pad;
            int ay = pad + head + 2;
            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND}) {
                ItemStack s = t.getItemBySlot(slot);
                if (s.isEmpty()) continue;
                var pose = g.pose();
                pose.pushMatrix();
                pose.translate(ax, ay);
                pose.scale(0.75f, 0.75f);
                g.renderItem(s, 0, 0);
                pose.popMatrix();
                if (s.getMaxDamage() > 0) {
                    double frac = 1 - s.getDamageValue() / (double) s.getMaxDamage();
                    Render2D.fill(g, ax, ay + 12, 12, 1, 0x80000000);
                    Render2D.fill(g, ax, ay + 12, Math.max(1, (int) Math.round(12 * frac)), 1, Colors.health(frac));
                }
                ax += 14;
            }
        }
        size(w, h);
    }
}
