package gg.shard.client.combat;

import gg.shard.client.ShardClient;
import gg.shard.client.event.ShardEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;

/**
 * Feeds {@link FightLog} from what this client already sees: its own attacks (after vanilla
 * sent them), its own health, crystals it placed, and the totem and death events the server
 * sends to everyone. Nothing is sent; Target HUD, Combo, Reach, Fight Recap and Session read it.
 */
public final class CombatTracker {
    private CombatTracker() {}

    public static final FightLog LOG = new FightLog();
    private static float lastHealth = -1;
    private static boolean wasDead;

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> tick(client));
        ShardEvents.onTotemPop(entity -> {
            if (!(entity instanceof Player)) return;
            LOG.pop(entity == Minecraft.getInstance().player, System.currentTimeMillis());
        });
        ShardEvents.onDeath(entity -> LOG.entityDied(entity.getId(), System.currentTimeMillis()));
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            LOG.resetSession();
            lastHealth = -1;
        });
    }

    /** Called after vanilla sent an attack. */
    public static void onAttack(Player player, Entity target) {
        if (!(target instanceof LivingEntity living) || target instanceof EndCrystal) return;
        double reach = Math.sqrt(target.getBoundingBox().distanceToSqr(player.getEyePosition()));
        LOG.hitDealt(living.getId(), living.getName().getString(), reach, System.currentTimeMillis());
    }

    public static void onCrystalPlaced() {
        LOG.crystalPlaced(System.currentTimeMillis());
    }

    private static void tick(Minecraft mc) {
        if (!ShardClient.isReady()) return;
        long now = System.currentTimeMillis();
        LocalPlayer p = mc.player;
        if (p == null) {
            lastHealth = -1;
            return;
        }
        float health = p.getHealth() + p.getAbsorptionAmount();
        if (lastHealth >= 0 && health < lastHealth - 0.01f && !p.isDeadOrDying()) LOG.hurt(lastHealth - health, now);
        lastHealth = health;
        boolean dead = p.isDeadOrDying();
        if (dead && !wasDead) LOG.died(now);
        wasDead = dead;
        LOG.tick(now);
    }
}
