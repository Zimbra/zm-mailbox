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
    public static final String UNSELECT = "UNSELECT";

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
