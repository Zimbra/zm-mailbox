/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Oct 4, 2004
 */
package com.zimbra.common.util;

import com.zimbra.common.service.ServiceException;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author schemers
 */
public class StringUtil {

    /** A user-friendly version of <tt>String.equals()</tt> that handles one or both nulls easily. */
    public static boolean equal(String s1, String s2) {
        if (s1 == null || s2 == null)
            return s1 == s2;
        return s1.equals(s2);
    }
    
    /** A user-friendly version of <tt>String.equalsIgnoreCase()</tt> that handles one or both nulls easily. */
    public static boolean equalIgnoreCase(String s1, String s2) {
        if (s1 == null || s2 == null)
            return s1 == s2;
        return s1.equalsIgnoreCase(s2);
    }

    /**
     * A user-friendly compareTo that handles one or both nulls easily.
     * @param s1
     * @param s2
     * @return 0 if s1 and s2 are equal, value less than 0 if s1 is before s2,
     *         or value greater than 0 if s1 is after s2; null is considered
     *         to come before any non-null value
     */
    public static int compareTo(String s1, String s2) {
        if (s1 != null) {
            if (s2 != null)
                return s1.compareTo(s2);
            else
                return 1;
        } else {  // s1 == null
            if (s2 != null)
                return -1;
            else
                return 0;
        }
    }

    public static int countOccurrences(String str, char c) {
        if (str == null)
            return 0;
        int count = 0;
        for (int i = 0, len = str.length(); i < len; i++)
            if (str.charAt(i) == c)
                count++;
        return count;
    }

