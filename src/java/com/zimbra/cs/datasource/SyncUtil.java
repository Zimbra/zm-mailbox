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
package com.zimbra.cs.datasource;

import com.zimbra.cs.mailclient.imap.Flags;
import com.zimbra.cs.mailclient.imap.CAtom;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Message;

import javax.mail.internet.MimeMessage;
import javax.mail.MessagingException;
import java.util.Date;

final class SyncUtil {
    private static final Flags EMPTY_FLAGS = new Flags();

    // Excludes flags which have no IMAP equivalent
    private static int ZFLAGS_MASK =
         Flag.BITMASK_REPLIED | Flag.BITMASK_DELETED |
         Flag.BITMASK_DRAFT | Flag.BITMASK_FLAGGED | Flag.BITMASK_UNREAD;
    
    public static int imapToZimbraFlags(Flags flags) {
        int zflags = 0;
        if (flags.isAnswered()) zflags |= Flag.BITMASK_REPLIED;
        if (flags.isDeleted())  zflags |= Flag.BITMASK_DELETED;
        if (flags.isDraft())    zflags |= Flag.BITMASK_DRAFT;
        if (flags.isFlagged())  zflags |= Flag.BITMASK_FLAGGED;
        if (!flags.isSeen())    zflags |= Flag.BITMASK_UNREAD;
        return zflags;
    }

    public static Flags zimbraToImapFlags(int zflags) {
        return getFlagsToAdd(EMPTY_FLAGS, zflags);
    }

    public static int imapFlagsOnly(int zflags) {
        return zflags & ZFLAGS_MASK;
    }
    
    public static Flags getFlagsToAdd(Flags flags, int zflags) {
        Flags toAdd = new Flags();
        if (!flags.isAnswered() && (zflags & Flag.BITMASK_REPLIED) != 0) {
            toAdd.set(CAtom.F_ANSWERED.atom());
        }
        if (!flags.isDeleted() && (zflags & Flag.BITMASK_DELETED) != 0) {
            toAdd.set(CAtom.F_DELETED.atom());
        }
        if (!flags.isDraft() && (zflags & Flag.BITMASK_DRAFT) != 0) {
            toAdd.set(CAtom.F_DRAFT.atom());
        }
        if (!flags.isFlagged() && (zflags & Flag.BITMASK_FLAGGED) != 0) {
            toAdd.set(CAtom.F_FLAGGED.atom());
        }
        if (!flags.isSeen() && (zflags & Flag.BITMASK_UNREAD) == 0) {
            toAdd.set(CAtom.F_SEEN.atom());
        }
        return toAdd;
    }

    public static Flags getFlagsToRemove(Flags flags, int zflags) {
        Flags toRemove = new Flags();
        if (flags.isAnswered() && (zflags & Flag.BITMASK_REPLIED) == 0) {
            toRemove.set(CAtom.F_ANSWERED.atom());
        }
        if (flags.isDeleted() && (zflags & Flag.BITMASK_DELETED) == 0) {
            toRemove.set(CAtom.F_DELETED.atom());
        }
        if (flags.isDraft() && (zflags & Flag.BITMASK_DRAFT) == 0) {
            toRemove.set(CAtom.F_DRAFT.atom());
        }
        if (flags.isFlagged() && (zflags & Flag.BITMASK_FLAGGED) == 0) {
            toRemove.set(CAtom.F_FLAGGED.atom());
        }
        if (flags.isSeen() && (zflags & Flag.BITMASK_UNREAD) != 0) {
            toRemove.set(CAtom.F_SEEN.atom());
        }
        return toRemove;
    }
    
    public static Date getInternalDate(Message msg, MimeMessage mm) {
        Date date = null;
        try {
            date = mm.getReceivedDate();
        } catch (MessagingException e) {
            // Fall through
        }
        return date != null ? date : new Date(msg.getDate());
    }
}
