package gg.shard.client.render;

import gg.shard.client.ShardClient;
import gg.shard.client.modules.visual.CosmeticsModule;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;

/**
 * The shield cosmetic on held shields. Vanilla resolves a held item's look (ItemModelResolver)
 * while it still knows who holds it, but draws the shield later from its components alone; so
 * while an item held in a hand is being resolved the holder is remembered here, and
 * ShieldSpecialRendererMixin wraps the shield's components in a {@link Skinned} that carries the
 * holder's cosmetic texture through to the draw. Render thread only.
 */
public final class ShieldCosmetics {
    private ShieldCosmetics() {}

    private record Frame(@Nullable LivingEntity owner, ItemDisplayContext context) {}

    private static final Deque<Frame> STACK = new ArrayDeque<>();

    /** Hands only: GUI icons, dropped items, item frames and heads keep vanilla's shield. */
    private static final Set<ItemDisplayContext> HELD = Set.of(ItemDisplayContext.FIRST_PERSON_LEFT_HAND, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
            ItemDisplayContext.THIRD_PERSON_LEFT_HAND, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND);

    public static void push(@Nullable ItemOwner owner, ItemDisplayContext context) {
        STACK.push(new Frame(owner == null ? null : owner.asLivingEntity(), context));
    }

    public static void pop() {
        STACK.poll();
    }

    /** The cosmetic texture for the shield being resolved right now, or null for vanilla's. */
    public static @Nullable Identifier currentTexture() {
        Frame f = STACK.peek();
        if (f == null || !(f.owner() instanceof Player player) || !HELD.contains(f.context())) return null;
        if (!ShardClient.isReady()) return null;
        CosmeticsModule cosmetics = ShardClient.modules().get(CosmeticsModule.class);
        if (cosmetics == null) return null;
        try {
            return cosmetics.shieldTexture(player.getUUID(), player == Minecraft.getInstance().player);
        } catch (RuntimeException e) {
            ShardClient.LOGGER.error("Cosmetics: shield lookup failed", e);
            return null;
        }
    }

    /**
     * A shield's components plus the cosmetic texture to draw it with. Equality covers both, so
     * vanilla's model identity (used to cache item pictures) changes with the texture.
     */
    public record Skinned(DataComponentMap base, Identifier texture) implements DataComponentMap {
        @Override
        public <T> @Nullable T get(DataComponentType<? extends T> type) {
            return base.get(type);
        }

        @Override
        public java.util.Set<DataComponentType<?>> keySet() {
            return base.keySet();
        }
    }
}