    public static String stripControlCharacters(String raw) {
        if (raw == null)
            return null;
        int i;
        for (i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            // invalid control characters
            if (c < 0x20 && c != 0x09 && c != 0x0A && c != 0x0D)
                break;
            // byte-order markers and high/low surrogates
            if (c == 0xFFFE || c == 0xFFFF || (c > 0xD7FF && c < 0xE000))
                break;
        }
        if (i >= raw.length())
            return raw;
        StringBuilder sb = new StringBuilder(raw.substring(0, i));
        for ( ; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c >= 0x20 || c == 0x09 || c == 0x0A || c == 0x0D)
                if (c != 0xFFFE && c != 0xFFFF && (c <= 0xD7FF || c >= 0xE000))
                    sb.append(c);
        }
        return sb.toString();
    }

    /** Returns whether the passed-in <code>String</code> is comprised only of
     *  printable ASCII characters.  The "printable ASCII characters" are CR,
     *  LF, TAB, and all characters from 0x20 to 0x7E. */
    public static boolean isAsciiString(String str) {
        if (str == null)
            return false;
        for (int i = 0, len = str.length(); i < len; i++) {
            char c = str.charAt(i);
            if ((c < 0x20 || c >= 0x7F) && c != '\r' && c != '\n' && c != '\t')
                return false;
        }
        return true;
    }

    /** Removes all spaces (and any character below 0x20) from the end of the
     *  passed-in <code>String</code>.  If nothing was trimmed, the original
     *  <code>String</code> is returned. */
    public static String trimTrailingSpaces(String raw) {
        if (raw == null)
            return null;
        int length = raw.length();
        while (length > 0 && raw.charAt(length - 1) <= ' ')
            length--;
        return length == raw.length() ? raw : raw.substring(0, length);
    }

    /**
     * add the name/value mapping to the map. If an entry doesn't exist, value remains
     * a String. If an entry already exists as a String, convert to String[] and add new
     * value. If entry already exists as a String[], grow array and add new value.
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
        for (int i = offset; i < args.length; i+=2) {
            String n = args[i];
            if (i+1 >= args.length)
                throw new IllegalArgumentException("not enough arguments");
            String v = args[i+1];
            addToMultiMap(attrs, n, v);
        }
        return attrs;
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
                if (sb == null) sb = new StringBuilder();
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
        if (value == null) return result;
        value = value.trim();
        if (value.length() == 0) return result;
        int i = 0;
        boolean inStr = false;
        boolean inList = false;
        StringBuilder sb = null;
        while(i < value.length()) {
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
        
        int i=0;
        
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
                        if (Character.isWhitespace(ch))
                            continue scan;
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

        if (sb.length() > 0)
            result.add(sb.toString());
        
        return result.toArray(new String[result.size()]);
    }

    private static void dump(String line) {
        String[] result = parseLine(line);
        System.out.println("line: "+line);
        for (int i=0; i < result.length; i++)
            System.out.println(i+": ("+result[i]+")");
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
                ZimbraLog.misc.info("fillTemplate(): could not find key '" + key + "'");
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
        if (array == null)
            return null;

        StringBuilder buf = new StringBuilder();
        for (int i = start, end = start + count; i < end; i++) {
            buf.append(array[i]);
            if (i + 1 < end)
                buf.append(delimiter);
        }
        return buf.toString();
    }

    public static String join(String delimiter, Iterable<? extends Object> array) {
        if (array == null)
            return null;

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
     * Returns the simple class name (the name after the last dot)
     * from a fully-qualified class name.  Behavior is the same as
     * {@link FileUtil#getExtension}. 
     */
    public static String getSimpleClassName(String className) {
        return FileUtil.getExtension(className);
    }
    
    /**
     * Returns the simple class name (the name after the last dot)
     * for the specified object.
     */
    public static String getSimpleClassName(Object o) {
        if (o == null) {
            return null;
        }
        return FileUtil.getExtension(o.getClass().getName());
    }
    
    /**
     * Returns <code>true</code> if the secified string is <code>null</code> or its
     * length is <code>0</code>.
     */
    public static boolean isNullOrEmpty(String s) {
        if (s == null || s.length() == 0) {
            return true;
        }
        return false;
    }

    private static final String[] JS_CHAR_ENCODINGS = {
        "\\u0000", "\\u0001", "\\u0002", "\\u0003", "\\u0004", "\\u0005", "\\u0006", "\\u0007",
        "\\b",     "\\t",     "\\n",     "\\u000B", "\\f",     "\\r",     "\\u000E", "\\u000F",
        "\\u0010", "\\u0011", "\\u0012", "\\u0013", "\\u0014", "\\u0015", "\\u0016", "\\u0017",
        "\\u0018", "\\u0019", "\\u001A", "\\u001B", "\\u001C", "\\u001D", "\\u001E", "\\u001F"
    };

    public static String jsEncode(Object obj) {
        if (obj == null)
            return "";
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
            if (sb == null)
                sb = new StringBuilder(str.substring(0, i));
            else
                sb.append(str.substring(last, i));
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
        if (text == null || text.length() == 0) return "";
		StringBuilder result = new StringBuilder(text.length());
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			switch (c) {
				case '<' : result.append("&lt;"); break;
				case '>' : result.append("&gt;"); break;
				case '&' : result.append("&amp;"); break;
				case '\"' : result.append("&quot;"); break;
				case '\'' : result.append("&#039;"); break;
				case 0x0a : // Follow through...
				case 0x0d : result.append(" "); break;
				default: result.append(c); break;
			}
		}
		return result.toString();
    }

    /**
     * Determines whether data can be encoded using <tt>requestedCharset</tt>.
     *  
     * @param data the data to be encoded
     * @param requestedCharset the character set
     * @return <tt>requestedCharset</tt> if encoding is supported, <tt>&quot;utf-8&quot;</tt>
     * if not, or <tt>&quot;us-ascii&quot;</tt> if data is <tt>null</tt>.
     */
    public static String checkCharset(String data, String requestedCharset) {
        if (data == null)
            return "us-ascii";

        if (requestedCharset != null && !requestedCharset.equalsIgnoreCase("utf-8")) {
            try {
                Charset cset = Charset.forName(requestedCharset);
                if (cset.canEncode() && cset.newEncoder().canEncode(data))
                    return requestedCharset;
            } catch (Exception e) {}
        }
        return "utf-8";
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
     * If the whole word is a Java Identifier, then it is prefixed with a "_".
     * @param s identifier that potentially needs escaping
     * @return escaped (if needed) identifier
     */
    public static String escapeJavaIdentifier(String s) {
        if (isNullOrEmpty(s)) return s;
        else if (isJavaReservedWord(s)) return s+"_";

        StringBuilder result = new StringBuilder();

        for (int i=0; i<s.length(); i++) {
            char ch = s.charAt(i);
            if (i == 0) result.append(Character.isJavaIdentifierStart(ch) ? ch : "_");
            else result.append(Character.isJavaIdentifierPart(ch) ? ch : "_");
         }
         return result.toString();
    }

    public static String capitalize(String s) {
        return s.substring(0,1).toUpperCase() + s.substring(1);
    }
}
