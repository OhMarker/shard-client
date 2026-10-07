package gg.shard.client.event;

import gg.shard.client.ShardClient;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/** Events that Fabric API does not provide; fired from Shard's mixins. */
public final class ShardEvents {
    private ShardEvents() {}

    private static final List<Consumer<LivingEntity>> TOTEM_POP = new CopyOnWriteArrayList<>();

    /** A living entity's totem of undying activated (client-side entity event 35). */
    public static void onTotemPop(Consumer<LivingEntity> listener) {
        TOTEM_POP.add(listener);
    }

    public static void fireTotemPop(LivingEntity entity) {
        for (Consumer<LivingEntity> l : TOTEM_POP) {
            try {
                l.accept(entity);
            } catch (RuntimeException e) {
                ShardClient.LOGGER.error("Totem pop listener failed", e);
            }
        }
    }
}
