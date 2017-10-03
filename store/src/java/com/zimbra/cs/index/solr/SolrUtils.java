package com.zimbra.cs.index.solr;

import java.util.regex.Pattern;

public class SolrUtils {
    private static final Pattern whitespace = Pattern.compile("\\s");
    private static final Pattern wildcard = Pattern.compile("(?<!\\\\)\\*");
    private static final String[] specialChars = new String[]{"&&", "||", "!", "(", ")", "{", "}", "[", "]", "^", "~", "?", ":"};

    public enum WildcardEscape {
        ALL, NONE, ZIMBRA
    }

    public static boolean isWildcardQuery(String text) {
        return wildcard.matcher(text).find();
    }

    public static boolean containsWhitespace(String text) {
        return whitespace.matcher(text).find();
    }

    public static String escapeSpecialChars(String text, WildcardEscape wildcards) {
        if (!containsWhitespace(text)) {
            for (int i = 0; i < specialChars.length; i++) {
                String c = specialChars[i];
                text = text.replace(c, "\\" + c);
            }
        }

        if (wildcards == WildcardEscape.ZIMBRA) {
            //escape asterisk if it's not at a valid wildcard location
            int index = text.indexOf("*");
            while (index >= 0) {
                if (index != 0 && index != text.length() - 1 && !Character.isWhitespace(text.charAt(index + 1))) {
                    text = text.substring(0, index) + "\\" + text.substring(index, text.length());
                    index++;
                }
                index = text.indexOf("*", index + 1);
            }
        } else if (wildcards == WildcardEscape.ALL) {
            text = text.replace("*", "\\*");
        }

        return text;
    }

    public static String escapeQuotes(String text) {
        return text.replace("\"", "\\\"");
    }

    public static String quoteText(String text) {
        return "\"" + escapeQuotes(text) + "\"";
    }
}
