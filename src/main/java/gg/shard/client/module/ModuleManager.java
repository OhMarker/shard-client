package gg.shard.client.module;

import com.mojang.blaze3d.platform.InputConstants;
import gg.shard.client.ShardClient;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/** Registry of modules plus the tick and keybind dispatch loop. */
public final class ModuleManager {
    private final List<Module> modules = new ArrayList<>();
    private final Set<Integer> heldKeys = new HashSet<>();
    private final List<Runnable> tickListeners = new ArrayList<>();
    private Consumer<Module> changeListener = m -> {};

    public void register(Module module) {
        if (find(module.key()) != null) throw new IllegalStateException("Duplicate module " + module.key());
        module.setChangeListener(m -> changeListener.accept(m));
        modules.add(module);
    }

    public void onChanged(Consumer<Module> listener) {
        this.changeListener = listener;
    }

    /** Extra per-tick work (config flushing) that is not a module. */
    public void onTick(Runnable listener) {
        tickListeners.add(listener);
    }

    public List<Module> all() {
        return Collections.unmodifiableList(modules);
    }

    public List<Module> byCategory(ModuleCategory category) {
        List<Module> out = new ArrayList<>();
        for (Module m : modules) if (m.category() == category && !m.hidden()) out.add(m);
        return out;
    }

    public Module find(String keyOrName) {
        for (Module m : modules) {
            if (m.key().equalsIgnoreCase(keyOrName) || m.name().equalsIgnoreCase(keyOrName)) return m;
        }
        String compact = keyOrName.replace("-", "").replace(" ", "").replace("_", "");
        for (Module m : modules) {
            if (m.key().replace("-", "").equalsIgnoreCase(compact)) return m;
        }
        return null;
    }

    public <T extends Module> T get(Class<T> type) {
        for (Module m : modules) if (type.isInstance(m)) return type.cast(m);
        throw new IllegalStateException("Module not registered: " + type.getSimpleName());
    }

    public void start() {
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void tick(Minecraft mc) {
        for (Runnable r : tickListeners) {
            try {
                r.run();
            } catch (RuntimeException e) {
                ShardClient.LOGGER.error("Tick listener failed", e);
            }
        }
        if (mc.player == null || mc.level == null) {
            heldKeys.clear();
            return;
        }
        if (mc.screen == null) handleKeybinds(mc);
        else heldKeys.clear();
        for (Module m : modules) {
            if (!m.isEnabled()) continue;
            try {
                m.onTick();
            } catch (RuntimeException e) {
                ShardClient.LOGGER.error("Module {} failed during tick; disabling it", m.name(), e);
                m.setEnabled(false);
            }
        }
    }

    private void handleKeybinds(Minecraft mc) {
        for (Module m : modules) {
            int key = m.keybind();
            if (key < 0) continue;
            boolean down = InputConstants.isKeyDown(mc.getWindow(), key);
            if (down && heldKeys.add(key)) m.toggle();
            else if (!down) heldKeys.remove(key);
        }
    }
}
