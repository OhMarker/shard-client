package gg.shard.client.modules.hud;

import gg.shard.client.ShardClient;
import gg.shard.client.cosmetics.ShardApi;
import gg.shard.client.hud.HudModule;
import gg.shard.client.module.setting.BoolSetting;
import gg.shard.client.modules.visual.CosmeticsModule;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;

/** Your Shard token balance and the time to the next 10 (from the Cosmetics module's account). */
public final class TokensModule extends HudModule {
    private final BoolSetting showNext = add(new BoolSetting("Time to next", "Minutes until the next 10 tokens", true));

    public TokensModule() {
        super("Tokens", "Your Shard tokens for buying capes.", 0.02, 0.40);
    }

    @Override
    protected String defaultLabel() {
        return "Tokens";
    }

    @Override
    public String about() {
        return "Shows how many Shard tokens you have and, optionally, the minutes until the next 10. You earn "
                + "10 tokens for every 10 minutes you play and spend them on capes in Shard Launcher.";
    }

    @Override
    public String icon() {
        return "cape";
    }

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        CosmeticsModule cosmetics = ShardClient.modules().get(CosmeticsModule.class);
        ShardApi.Me me = cosmetics == null ? null : cosmetics.account();
        if (me == null) {
            line(g, "--", 0);
            return;
        }
        String text = String.format(java.util.Locale.ROOT, "%,d", me.tokens());
        if (showNext.get()) text += "  " + Math.max(1, (me.secondsToNextTokens() + 59) / 60) + "m";
        line(g, text, 0);
    }
}
