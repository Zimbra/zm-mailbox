/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

import com.google.common.base.MoreObjects;
import com.zimbra.common.util.ZimbraLog;

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
        //bug 52019 Exchange IMAP doesn't quote folders w/ ()
        if (is.peek() == '(' || is.peek() == ')') {
            //read until we get to space. if there happens to be spaces in the name it should already be quoted..
            name += is.readText(" ");
        }
        // bug 67924 Read until ( in case buggy IMAP server doesn't quote mailbox name
        String remaining = is.readText("(");
        if (remaining.trim().length() > 0) {
            name += remaining.substring(0, remaining.length() - 1);
        }
        is.skipChar('(');
        while (!is.match(')')) {
            Atom attr = is.readAtom();
            is.skipSpaces();
            switch (attr.getCAtom()) {
            case MESSAGES:
                exists = is.readNumber();
                break;
            case RECENT:
                recent = is.readNumber();
                break;
            case UIDNEXT:
                uidNext = is.readNumber();
                break;
            case UIDVALIDITY:
                uidValidity = is.readNumber();
                break;
            case UNSEEN:
                unseen = is.readNumber();
                break;
            default:
                ZimbraLog.imap_client.debug("Ignoring invalid STATUS response attribute: %s", attr);
            }
            is.skipSpaces();
        }
    }

    @Override
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

    public void setExists(int exists) {
        this.exists = exists;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("name", name != null ? new MailboxName(name).encode() : null)
            .add("exists", exists)
            .add("recent", recent)
            .add("unseen", unseen)
            .add("flags", flags)
            .add("permanent_flags", permanentFlags)
            .add("uid_next", uidNext)
            .add("uid_validity", uidValidity)
            .add("access", access)
            .toString();
    }
}
