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

import com.zimbra.cs.mailclient.ParseException;

import java.io.IOException;

/**
 * IMAP Mailbox information.
 */
public final class MailboxInfo implements ResponseHandler {
    private String name;
    private Flags flags;
    private Flags permanentFlags;
    private long exists = -1;
    private long recent = -1;
    private long uidNext = -1;
    private long uidValidity = -1;
    private long unseen = -1;
    private CAtom access;

    public MailboxInfo(String name) {
        this.name = name;
    }

    public MailboxInfo(MailboxInfo mb) {
        name = mb.name;
        flags = mb.flags;
        permanentFlags = mb.permanentFlags;
        exists = mb.exists;
        recent = mb.recent;
        uidNext = mb.uidNext;
        uidValidity = mb.uidValidity;
        unseen = mb.unseen;
        access = mb.access;
    }
    
    private MailboxInfo() {}
    
    // IMAP mailbox STATUS response:
    //
    // status-response = "STATUS" SP mailbox SP "(" [status-att-list] ")"
    //
    // status-att-list =  status-att SP number *(SP status-att SP number)
    //
    // status-att      = "MESSAGES" / "RECENT" / "UIDNEXT" / "UIDVALIDITY" /
    //                   "UNSEEN"
    //
    public static MailboxInfo readStatus(ImapInputStream is) throws IOException {
        MailboxInfo mbox = new MailboxInfo();
        mbox.parseStatus(is);
        return mbox;
    }

    private void parseStatus(ImapInputStream is) throws IOException {
        name = MailboxName.decode(is.readAString()).toString();
        is.skipChar(' ');
        is.skipSpaces();
        is.skipChar('(');
        while (!is.match(')')) {
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
            is.skipSpaces();
        }
    }

    public void handleResponse(ImapResponse res) {
        switch (res.getCCode()) {
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
            handleResponseText(res.getResponseText());
            break;
        }
    }

    private void handleResponseText(ResponseText rt) {
        long n;
        switch (rt.getCCode()) {
        case UNSEEN:
            unseen = (Long) rt.getData();
            break;
        case UIDNEXT:
            n = (Long) rt.getData();
            if (n > 0) uidNext = n;     // bug 38521
            break;
        case UIDVALIDITY:
            n = (Long) rt.getData();
            if (n > 0) uidValidity = n; // bug 38521
            break;
        case PERMANENTFLAGS:
            permanentFlags = (Flags) rt.getData();
            break;
        case READ_WRITE:
            access = CAtom.READ_WRITE;
            break;
        case READ_ONLY:
            access = CAtom.READ_ONLY;
            break;
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
    
    public void setUidValidity(long uidValidity) {
        this.uidValidity = uidValidity;
    }

    public String toString() {
        String encoded = name != null ? new MailboxName(name).encode() : null;
        return String.format(
            "{name=%s, exists=%d, recent=%d, unseen=%d, flags=%s, " +
            "permanent_flags=%s, uid_next=%d, uid_validity=%d, access=%s}",
            encoded, exists, recent, unseen, flags, permanentFlags, uidNext,
            uidValidity, access
        );
    }
}
