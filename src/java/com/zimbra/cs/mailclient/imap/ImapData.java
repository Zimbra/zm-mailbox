/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2012, 2013, 2014, 2016 Synacor, Inc.
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

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.List;

import javax.mail.util.SharedByteArrayInputStream;

import com.zimbra.cs.mailclient.util.Ascii;

/**
 * Base class for basic IMAP data types.
 */
public abstract class ImapData {
    public static enum Type { ATOM, QUOTED, LITERAL }

    public static ImapData asAString(String s) {
        if (s.length() > 64) {
            return new Literal(Ascii.getBytes(s));
        }
        switch (getType(s)) {
            case ATOM:
                return new Atom(s);
            case QUOTED:
                return new Quoted(s);
            case LITERAL:
                return new Literal(encodeUtf8(s));
        }
        return null;
    }

    public static ImapData asNString(String s) {
        return s != null ? asString(s) : Atom.NIL;
    }

    public static ImapData asString(String s) {
        return s.length() <= 64 && Chars.isText(s) ?
            new Quoted(s) : new Literal(encodeUtf8(s));
    }

    private static Type getType(String s) {
        if (s.length() == 0) {
            return Type.QUOTED; // Empty string
        }
        Type type = Type.ATOM;  // Assume it's an atom for now
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!Chars.isTextChar(c)) {
                return Type.LITERAL;
            }
            if (!Chars.isAStringChar(c)) {
                type = Type.QUOTED; // Must be QUOTED or LITERAL
            }
        }
        return type;
    }

    private static byte[] encodeUtf8(String s) {
        try {
            return s.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new InternalError("UTF-8 charset not found");
        }

    }

    public static String asSequenceSet(List<? extends Number> ids) {
        StringBuilder sb = new StringBuilder();
        if (ids.isEmpty()) {
            return null;
        }
        Iterator<? extends Number> it = ids.iterator();
        sb.append(it.next().longValue());
        while (it.hasNext()) {
            sb.append(',').append(it.next().longValue());
        }
        return sb.toString();
    }

    public abstract Type getType();

    public boolean isAtom() {
        return getType() == Type.ATOM;
    }

    public boolean isQuoted() {
        return getType() == Type.QUOTED;
    }

    public boolean isLiteral() {
        return getType() == Type.LITERAL;
    }

    public boolean isString() {
        return isQuoted() || isLiteral();
    }

    public boolean isAString() {
        return isAtom() || isString();
    }

    public boolean isNString() {
        return isNil() || isString();
    }

    public boolean isNil() {
        return equals(Atom.NIL);
    }

    public abstract int getSize();

    public abstract byte[] getBytes() throws IOException;

    public InputStream getInputStream() throws IOException {
        return new SharedByteArrayInputStream(getBytes());
    }

    public void dispose() {}
}
