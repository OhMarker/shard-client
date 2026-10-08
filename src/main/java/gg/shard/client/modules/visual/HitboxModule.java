package gg.shard.client.modules.visual;

import gg.shard.client.module.Module;
import gg.shard.client.module.ModuleCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus;

/**
 * Keeps vanilla's F3+B hitboxes on without the debug screen. Nothing is drawn that vanilla
 * cannot draw itself; this only flips the same switch F3+B does and restores it afterwards.
 */
public final class HitboxModule extends Module {
    private DebugScreenEntryStatus previous;

    public HitboxModule() {
        super("Hitboxes", "Show vanilla's F3+B hitboxes without opening the debug screen.", ModuleCategory.VISUALS);
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

    @Override
    public String about() {
        return "Turns on vanilla's own F3+B hitboxes without the debug screen and restores your setting when switched off. Nothing is drawn that vanilla cannot draw itself.";
    }
}
