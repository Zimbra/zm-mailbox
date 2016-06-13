/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.mailclient.imap;

/**
 * IMAP character class support:
 *
 * ATOM-CHAR       = <any CHAR except atom-specials>
 * atom-specials   = "(" / ")" / "{" / SP / CTL / list-wildcards /
 *                   quoted-specials / resp-specials
 * list-wildcards  = "%" / "*"
 * quoted-specials = DQUOTE / "\"
 * resp-specials   = "]"
 * ASTRING-CHAR    = ATOM-CHAR / resp-specials
 * tag             = 1*<any ASTRING-CHAR except "+">
 */
public final class Chars {
    public static final boolean[] ATOM_CHARS = new boolean[256];
    public static final boolean[] ASTRING_CHARS = new boolean[256];
    public static final boolean[] TAG_CHARS = new boolean[256];
    public static final boolean[] FETCH_CHARS = new boolean[256];
    public static final boolean[] NUMBER_CHARS = new boolean[256];
    public static final boolean[] TEXT_CHARS = new boolean[256];
    public static final boolean[] CAPABILITY_CHARS = new boolean[256];

    private static final String QUOTED_SPECIALS = "\"\\";
    private static final String ASTRING_SPECIALS = "(){%*" + QUOTED_SPECIALS;
    private static final String ATOM_SPECIALS = ASTRING_SPECIALS + "]";

    static {
        for (int i = 0x21; i < 0x7f; i++) {
            ATOM_CHARS[i] = ASTRING_CHARS[i] = TAG_CHARS[i] = FETCH_CHARS[i] =
            CAPABILITY_CHARS[i] = true;
        }
        for (int i = 0x01; i < 0x80; i++) {
            TEXT_CHARS[i] = true;
        }
        set(ATOM_CHARS, ATOM_SPECIALS, false);
        set(ASTRING_CHARS, ASTRING_SPECIALS, false);
        set(TAG_CHARS, ASTRING_SPECIALS + "+", false);
        set(FETCH_CHARS, ATOM_SPECIALS + "[]", false);
        set(NUMBER_CHARS, "0123456789", true);
        set(TEXT_CHARS, "\000\r\n", false);
        set(CAPABILITY_CHARS, QUOTED_SPECIALS + "]", false);
    }

    private static void set(boolean[] ba, String chars, boolean b) {
        for (int i = 0; i < chars.length(); i++) {
            ba[chars.charAt(i)] = b;
        }
    }

    public static boolean isNumber(char c) {
        return c < 256 && NUMBER_CHARS[c];
    }

    public static boolean isTextChar(char c) {
        return c < 256 && TEXT_CHARS[c];
    }

    public static boolean isAtomChar(char c) {
        return c < 256 && ATOM_CHARS[c];    
    }
    
    public static boolean isAStringChar(char c) {
        return c < 256 && ASTRING_CHARS[c];
    }

    public static boolean isCapabilityChar(char c) {
        return c < 256 && CAPABILITY_CHARS[c];
    }
    
    public static boolean isQuotedSpecialChar(char c) {
        return c == '\\' || c == '\"';
    }

    public static boolean isNumber(String s) {
        return isValid(s, NUMBER_CHARS);
    }

    public static boolean isTag(String s) {
        return isValid(s, TAG_CHARS);
    }

    public static boolean isAtom(String s) {
        return isValid(s, ATOM_CHARS);
    }

    public static boolean isText(String s) {
        return isValid(s, TEXT_CHARS);
    }
    
    public static long getNumber(String s) {
        long n = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!isNumber(c)) return -1;
            n = n * 10 + (c - '0');
            if (n < 0) return -1;
        }
        return n;
    }
    
    public static boolean isValid(String s, boolean[] chars) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c > 255 || !chars[c]) return false;
        }
        return true;
    }
}
