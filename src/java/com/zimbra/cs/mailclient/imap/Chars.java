/*
 * ***** BEGIN LICENSE BLOCK *****
 *
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008 Zimbra, Inc.
 *
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 *
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailclient.imap;

import java.io.OutputStream;
import java.io.IOException;

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

    private static final String SPECIALS = "(){%*\"\\";

    static {
        for (int i = 0x21; i < 0x7f; i++) {
            ATOM_CHARS[i] = ASTRING_CHARS[i] = TAG_CHARS[i] = FETCH_CHARS[i] = true;
        }
        for (int i = 1; i < 0xff; i++) {
            TEXT_CHARS[i] = true;
        }
        set(ATOM_CHARS, SPECIALS + "]", false);
        set(ASTRING_CHARS, SPECIALS, false);
        set(TAG_CHARS, SPECIALS + "+", false);
        set(FETCH_CHARS, SPECIALS + "[]", false);
        set(NUMBER_CHARS, "0123456789", true);
        set(TEXT_CHARS, "\000\r\n", false);
    }

    private static void set(boolean[] ba, String chars, boolean b) {
        for (int i = 0; i < chars.length(); i++) {
            ba[chars.charAt(i)] = b;
        }
    }

    public static boolean isNumber(char c) {
        return NUMBER_CHARS[c];
    }

    public static boolean isText(char c) {
        return TEXT_CHARS[c];
    }
    
    public static boolean isNumber(String s) {
        return isValid(s, NUMBER_CHARS);
    }

    public static boolean isTag(String s) {
        return isValid(s, TAG_CHARS);
    }

    public static long getNumber(String s) {
        long n = 0;
        for (int i = 0; i < s.length(); i++) {
            int c = s.charAt(i);
            if (!NUMBER_CHARS[c]) return -1;
            n = n * 10 + (c - '0');
            if (n > 0xffffffffL) return -1;
        }
        return n;
    }
    
    public static boolean isValid(String s, boolean[] chars) {
        for (int i = 0; i < s.length(); i++) {
            if (!chars[s.charAt(i) & 0xff]) return false;
        }
        return true;
    }

}
