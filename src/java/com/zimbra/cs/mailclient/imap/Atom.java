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
