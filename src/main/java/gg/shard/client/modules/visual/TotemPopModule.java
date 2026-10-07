package gg.shard.client.modules.visual;

import gg.shard.client.command.CommandManager;
import gg.shard.client.event.ShardEvents;
import gg.shard.client.gui.Theme;
import gg.shard.client.module.Module;
import gg.shard.client.module.ModuleCategory;
import gg.shard.client.module.setting.BoolSetting;
import gg.shard.client.module.setting.DoubleSetting;
import gg.shard.client.module.setting.IntSetting;
import gg.shard.client.util.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Everything that happens when a totem pops: the full-screen animation, the sound, the
 * particles, an optional screen flash and the chat line. Hooks live in the packet-listener
 * mixin because 1.21.11 handles entity event 35 there, not in LivingEntity.
 */
public final class TotemPopModule extends Module {
    private final BoolSetting hideAnimation = add(new BoolSetting("Hide animation", "Skip the full-screen totem animation (a frame-time spike mid-fight)", true).group("Animation"));
    private final BoolSetting flash = add(new BoolSetting("Flash", "Brief accent-coloured flash when you pop", false).group("Animation"));
    private final IntSetting volume = add(new IntSetting("Pop volume", "Totem sound volume", 100, 0, 200, 10, "%").group("Sound"));
    private final DoubleSetting pitch = add(new DoubleSetting("Pop pitch", "Totem sound pitch", 1.0, 0.5, 2.0, 0.1).group("Sound"));
    private final IntSetting particles = add(new IntSetting("Pop particles", "Totem particles to keep", 50, 0, 100, 5, "%").group("Particles"));
    private final BoolSetting messages = add(new BoolSetting("Chat line", "Say who popped and how many times this session", true).group("Messages"));
    private final BoolSetting ownPops = add(new BoolSetting("Own pops", "Include your own pops", true).group("Messages"));
    private final BoolSetting ping = add(new BoolSetting("Ping sound", "Play a ping when someone pops", true).group("Messages"));
    private final DoubleSetting pingPitch = add(new DoubleSetting("Ping pitch", "Ping sound pitch", 1.4, 0.5, 2.0, 0.1).group("Messages"));

    private final Map<UUID, Integer> popsByPlayer = new HashMap<>();
    private long flashAt;

    public TotemPopModule() {
        super("Totem Pop Tweaks", "Tame the totem animation, sound and particles, and announce pops in chat.", ModuleCategory.VISUALS);
        ShardEvents.onTotemPop(this::onPop);
    }

    @Override
    public boolean defaultEnabled() {
        return true;
    }

    @Override
    public String icon() {
        return "item:totem_of_undying";
    }

    public boolean hideAnimation() {
        return isEnabled() && hideAnimation.get();
    }

    public float soundVolume(float original) {
        return isEnabled() ? original * volume.get() / 100f : original;
    }

    public float soundPitch(float original) {
        return isEnabled() ? original * pitch.getFloat() : original;
    }

    /** Percent of totem particles to keep (100 when the module is off). */
    public int particlePercent() {
        return isEnabled() ? particles.get() : 100;
    }

    /** Pops seen for a player this session (used by the nametag module). */
    public int popsFor(UUID player) {
        return popsByPlayer.getOrDefault(player, 0);
    }

    public void resetSession() {
        popsByPlayer.clear();
    }

    private void onPop(LivingEntity entity) {
        if (!isEnabled() || !(entity instanceof Player player)) return;
        Minecraft mc = Minecraft.getInstance();
        boolean self = player == mc.player;
        int n = popsByPlayer.merge(player.getUUID(), 1, Integer::sum);
        if (self && flash.get()) flashAt = System.currentTimeMillis();
        if (!messages.get() || (self && !ownPops.get())) return;
        String name = self ? "You" : player.getName().getString();
        CommandManager.reply(name + " popped " + ordinal(n) + " totem" + (n == 1 ? "" : "s"), self ? Theme.warning() : Theme.text());
        if (ping.get()) mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, pingPitch.getFloat()));
    }

    /** Draws the flash overlay; called from the HUD layer every frame. */
    public void renderFlash(GuiGraphics g) {
        if (!isEnabled() || !flash.get()) return;
        long age = System.currentTimeMillis() - flashAt;
        if (age < 0 || age > 350) return;
        float alpha = 0.35f * (1f - age / 350f);
        g.fill(0, 0, g.guiWidth(), g.guiHeight(), Colors.fade(Theme.accent(), alpha));
    }

    static String ordinal(int n) {
        if (n % 100 >= 11 && n % 100 <= 13) return n + "th";
        return switch (n % 10) {
            case 1 -> n + "st";
            case 2 -> n + "nd";
            case 3 -> n + "rd";
            default -> n + "th";
        };
    }
}
