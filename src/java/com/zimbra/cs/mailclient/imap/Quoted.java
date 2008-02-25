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
 * IMAP quoted string data type.
 */
public final class Quoted extends ImapData {
    private final String mString;

    public Quoted(String s) {
        mString = s;
    }

    public Type getType() {
        return Type.QUOTED;
    }

    public String getString() {
        return mString;
    }

    public int getSize() {
        return mString.length();
    }

    public byte[] getBytes() {
        return Ascii.getBytes(mString);
    }
    
    public void write(OutputStream os) throws IOException {
        os.write('"');
        for (int i = 0; i < mString.length(); i++) {
            char c = mString.charAt(i);
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
        return mString.hashCode();
    }

    public boolean equals(Object obj) {
        return this == obj ||
            obj != null && obj.getClass() == Quoted.class &&
            mString.equals(((Quoted) obj).mString);
    }
    
    public String toString() {
        return mString;
    }
}
