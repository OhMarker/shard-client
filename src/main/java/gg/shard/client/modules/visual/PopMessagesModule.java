package gg.shard.client.modules.visual;

import gg.shard.client.command.CommandManager;
import gg.shard.client.event.ShardEvents;
import gg.shard.client.gui.Theme;
import gg.shard.client.module.Module;
import gg.shard.client.module.ModuleCategory;
import gg.shard.client.module.setting.BoolSetting;
import gg.shard.client.module.setting.DoubleSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Chat line and sound whenever a nearby player (or you) pops a totem. */
public final class PopMessagesModule extends Module {
    private final BoolSetting self = add(new BoolSetting("Own pops", "Announce your own pops too", true));
    private final BoolSetting sound = add(new BoolSetting("Sound", "Play a ping when someone pops", true));
    private final DoubleSetting pitch = add(new DoubleSetting("Pitch", "Sound pitch", 1.4, 0.5, 2.0, 0.1));

    private final Map<UUID, Integer> popsByPlayer = new HashMap<>();

    public PopMessagesModule() {
        super("Pop Messages", "Says who popped a totem and how many times this session.", ModuleCategory.VISUALS);
        ShardEvents.onTotemPop(this::onPop);
    }

    @Override
    public boolean defaultEnabled() {
        return true;
    }

    private void onPop(LivingEntity entity) {
        if (!isEnabled() || !(entity instanceof Player player)) return;
        Minecraft mc = Minecraft.getInstance();
        boolean isSelf = player == mc.player;
        if (isSelf && !self.get()) return;
        int n = popsByPlayer.merge(player.getUUID(), 1, Integer::sum);
        String name = isSelf ? "You" : player.getName().getString();
        CommandManager.reply(name + " popped " + ordinal(n) + " totem" + (n == 1 ? "" : "s"), isSelf ? Theme.warning() : Theme.text());
        if (sound.get()) mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, pitch.getFloat()));
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
