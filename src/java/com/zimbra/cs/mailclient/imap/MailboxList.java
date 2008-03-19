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
import java.util.Set;
import java.util.HashSet;

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
public final class MailboxList {
    private CAtom sflag;
    private boolean noInferiors;
    private Set<Atom> otherFlags;
    private Character delimiter;
    private String mailbox;

    public static MailboxList read(ImapInputStream is) throws IOException {
        MailboxList mb = new MailboxList();
        mb.readMailboxList(is);
        return mb;
    }

    private void readMailboxList(ImapInputStream is) throws IOException {
        readFlags(is);
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
        mailbox = is.readAString();
    }

    private void readFlags(ImapInputStream is) throws IOException {
        is.skipChar('(');
        do {
            Atom atom = is.readAtom();
            CAtom catom = atom.getCAtom();
            switch (catom) {
            case F_NOINFERIORS:
                noInferiors = true;
                break;
            case F_NOSELECT: case F_MARKED: case F_UNMARKED:
                if (sflag != null) {
                    throw new ParseException(
                        "Invalid LIST flags - only one of \\Noselect, " +
                        " \\Nomarked, or \\Unmarked expected");
                }
                sflag = catom;
                break;
            default:
                if (otherFlags == null) {
                    otherFlags = new HashSet<Atom>();
                }
                otherFlags.add(atom);
            }
        } while (is.match(' '));
        is.skipChar(')');
    }

    public boolean isNoinferiors() { return noInferiors; }
    public boolean isNoselect() { return sflag == CAtom.F_NOSELECT; }
    public boolean isMarked() { return sflag == CAtom.F_MARKED; }
    public boolean isUnmarked() { return sflag == CAtom.F_UNMARKED; }
    public boolean isOther(String flag) {
        return otherFlags != null && otherFlags.contains(new Atom(flag));
    }
    public String getMailbox() { return mailbox; }
    public Character getDelimiter() { return delimiter; }
}
