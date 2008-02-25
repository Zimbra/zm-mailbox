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

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Base class for basic IMAP data types.
 */
public abstract class ImapData {
    public static enum Type { ATOM, QUOTED, LITERAL }

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
        return new ByteArrayInputStream(getBytes());
    }

    public void dispose() {}
}
