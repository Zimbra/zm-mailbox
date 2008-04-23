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
public final class Mailbox implements ResponseHandler {
    private String name;
    private Flags flags;
    private Flags permanentFlags;
    private long exists = -1;
    private long recent = -1;
    private long uidNext = -1;
    private long uidValidity = -1;
    private long unseen = -1;
    private CAtom access;

    public Mailbox(String name) {
        this.name = name;
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
        name = MailboxName.decode(is.readAString()).toString();
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

    public boolean handleResponse(ImapResponse res) {
        switch (res.getCode()) {
        case EXISTS:
            exists = res.getNumber();
            return true;
        case RECENT:
            recent = res.getNumber();
            return true;
        case FLAGS:
            flags = (Flags) res.getData();
            return true;
        case OK:
            return handleResponseText(res.getResponseText());
        default:
            return false;
        }
    }

    private boolean handleResponseText(ResponseText rt) {
        switch (rt.getCode().getCAtom()) {
        case UNSEEN:
            unseen = (Long) rt.getData();
            return true;
        case UIDNEXT:
            uidNext = (Long) rt.getData();
            return true;
        case UIDVALIDITY:
            uidValidity = (Long) rt.getData();
            return true;
        case PERMANENTFLAGS:
            permanentFlags = (Flags) rt.getData();
            return true;
        case READ_WRITE:
            access = CAtom.READ_WRITE;
            return true;
        case READ_ONLY:
            access = CAtom.READ_ONLY;
            return true;
        default:
            return false;
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

    public void setName(String name) { this.name = name; }

    public String toString() {
        String encoded = name != null ? new MailboxName(name).encode() : null;
        return String.format(
            "{name=%s, exists=%d, recent=%d, unseen=%d, flags=%s, " +
            "permanent_flags=%s, uid_next=%d, uid_validity=%x, access=%s}",
            encoded, exists, recent, unseen, flags, permanentFlags, uidNext,
            uidValidity, access
        );
    }
}
