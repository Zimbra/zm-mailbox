/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Oct 4, 2004
 */
package com.zimbra.common.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;

/**
 * @author schemers
 */
public class StringUtil {

    /** A user-friendly version of {@link String#equals(Object)} that handles
     *  one or both nulls easily. */
    public static boolean equal(String s1, String s2) {
        if (s1 == null || s2 == null) {
            return s1 == s2;
        }
        return s1.equals(s2);
    }

    /** A user-friendly version of {@link String#equalsIgnoreCase(String)}
     *  that handles one or both nulls easily. */
    public static boolean equalIgnoreCase(String s1, String s2) {
        if (s1 == null || s2 == null) {
            return s1 == s2;
        }
        return s1.equalsIgnoreCase(s2);
    }

    /** A user-friendly compareTo that handles one or both nulls easily.
     * @return 0 if s1 and s2 are equal, value less than 0 if s1 is before s2,
     *         or value greater than 0 if s1 is after s2; null is considered
     *         to come before any non-null value */
    public static int compareTo(String s1, String s2) {
        if (s1 != null) {
            if (s2 != null) {
                return s1.compareTo(s2);
            } else {
                return 1;
            }
        } else {  // s1 == null
            if (s2 != null) {
                return -1;
            } else {
                return 0;
            }
        }
    }

    /** Compares two string ignoring case differences.
     *
     */
    public static int compareToIgnoreCase(String s1, String s2) {
        if (s1 != null) {
            if (s2 != null) {
                return s1.compareToIgnoreCase(s2);
            } else {
                return 1;
            }
        } else {  // s1 == null
            if (s2 != null) {
                return -1;
            } else {
                return 0;
            }
        }
    }

    public static int countOccurrences(String str, char c) {
        if (str == null) {
            return 0;
        }

        int count = 0;
        for (int i = 0, len = str.length(); i < len; i++) {
            if (str.charAt(i) == c) {
                count++;
            }
        }
        return count;
    }

