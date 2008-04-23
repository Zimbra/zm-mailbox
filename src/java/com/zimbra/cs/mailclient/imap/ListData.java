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
 * IMAP mailbox LIST response:
 *
 * mailbox-list    = "(" [mbx-list-flags] ")" SP
 *                 (DQUOTE QUOTED-CHAR DQUOTE / nil) SP mailbox
 *
 * mbx-list-flags  = *(mbx-list-oflag SP) mbx-list-sflag
 *                 *(SP mbx-list-oflag) /
 *                 mbx-list-oflag *(SP mbx-list-oflag)
 *
 * mbx-list-oflag  = "\Noinferiors" / flag-extension
 *                   ; Other flags; multiple possible per LIST response
 *
 * mbx-list-sflag  = "\Noselect" / "\Marked" / "\Unmarked"
 *                   ; Selectability flags; only one per LIST response
 */
public final class ListData {
    private Flags flags;
    private Character delimiter;
    private String mailbox;

    public static ListData read(ImapInputStream is) throws IOException {
        ListData mb = new ListData();
        mb.readMailboxList(is);
        return mb;
    }

    private void readMailboxList(ImapInputStream is) throws IOException {
        flags = readFlags(is);
        is.skipChar(' ');
        String delim = is.readNString();
        if (delim != null) {
            if (delim.length() != 1) {
                throw new ParseException(
                    "Invalid delimiter specification in LIST data: " + delim);
            }
            delimiter = delim.charAt(0);
        }
        is.skipChar(' ');
        mailbox = MailboxName.decode(is.readAString()).toString();
    }

    private static Flags readFlags(ImapInputStream is) throws IOException {
        Flags flags = Flags.read(is);
        int count = 0;
        if (flags.isNoselect()) count++;
        if (flags.isMarked()) count++;
        if (flags.isUnmarked()) count++;
        if (count > 0) {
            throw new ParseException(
                "Invalid LIST flags - only one of \\Noselect, \\Marked, or" +
                 " \\Unmarked expected");
        }
        return flags;
    }

    public Flags getFlags() { return flags; }
    public String getMailbox() { return mailbox; }
    public Character getDelimiter() { return delimiter; }
}
