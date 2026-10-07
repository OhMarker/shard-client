package gg.shard.client.command;

import gg.shard.client.ShardClient;
import gg.shard.client.gui.ClickGuiScreen;
import gg.shard.client.gui.Theme;
import gg.shard.client.hud.HudEditorScreen;
import gg.shard.client.module.Module;
import gg.shard.client.module.setting.Setting;
import gg.shard.client.util.Keys;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Chat commands with the `.` prefix. Messages starting with `.` never reach the server. */
public final class CommandManager {
    public static final String PREFIX = ".";

    public void start() {
        ClientSendMessageEvents.ALLOW_CHAT.register(message -> {
            if (!message.startsWith(PREFIX) || message.length() < 2 || message.startsWith("..")) return true;
            try {
                handle(message.substring(1).trim());
            } catch (RuntimeException e) {
                ShardClient.LOGGER.error("Command failed: {}", message, e);
                reply("Something went wrong running that command; see the log.", Theme.danger());
            }
            return false;
        });
    }

    void handle(String line) {
        String[] parts = line.split("\\s+");
        String cmd = parts[0].toLowerCase(Locale.ROOT);
        List<String> args = Arrays.asList(parts).subList(1, parts.length);
        switch (cmd) {
            case "help", "?" -> help();
            case "toggle", "t" -> toggle(args);
            case "bind", "b" -> bind(args);
            case "set", "s" -> set(args);
            case "reset" -> reset(args);
            case "config", "profile", "p" -> config(args);
            case "gui", "menu" -> openLater(new ClickGuiScreen(null));
            case "hud" -> openLater(new HudEditorScreen(null, ShardClient.hud()));
            case "list", "modules" -> list();
            default -> reply("Unknown command. Try " + PREFIX + "help", Theme.warning());
        }
    }

    private void help() {
        reply("Commands:", Theme.text());
        reply(".toggle <module>  ·  .bind <module> <key|none>  ·  .set <module> <setting> <value>", Theme.muted());
        reply(".reset <module>  ·  .config save|load|list [name]  ·  .gui  ·  .hud  ·  .list", Theme.muted());
    }

    private void list() {
        StringBuilder on = new StringBuilder();
        for (Module m : ShardClient.modules().all()) if (m.isEnabled() && !m.hidden()) on.append(on.isEmpty() ? "" : ", ").append(m.name());
        reply("Enabled: " + (on.isEmpty() ? "nothing" : on), Theme.muted());
    }

    private void toggle(List<String> args) {
        Module m = module(args, 0);
        if (m == null) return;
        m.toggle();
        reply(m.name() + (m.isEnabled() ? " enabled" : " disabled"), m.isEnabled() ? Theme.success() : Theme.muted());
    }

    private void bind(List<String> args) {
        Module m = module(args, 0);
        if (m == null) return;
        if (args.size() < 2) {
            reply(m.name() + " is bound to " + Keys.name(m.keybind()), Theme.muted());
            return;
        }
        int key = Keys.fromName(args.get(1));
        if (key == Integer.MIN_VALUE) {
            reply("Unknown key \"" + args.get(1) + "\". Use a letter, digit, F-key, or none.", Theme.warning());
            return;
        }
        m.setKeybind(key);
        reply(m.name() + " bound to " + Keys.name(key), Theme.success());
    }

    private void set(List<String> args) {
        Module m = module(args, 0);
        if (m == null) return;
        if (args.size() < 2) {
            StringBuilder sb = new StringBuilder();
            for (Setting<?> s : m.settings()) sb.append(sb.isEmpty() ? "" : ", ").append(s.key()).append('=').append(s.display());
            reply(m.name() + " settings: " + (sb.isEmpty() ? "none" : sb), Theme.muted());
            return;
        }
        Setting<?> s = m.setting(args.get(1));
        if (s == null) {
            reply("No setting \"" + args.get(1) + "\" on " + m.name(), Theme.warning());
            return;
        }
        if (args.size() < 3) {
            reply(s.name() + " = " + s.display() + "  (" + s.description() + ")", Theme.muted());
            return;
        }
        String value = String.join(" ", args.subList(2, args.size()));
        if (s.parse(value)) reply(m.name() + " · " + s.name() + " = " + s.display(), Theme.success());
        else reply("\"" + value + "\" is not valid for " + s.name(), Theme.warning());
    }

    private void reset(List<String> args) {
        Module m = module(args, 0);
        if (m == null) return;
        for (Setting<?> s : m.settings()) s.reset();
        reply(m.name() + " settings reset", Theme.success());
    }

    private void config(List<String> args) {
        if (args.isEmpty()) {
            reply("Usage: .config save|load|list [name]", Theme.warning());
            return;
        }
        switch (args.get(0).toLowerCase(Locale.ROOT)) {
            case "save" -> {
                if (args.size() < 2) {
                    ShardClient.config().save();
                    reply("Config saved", Theme.success());
                } else if (ShardClient.config().saveProfile(args.get(1))) reply("Profile \"" + args.get(1) + "\" saved", Theme.success());
                else reply("Could not save that profile (letters, digits, - and _ only)", Theme.warning());
            }
            case "load" -> {
                if (args.size() < 2) {
                    reply("Usage: .config load <name>", Theme.warning());
                } else if (ShardClient.config().loadProfile(args.get(1))) reply("Profile \"" + args.get(1) + "\" loaded", Theme.success());
                else reply("No profile named \"" + args.get(1) + "\"", Theme.warning());
            }
            case "list" -> {
                List<String> profiles = ShardClient.config().profiles();
                reply(profiles.isEmpty() ? "No saved profiles" : "Profiles: " + String.join(", ", profiles), Theme.muted());
            }
            default -> reply("Usage: .config save|load|list [name]", Theme.warning());
        }
    }

    private Module module(List<String> args, int index) {
        if (args.size() <= index) {
            reply("Which module? Try .list", Theme.warning());
            return null;
        }
        Module m = ShardClient.modules().find(args.get(index));
        if (m == null) reply("No module named \"" + args.get(index) + "\"", Theme.warning());
        return m;
    }

    private void openLater(net.minecraft.client.gui.screens.Screen screen) {
        Minecraft mc = Minecraft.getInstance();
        // The chat screen is still open while this runs; switch on the next tick.
        mc.execute(() -> mc.setScreen(screen));
    }

    public static void reply(String text, int color) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        MutableComponent prefix = Component.literal("Shard ").withColor(Theme.accent() & 0xFFFFFF);
        MutableComponent body = Component.literal(text).withColor(color & 0xFFFFFF);
        mc.gui.getChat().addMessage(prefix.append(body));
    }
}
