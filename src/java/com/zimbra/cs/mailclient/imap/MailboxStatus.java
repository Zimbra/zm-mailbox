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
 * IMAP mailbox STATUS response:
 *
 * status-response = "STATUS" SP mailbox SP "(" [status-att-list] ")"
 * 
 * status-att-list =  status-att SP number *(SP status-att SP number)
 * 
 * status-att      = "MESSAGES" / "RECENT" / "UIDNEXT" / "UIDVALIDITY" /
 *                   "UNSEEN"
 *
 */
public final class MailboxStatus {
    private String mMailbox;
    private long mMessages = -1;
    private long mRecent = -1;
    private long mUidNext = -1;
    private long mUidValidity = -1;
    private long mUnseen = -1;

    public static MailboxStatus read(ImapInputStream is) throws IOException {
        MailboxStatus mbs = new MailboxStatus();
        mbs.readStatus(is);
        return mbs;
    }

    private void readStatus(ImapInputStream is) throws IOException {
        mMailbox = is.readAString();
        is.skipChar(' ');
        is.skipChar('(');
        do {
            Atom attr = is.readAtom();
            is.skipChar(' ');
            long num = is.readNumber();
            switch (attr.getCAtom()) {
            case MESSAGES:
                mMessages = num;
                break;
            case RECENT:
                mRecent = num;
                break;
            case UIDNEXT:
                mUidNext = num;
                break;
            case UIDVALIDITY:
                mUidValidity = num;
                break;
            case UNSEEN:
                mUnseen = num;
                break;
            default:
                throw new ParseException(
                    "Invalid STATUS response attribute: " + attr);
            }
        } while (is.match(' '));
        is.skipChar(')');
    }

    public String getMailbox() { return mMailbox; }
    public long getMessages() { return mMessages; }
    public long getRecent() { return mRecent; }
    public long getUidNext() { return mUidNext; }
    public long getUidValidity() { return mUidValidity; }
    public long getUnseen() { return mUnseen; }
}
