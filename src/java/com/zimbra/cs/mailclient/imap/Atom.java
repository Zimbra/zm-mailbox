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
 * IMAP atom data type.
 */
public final class Atom extends ImapData {
    private final String name;

    public static final Atom NIL = new Atom("nil");
    
    public Atom(String name) {
        this.name = name;
    }

    public Type getType() {
        return Type.ATOM;
    }
    
    public String getName() {
        return name;
    }

    public CAtom getCAtom() {
        return CAtom.get(this);
    }

    public boolean isNumber() {
        return Chars.isNumber(name);
    }
    
    public long getNumber() {
        return Chars.getNumber(name);
    }

    public int getSize() {
        return name.length();
    }

    public byte[] getBytes() {
        return Ascii.getBytes(name);
    }
    
    public void write(OutputStream os) throws IOException {
        Ascii.write(os, name);
    }
    
    public int hashCode() {
        return name.toUpperCase().hashCode();
    }
    
    public boolean equals(Object obj) {
        return this == obj || obj != null && obj.getClass() == Atom.class &&
                              name.equalsIgnoreCase(((Atom) obj).name);
    }

    public String toString() {
        return name;
    }
}
