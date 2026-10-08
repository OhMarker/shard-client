package gg.shard.client.modules.hud;

import gg.shard.client.event.ShardEvents;
import gg.shard.client.hud.HudModule;
import gg.shard.client.module.setting.BoolSetting;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/** Pops given and taken, deaths and playtime since joining the server (vanilla-visible data only). */
public final class SessionStatsModule extends HudModule {
    private final BoolSetting playtime = add(new BoolSetting("Playtime", "Time since you joined", true));

    private int popsTaken;
    private int popsGiven;
    private int deaths;
    private long joinedAt = System.currentTimeMillis();
    private boolean wasDead;

    public SessionStatsModule() {
        super("Session", "Totems you popped, totems others popped near you, deaths and playtime.", 0.86, 0.30);
        ShardEvents.onTotemPop(entity -> {
            LocalPlayer p = mc().player;
            if (p == null) return;
            if (entity == p) popsTaken++;
            else if (entity instanceof Player) popsGiven++;
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> reset());
    }

    @Override
    protected String legacyKey() {
        return "session-stats";
    }

    @Override
    protected boolean hasAlignment() {
        return true;
    }

    @Override
    public String about() {
        return "Session counters built from events every client receives: your pops, pops you saw other players take, your deaths and the time since you joined. Resets on every join.";
    }

    public void reset() {
        popsTaken = 0;
        popsGiven = 0;
        deaths = 0;
        joinedAt = System.currentTimeMillis();
    }

    @Override
    public void onTick() {
        LocalPlayer p = mc().player;
        if (p == null) return;
        boolean dead = p.isDeadOrDying();
        if (dead && !wasDead) deaths++;
        wasDead = dead;
    }

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        List<String> lines = new ArrayList<>();
        lines.add("Pops taken  " + popsTaken);
        lines.add("Pops seen  " + popsGiven);
        lines.add("Deaths  " + deaths);
        if (playtime.get()) lines.add("Time  " + formatMs(System.currentTimeMillis() - joinedAt));
        lines(g, lines, null);
    }

    static String formatMs(long ms) {
        long s = ms / 1000;
        if (s >= 3600) return String.format("%dh %02dm", s / 3600, (s % 3600) / 60);
        return String.format("%dm %02ds", s / 60, s % 60);
    }

    @Override
    public String icon() {
        return "session";
    }
}
