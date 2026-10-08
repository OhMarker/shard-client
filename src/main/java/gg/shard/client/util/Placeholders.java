package gg.shard.client.util;

import java.util.Locale;
import java.util.Map;

/** {name}-style placeholder substitution for user-editable message formats. No Minecraft imports. */
public final class Placeholders {
    private Placeholders() {}

    /**
     * Replaces every {@code {key}} whose key is in {@code values}; unknown placeholders and stray
     * braces are left exactly as typed so a typo never eats the message.
     */
    public static String format(String template, Map<String, String> values) {
        if (template == null || template.isEmpty()) return "";
        StringBuilder out = new StringBuilder(template.length() + 16);
        int i = 0;
        while (i < template.length()) {
            char c = template.charAt(i);
            if (c == '{') {
                int close = template.indexOf('}', i + 1);
                if (close > i) {
                    String key = template.substring(i + 1, close).trim().toLowerCase(Locale.ROOT);
                    String value = values.get(key);
                    if (value != null) {
                        out.append(value);
                        i = close + 1;
                        continue;
                    }
                }
            }
            out.append(c);
            i++;
        }
        return out.toString();
    }

    /** The totem pop line: {name}, {count}, {ordinal} and {s} (plural suffix) are available. */
    public static String popMessage(String template, String name, int count) {
        return format(template, Map.of(
                "name", name,
                "count", String.valueOf(count),
                "ordinal", ordinal(count),
                "s", count == 1 ? "" : "s"));
    }

    public static String ordinal(int n) {
        if (n % 100 >= 11 && n % 100 <= 13) return n + "th";
        return switch (n % 10) {
            case 1 -> n + "st";
            case 2 -> n + "nd";
            case 3 -> n + "rd";
            default -> n + "th";
        };
    }
}
