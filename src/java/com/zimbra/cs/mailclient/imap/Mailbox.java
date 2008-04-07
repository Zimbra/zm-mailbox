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

import com.zimbra.cs.mailclient.ParseException;

import java.io.IOException;

/**
 * IMAP Mailbox information.
 */
public final class Mailbox {
    private String name;
    private Flags flags;
    private Flags permanentFlags;
    private long exists = -1;
    private long recent = -1;
    private long uidNext = -1;
    private long uidValidity = -1;
    private long unseen = -1;
    private CAtom access;

    public static final String INBOX = "INBOX";

    public Mailbox(String name) {
        this.name = name.equalsIgnoreCase(INBOX) ? INBOX : name;
    }

    private Mailbox() {}
    
    // IMAP mailbox STATUS response:
    //
    // status-response = "STATUS" SP mailbox SP "(" [status-att-list] ")"
    //
    // status-att-list =  status-att SP number *(SP status-att SP number)
    //
    // status-att      = "MESSAGES" / "RECENT" / "UIDNEXT" / "UIDVALIDITY" /
    //                   "UNSEEN"
    //
    public static Mailbox readStatus(ImapInputStream is) throws IOException {
        Mailbox mbox = new Mailbox();
        mbox.parseStatus(is);
        return mbox;
    }

    private void parseStatus(ImapInputStream is) throws IOException {
        name = is.readAString();
        is.skipChar(' ');
        is.skipChar('(');
        do {
            Atom attr = is.readAtom();
            is.skipChar(' ');
            long n = is.readNumber();
            switch (attr.getCAtom()) {
            case MESSAGES:
                exists = n;
                break;
            case RECENT:
                recent = n;
                break;
            case UIDNEXT:
                uidNext = n;
                break;
            case UIDVALIDITY:
                uidValidity = n;
                break;
            case UNSEEN:
                unseen = n;
                break;
            default:
                throw new ParseException(
                    "Invalid STATUS response attribute: " + attr);
            }
        } while (is.match(' '));
        is.skipChar(')');
    }

    /**
     * Updates currently selected mailbox info according to the specified
     * ImapResponse.
     *
     * @param res the ImapResponse containing possible updates
     */
    public void processResponse(ImapResponse res) {
        switch (res.getCode()) {
        case EXISTS:
            exists = res.getNumber();
            break;
        case RECENT:
            recent = res.getNumber();
            break;
        case FLAGS:
            flags = (Flags) res.getData();
            break;
        case OK:
            processResponseText(res.getResponseText());
        }
    }

    private void processResponseText(ResponseText rt) {
        switch (rt.getCode().getCAtom()) {
        case UNSEEN:
            unseen = (Long) rt.getData();
            break;
        case UIDNEXT:
            uidNext = (Long) rt.getData();
            break;
        case UIDVALIDITY:
            uidValidity = (Long) rt.getData();
            break;
        case PERMANENTFLAGS:
            permanentFlags = (Flags) rt.getData();
            break;
        case READ_WRITE:
            access = CAtom.READ_WRITE;
            break;
        case READ_ONLY:
            access = CAtom.READ_ONLY;
        }
    }

    public String getName() { return name; }
    public Flags getFlags() { return flags; }
    public Flags getPermanentFlags() { return permanentFlags; }
    public long getExists() { return exists; }
    public long getRecent() { return recent; }
    public long getUidNext() { return uidNext; }
    public long getUidValidity() { return uidValidity; }
    public long getUnseen() { return unseen; }
    public boolean isReadOnly() { return access == CAtom.READ_ONLY; }
    public boolean isReadWrite() { return access == CAtom.READ_WRITE; }
}
