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

import com.zimbra.cs.mailclient.MailException;

import java.util.List;
import java.util.ArrayList;
import java.io.IOException;

/**
 * IMAP server capabilities
 */
public class Capabilities {
    private final List<Atom> capabilities = new ArrayList<Atom>();
    private int mask;

    private static final int MASK_IMAP4 = 0x1;
    private static final int MASK_LITERAL_PLUS = 0x2;

    public static final String IMAP4REV1 = "IMAP4rev1";
    public static final String STARTTLS = "STARTTLS";
    public static final String LOGINDISABLED = "LOGINDISABLED";
    public static final String IMAP4 = "IMAP4";
    public static final String LITERAL_PLUS = "LITERAL+";

    private static final String[] REQUIRED_CAPABILITIES =
        { IMAP4REV1, STARTTLS, LOGINDISABLED };

    public static Capabilities read(ImapInputStream is) throws IOException {
        Capabilities caps = new Capabilities();
        caps.readCapabilities(is);
        return caps;
    }

    private Capabilities() {}

    private void readCapabilities(ImapInputStream is) throws IOException {
        do {
            addCapability(is.readAtom().getName());
        } while (is.match(' '));
        if (hasCapability(IMAP4)) {
            mask |= MASK_IMAP4;
        } else if (hasCapability(LITERAL_PLUS)) {
            mask |= MASK_LITERAL_PLUS;
        }
    }
    
    private void addCapability(String cap) {
        capabilities.add(new Atom(cap));
    }

    public boolean hasCapability(String cap) {
        return capabilities.contains(new Atom(cap));
    }

    public boolean hasAuthMethod(String method) {
        return hasCapability("AUTH=" + method);
    }

    public boolean hasImap4() {
        return (mask & MASK_IMAP4) != 0;
    }
    
    public boolean hasLiteralPlus() {
        return (mask & MASK_LITERAL_PLUS) != 0;
    }

    public String[] getCapabilities() {
        String[] caps = new String[capabilities.size()];
        for (int i = 0; i < caps.length; i++) {
            caps[i] = capabilities.get(i).getName();
        }
        return caps;
    }

    public void validate() throws MailException {
        for (String cap : REQUIRED_CAPABILITIES) {
            if (!hasCapability(cap)) {
                throw new MailException("Capability '" + cap + "' must be supported");
            }
        }
    }
        
    public String toString() {
        StringBuilder sb = new StringBuilder("CAPABILITIES[");
        for (Atom cap : capabilities) {
            sb.append(' ').append(cap);
        }
        return sb.append(']').toString();
    }
}
