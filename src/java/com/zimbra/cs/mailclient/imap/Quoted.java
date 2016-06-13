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

import com.zimbra.cs.mailclient.util.Ascii;

import java.io.OutputStream;
import java.io.IOException;

/**
 * IMAP quoted string data type.
 */
public final class Quoted extends ImapData {
    private final String string;

    public Quoted(String s) {
        string = s;
    }

    public Type getType() {
        return Type.QUOTED;
    }

    public int getSize() {
        return string.length();
    }

    public byte[] getBytes() {
        return Ascii.getBytes(string);
    }
    
    public void write(OutputStream os) throws IOException {
        os.write('"');
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            switch (c) {
            case '\\': case '"':
                os.write('\\');
            default:
                os.write(c);
            }
        }
        os.write('"');
    }

    public int hashCode() {
        return string.hashCode();
    }

    public boolean equals(Object obj) {
        return this == obj ||
            obj != null && obj.getClass() == Quoted.class &&
            string.equals(((Quoted) obj).string);
    }

    public String toString() {
        return string;
    }
}
