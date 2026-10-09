package gg.shard.client.modules.perf;

import gg.shard.client.mixin.AbstractArrowInvoker;
import gg.shard.client.module.Module;
import gg.shard.client.module.ModuleCategory;
import gg.shard.client.module.setting.BoolSetting;
import gg.shard.client.module.setting.IntSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Draws fewer of the small entities that pile up in fights: experience orbs stacked in the same
 * block, item stacks, arrows stuck in blocks, the portal particles around ender pearls, and an
 * optional cap on new particles per tick. Rendering only: every entity stays in the world, keeps
 * ticking and can be picked up exactly as before; nothing is sent to the server.
 * EntityRenderDispatcherMixin asks {@link #hides} after vanilla's own culling said "draw it";
 * LevelRendererEntitiesMixin calls {@link #beginFrame}; ParticleLimitMixin asks
 * {@link #skipsParticle}.
 */
public final class EntityOptimizerModule extends Module {
    private final IntSetting orbsPerBlock = add(new IntSetting("Orbs per block", "Experience orbs drawn in one block; 0 draws all", 2, 0, 10, 1, "").group("Experience orbs")
            .details("Orbs in the same block overlap almost exactly, so drawing one or two looks the same."));
    private final IntSetting maxOrbs = add(new IntSetting("Max orbs", "Experience orbs drawn in total; 0 draws all", 100, 0, 500, 10, "").group("Experience orbs"));
    private final IntSetting itemsPerBlock = add(new IntSetting("Items per block", "Dropped item stacks drawn in one block; 0 draws all", 0, 0, 10, 1, "").group("Items")
            .details("Hidden stacks are still there and can be picked up; only the drawing is skipped."));
    private final IntSetting maxItems = add(new IntSetting("Max items", "Dropped item stacks drawn in total; 0 draws all", 0, 0, 500, 10, "").group("Items"));
    private final BoolSetting limitArrows = add(new BoolSetting("Limit stuck arrows", "Draw only some of the arrows stuck in blocks", false).group("Arrows")
            .details("Arrows in flight are always drawn."));
    private final IntSetting maxArrows = add(new IntSetting("Max stuck arrows", "Stuck arrows drawn in total", 20, 0, 200, 5, "").group("Arrows"));
    private final IntSetting pearlParticles = add(new IntSetting("Pearl particles", "Share of the purple portal particles kept around ender pearls and teleports", 100, 0, 100, 5, "%")
            .group("Ender pearls"));
    private final IntSetting particlesPerTick = add(new IntSetting("New particles per tick", "Cap on particles created in one tick; 0 means no cap", 0, 0, 2000, 50, "")
            .group("Particles").details("Particles beyond the cap are simply not created this tick; the rest play normally."));

    private final CellCounter orbCells = new CellCounter(1024);
    private final CellCounter itemCells = new CellCounter(1024);
    private int orbs;
    private int items;
    private int arrows;
    private long particleTick = Long.MIN_VALUE;
    private int particlesThisTick;

    public EntityOptimizerModule() {
        super("Entity Optimizer", "Draw fewer stacked XP orbs, items and stuck arrows, thin pearl particles, cap particles.", ModuleCategory.PERFORMANCE);
        maxArrows.visibleWhen(limitArrows::get);
    }

    @Override
    public boolean defaultEnabled() {
        return true;
    }

    @Override
    public String icon() {
        return "particles";
    }

    @Override
    public String about() {
        return "Skips drawing experience orbs stacked in the same block (and past a total), optionally dropped items and arrows stuck in blocks past "
                + "a limit, thins the portal particles around ender pearls and can cap new particles per tick. Only the drawing changes: every orb, "
                + "item and arrow is still in the world, ticks and can be picked up as normal, and nothing is sent to the server.";
    }

    /** Called once per frame before vanilla walks the entities to draw. */
    public void beginFrame() {
        orbCells.reset();
        itemCells.reset();
        orbs = 0;
        items = 0;
        arrows = 0;
    }

    /** True when an entity vanilla would draw this frame should be skipped. */
    public boolean hides(Entity entity) {
        if (!isEnabled()) return false;
        if (entity instanceof ExperienceOrb) {
            if (keepsSpecial(entity)) return false;
            if (orbsPerBlock.get() > 0 && orbCells.increment(cell(entity)) > orbsPerBlock.get()) return true;
            return maxOrbs.get() > 0 && ++orbs > maxOrbs.get();
        }
        if (entity instanceof ItemEntity) {
            if (keepsSpecial(entity)) return false;
            if (itemsPerBlock.get() > 0 && itemCells.increment(cell(entity)) > itemsPerBlock.get()) return true;
            return maxItems.get() > 0 && ++items > maxItems.get();
        }
        if (entity instanceof AbstractArrow arrow && limitArrows.get()) {
            if (keepsSpecial(entity) || !((AbstractArrowInvoker) arrow).shard$isInGround()) return false;
            return ++arrows > maxArrows.get();
        }
        return false;
    }

    /** Never hide what you are aiming at or what glows. */
    private static boolean keepsSpecial(Entity entity) {
        return entity == Minecraft.getInstance().crosshairPickEntity || entity.isCurrentlyGlowing();
    }

    private static long cell(Entity entity) {
        return BlockPos.asLong(entity.getBlockX(), entity.getBlockY(), entity.getBlockZ());
    }

    /** Called from the particle hook for every particle spawn. */
    public boolean skipsParticle(ParticleOptions options) {
        if (!isEnabled()) return false;
        if (options.getType() == ParticleTypes.PORTAL) {
            int keep = pearlParticles.get();
            if (keep <= 0 || (keep < 100 && ThreadLocalRandom.current().nextInt(100) >= keep)) return true;
        }
        int cap = particlesPerTick.get();
        if (cap <= 0) return false;
        Minecraft mc = Minecraft.getInstance();
        long tick = mc.level == null ? 0 : mc.level.getGameTime();
        if (tick != particleTick) {
            particleTick = tick;
            particlesThisTick = 0;
        }
        return ++particlesThisTick > cap;
    }
}
