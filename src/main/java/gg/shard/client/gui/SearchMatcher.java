package gg.shard.client.gui;

import java.util.Locale;

/**
 * Ranking for the menu's search, pure so it is unit-tested. Every word of the query must appear
 * somewhere in the fields (so "outline colour" finds Block Outline → Colour); results that match
 * the name do best, then a word start in the name, then anything else. "color" and "colour" are
 * treated as the same word.
 */
public final class SearchMatcher {
    private SearchMatcher() {}

    /** -1 when {@code query} does not match; otherwise higher is better. {@code name} is the main field. */
    public static int score(String query, String name, String... others) {
        String q = normalise(query);
        if (q.isEmpty()) return 0;
        String n = normalise(name);
        StringBuilder all = new StringBuilder(n);
        for (String o : others) if (o != null) all.append(' ').append(normalise(o));
        String hay = all.toString();
        int score = 1;
        for (String word : q.split("\\s+")) {
            if (word.isEmpty()) continue;
            int at = hay.indexOf(word);
            if (at < 0) return -1;
            if (n.contains(word)) score += 10;
            if (n.startsWith(word) || n.contains(" " + word)) score += 5;
        }
        if (n.equals(q)) score += 100;
        else if (n.startsWith(q)) score += 50;
        return score;
    }

    static String normalise(String text) {
        if (text == null) return "";
        return text.toLowerCase(Locale.ROOT).replace("colour", "color").replace('-', ' ').trim();
    }
}
