package com.zimbra.cs.mailtest;

import java.io.OutputStream;
import java.io.IOException;

public final class Chars {
    // ATOM-CHAR       = <any CHAR except atom-specials>
    // atom-specials   = "(" / ")" / "{" / SP / CTL / list-wildcards /
    //                   quoted-specials / resp-specials
    // list-wildcards  = "%" / "*"
    // quoted-specials = DQUOTE / "\"
    //
    // resp-specials   = "]"
    //
    // ASTRING-CHAR    = ATOM-CHAR / resp-specials
    //
    // tag             = 1*<any ASTRING-CHAR except "+">

    public static final boolean[] ATOM_CHARS = new boolean[256];
    public static final boolean[] ASTRING_CHARS = new boolean[256];
    public static final boolean[] NUMBER_CHARS = new boolean[256];
    public static final boolean[] TEXT_CHARS = new boolean[256];

    private static final String SPECIALS = "(){%*\"\\";

    static {
        for (int i = 0x21; i < 0x7f; i++) {
            ATOM_CHARS[i] = ASTRING_CHARS[i] = true;
        }
        for (int i = 1; i < 0xff; i++) {
            TEXT_CHARS[i] = true;
        }
        set(ATOM_CHARS, SPECIALS + "]", false);
        set(ASTRING_CHARS, SPECIALS, false);
        set(NUMBER_CHARS, "0123456789", true);
        set(TEXT_CHARS, "\000\r\n", false);
    }

    private static void set(boolean[] ba, String chars, boolean b) {
        for (int i = 0; i < chars.length(); i++) {
            ba[chars.charAt(i)] = b;
        }
    }

    public static boolean isAtom(char c) {
        return ATOM_CHARS[c];
    }

    public static boolean isAString(char c) {
        return ASTRING_CHARS[c];
    }

    public static boolean isNumber(char c) {
        return NUMBER_CHARS[c];
    }

    public static boolean isText(char c) {
        return TEXT_CHARS[c];
    }

    public static boolean isAtom(String s) {
        return isValid(s, ATOM_CHARS);
    }

    public static boolean isNumber(String s) {
        return isValid(s, NUMBER_CHARS);
    }

    public static boolean isValid(String s, boolean[] chars) {
        for (int i = 0; i < s.length(); i++) {
            if (!chars[s.charAt(i) & 0xff]) return false;
        }
        return true;
    }

    public static String pp(char c) {
        switch (c) {
        case '\r': return "\\r";
        case '\n': return "\\n";
        case '\t': return "\\t";
        case '\f': return "\\f";
        case '\\': return "\\\\";
        }
        return c >= 0x20 && c <= 0x7e ?
            String.valueOf(c) : "\\0x" + toHexChar(c >> 8) + toHexChar(c & 0xf);
    }

    public static char toHexChar(int digit) {
        switch (digit) {
        case 0: case 1: case 2: case 3: case 4:
        case 5: case 6: case 7: case 8: case 9:
            return (char)('0' + digit);
        case 10: case 11: case 12: case 13: case 14: case 15:
            return (char)('a' + digit - 10);
        default:
            throw new IllegalArgumentException();
        }
    }

    public static String toString(byte[] b) {
        char[] c = new char[b.length];
        for (int i = 0; i < b.length; i++) {
            c[i] = (char) (b[i] & 0xff);
        }
        return new String(c);
    }
    
    public static void write(OutputStream os, String s) throws IOException {
        for (int i = 0; i < s.length(); i++) {
            os.write(s.charAt(i));
        }
    }
}
