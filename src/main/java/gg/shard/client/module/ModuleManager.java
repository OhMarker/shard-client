package gg.shard.client.module;

import com.mojang.blaze3d.platform.InputConstants;
import gg.shard.client.ShardClient;
import gg.shard.client.server.ServerBlacklist;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/** Registry of modules plus the tick and keybind dispatch loop, server rules and mod conflicts. */
public final class ModuleManager {
    /** Answers "is this Fabric mod loaded, and what is it called?"; null when it is not. */
    public interface ModResolver {
        String nameIfLoaded(String modId);
    }

    private final List<Module> modules = new ArrayList<>();
    private final Set<Integer> heldKeys = new HashSet<>();
    private final List<Runnable> tickListeners = new ArrayList<>();
    private Consumer<Module> changeListener = m -> {};
    private ServerBlacklist blacklist = new ServerBlacklist();
    private String currentServer;

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

    /**
     * The module of this class. Mixins call this many times per frame (weather, time, crystals,
     * sounds, the HUD), so lookups are cached by class instead of scanning the list.
     */
    public <T extends Module> T get(Class<T> type) {
        Module cached = BY_CLASS.get(type);
        if (cached == null) {
            for (Module m : modules) {
                if (type.isInstance(m)) {
                    cached = m;
                    break;
                }
            }
            if (cached == null) throw new IllegalStateException("Module not registered: " + type.getSimpleName());
            BY_CLASS.put(type, cached);
        }
        return type.cast(cached);
    }

    private final java.util.Map<Class<?>, Module> BY_CLASS = new java.util.concurrent.ConcurrentHashMap<>();

    // ---- mod conflicts ------------------------------------------------------------------------

    /** Marks every module whose feature another installed mod already provides. */
    public List<String> applyModConflicts(ModResolver resolver) {
        List<String> notices = new ArrayList<>();
        for (Module m : modules) {
            String blocker = null;
            for (String id : m.conflictingMods()) {
                String name = resolver.nameIfLoaded(id);
                if (name != null) {
                    blocker = name;
                    break;
                }
            }
            m.setBlockedBy(blocker);
            if (blocker != null) notices.add(m.name() + " stays off because " + blocker + " is installed");
        }
        return notices;
    }

    // ---- server blacklist ---------------------------------------------------------------------

    public ServerBlacklist blacklist() {
        return blacklist;
    }

    public void setBlacklist(ServerBlacklist list) {
        this.blacklist = list == null ? new ServerBlacklist() : list;
        refreshSuppression();
    }

    /** The multiplayer address the client is connected to, or null (menu, singleplayer). */
    public String currentServer() {
        return currentServer;
    }

    public void setCurrentServer(String address) {
        this.currentServer = address == null || address.isBlank() ? null : address;
        refreshSuppression();
    }

    /** Re-evaluates which modules the current server forces off. */
    public void refreshSuppression() {
        Set<String> off = currentServer == null ? Set.of() : blacklist.modulesFor(currentServer);
        for (Module m : modules) m.setSuppressed(off.contains(m.key()));
    }

    /** Toggles a module on the current server's rule and applies it immediately. */
    public void setDisabledOnCurrentServer(Module module, boolean disabled) {
        if (currentServer == null) return;
        blacklist.setDisabled(currentServer, module.key(), disabled);
        refreshSuppression();
        changeListener.accept(module);
    }

    // ---- tick loop ----------------------------------------------------------------------------

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
