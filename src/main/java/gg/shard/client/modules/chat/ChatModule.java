package gg.shard.client.modules.chat;

import gg.shard.client.module.Module;
import gg.shard.client.module.ModuleCategory;
import gg.shard.client.module.setting.BoolSetting;
import gg.shard.client.module.setting.ColorSetting;
import gg.shard.client.module.setting.StringSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Chat: timestamps, repeated lines stacked as "(x3)", join and leave messages hidden, and your
 * name or keywords highlighted. Only how incoming chat is shown changes; nothing is sent.
 */
public final class ChatModule extends Module {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final BoolSetting timestamps = add(new BoolSetting("Timestamps", "Show the time before each message", true));
    private final BoolSetting stack = add(new BoolSetting("Stack repeats", "Show a repeated message once with (x2), (x3)...", true));
    private final BoolSetting hideJoins = add(new BoolSetting("Hide joins and leaves", "Hide \"joined the game\" and \"left the game\" lines", false));
    private final BoolSetting highlight = add(new BoolSetting("Highlight mentions", "Colour messages that mention your name or a keyword", true));
    private final StringSetting keywords = add(new StringSetting("Keywords", "Extra words to highlight, separated by commas", "", 120));
    private final ColorSetting color = add(new ColorSetting("Highlight colour", "Colour for mentions", 0xFFFACC15, false));

    private String lastText = "";
    private int repeats = 1;

    public ChatModule() {
        super("Chat", "Timestamps, stacked repeats, hidden joins and highlighted mentions.", ModuleCategory.CHAT);
        keywords.visibleWhen(highlight::get);
        color.visibleWhen(highlight::get);
    }

    @Override
    public String icon() {
        return "chat";
    }

    @Override
    public String about() {
        return "Cleans up incoming chat: a timestamp on each line, repeated messages stacked into one with a count, join and leave spam hidden, "
                + "and messages that mention you or your keywords coloured. Nothing is sent and nothing is filtered on the server.";
    }

    /** True to drop the message entirely. */
    public boolean hide(Component message) {
        if (!isEnabled() || !hideJoins.get()) return false;
        if (message.getContents() instanceof TranslatableContents t) {
            String key = t.getKey();
            return key.equals("multiplayer.player.joined") || key.equals("multiplayer.player.left") || key.equals("multiplayer.player.joined.renamed");
        }
        return false;
    }

    /** Whether the previous line should be replaced (a repeat), and the count to show. */
    public int repeatCount(Component message) {
        if (!isEnabled() || !stack.get()) return 1;
        String text = message.getString();
        if (text.equals(lastText)) repeats++;
        else {
            lastText = text;
            repeats = 1;
        }
        return repeats;
    }

    /** The message as shown: timestamp, highlight and repeat count. */
    public Component decorate(Component message, int count) {
        if (!isEnabled()) return message;
        MutableComponent out = Component.empty();
        if (timestamps.get()) out.append(Component.literal("[" + LocalTime.now().format(TIME) + "] ").withStyle(Style.EMPTY.withColor(0x8A8F98)));
        MutableComponent body = message.copy();
        if (highlight.get() && mentions(message.getString())) body = body.withStyle(s -> s.withColor(color.get() & 0xFFFFFF));
        out.append(body);
        if (count > 1) out.append(Component.literal(" (x" + count + ")").withStyle(Style.EMPTY.withColor(0x8A8F98)));
        return out;
    }

    private boolean mentions(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        var p = Minecraft.getInstance().player;
        if (p != null) {
            String name = p.getName().getString().toLowerCase(Locale.ROOT);
            // Not our own chat line ("<name> ...").
            if (!lower.startsWith("<" + name + ">") && lower.contains(name)) return true;
        }
        for (String k : keywords.get().split(",")) {
            String kk = k.trim().toLowerCase(Locale.ROOT);
            if (!kk.isEmpty() && lower.contains(kk)) return true;
        }
        return false;
    }
}
