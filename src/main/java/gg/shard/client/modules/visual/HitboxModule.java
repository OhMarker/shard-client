package gg.shard.client.modules.visual;

import gg.shard.client.module.Module;
import gg.shard.client.module.ModuleCategory;
import gg.shard.client.module.setting.BoolSetting;
import gg.shard.client.module.setting.ColorSetting;
import gg.shard.client.module.setting.DoubleSetting;
import gg.shard.client.module.setting.IntSetting;
import gg.shard.client.util.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Hitboxes in your own style. The module turns on vanilla's F3+B hitboxes (the same switch, and
 * restores it afterwards); EntityHitboxDebugRendererMixin then hands each box vanilla is about to
 * draw to {@link #draw}, which draws it with your colour, opacity, line width, optional fill, eye
 * line and look arrow. Vanilla still decides which entities get a box (visible, in view, not
 * invisible), so nothing appears that F3+B would not show.
 */
public final class HitboxModule extends Module {
    private final ColorSetting lineColor = add(new ColorSetting("Line colour", "Colour of the box outline", 0xFFFFFFFF, false).group("Box"));
    private final IntSetting lineOpacity = add(new IntSetting("Line opacity", "How see-through the outline is", 70, 10, 100, 5, "%").group("Box"));
    private final DoubleSetting lineWidth = add(new DoubleSetting("Line width", "Thickness of the outline; vanilla is 2.5", 1.5, 0.5, 6.0, 0.5, " px").group("Box"));
    private final BoolSetting fill = add(new BoolSetting("Fill", "Shade the inside of the box", false).group("Box"));
    private final ColorSetting fillColor = add(new ColorSetting("Fill colour", "Colour of the shading", 0xFFFFFFFF, false).group("Box"));
    private final IntSetting fillOpacity = add(new IntSetting("Fill opacity", "How strong the shading is", 12, 2, 60, 2, "%").group("Box"));
    private final BoolSetting targetColour = add(new BoolSetting("Highlight target", "Another colour for the entity under your crosshair", true).group("Box"));
    private final ColorSetting targetColor = add(new ColorSetting("Target colour", "Outline colour for the entity under your crosshair", 0xFFFB7185, false).group("Box"));
    private final BoolSetting eyeLine = add(new BoolSetting("Eye height", "A line at the entity's eye height", true).group("Details"));
    private final ColorSetting eyeColor = add(new ColorSetting("Eye line colour", "Colour of the eye-height line", 0xFFF87171, false).group("Details"));
    private final BoolSetting lookVector = add(new BoolSetting("Look direction", "An arrow showing where the entity is looking", false).group("Details"));
    private final ColorSetting lookColor = add(new ColorSetting("Arrow colour", "Colour of the look arrow", 0xFF60A5FA, false).group("Details"));
    private final BoolSetting playersOnly = add(new BoolSetting("Players only", "Only draw boxes for other players", false).group("Entities"));
    private final BoolSetting livingOnly = add(new BoolSetting("Living only", "Skip items, arrows, orbs and other non-living entities", false).group("Entities"));

    private DebugScreenEntryStatus previous;

    public HitboxModule() {
        super("Hitboxes", "Hitboxes in your style: colour, opacity, line width, fill, eye line and look arrow.", ModuleCategory.VISUALS);
        fillColor.visibleWhen(fill::get);
        fillOpacity.visibleWhen(fill::get);
        targetColor.visibleWhen(targetColour::get);
        eyeColor.visibleWhen(eyeLine::get);
        lookColor.visibleWhen(lookVector::get);
        livingOnly.visibleWhen(() -> !playersOnly.get());
    }

    @Override
    public String icon() {
        return "hitbox";
    }

    @Override
    protected void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.debugEntries == null) return;
        previous = mc.debugEntries.getStatus(DebugScreenEntries.ENTITY_HITBOXES);
        mc.debugEntries.setStatus(DebugScreenEntries.ENTITY_HITBOXES, DebugScreenEntryStatus.ALWAYS_ON);
    }

    @Override
    protected void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.debugEntries == null) return;
        mc.debugEntries.setStatus(DebugScreenEntries.ENTITY_HITBOXES, previous == null ? DebugScreenEntryStatus.NEVER : previous);
        previous = null;
    }

    /** True when Shard draws the boxes vanilla's F3+B renderer is about to draw. */
    public boolean styles() {
        return isEnabled();
    }

    /**
     * Draws the box for one entity vanilla already chose to show (called in place of vanilla's
     * own drawing). Skipped entities simply get no box.
     */
    public void draw(Entity entity, float partialTick) {
        if (playersOnly.get() ? !(entity instanceof Player) : livingOnly.get() && !(entity instanceof LivingEntity)) return;
        float alpha = lineOpacity.get() / 100f;
        Vec3 pos = entity.getPosition(partialTick);
        Vec3 offset = pos.subtract(entity.position());
        AABB box = entity.getBoundingBox().move(offset);
        boolean aimed = targetColour.get() && Minecraft.getInstance().crosshairPickEntity == entity;
        int stroke = Colors.fade(aimed ? targetColor.get() : lineColor.get(), alpha);
        float width = lineWidth.getFloat();
        GizmoStyle style = fill.get()
                ? GizmoStyle.strokeAndFill(stroke, width, Colors.fade(fillColor.get(), fillOpacity.get() / 100f))
                : GizmoStyle.stroke(stroke, width);
        Gizmos.cuboid(box, style);
        if (entity instanceof EnderDragon dragon) {
            for (EnderDragonPart part : dragon.getSubEntities()) {
                Vec3 partOffset = part.getPosition(partialTick).subtract(part.position());
                Gizmos.cuboid(part.getBoundingBox().move(partOffset), GizmoStyle.stroke(stroke, width));
            }
        }
        if (eyeLine.get() && entity instanceof LivingEntity) {
            double eye = box.minY + entity.getEyeHeight();
            Gizmos.cuboid(new AABB(box.minX, eye - 0.01, box.minZ, box.maxX, eye + 0.01, box.maxZ),
                    GizmoStyle.stroke(Colors.fade(eyeColor.get(), alpha), width));
        }
        if (lookVector.get()) {
            Vec3 eyePos = pos.add(0.0, entity.getEyeHeight(), 0.0);
            Gizmos.arrow(eyePos, eyePos.add(entity.getViewVector(partialTick).scale(2.0)), Colors.fade(lookColor.get(), alpha), width);
        }
    }

    @Override
    public String about() {
        return "Shows hitboxes like F3+B, drawn your way: line colour, opacity and width, an optional soft fill, a target colour for the entity under "
                + "your crosshair, the eye-height line and the look arrow. Only entities vanilla's F3+B would box are drawn, and your F3+B setting "
                + "comes back when you switch this off.";
    }
}
