/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.mailclient.imap;

import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.io.IOException;

/**
 * IMAP server capabilities
 */
public class ImapCapabilities {
    private final Set<Atom> capabilities = new HashSet<Atom>();

    public static final String IMAP4REV1 = "IMAP4rev1";
    public static final String STARTTLS = "STARTTLS";
    public static final String LOGINDISABLED = "LOGINDISABLED";
    public static final String IMAP4 = "IMAP4";
    public static final String LITERAL_PLUS = "LITERAL+";
    public static final String SASL_IR = "SASL-IR";
    public static final String UIDPLUS = "UIDPLUS";
    public static final String ID = "ID";
    public static final String IDLE = "IDLE";
    public static final String AUTH_PLAIN = "AUTH=PLAIN";
    public static final String AUTH_GSSAPI = "AUTH=GSSAPI";

    public static ImapCapabilities read(ImapInputStream is) throws IOException {
        ImapCapabilities caps = new ImapCapabilities();
        caps.readCapabilities(is);
        return caps;
    }

    public ImapCapabilities() {}

    private void readCapabilities(ImapInputStream is) throws IOException {
        is.skipSpaces();
        while (Chars.isCapabilityChar(is.peekChar())) {
            addCapability(is.readChars(Chars.CAPABILITY_CHARS));
            is.skipSpaces();
        }
    }

    private void addCapability(String cap) {
        capabilities.add(new Atom(cap));
    }

    public boolean hasCapability(String cap) {
        return capabilities.contains(new Atom(cap));
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        Iterator<Atom> it = capabilities.iterator();
        if (it.hasNext()) {
            sb.append(it.next());
            while (it.hasNext()) {
                sb.append(' ').append(it.next());
            }
        }
        return sb.append('}').toString();
    }
}