    /** Returns the first instance in {@code str} of any character in {@code
     *  values}, or {@code -1} if no characters from {@code values} appear in
     *   {@code str}. */
    public static int indexOfAny(String str, String values) {
        if (!isNullOrEmpty(str) && !isNullOrEmpty(values)) {
            for (int i = 0, len = str.length(); i < len; i++) {
                if (values.indexOf(str.charAt(i)) != -1) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Replaces control characters in the filename with space. Windows has
     * a problem with downloading such filenames.
     *
     * @param filename filename to sanitize
     * @return sanitized filename
     */
    public static String sanitizeFilename(String filename) {
        if (Strings.isNullOrEmpty(filename)) {
            return filename;
        }
        return filename.replaceAll("\\p{Cntrl}", " ");
    }

    /** Returns the passed-in {@code string} with all XML-unsafe characters
     *  removed.  If nothing needs to be removed, the original {@code String}
     *  is returned.  Those characters considered "unsafe" are:<ul>
     *    <li>control characters below 0x20 other than TAB, CR, and LF
     *    <li>byte-order markers (0xFFFE and 0xFFFF)
     *    <li>unmatched UTF-16 surrogates</ul>
     *  XML 1.1 permits all control characters other than NUL (0x00), but
     *  this method sanitizes for XML 1.0 with its more restrictive rules. */
    public static String stripControlCharacters(String string) {
        if (isNullOrEmpty(string)) {
            return string;
        }

        StringBuilder sb = null;
        int start = 0;
        for (int i = 0, len = string.length(); i < len; i++) {
            for ( ; i < len; i++) {
                char c = string.charAt(i);
                // invalid control characters (note: XML 1.1 disallows only 0x00)
                if (c < 0x20 && c != 0x09 && c != 0x0A && c != 0x0D) {
                    break;
                }
                // byte-order markers and unmatched low surrogates
                if (c == 0xFFFE || c == 0xFFFF || Character.isLowSurrogate(c)) {
                    break;
                }
                // high surrogates without a subsequent low surrogate
                if (Character.isHighSurrogate(c)) {
                    if (i == string.length() - 1 || !Character.isLowSurrogate(string.charAt(i + 1))) {
                        break;
                    }
                    i++;
                }
            }
            if (i >= len) {
                break;
            }

            if (sb == null) {
                sb = new StringBuilder(len - 1);
            }
            if (start < i) {
                sb.append(string.substring(start, i));
            }
            start = i + 1;
        }

        if (sb == null) {
            return string;
        } else {
            start = Math.min(start, string.length());
            return sb.append(string.substring(start)).toString();
        }
    }

    /** Replaces all high and low surrogate character pairs (0xD800-0xDFFF)
     *  with the '?' character.  This is sometimes needed when handing data to
     *  third-party applications which cannot handle characters outside the
     *  Basic Multilingual Plane (U+10000 and higher). */
    public static String removeSurrogates(String string) {
        if (isNullOrEmpty(string)) {
            return string;
        }

        StringBuilder sb = null;
        int start = 0;
        for (int i = 0, len = string.length(); i < len; i++) {
            char c = string.charAt(i);
            if (c >= 0xD800 && c <= 0xDFFF) {
                if (sb == null) {
                    sb = new StringBuilder(len - 1);
                }
                if (start < i) {
                    sb.append(string.substring(start, i));
                }
                sb.append('?');
                start = ++i + 1;
            }
        }
        if (sb == null) {
            return string;
        } else {
            start = Math.min(start, string.length());
            return sb.append(string.substring(start)).toString();
        }
    }

    /** Returns whether the passed-in {@code string} contains any surrogates
     *  (0xD800-0xDFFF).  Two surrogate chars are used to encode a character
     *  outside the Basic Multilingual Plane (U+10000 and higher). */
    public static boolean containsSurrogates(String string) {
        if (!isNullOrEmpty(string)) {
            for (int i = 0, len = string.length(); i < len; i++) {
                char c = string.charAt(i);
                if (c >= 0xD800 && c <= 0xDFFF) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Returns whether the passed-in {@code String} is comprised only of
     *  printable ASCII characters.  The "printable ASCII characters" are CR,
     *  LF, TAB, and all characters from 0x20 to 0x7E.  If the argument is
     *  {@code null}, returns {@code false}. */
    public static boolean isAsciiString(String string) {
        if (string == null) {
            return false;
        }

        for (int i = 0, len = string.length(); i < len; i++) {
            char c = string.charAt(i);
            if ((c < 0x20 || c >= 0x7F) && c != '\r' && c != '\n' && c != '\t') {
                return false;
            }
        }
        return true;
    }

    /** Removes all spaces (and any character below 0x20) from the end of the
     *  passed-in {@code String}.  If nothing was trimmed, the original
     *  {@code String} is returned. */
    public static String trimTrailingSpaces(String raw) {
        if (raw == null) {
            return null;
        }

        int length = raw.length();
        while (length > 0 && raw.charAt(length - 1) <= ' ') {
            length--;
        }
        return length == raw.length() ? raw : raw.substring(0, length);
    }

    /** Add the name/value mapping to the map. If an entry doesn't exist,
     *  value remains a {@code String}.  If an entry already exists as a
     *  {@code String}, convert to {@code String[]} and add new value.
     *  If entry already exists as a {@code String[]}, grow array and add
     *  new value.
     * @param result result map
     * @param name
     * @param value
     */
    public static void addToMultiMap(Map<String, Object> result, String name, String value) {
        Object currentValue = result.get(name);
        if (currentValue == null) {
            result.put(name, value);
        } else if (currentValue instanceof String){
            result.put(name, new String[] { (String)currentValue, value});
        } else if (currentValue instanceof String[]) {
            String[] ov = (String[]) currentValue;
            String[] nv = new String[ov.length+1];
            System.arraycopy(ov, 0, nv, 0, ov.length);
            nv[ov.length] = value;
            result.put(name, nv);
        }
    }

    /**
     * Convert an array of the form:
     *
     *    a1 v1 a2 v2 a2 v3
     *
     * to a map of the form:
     *
     *    a1 -> v1
     *    a2 -> [v2, v3]
     */
    public static Map<String, Object> keyValueArrayToMultiMap(String[] args, int offset) {
        Map<String, Object> attrs = new HashMap<String, Object>();
        for (int i = offset; i < args.length; i += 2) {
            String n = args[i];
            if (i + 1 >= args.length) {
                throw new IllegalArgumentException("not enough arguments");
            }
            String v = args[i + 1];
            addToMultiMap(attrs, n, v);
        }
        return attrs;
    }

    /**
     * Converts an old-style multimap to Guava's version.
     */
    public static Multimap<String, String> toNewMultimap(Map<String, Object> oldMultimap) {
        Multimap<String, String> newMap = ArrayListMultimap.create();
        for (String key : oldMultimap.keySet()) {
            Object value = oldMultimap.get(key);
            if (value instanceof String[]) {
                for (String sVal : (String[]) value) {
                    newMap.put(key, sVal);
                }
            } else if (value == null) {
                newMap.put(key, null);
            } else {
                newMap.put(key, value.toString());
            }
        }
        return newMap;
    }

    /**
     * Converts a Guava multimap to an old-style version.
     */
    public static Map<String, Object> toOldMultimap(Multimap<String, String> newMultimap) {
        Map<String, Object> oldMap = new HashMap<String, Object>();
        for (Map.Entry<String, String> entry : newMultimap.entries()) {
            addToMultiMap(oldMap, entry.getKey(), entry.getValue());
        }
        return oldMap;
    }

    public static String[] toStringArray(Object obj) {

        if (obj == null) {
            return null;
        }

        String[] strArray;
        if (obj instanceof String) {
            strArray = new String[]{(String)obj};
        } else if (obj instanceof String[]) {
            strArray = (String[])obj;
        } else {
            throw new UnsupportedOperationException();
        }
        return strArray;
    }

    private static final int TERM_WHITESPACE = 1;
    private static final int TERM_SINGLEQUOTE = 2;
    private static final int TERM_DBLQUOTE = 3;

    /**
     * open the specified file and return the first line in the file, without the end of line character(s).
     * @param file
     * @return
     * @throws IOException
     */
    public static String readSingleLineFromFile(String file) throws IOException {
        InputStream is = null;
        try {
            is = new FileInputStream(file);
            BufferedReader in = new BufferedReader(new InputStreamReader(is));
            return in.readLine();
        } finally {
            ByteUtil.closeStream(is);
        }
    }

    /**
     * read a line from "in", using readLine(). A trailing '\\' on the line will
     * be treated as continuation and the next line will be read and appended to the line,
     * without the \\.
     * @param in
     * @return complete line or null on end of file.
     * @throws IOException
     */
    public static String readLine(BufferedReader in) throws IOException {
        String line;
        StringBuilder sb = null;

        while ((line = in.readLine()) != null) {
            if (line.length() == 0) {
                break;
            } else if (line.charAt(line.length()-1) == '\\') {
                if (sb == null) {
                    sb = new StringBuilder();
                }
                sb.append(line.substring(0, line.length()-1));
            } else {
                break;
            }
        }

        if (line == null) {
            if (sb == null) {
                return null;
            } else {
                return sb.toString();
            }
        } else {
            if (sb == null) {
                return line;
            } else {
                sb.append(line);
                return sb.toString();
            }
        }
    }

    public static List<String> parseSieveStringList(String value) throws ServiceException {
        List<String> result = new ArrayList<String>();
        if (value == null) {
            return result;
        }
        value = value.trim();
        if (value.isEmpty()) {
            return result;
        }

        int i = 0;
        boolean inStr = false;
        boolean inList = false;
        StringBuilder sb = null;
        while (i < value.length()) {
            char ch = value.charAt(i++);
            if (inStr) {
                if (ch == '"') {
                    result.add(sb.toString());
                    inStr = false;
                } else {
                    if (ch == '\\' && i < value.length())
                        ch = value.charAt(i++);
                    sb.append(ch);
                }
            } else {
                if (ch == '"') {
                    inStr = true;
                    sb = new StringBuilder();
                } else if (ch == '[' && !inList) {
                    inList = true;
                } else if (ch ==']' && inList) {
                    inList = false;
                } else if (!Character.isWhitespace(ch)) {
                    throw ServiceException.INVALID_REQUEST("unable to parse string list: "+value, null);
                }
            }
        }
        if (inStr || inList) {
            throw ServiceException.INVALID_REQUEST("unable to parse string list2: "+value, null);
        }
        return result;
    }


    /**
     * split a line into array of Strings, using a shell-style syntax for tokenizing words.
     *
     * @param line
     * @return
     */
    public static String[] parseLine(String line) {
        ArrayList<String> result = new ArrayList<String>();

        int i = 0;

        StringBuilder sb = new StringBuilder(32);
        int term = TERM_WHITESPACE;
        boolean inStr = false;

        scan: while (i < line.length()) {
            char ch = line.charAt(i++);
            boolean escapedTerm = false;

            if (ch == '\\' && i < line.length()) {
                ch = line.charAt(i++);
                switch (ch) {
                    case '\\':
                        break;
                    case 'n':
                        ch = '\n';
                        escapedTerm = true;
                        break;
                    case 't':
                        ch = '\t';
                        escapedTerm = true;
                        break;
                    case 'r':
                        ch = '\r';
                        escapedTerm = true;
                        break;
                    case '\'':
                        ch = '\'';
                        escapedTerm = true;
                        break;
                    case '"':
                        ch = '"';
                        escapedTerm = true;
                        break;
                    default:
                        escapedTerm = Character.isWhitespace(ch);
                        break;
                }
            }

            if (inStr) {
                if (!escapedTerm && (
                            (term == TERM_WHITESPACE && Character.isWhitespace(ch)) ||
                            (term == TERM_SINGLEQUOTE && ch == '\'') ||
                            (term == TERM_DBLQUOTE && ch == '"'))) {
                    inStr = false;
                    result.add(sb.toString());
                    sb = new StringBuilder(32);
                    term = TERM_WHITESPACE;
                    continue scan;
                }
                sb.append(ch);
            } else {
                if (!escapedTerm) {
                    switch (ch) {
                        case '\'':
                            term = TERM_SINGLEQUOTE;
                            inStr = true;
                            continue scan;
                        case '"':
                            term = TERM_DBLQUOTE;
                            inStr = true;
                            continue scan;
                        default:
                            if (Character.isWhitespace(ch)) {
                                continue scan;
                            }
                            inStr = true;
                            sb.append(ch);
                            break;
                    }
                } else {
                    // we had an escaped terminator, start a new string
                    inStr = true;
                    sb.append(ch);
                }
            }
        }

        if (sb.length() > 0) {
            result.add(sb.toString());
        }

        return result.toArray(new String[result.size()]);
    }

    private static void dump(String line) {
        String[] result = parseLine(line);
        System.out.println("line: "+line);
        for (int i=0; i < result.length; i++) {
            System.out.println(i + ": (" + result[i] + ")");
        }
        System.out.println();
    }

    public static void main(String args[]) {
        dump("this is a test");
        dump("this is 'a nother' test");
        dump("this is\\ test");
        dump("first Roland last 'Schemers' full 'Roland Schemers'");
        dump("multi 'Roland\\nSchemers'");
        dump("a");
        dump("");
        dump("\\  \\ ");
        dump("backslash \\\\");
        dump("backslash \\f");
        dump("a           b");
    }

    // A pattern that matches the beginning of a string followed by ${KEY_NAME} followed
    // by the end.  There are three groups:  the beginning, KEY_NAME and the end.
    // Pattern.DOTALL is required in case one of the values in the map has a newline
    // in it.
    public static Pattern atPattern =
        Pattern.compile("(.*)\\@([^\\@]+)\\@(.*)", Pattern.DOTALL);

    public static Pattern varPattern =
        Pattern.compile("(.*)\\$\\{([^\\}]+)\\}(.*)", Pattern.DOTALL);

    /**
     * Substitutes all occurrences of the specified values into a template. Keys
     * for the values are specified in the template as <code>${KEY_NAME}</code>.
     * @param template the template
     * @param vars a <code>Map</code> filled with keys and values
     * @return the template with substituted values
     */
    public static String fillTemplate(String template, Map<String, ? extends Object> vars) {
        return fillTemplate(template, vars, varPattern);
    }

    /**
     * Substitutes all occurrences of the specified values into a template. Keys
     * for the values are specified in the template as a regex <code>Pattern</code>.
     * @param template the template
     * @param vars a <code>Map</code> filled with keys and values
     * @param pattern a <code>Pattern</code> to match tokens
     * @return the template with substituted values
     */
    public static String fillTemplate(String template, Map<String, ? extends Object> vars, Pattern pattern) {
        if (template == null) {
            return null;
        }

        String line = template;
        Matcher matcher = pattern.matcher(line);

        // Substitute multiple variables per line
        while (matcher.matches()) {
            String key = matcher.group(2);
            Object value = vars.get(key);
            if (value == null) {
                ZimbraLog.misc.error("fillTemplate(): could not find key '" + key + "'");
                value = "";
            }
            line = matcher.group(1) + value + matcher.group(3);
            matcher.reset(line);
        }
        return line;
    }

    /**
     * Joins an array of <code>short</code>s, separated by a delimiter.
     */
    public static String join(String delimiter, short[] array) {
        if (array == null) {
            return null;
        }

        StringBuilder buf = new StringBuilder();

        for (int i = 0; i < array.length; i++) {
            buf.append(array[i]);
            if (i + 1 < array.length) {
                buf.append(delimiter);
            }
        }
        return buf.toString();
    }

    /**
     * Joins an array of objects, separated by a delimiter.
     */
    public static String join(String delimiter, Object[] array) {
        return (array == null ? null : join(delimiter, array, 0, array.length));
    }

    public static String join(String delimiter, Object[] array, final int start, final int count) {
        if (array == null) {
            return null;
        }

        StringBuilder buf = new StringBuilder();
        for (int i = start, end = start + count; i < end; i++) {
            buf.append(array[i]);
            if (i + 1 < end) {
                buf.append(delimiter);
            }
        }
        return buf.toString();
    }

    public static String join(String delimiter, Iterable<? extends Object> array) {
        if (array == null) {
            return null;
        }

        boolean firstTime = true;
        StringBuilder buf = new StringBuilder();
        for (Object obj : array) {
            if (firstTime) {
                firstTime = false;
            } else {
                buf.append(delimiter);
            }
            buf.append(obj);
        }
        return buf.toString();
    }

    /**
     * Returns <code>true</code> if the secified string is <code>null</code> or its
     * length is <code>0</code>.
     */
    public static boolean isNullOrEmpty(String s) {
        return s == null || s.isEmpty();
    }

    private static final String[] JS_CHAR_ENCODINGS = {
        "\\u0000", "\\u0001", "\\u0002", "\\u0003", "\\u0004", "\\u0005", "\\u0006", "\\u0007",
        "\\b",     "\\t",     "\\n",     "\\u000B", "\\f",     "\\r",     "\\u000E", "\\u000F",
        "\\u0010", "\\u0011", "\\u0012", "\\u0013", "\\u0014", "\\u0015", "\\u0016", "\\u0017",
        "\\u0018", "\\u0019", "\\u001A", "\\u001B", "\\u001C", "\\u001D", "\\u001E", "\\u001F"
    };

    public static String jsEncode(Object obj) {
        if (obj == null) {
            return "";
        }

        String replacement, str = obj.toString();
        StringBuilder sb = null;
        int i, last, length = str.length();
        for (i = 0, last = -1; i < length; i++) {
            char c = str.charAt(i);
            switch (c) {
                case '<':       replacement = "\\u003C";             break;
                case '>':       replacement = "\\u003E";             break;
                case '\\':      replacement = "\\\\";                break;
                case '"':       replacement = "\\\"";                break;
                case '\u2028':  replacement = "\\u2028";             break;
                case '\u2029':  replacement = "\\u2029";             break;
                default:        if (c >= ' ')                        continue;
                                replacement = JS_CHAR_ENCODINGS[c];  break;
            }
            if (sb == null) {
                sb = new StringBuilder(str.substring(0, i));
            } else {
                sb.append(str.substring(last, i));
            }
            sb.append(replacement);
            last = i + 1;
        }
        return (sb == null ? str : sb.append(str.substring(last, i)).toString());
    }

    public static String jsEncodeKey(String key) {
        return '"' + key + '"';
    }

    //
    // HTML methods
    //

    /**
     * Escapes special characters with their HTML equivalents.
     */
    public static String escapeHtml(String text) {
        if (text == null || text.length() == 0) {
            return "";
        }

        StringBuilder result = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '<':  result.append("&lt;");   break;
                case '>':  result.append("&gt;");   break;
                case '&':  result.append("&amp;");  break;
                case '\"': result.append("&quot;"); break;
                case '\'': result.append("&#039;"); break;
                case 0x0A: // fall through...
                case 0x0D: result.append(" ");      break;
                default:   result.append(c);        break;
            }
        }
        return result.toString();
    }

    /**
     * Unescapes a string containing entity escapes to a string containing the actual Unicode characters
     * corresponding to the escapes.
     */
    public static String unEscapeHtml(String text) {
        if (text == null || text.length() == 0) {
            return "";
        }

        String result = text;
        result = result.replace("&lt;", "<");
        result = result.replace("&gt;", ">");
        result = result.replace("&amp;", "&");
        result = result.replace("&quot;", "\"");
        result = result.replace("&#034;", "\"");
        result = result.replace("&#039;", "\'");

        return result;
    }

    private static Set<String> sJavaReservedWords =
            new HashSet<String>(Arrays.asList(
                    "abstract",
                    "assert",
                    "boolean",
                    "break",
                    "byte",
                    "case",
                    "catch",
                    "char",
                    "class",
                    "const",
                    "continue",
                    "default",
                    "do",
                    "double",
                    "else",
                    "enum",
                    "extends",
                    "false",
                    "final",
                    "finally",
                    "float",
                    "for",
                    "goto",
                    "if",
                    "implements",
                    "import",
                    "instanceof",
                    "int",
                    "interface",
                    "long",
                    "native",
                    "new",
                    "null",
                    "package",
                    "private",
                    "protected",
                    "public",
                    "return",
                    "short",
                    "static",
                    "strictfp",
                    "super",
                    "switch",
                    "synchronized",
                    "this",
                    "throw",
                    "throws",
                    "transient",
                    "true",
                    "try",
                    "void",
                    "volatile",
                    "while"
            ));

    public static boolean isJavaReservedWord(String s) {
        return sJavaReservedWords.contains(s);
    }

    /**
     * Escape (if needed) given identifer. Invalid characters are replaced with an "_".
     * If the whole word is a Java Identifier, then it is suffixed with a "_".
     * @param s identifier that potentially needs escaping
     * @return escaped (if needed) identifier
     */
    public static String escapeJavaIdentifier(String s) {
        if (isNullOrEmpty(s)) {
            return s;
        } else if (isJavaReservedWord(s)) {
            return s + "_";
        }

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (i == 0) {
                result.append(Character.isJavaIdentifierStart(ch) ? ch : "_" + ch);
            } else {
                result.append(Character.isJavaIdentifierPart(ch) ? ch : "_");
            }
         }
         return result.toString();
    }

    public static String capitalize(String s) {
        return s.substring(0,1).toUpperCase() + s.substring(1);
    }

    public static String enclose(String strToEnclose, char encloseWith) {
        return new StringBuilder().append(encloseWith).append(strToEnclose).append(encloseWith).toString();
    }

    private static Map<String, Pattern> patternCache = MapUtil.newLruMap(1000);

    /**
     * Returns a new {@code Matcher} object for the given regular expression and
     * string.  The underlying {@link Pattern} is cached, so that regular expressions
     * aren't recompiled.
     */
    public static Matcher newMatcher(String regex, CharSequence s) {
        return getCachedPattern(regex).matcher(s);
    }

    /**
     * Returns a precompiled {@link Pattern} for the given regular expression.
     */
    public static Pattern getCachedPattern(String regex) {
        Pattern pattern = null;
        synchronized (patternCache) {
            pattern = patternCache.get(regex);
        }

        if (pattern == null) {
            pattern = Pattern.compile(regex);
            synchronized (patternCache) {
                patternCache.put(regex, pattern);
            }
        }
        return pattern;
    }

    /**
     * Does the same thing as {@link String#replaceAll(String, String)}, but uses
     * the pattern cache to avoid recompiling.
     */
    public static String replaceAll(CharSequence s, String regex, String replacement) {
        if (s == null) {
            return null;
        }
        Matcher m = newMatcher(regex, s);
        return m.replaceAll(replacement);
    }

    /**
     * Converts all instances of LF to CRLF.  Preserves existing CRLF combinations.
     */
    public static String lfToCrlf(CharSequence s) {
        return replaceAll(s, "\r?\n", "\r\n");
    }

    /** Replaces all non-printable-ASCII characters with their Java Unicode escape sequences. */
    public static String escapeString(String string) {
        if (Strings.isNullOrEmpty(string))
            return string;

        StringBuilder sb = new StringBuilder(string.length());
        for (int index = 0, len = string.length(); index < len; index++) {
            char c = string.charAt(index);
            if (c >= 0x20 && c < 0x7F) {
                sb.append(c);
            } else if (c == '\t') {
                sb.append("\\t");
            } else if (c == '\r') {
                sb.append("\\r");
            } else if (c == '\n') {
                sb.append("\\n");
            } else {
                sb.append(String.format("\\u%04x", (int) c));
            }
        }
        return sb.toString();
    }

    public static boolean isUUID(String value) {
        if (value.length() == 36 &&
            value.charAt(8) == '-' &&
            value.charAt(13) == '-' &&
            value.charAt(18) == '-' &&
            value.charAt(23) == '-')
            return true;
        return false;
    }

    public static String nonNull(String s) {
        return s == null ? "" : s;
    }

    public static String truncateIfRequired(String body, int maxBodyBytes) {
        try {
            byte[] bodyBytes = body.getBytes(MimeConstants.P_CHARSET_UTF8);
            if (maxBodyBytes > -1 && bodyBytes.length > maxBodyBytes) {
                // During truncation make sure that we don't end up with a partial char at the end of the body.
                // Start from index maxBodyBytes and going backwards determine the first byte that is a starting
                // byte for a character. Such a byte is one whose top bit is 0 or whose top 2 bits are 11.
                int indexToExclude = maxBodyBytes;
                while (indexToExclude > 0 && bodyBytes[indexToExclude] < -64) {
                    indexToExclude--;
                }
                return new String(bodyBytes, 0, indexToExclude, MimeConstants.P_CHARSET_UTF8);
            }
        } catch (UnsupportedEncodingException e) {
            ZimbraLog.filter.error("Error while truncating body", e);
        }
        return body;
    }

    /**
     * Removes all occurrences of "Other, Control", "Other, Private Use",
     * "Other, Unassigned", "Other, Format" and "Other, Surrogate"
     * Replaces all "Separator, *" characters with a simple space (U+0020)
     *
     * @param string string to sanitize
     * @return sanitized string
     */
    public static String sanitizeString(String string) {
        if (string != null) {
            StringBuilder sanitizedString = new StringBuilder(string.length());
            for (int offset = 0; offset < string.length();) {
                int codePoint = string.codePointAt(offset);
                offset += Character.charCount(codePoint);
                // Remove invisible control characters and unused code points
                switch (Character.getType(codePoint)) {
                    case Character.CONTROL: // \p{Cc}
                    case Character.FORMAT: // \p{Cf}
                    case Character.PRIVATE_USE: // \p{Co}
                    case Character.SURROGATE: // \p{Cs}
                    case Character.UNASSIGNED: // \p{Cn}
                        break;
                    case Character.LINE_SEPARATOR:
                    case Character.PARAGRAPH_SEPARATOR:
                    case Character.SPACE_SEPARATOR:
                        sanitizedString.append(" ");
                        break;
                    default:
                        sanitizedString.append(Character.toChars(codePoint));
                        break;
                }
            }
            return sanitizedString.toString();
        }
        return null;
    }

    public static String maskEmail(String recoveryEmail) {
        if (isNullOrEmpty(recoveryEmail)) {
            return null;
        }
        StringBuilder maskedEmail = new StringBuilder();
        String[] parts = EmailUtil.getLocalPartAndDomain(recoveryEmail);
        String local = parts[0];
        int len = local.length();
        switch (len) {
            case 1:
                maskedEmail.append("*");
                break;
            case 2:
                maskedEmail.append(local.charAt(0)).append("*");
                break;
            case 3:
                maskedEmail.append(local.charAt(0)).append("**");
                break;
            default:
                int maskLen = len - 3;
                maskedEmail.append(local.substring(0, 3)).append(new String(new char[maskLen]).replace("\0", "*"));
                break;
        }
        maskedEmail.append("@").append(parts[1]);
        return maskedEmail.toString();
    }
}
